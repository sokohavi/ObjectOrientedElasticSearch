package es.oo.endpoint;

import org.apache.http.Header;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RestClient;

import java.io.IOException;

/**
 * Adapter for the high level rest client.
 */
public interface RestHighLevelClientAdapter {
    RestClient getLowLevelClient();

    BulkResponse bulk(BulkRequest bulkRequest) throws IOException;

    void createIndex(final String indexName, final String typeName);

    void deleteIndex(final String indexName);
}
