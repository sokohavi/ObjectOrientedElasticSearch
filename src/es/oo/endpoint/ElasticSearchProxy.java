package es.oo.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import es.oo.exceptions.IndexingException;
import es.oo.exceptions.InternalServiceException;
import es.oo.exceptions.InvalidDataException;
import es.oo.exceptions.SameItemIndexCollisionException;
import es.oo.model.attributes.AttributesMap;
import es.oo.model.attributes.AttributesMapsList;
import es.oo.model.attributes.searchable.SearchableAttributesMapsList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import javax.ws.rs.core.Response.Status;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * An proxy for elastic search.
 */
public class ElasticSearchProxy implements Runnable {
    private static final Integer MIN_MILLIS_BETWEEN_CALLS = 500;
    private static final String INDEX_NAME = "entities";
    private static final String TYPE_NAME = "attributes";
    private static final String SEARCH_PATH = "_search";

    private CountDownLatch indexingIsDone = new CountDownLatch(1);
    private final Lock lock = new ReentrantLock();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, String> idToItemMap = new HashMap<>();
    private final Thread worker;

    private boolean stop = false;
    private Map<String, BulkItemResponse> bulkResult;


    private static final String HITS = "hits";
    private static final String ID = "_id";
    private static final String SOURCE = "_source";

    /**
     * REST client to interact with elastic search.
     */
    private final RestHighLevelClientAdapter elasticSearchClient;

    /**
     * Low level client to perform direct REST requests.
     */
    private final RestClient elasticLowLevelClient;

    private static final Log log = LogFactory.getLog(ElasticSearchProxy.class);

    public ElasticSearchProxy(final RestHighLevelClient elasticSearchClient) throws IOException {
        this(new RestHighLevelClientAdapterImpl(elasticSearchClient));
    }

    public ElasticSearchProxy(final RestHighLevelClientAdapter elasticSearchClient) {
        this.elasticSearchClient = elasticSearchClient;
        this.elasticLowLevelClient = elasticSearchClient.getLowLevelClient();

        this.elasticSearchClient.createIndex(INDEX_NAME, TYPE_NAME);

        this.worker = new Thread(this);
        this.worker.start();
    }

    @Override
    public void finalize() {
        stop = true;
    }

