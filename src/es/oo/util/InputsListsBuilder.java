package es.oo.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An help class for building a list of objects list.
 */
public class InputsListsBuilder {
    final List<List<?>> lists = new ArrayList<>();

    public static InputsListsBuilder createBuilder() {
        return new InputsListsBuilder();
    }

    public InputsListsBuilder add(final Object object) {
        this.lists.add(Collections.singletonList(object));
        return this;
    }


    public InputsListsBuilder add(final List<Object> objectsList) {
        this.lists.add(objectsList);
        return this;
    }

    public List<List<?>> build() {
        return lists;
    }
}
