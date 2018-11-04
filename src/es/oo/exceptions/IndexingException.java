package es.oo.exceptions;
import org.elasticsearch.action.bulk.BulkItemResponse;

/**
 * An item indexing exception (to ES).
 */
public class IndexingException extends RuntimeException {
    private static final String MESSAGE_FORMAT = "Indexing failure for providerId: %s, index: %s.\n Error: %s";

    public IndexingException(final Exception e) {
        super(e);
    }

    public IndexingException(final String id) {
        super("Unknown indexing error for id: " + id);
    }

    public IndexingException(final String id, final String indexName, final BulkItemResponse.Failure failure) {
        super(String.format(MESSAGE_FORMAT, id, indexName, failure));
    }
}
