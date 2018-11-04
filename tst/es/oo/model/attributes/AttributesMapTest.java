package es.oo.model.attributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test Class {@link AttributesMap}.
 */
public class AttributesMapTest {
    final static String KEY =  "key";
    final static String STRING_VALUE =  "fakeValue";
    final static int INT_VALUE =  123;
    final static int INT_VALUE_2 =  456;
    final static Map<String, String> STRINGS_MAP =  new HashMap<String, String>();
    final static List<String> STRINGS_LIST =  new ArrayList<>();

    static {
        STRINGS_MAP.put(KEY, STRING_VALUE);
        STRINGS_LIST.add(STRING_VALUE);
    }

    /**
     * This test is just to demonstrate how a serialized value of {@link SomeObject} will look like.
     */
    @Test
    public void serialization_FromAttribute_ValidObject() throws IOException {
        final SomeObject someObject = new SomeObject(STRING_VALUE, INT_VALUE, STRINGS_MAP, STRINGS_LIST);

        final AttributesMap attributesMap = AttributesMap.toAttributesMap(someObject);

        final String attributeMapString = attributesMap.serialize();
        final String expectedAttributesMapString =
                "{\"namespaceMap\":{\"SomeObject\":{\"valuesList\":[\"fakeValue\"],\"valuesMap\":{\"key\":\"fakeValue\"},\"someString\":\"fakeValue\",\"someInt\":123}}}";

        assertThat(attributeMapString, equalTo(expectedAttributesMapString));
    }

    /**
     * This test is just to demonstrate how a serialization and deserialized value of {@link AttributesMap}.
     */
    @Test
    public void deserialize_ValidAttributeObject() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final SomeObject someObject = new SomeObject(STRING_VALUE, INT_VALUE, STRINGS_MAP, STRINGS_LIST);
        final AttributesMap attributesMap = AttributesMap.toAttributesMap(someObject);

        final String attributeMapString = mapper.writeValueAsString(attributesMap);
        final AttributesMap deserializedAttributeMap = AttributesMap.deserialize(attributeMapString);

        assertThat(deserializedAttributeMap.getNamespaceMap().size(), equalTo(1));

        final Map<String, Object> attributeContentMap = deserializedAttributeMap.getNamespaceMap()
                .get(SomeObject.class.getSimpleName());
        assertThat(attributeContentMap.size(), equalTo(4));

        final SomeObject deserializedObject = mapper.convertValue(attributeContentMap, SomeObject.class);
        assertThat(deserializedObject.getSomeString(), equalTo(STRING_VALUE));
        assertThat(deserializedObject.getSomeInt(), equalTo(INT_VALUE));
        assertThat(deserializedObject.getValuesMap(), equalTo(STRINGS_MAP));
        assertThat(deserializedObject.getValuesList(), equalTo(STRINGS_LIST));
    }

    /**
     * Tests {@link AttributesMap#toAttributesMap(List)} for a case where the attribute maps contains a single
     * {@Attribute}.
     */
    @Test
    public void toAttributesMap_SingleFullAttribute() throws IOException {
        final List<Object> inputList = new ArrayList<>();
        final SomeObject someObject = new SomeObject(STRING_VALUE, INT_VALUE, STRINGS_MAP, STRINGS_LIST);
        inputList.add(someObject);

        final AttributesMap attributesMap = AttributesMap.toAttributesMap(inputList);

        assertThat(attributesMap.getNamespaceMap().size(), equalTo(1));

        final Map<String, Object> attributes = attributesMap.getNamespaceMap().get(SomeObject.class.getSimpleName());
        assertThat(attributes.size(), equalTo(4));
        assertThat(attributes.get("someString"), equalTo(STRING_VALUE));
        assertThat(attributes.get("someInt"), equalTo(INT_VALUE));
        assertThat(attributes.get("valuesMap"), equalTo(STRINGS_MAP));
        assertThat(attributes.get("valuesList"), equalTo(STRINGS_LIST));
    }

    /**
     * Tests {@link AttributesMap#toAttributesMap(List)} for a case where the attribute maps contains a single
     * {@Attribute} which doesn't have any populated attributes except for it value.
     */
    @Test
    public void toAttributesMap_FromObjectList_SinglePartialAttribute() throws IOException {
        final List<Object> inputList = new ArrayList<>();
        final SomeObject someObject = new SomeObject(null, INT_VALUE, STRINGS_MAP, null);
        inputList.add(someObject);

        final AttributesMap attributesMap = AttributesMap.toAttributesMap(inputList);

        assertThat(attributesMap.getNamespaceMap().size(), equalTo(1));

        final Map<String, Object> attributes = attributesMap.getNamespaceMap().get(SomeObject.class.getSimpleName());
        assertThat(attributes.size(), equalTo(2));
        assertThat(attributes.get("someInt"), equalTo(INT_VALUE));
        assertThat(attributes.get("valuesMap"), equalTo(STRINGS_MAP));
    }

    /**
     * This test is just to show the currently the {@link AttributesMap} can't handle more than one object of the same
     * type. If we do send two or more object of the same type the values of the attribute will be overrided.
     */
    @Test
    public void toAttributesMap_FromObjectList_MultipleAttributes_SameTypeId() throws IOException {
        final List<Object> inputList = new ArrayList<>();
        final SomeObject someObject1 = new SomeObject(STRING_VALUE, INT_VALUE, null, null);
        final SomeObject someObject2 = new SomeObject(null, INT_VALUE_2, STRINGS_MAP, STRINGS_LIST);
        inputList.add(someObject1);
        inputList.add(someObject2);

        final AttributesMap attributesMap = AttributesMap.toAttributesMap(inputList);

        assertThat(attributesMap.getNamespaceMap().size(), equalTo(1));

        final Map<String, Object> attributes = attributesMap.getNamespaceMap().get(SomeObject.class.getSimpleName());
        assertThat(attributes.size(), equalTo(3));
        assertThat(attributes.get("someString"), equalTo(null));
        assertThat(attributes.get("someInt"), equalTo(INT_VALUE_2));
        assertThat(attributes.get("valuesMap"), equalTo(STRINGS_MAP));
        assertThat(attributes.get("valuesList"), equalTo(STRINGS_LIST));
    }

    /**
     * This test is just to show the currently the {@link AttributesMap} can handle more than one item on the given list
     * as long as the types of the items on the given list are different.
     */
    @Test
    public void toAttributesMap_FromObjectList_MultipleDifferentObjects() throws IOException {
        final List<Object> inputList = new ArrayList<>();
        final SomeObject someObject = new SomeObject(null, INT_VALUE, STRINGS_MAP, null);
        final Optional<Object> optional = Optional.empty();
        inputList.add(someObject);
        inputList.add(optional);

        final AttributesMap attributesMap = AttributesMap.toAttributesMap(inputList);

        assertThat(attributesMap.getNamespaceMap().size(), equalTo(2));

        final Map<String, Object> attributes = attributesMap.getNamespaceMap().get(SomeObject.class.getSimpleName());
        assertThat(attributes.size(), equalTo(2));

        final Map<String, Object> optionals = attributesMap.getNamespaceMap().get("Optional");
        assertThat(optionals.size(), equalTo(1));
        assertThat(Boolean.parseBoolean(optionals.get("present").toString()), equalTo(false));
    }
}