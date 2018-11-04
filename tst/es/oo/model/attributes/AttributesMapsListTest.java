package es.oo.model.attributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test class for {@link AttributesMapsList}.
 */
public class AttributesMapsListTest {
    final static String KEY =  "key";
    final static String STRING_VALUE =  "fakeValue";
    final static int INT_VALUE =  123;
    final static int INT_VALUE_2 =  456;
    final static Map<String, String> STRINGS_MAP =  new HashMap<String, String>();
    final static List<String> STRINGS_LIST =  new ArrayList<>();

    private static final SomeObject OBJECT_STUB = new SomeObject(STRING_VALUE, INT_VALUE, STRINGS_MAP, STRINGS_LIST);

    /**
     * Tests {@link AttributesMapsList#toAttributesMapsList(Object)} for a {@link SomeObject}.
     */
    @Test
    public void toAttributesMapsList_FromObject_SingleObject() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final AttributesMapsList attributesMapsList = AttributesMapsList.toAttributesMapsList(OBJECT_STUB);


        assertThat(attributesMapsList.size(), equalTo(1));

        final AttributesMap attributesMap = attributesMapsList.get(0);
        assertObjectStub(mapper, attributesMap, OBJECT_STUB);
    }

    /**
     * Tests {@link AttributesMapsList#toAttributesMapsList(List)} for a simple list with one {@link SomeObject}.
     */
    @Test
    public void toAttributesMapsList_FromListOfLists_SingleObject() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final List<Object> objectList = new ArrayList<>();
        objectList.add(OBJECT_STUB);

        final List<List<Object>> objectLists = new ArrayList<>();
        objectLists.add(objectList);

        final AttributesMapsList attributesMapsList = AttributesMapsList.toAttributesMapsList(objectLists);


        assertThat(attributesMapsList.size(), equalTo(1));

        final AttributesMap attributesMap = attributesMapsList.get(0);
        assertObjectStub(mapper, attributesMap, OBJECT_STUB);
    }

    /**
     * Tests {@link AttributesMapsList#toAttributesMapsList(List)} for a simple list with multiple {@link SomeObject}
     * objects which are the same.
     */
    @Test
    public void toAttributesMapsList_FromListOfLists_MultipleSameObjects() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final List<Object> objectList1 = new ArrayList<>();
        objectList1.add(OBJECT_STUB);

        final List<Object> objectList2 = new ArrayList<>();
        objectList2.add(OBJECT_STUB);

        final List<List<Object>> objectLists = new ArrayList<>();
        objectLists.add(objectList1);
        objectLists.add(objectList2);

        final AttributesMapsList attributesMapsList = AttributesMapsList.toAttributesMapsList(objectLists);


        assertThat(attributesMapsList.size(), equalTo(2));

        final AttributesMap attributesMap1 = attributesMapsList.get(0);
        assertObjectStub(mapper, attributesMap1, OBJECT_STUB);

        final AttributesMap attributesMap2 = attributesMapsList.get(1);
        assertObjectStub(mapper, attributesMap2, OBJECT_STUB);
    }

    /**
     * Tests {@link AttributesMapsList#toAttributesMapsList(List)} for a simple list with multiple {@link SomeObject}
     * objects which are the different.
     */
    @Test
    public void toAttributesMapsList_FromListOfLists_MultipleDifferentObjects_SameType()
            throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final List<Object> objectList1 = new ArrayList<>();
        objectList1.add(OBJECT_STUB);

        final List<Object> objectList2 = new ArrayList<>();
        final SomeObject objectStub = new SomeObject(null, INT_VALUE_2, null, null);
        objectList2.add(objectStub);

        final List<List<Object>> objectLists = new ArrayList<>();
        objectLists.add(objectList1);
        objectLists.add(objectList2);

        final AttributesMapsList attributesMapsList = AttributesMapsList.toAttributesMapsList(objectLists);


        assertThat(attributesMapsList.size(), equalTo(2));

        final AttributesMap attributesMap1 = attributesMapsList.get(0);
        assertObjectStub(mapper, attributesMap1, OBJECT_STUB);

        final AttributesMap attributesMap2 = attributesMapsList.get(1);
        assertObjectStub(mapper, attributesMap2, objectStub);
    }

    /**
     * Tests {@link AttributesMapsList#toAttributesMapsList(List)} for a simple list with multiple objects:
     * one {@link SomeObject} and the {@link SomeObject2}.
     */
    @Test
    public void toAttributesMapsList_FromListOfLists_MultipleDifferentObjects_DifferentType()
            throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final List<Object> objectList1 = new ArrayList<>();
        objectList1.add(OBJECT_STUB);

        final List<Object> objectList2 = new ArrayList<>();
        final SomeObject2 someObject2 = new SomeObject2(STRING_VALUE, INT_VALUE_2);
        objectList2.add(someObject2);

        final List<List<Object>> objectLists = new ArrayList<>();
        objectLists.add(objectList1);
        objectLists.add(objectList2);

        final AttributesMapsList attributesMapsList = AttributesMapsList.toAttributesMapsList(objectLists);


        assertThat(attributesMapsList.size(), equalTo(2));

        final AttributesMap attributesMap1 = attributesMapsList.get(0);
        assertObjectStub(mapper, attributesMap1, OBJECT_STUB);

        final AttributesMap attributesMap2 = attributesMapsList.get(1);

        final Map attributeMap = attributesMap2.getNamespaceMap().get(someObject2.getClass().getSimpleName());
        final SomeObject2 deserializedAttribute = mapper.convertValue(attributeMap, SomeObject2.class);
        assertThat(deserializedAttribute.getName(), equalTo(STRING_VALUE));
        assertThat(deserializedAttribute.getValue(), equalTo(INT_VALUE_2));
    }

    /**
     * This test is just to demonstrate how a serialized value of {@link SomeObject2} will look like.
     */
    @Test
    public void serialization_ValidObject() throws IOException {
        final AttributesMapsList attributesMapsList = createAttributesMapList();

        final String attributeMapString = attributesMapsList.serialize();
        final String expectedAttributesMapsListString =
                "[{\"namespaceMap\":{\"SomeObject\":{\"valuesList\":[],\"valuesMap\":{},\"someString\":\"fakeValue\",\"someInt\":123}}}]";

        assertThat(attributeMapString, equalTo(expectedAttributesMapsListString));
    }

    /**
     * This test is just to demonstrate how the serialization and deserialization work.
     */
    @Test
    public void deserialization_ValidObject() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final AttributesMapsList attributesMapsList = createAttributesMapList();

        final String attributeMapString = attributesMapsList.serialize();
        final AttributesMapsList deserializedList = AttributesMapsList.deserialize(attributeMapString);

        assertThat(deserializedList.size(), equalTo(1));

        final AttributesMap attributesMap = deserializedList.get(0);
        assertObjectStub(mapper, attributesMap, OBJECT_STUB);
    }

    private AttributesMapsList createAttributesMapList() throws IOException {
        final AttributesMap attributesMap = AttributesMap.toAttributesMap(OBJECT_STUB);

        final AttributesMapsList attributesMapsList = new AttributesMapsList();
        attributesMapsList.add(attributesMap);
        return attributesMapsList;
    }

    private void assertObjectStub(ObjectMapper mapper, AttributesMap attributesMap, SomeObject expectedStub) {
        assertThat(attributesMap.getNamespaceMap().size(), equalTo(1));
        final SomeObject objectStub = mapper.convertValue(attributesMap.getNamespaceMap().get("SomeObject"),
                SomeObject.class);
        assertThat(objectStub.getSomeInt(), equalTo(expectedStub.getSomeInt()));
        assertThat(objectStub.getSomeString(), equalTo(expectedStub.getSomeString()));
        assertThat(objectStub.getValuesList(), equalTo(expectedStub.getValuesList()));
        assertThat(objectStub.getValuesMap(), equalTo(expectedStub.getValuesMap()));
    }
}