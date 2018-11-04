package es.oo.endpoint;

import es.oo.exceptions.IndexingException;
import es.oo.exceptions.InternalServiceException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.Header;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.shard.ShardId;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

/**
 * Test class for {@link ElasticSearchProxy}.
 */
public class ElasticSearchProxyTest {
    private final static String ID = "12345";
    private final static String VALUE_STRING = "Value";

    /**
     * Test {@link ElasticSearchProxy} constructor for a null {@link RestHighLevelClient} input.
     */
    @Test(expected = NullPointerException.class)
    public void constructor_NullRestClient() throws IOException {
        final RestHighLevelClient restHighLevelClient = null;
        new ElasticSearchProxy(restHighLevelClient);
    }

    /**
     * Test {@link ElasticSearchProxy#writeItem(String, Object)} for a case where {@link RestClient} throws exceptions.
     */
    @Test(expected = IndexingException.class)
    public void writeToTarget_RestException_IOException() throws IOException, InterruptedException, IndexingException {
        final RestHighLevelClientAdapter highLevelRestClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final RestClient restClient = Mockito.mock(RestClient.class);
        Mockito.when(highLevelRestClient.getLowLevelClient()).thenReturn(restClient);
        Mockito.when(highLevelRestClient.bulk(Mockito.any())).thenThrow(new IOException());

        final ElasticSearchProxy unitForTest = new ElasticSearchProxy(highLevelRestClient);
        unitForTest.writeItem(ID, VALUE_STRING);
    }

    /**
     * Test {@link ElasticSearchProxy#writeItem(String, Object)} for a case where {@link RestClient} has failures
     * for indexing the new items.
     */
    @Test(expected = IndexingException.class)
    public void writeToTarget_BulkItemFailed() throws IOException, IndexingException {
        final RestHighLevelClientAdapter highLevelRestClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final RestClient restClient = Mockito.mock(RestClient.class);
        Mockito.when(highLevelRestClient.getLowLevelClient()).thenReturn(restClient);

        final BulkResponse bulkResponse = Mockito.mock(BulkResponse.class);
        Mockito.when(highLevelRestClient.bulk(Mockito.any())).thenReturn(bulkResponse);

        final ElasticSearchProxy unitForTest = new ElasticSearchProxy(highLevelRestClient) {
            @Override
            protected BulkItemResponse getItemFromBulkResponse(String id) {
                return createBulkItemResponse(id);
            }
        };

        unitForTest.writeItem(ID, VALUE_STRING);
    }

    /**
     * Test {@link ElasticSearchProxy#writeItem(String, Object)} for a case where we have a serialization issue for
     * the item we want to index.
     */
    @Test(expected = RuntimeException.class)
    public void writeToTarget_SerializationException()
            throws IOException, IndexingException {
        final RestHighLevelClientAdapter highLevelRestClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final RestClient restClient = Mockito.mock(RestClient.class);
        Mockito.when(highLevelRestClient.getLowLevelClient()).thenReturn(restClient);

        final ElasticSearchProxy unitForTest = new ElasticSearchProxy(highLevelRestClient) {
            @Override
            protected String serializedItem(Object item) {
                throw new RuntimeException();
            }
        };

        unitForTest.writeItem(ID, VALUE_STRING);
    }

    /**
     * Test {@link ElasticSearchProxy#writeItem(String, Object)} for a case only one thread tries to write to
     * the same instance of the {@link ElasticSearchProxy}.
     */
    @Test
    public void writeToTarget_OnlyOneThreadTriesToWrite()
            throws IOException, IndexingException {
        final RestHighLevelClientAdapter highLevelRestClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final RestClient restClient = Mockito.mock(RestClient.class);
        Mockito.when(highLevelRestClient.getLowLevelClient()).thenReturn(restClient);

        createBulkResponseExpectations(highLevelRestClient, ID);

        final ElasticSearchProxy unitForTest = new ElasticSearchProxy(highLevelRestClient);

        unitForTest.writeItem(ID, VALUE_STRING);
    }

    /**
     * Test {@link ElasticSearchProxy#writeItem(String, Object)} for a case many threads tries to write to
     * the same instance of the {@link ElasticSearchProxy} at a single point in time.
     */
    @Test
    public void writeToTarget_ManyThreadsTryToWrite_Burst() throws IOException, ExecutionException, InterruptedException {
        final RestHighLevelClientAdapter highLevelRestClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final RestClient restClient = Mockito.mock(RestClient.class);
        Mockito.when(highLevelRestClient.getLowLevelClient()).thenReturn(restClient);

        final BulkResponse bulkResponse = createBulkResponseExpectations(highLevelRestClient, ID);
        Mockito.when(highLevelRestClient.bulk(Mockito.any())).thenReturn(bulkResponse);

        final ElasticSearchProxy unitForTest = new ElasticSearchProxy(highLevelRestClient) {
            @Override
            protected BulkItemResponse getItemFromBulkResponse(String id) {
                return createBulkItemResponse(id);
            }
        };

        final int numOfThreads = 100;
        final ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);
        final List<Future<Boolean>> futuresList = new ArrayList<>();
        for (Integer threadIndex = 0; threadIndex < numOfThreads; threadIndex++) {
            final String id = threadIndex.toString();
            Callable<Boolean> writeItemCallable = () -> writeItem(unitForTest, id);

            futuresList.add(executorService.submit(writeItemCallable));
        }

