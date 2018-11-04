package es.oo.exceptions;

/**
 * Exception class for any unexpected exception
 * from the service.
 */
public class InternalServiceException extends RuntimeException {
    public InternalServiceException(final Throwable t) {
        super(t);
    }

    public InternalServiceException(String internalFailure) {
        this(new Exception(internalFailure));
    }
}