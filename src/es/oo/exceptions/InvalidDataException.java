package es.oo.exceptions;

/**
 * Exception thrown when the service invalid data
 * from what is expected.
 */
public class InvalidDataException extends RuntimeException {
    public InvalidDataException(final String message) {
        super(message);
    }
}