        for (final Future<Boolean> future : futuresList) {
            Assert.assertTrue(future.get());
        }
    }

    /**
     * Test {@link ElasticSearchProxy#writeItem(String, Object)} for a case multiple threads tries to write to
     * the same instance of the {@link ElasticSearchProxy} at multiple points in time.
     */
    @Test
    public void writeToTarget_ManyThreadsTryToWrite_SlowArrivalRate() throws IOException, ExecutionException, InterruptedException {
        final RestHighLevelClientAdapter highLevelRestClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final RestClient restClient = Mockito.mock(RestClient.class);
        Mockito.when(highLevelRestClient.getLowLevelClient()).thenReturn(restClient);

        final BulkResponse bulkResponse = createBulkResponseExpectations(highLevelRestClient,ID);
        Mockito.when(highLevelRestClient.bulk(Mockito.any())).thenReturn(bulkResponse);

        final ElasticSearchGatewayHelper unitForTest = new ElasticSearchGatewayHelper(highLevelRestClient){
            @Override
            protected BulkItemResponse getItemFromBulkResponse(String id) {
                return createBulkItemResponse(id);
            }
        };

        final int numOfThreads = 10;
        final ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);
        final List<Future<Boolean>> futuresList = new ArrayList<>();
        for (Integer threadIndex = 0; threadIndex < numOfThreads; threadIndex++) {
            final String id = threadIndex.toString();
            Callable<Boolean> writeItemCallable = () -> writeItem(unitForTest, id);

            futuresList.add(executorService.submit(writeItemCallable));

            Thread.sleep(3);
        }

        for (final Future<Boolean> future : futuresList) {
            Assert.assertTrue(future.get());
        }

        Assert.assertTrue(unitForTest.indexToEsCounter > 1);
    }

    /**
     * Test {@link ElasticSearchProxy#writeItem(String, Object)} where multiple threads try to update the same item.
     * Currently this isn't allowed.
     */
    @Test(expected = ExecutionException.class)
    public void writeItem_MultipleThreadsSameId() throws IOException, ExecutionException, InterruptedException {
        final RestHighLevelClientAdapter highLevelRestClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final RestClient restClient = Mockito.mock(RestClient.class);
        Mockito.when(highLevelRestClient.getLowLevelClient()).thenReturn(restClient);

        final BulkResponse bulkResponse = createBulkResponseExpectations(highLevelRestClient,ID);
        Mockito.when(highLevelRestClient.bulk(Mockito.any())).thenReturn(bulkResponse);

        final ElasticSearchProxy unitForTest = new ElasticSearchProxy(highLevelRestClient);
        Thread.sleep(500);

        final int numOfThreads = 20;
        final ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);
        final List<Future<Boolean>> futuresList = new ArrayList<>();
        for (Integer threadIndex = 0; threadIndex < numOfThreads; threadIndex++) {
            final Callable<Boolean> writeItemCallable = () -> writeItem(unitForTest, "ID");
            futuresList.add(executorService.submit(writeItemCallable));
        }

        for (final Future<Boolean> future : futuresList) {
            Assert.assertTrue(future.get());
        }
    }

    /**
     * Test {@link ElasticSearchProxy#writeItem(String, Object)} for a case there is no write request.
     * What we actually testing is the internal worker - we want to make sure it doesn't try to index anything.
     */
    @Test
    public void writeToTarget_NoWriteRequests() throws InterruptedException {
        final RestHighLevelClientAdapter restClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final ElasticSearchGatewayHelper unitForTest = new ElasticSearchGatewayHelper(restClient);
        Thread.sleep(50);
        Assert.assertTrue(unitForTest.indexToEsCounter == 0);
    }

    /**
     * Test {@link ElasticSearchProxy#indexDataToEs()} for a case there is no write request.
     * What we actually testing is the internal worker - we want to make sure it doesn't try to index anything.
     */
    @Test
    public void indexDataToEs_NoWriteRequests() throws InterruptedException {
        final RestHighLevelClientAdapter restClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final ElasticSearchGatewayHelper unitForTest = new ElasticSearchGatewayHelper(restClient);
        Thread.sleep(50);
        Assert.assertTrue(unitForTest.indexToEsCounter == 0);
        Assert.assertTrue(unitForTest.runWorkerCounter > 0);
    }

    /**
     * Test {@link ElasticSearchGatewayHelper#finalize()}
     */
    @Test
    public void finalize_NoNewRequests() throws InterruptedException {
        final RestHighLevelClientAdapter highLevelRestClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final RestClient restClient = Mockito.mock(RestClient.class);
        Mockito.when(highLevelRestClient.getLowLevelClient()).thenReturn(restClient);

        final ElasticSearchGatewayHelper unitForTest = new ElasticSearchGatewayHelper(highLevelRestClient);
        unitForTest.finalize();

        Thread.sleep(50);

        final int runCallsCounter = unitForTest.runWorkerCounter;

        Thread.sleep(50);

        Assert.assertEquals(runCallsCounter, unitForTest.runWorkerCounter);
    }

    /**
     * Test {@link ElasticSearchProxy#search(String)} for a null input.
     */
    @Test(expected = IllegalArgumentException.class)
    public void search_NullQuery() {
        final RestHighLevelClientAdapter restClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final ElasticSearchProxy unitForTest = new ElasticSearchProxy(restClient);
        final String query = null;
        unitForTest.search(query);
    }

    /**
     * Test {@link ElasticSearchProxy#search(String)} for a case where {@link RestClient} throws an exception
     * when we try to call the search api.
     */
    @Test(expected = InternalServiceException.class)
    public void search_RestClientException() throws IOException {
        final RestHighLevelClientAdapter highLevelRestClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final RestClient restClient = Mockito.mock(RestClient.class);
        Mockito.when(highLevelRestClient.getLowLevelClient()).thenReturn(restClient);

        Mockito.when(restClient.performRequest(Mockito.any(String.class), Mockito.any(String.class),
                Mockito.any(Map.class), Mockito.any(NStringEntity.class), Mockito.any(Header[].class)))
                .thenThrow(new IOException());

        final ElasticSearchProxy unitForTest = new ElasticSearchProxy(highLevelRestClient);
        unitForTest.search("");
    }

    /**
     * Test {@link ElasticSearchProxy#search(String)} for a case where {@link RestClient} returns null response for the
     * given query.
     */
    @Test
    public void search_NullResponse() {
        final RestHighLevelClientAdapter highLevelRestClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final RestClient restClient = Mockito.mock(RestClient.class);
        Mockito.when(highLevelRestClient.getLowLevelClient()).thenReturn(restClient);

        final ElasticSearchProxy unitForTest = new ElasticSearchProxy(highLevelRestClient);
        final Response searchResponse = unitForTest.search("");

        Assert.assertNull(searchResponse);
    }

    /**
     * Test {@link ElasticSearchProxy#search(String)} for a case where {@link RestClient} returns results for the
     * given query.
     */
    @Test
    public void search_WithResults() throws IOException {
        final Response searchResult = Mockito.mock(Response.class);

        final RestHighLevelClientAdapter highLevelRestClient = Mockito.mock(RestHighLevelClientAdapter.class);
        final RestClient restClient = Mockito.mock(RestClient.class);
        Mockito.when(highLevelRestClient.getLowLevelClient()).thenReturn(restClient);

        Mockito.when(restClient.performRequest(Mockito.any(String.class), Mockito.any(String.class),
                Mockito.any(Map.class), Mockito.any(NStringEntity.class), Mockito.any(Header[].class)))
                .thenReturn(searchResult);

        final ElasticSearchProxy unitForTest = new ElasticSearchProxy(highLevelRestClient);
        final Response searchResponse = unitForTest.search("");

        Assert.assertNotNull(searchResponse);
    }

    private BulkResponse createBulkResponseExpectations(final RestHighLevelClientAdapter highLevelRestClient, final String id)
            throws IOException {
        final BulkItemResponse bulkItemResponse = createBulkItemResponse(id);
        final BulkItemResponse[] bulkItemResponses = new BulkItemResponse[] { bulkItemResponse };
        final BulkResponse bulkResponse = new BulkResponse(bulkItemResponses, 1);
        Mockito.when(highLevelRestClient.bulk(Mockito.any())).thenReturn(bulkResponse);
        return bulkResponse;
    }

    private BulkItemResponse createBulkItemResponse(final String id) {
        final int version = 1;
        final ShardId shardId = Mockito.mock(ShardId.class);
        return new BulkItemResponse(1, DocWriteRequest.OpType.UPDATE,
                new UpdateResponse(shardId, "Type", id, version, DocWriteResponse.Result.UPDATED));
    }

    private class ElasticSearchGatewayHelper extends ElasticSearchProxy {
        public int indexToEsCounter = 0;
        public int runWorkerCounter = 0;

        public ElasticSearchGatewayHelper(final RestHighLevelClientAdapter client) {
            super(client);
        }

        @Override
        protected int getMinMillisBetweenCalls() {
            return 5;
        }

        @Override
        protected void indexDataToEs() {
            indexToEsCounter++;
            super.indexDataToEs();
        }

        @Override
        public void run() {
            runWorkerCounter++;
            super.run();
        }
    }

    private Boolean writeItem(final ElasticSearchProxy unitForTest, final String id) throws Exception {
        unitForTest.writeItem(id, VALUE_STRING);
        return true;
    }
}