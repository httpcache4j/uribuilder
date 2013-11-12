package org.codehaus.httpcache4j.uri;

public class QueryParam {
    private String name;
    private String value;

    public QueryParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean isEmpty() {
        return value == null || value.trim().isEmpty();
    }

}