    public void run() {
        while (!stop) {
            try {
                Thread.sleep(getMinMillisBetweenCalls());
                if (this.idToItemMap.size() > 0) {
                    indexDataToEs();
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    /**
     * Search for objects similar to the given object, and translate the results to the given class type.
     */
    public <T> List<T> searchForSingleObject(final T searchObject, Class<T> tClass) {
        return searchForSingleMap(searchObject).stream()
                .map((attributesMap ->attributesMap.toObject(tClass)))
                .collect(Collectors.toList());
    }

    /**
     * Search for objects similar to the given object, and translate the results to the given class type.
     */
    public <T> List<T> searchForMultipleObjects(final List<T> searchList, Class<T> tClass) {
        final List<Object> searchObjectsList = Lists.newArrayList(searchList);
        return searchForMultipleMaps(searchObjectsList).stream()
                .map((attributesMap ->attributesMap.toObject(tClass)))
                .collect(Collectors.toList());
    }


    /**
     * Search for objects similar to the given object, return the result as a list of {@link AttributesMap}.
     */
    public List<AttributesMap> searchForSingleMap(final Object searchObject) {
        final List<Object> searchObjectsList = Lists.newArrayList(searchObject);
        return searchForMultipleMaps(searchObjectsList);
    }

    /**
     * Search for objects similar to at least one of the given objects, return the result as a list of {@link AttributesMap}.
     */
    public List<AttributesMap> searchForMultipleMaps(final List<Object> searchObjectsList) {
        try {
            final List<List<Object>> searchObjectsLists =
                    searchObjectsList.stream().map(entity -> Lists.newArrayList(entity))
                            .collect(Collectors.toList());
            final AttributesMapsList attributesMapsList =
                    AttributesMapsList.toAttributesMapsList(searchObjectsLists);
            final SearchableAttributesMapsList searchableAttributesMap =
                    new SearchableAttributesMapsList(attributesMapsList);
            final String queryDsl = searchableAttributesMap.toDslQueryString();

            final Response response = search(queryDsl);

            if (response.getStatusLine().getStatusCode() != Status.OK.getStatusCode()) {
                throw new InternalServiceException("internal failure");
            }

            if (Objects.isNull(response.getEntity())) {
                return null;
            }

            final String responseEntity = EntityUtils.toString(response.getEntity());
            return buildIdToAttributesMap(responseEntity);
        } catch (IOException e) {
            throw new InternalServiceException(e);
        }
    }

    public Response search(final String queryDsl) {
        try {
            final String queryPath = new StringBuilder().append("/").append(INDEX_NAME).append("/").append(SEARCH_PATH)
                    .toString();
            final HttpEntity entity = new NStringEntity(queryDsl, ContentType.APPLICATION_JSON);
            final Map<String, String> params = Maps.newHashMap();
            final Header[] headers = new Header[0];
            final String method = HttpMethod.GET;
            final Response response =
                    this.elasticLowLevelClient.performRequest(method, queryPath, params, entity, headers);

            return response;
        } catch (IOException e) {
            throw new InternalServiceException(e);
        }
    }

    public void writeItem(final String id, final Object item) throws IndexingException,
            IOException {

        addItemToMap(id, AttributesMap.toAttributesMap(item));

        try {
            this.indexingIsDone.await();
        } catch (final Exception e) {
            throw new IndexingException(e);
        }

        if (bulkResult == null) {
            throw new IndexingException(id);
        }

        final BulkItemResponse bulkResultItem = getItemFromBulkResponse(id);

        if (bulkResultItem.isFailed()) {
            throw new IndexingException(id, INDEX_NAME, bulkResultItem.getFailure());
        }
    }

    @VisibleForTesting
    protected BulkItemResponse getItemFromBulkResponse(final String id) {
        return this.bulkResult.get(id);
    }

    @VisibleForTesting
    protected String serializedItem(final Object item) throws JsonProcessingException {
        return objectMapper.writeValueAsString(item);
    }

    @VisibleForTesting
    protected void indexDataToEs() {
        try {
            lock.lock();

            if (this.idToItemMap.size() > 0) {
                final BulkRequest bulkUpdateRequest = new BulkRequest();

                this.idToItemMap.entrySet().stream().forEach(entry -> {
                    // More details about upsert at
                    // https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/
                    // java-rest-high-document-update.html
                    final UpdateRequest updateRequest = new UpdateRequest(INDEX_NAME, TYPE_NAME, entry.getKey());
                    updateRequest.doc(entry.getValue(), XContentType.JSON);
                    updateRequest.docAsUpsert(true);

                    bulkUpdateRequest.add(updateRequest);
                });

                final BulkResponse bulkResponse = this.elasticSearchClient.bulk(bulkUpdateRequest);

                final Map<String, BulkItemResponse> bulkIndexResponse = new HashMap<>();
                for (BulkItemResponse bulkItemResponse : bulkResponse.getItems()) {
                    bulkIndexResponse.put(bulkItemResponse.getId(), bulkItemResponse);
                }

                this.bulkResult = bulkIndexResponse;
            }
        } catch (final Exception e) {
            log.error(e);
        } finally {
            try {
                idToItemMap.clear();
            } catch (final Exception e) {
                log.error(e);
            }

            lock.unlock();

            if (indexingIsDone.getCount() > 0) {
                indexingIsDone.countDown();
            }

            indexingIsDone = new CountDownLatch(1);
        }
    }

    /**
     * Visable for testing.
     */
    protected int getMinMillisBetweenCalls() {
        return MIN_MILLIS_BETWEEN_CALLS;
    }

    private List<AttributesMap> buildIdToAttributesMap(final String responseEntity) throws IOException {
        if (responseEntity == null || responseEntity == "") {
            return Lists.newArrayList();
        }

        final JsonNode rootNode = objectMapper.readTree(responseEntity);
        final JsonNode jsonNodeHits =  rootNode.path(HITS);
        if (jsonNodeHits.isMissingNode()) {
            log.error("jsonNodeHits is required to process response");
            throw new InvalidDataException("hits node is missing from search results");
        }
        final JsonNode jsonNodeHitsArray = jsonNodeHits.path(HITS);
        if (jsonNodeHitsArray.isMissingNode()) {
            log.error("jsonNodeHitsArray is required to process response");
            throw new InvalidDataException("hits array node is missing from search results");
        }
        final Iterator<JsonNode> hitsIterator = jsonNodeHitsArray.elements();
        final ArrayList<AttributesMap> attributesMapsList = Lists.newArrayList();

        while (hitsIterator.hasNext()) {
            final JsonNode hitsElement = hitsIterator.next();
            final JsonNode sourceNode = hitsElement.get(SOURCE);
            final AttributesMap attributesMap =
                    this.objectMapper.readValue(sourceNode.toString(), AttributesMap.class);
            attributesMapsList.add(attributesMap);
        }
        return attributesMapsList;
    }

    private void addItemToMap(final String id, final Object item) throws JsonProcessingException {
        final String serializedItem = serializedItem(item);

        try {
            this.lock.lock();
            if (this.idToItemMap.containsKey(id)) {
                throw new SameItemIndexCollisionException(id);
            }

            this.idToItemMap.put(id, serializedItem);
        } catch (Exception e) {
            log.error(e);
        } finally {
            this.lock.unlock();
        }
    }
}
