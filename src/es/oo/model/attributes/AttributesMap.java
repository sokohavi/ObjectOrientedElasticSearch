package es.oo.model.attributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An attributes map is the object we use to serialize any object.
 */
public class AttributesMap implements Serializable {
    public static final String NAMESPACE_MAP = "namespaceMap";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Map<String, Map<String, Object>> namespaceMap;

    public AttributesMap() {
        this.namespaceMap = new HashMap<>();
    }

    public AttributesMap(final Map<String, Map<String, Object>> namespaceMap) {
        this.namespaceMap = namespaceMap != null ? namespaceMap : new HashMap<>();
    }

    /**
     * Returns a map of attributes:
     * 1. Key of the external map is the namespace value.
     * 2. Key of the internal map is the attribute name.
     * 3. Value of the internal map is the serialize value.
     */
    public  Map<String, Map<String, Object>> getNamespaceMap() {
        return this.namespaceMap;
    }

    public <T> T toObject(Class<T> tClass) {
        return OBJECT_MAPPER.convertValue(this.getNamespaceMap().get(tClass.getSimpleName()), tClass);
    }

    /**
     * A list of objects which you want to index or search.
     */
    public static AttributesMap toAttributesMap(final List<Object> objectsList)
            throws IOException {
        final AttributesMap attributesMap = new AttributesMap(new HashMap<>());

        for (final Object object: objectsList) {
            attributesMap.getNamespaceMap().putAll(toAttributesMap(object).getNamespaceMap());
        }

        return attributesMap;
    }

    public static AttributesMap toAttributesMap(final Object object) throws IOException {

        if (Objects.isNull(object)) {
            return new AttributesMap();
        }

        final Map<String, Map<String, Object>> namespaceMap = new HashMap<>();
        final Map<String, Object> attributesMap = new HashMap<>();

        final String namespace = object.getClass().getSimpleName();
        namespaceMap.put(namespace, attributesMap);


        if (object instanceof Boolean || object instanceof String || object instanceof Integer ||
                object instanceof Float || object instanceof Character || object instanceof Double ||
                object instanceof Long || object instanceof Short) {
            attributesMap.put(object.getClass().getSimpleName(), object);
            return new AttributesMap(namespaceMap);
        }

        Map<String, Object> objectProperties;
        try {
            objectProperties = OBJECT_MAPPER.convertValue(object, Map.class);
        } catch(final Exception e) {
            throw e;
        }


        for (final Map.Entry<String, Object> attributeEntry: objectProperties.entrySet()) {
            final String attributeName = attributeEntry.getKey();
            final Object value = attributeEntry.getValue();

            if (value == null) {
                continue;
            }

            attributesMap.put(attributeName, value);
        }

        return new AttributesMap(namespaceMap);
    }

    public static AttributesMap deserialize(final String attributeMapString) throws IOException {
        return OBJECT_MAPPER.readValue(attributeMapString, AttributesMap.class);
    }

    public String serialize() throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(this);
    }
}
