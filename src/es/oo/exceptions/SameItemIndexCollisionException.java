package es.oo.exceptions;

/**
 * A sub case of {@link IndexingException} where we tried to index data about an item where we
 * have a another source trying to update the same item.
 */
public class SameItemIndexCollisionException extends IndexingException {
    public SameItemIndexCollisionException(final String id) {
        super(id);
    }
}
