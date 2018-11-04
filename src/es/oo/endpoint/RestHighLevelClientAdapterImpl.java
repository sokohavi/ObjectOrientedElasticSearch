package es.oo.endpoint;

import es.oo.exceptions.InternalServiceException;
import es.oo.model.attributes.AttributesMap;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;

/**
 * An adapter impl for the {@link RestHighLevelClientAdapter}.
 */
public class RestHighLevelClientAdapterImpl implements RestHighLevelClientAdapter {

    XContentBuilder mappingBuilder = XContentFactory.jsonBuilder()
            .startObject().startObject("attributes")
            .startObject("properties").startObject(AttributesMap.NAMESPACE_MAP).field("type", "nested")
            .endObject().endObject().endObject().endObject();

    private final static String MAPPINGS =
            "{\n" +
            "  \"mappings\": {\n" +
            "    \"attributes\": {\n" +
            "      \"properties\": {\n" +
            "        \""+ AttributesMap.NAMESPACE_MAP + "\": {\n" +
            "          \"type\": \"nested\" \n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private RestHighLevelClient restHighLevelClient;

    public RestHighLevelClientAdapterImpl(RestHighLevelClient restHighLevelClient) throws IOException {
        this.restHighLevelClient = restHighLevelClient;
    }

    @Override
    public RestClient getLowLevelClient() {
        return this.restHighLevelClient.getLowLevelClient();
    }

    @Override
    public BulkResponse bulk(BulkRequest bulkRequest) throws IOException {
        return this.restHighLevelClient.bulk(bulkRequest);
    }

    @Override
    public void deleteIndex(final String indexName) {
        try {
            final IndicesClient indices = this.restHighLevelClient.indices();

            final GetIndexRequest getIndexRequest = new GetIndexRequest();
            getIndexRequest.indices(indexName);

            if (indices.exists(getIndexRequest)) {
                final DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest();
                deleteIndexRequest.indices(indexName);
                indices.delete(deleteIndexRequest);
            }
        } catch (IOException e) {
            throw new InternalServiceException(e);
        }
    }

    @Override
    public void createIndex(final String indexName, final String typeName) {
        try {
            final IndicesClient indices = this.restHighLevelClient.indices();

            final GetIndexRequest getIndexRequest = new GetIndexRequest();
            getIndexRequest.indices(indexName);

            if (!indices.exists(getIndexRequest)) {

                final CreateIndexRequest createIndexRequest = new CreateIndexRequest();
                createIndexRequest.index(indexName);
                createIndexRequest.mapping(typeName, mappingBuilder);
                indices.create(createIndexRequest);
            }
        } catch (IOException e) {
            throw new InternalServiceException(e);
        }
    }
}
