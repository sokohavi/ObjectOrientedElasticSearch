package es.oo.util;

import java.util.ArrayList;
import java.util.List;

/**
 * An help class for building a list of objects list.
 */
public class InputsListBuilder<T> {
    final List<T> list = new ArrayList<>();

    public static InputsListBuilder createBuilder() {
        return new InputsListBuilder();
    }

    public InputsListBuilder add(final T object) {
        this.list.add(object);
        return this;
    }

    public List<T> build() {
        return list;
    }
}
