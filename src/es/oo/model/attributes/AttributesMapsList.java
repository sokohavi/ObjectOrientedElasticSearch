package es.oo.model.attributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is a list of {@link AttributesMap} with no intersection between then.
 */
public class AttributesMapsList extends ArrayList<AttributesMap> {
    public static AttributesMapsList toAttributesMapsList(final List<List<Object>> objectLists)
            throws IOException {
        final AttributesMapsList attributesMapsList = new AttributesMapsList();

        for (final List<Object> objectList: objectLists) {
            attributesMapsList.add(AttributesMap.toAttributesMap(objectList));
        }

        return attributesMapsList;
    }

    public static AttributesMapsList toAttributesMapsList(final Object object)
            throws IOException {
        final AttributesMapsList attributesMapsList = new AttributesMapsList();
        attributesMapsList.add(AttributesMap.toAttributesMap(object));

        return attributesMapsList;
    }

    public static AttributesMapsList deserialize(final String attributeMapString) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(attributeMapString, AttributesMapsList.class);
    }

    public String serialize() throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }
}