package es.oo.model.attributes;

import java.util.List;
import java.util.Map;

public class SomeObject {
    private String someString;
    private int someInt;
    private Map<String, String> valuesMap;
    private List<String> valuesList;

    public SomeObject() {}

    public SomeObject(final String someString,
                      final int someInt,
                      final Map<String, String> valuesMap,
                      final List<String> valuesList) {
        this.someString = someString;
        this.someInt = someInt;
        this.valuesMap = valuesMap;
        this.valuesList = valuesList;
    }

    public String getSomeString() {
        return someString;
    }

    public int getSomeInt() {
        return someInt;
    }

    public Map<String, String> getValuesMap() {
        return valuesMap;
    }

    public List<String> getValuesList() {
        return valuesList;
    }
}
