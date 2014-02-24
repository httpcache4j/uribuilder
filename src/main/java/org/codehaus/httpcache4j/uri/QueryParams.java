package org.codehaus.httpcache4j.uri;

import java.text.Collator;
import java.util.*;

/**
 * @author <a href="mailto:hamnis@codehaus.org">Erlend Hamnaberg</a>
 */
public final class QueryParams implements Iterable<QueryParam> {
    private final Map<String, List<String>> parameters = new LinkedHashMap<String, List<String>>();

    public QueryParams() {
        this(Collections.<String, List<String>>emptyMap());
    }

    public QueryParams(Map<String, List<String>> parameters) {
        this.parameters.putAll(parameters);
    }

    public QueryParams(Iterable<QueryParam> parameters) {
        this(toMap(parameters));
    }

    public QueryParams empty() {
        return new QueryParams();
    }

    public boolean isEmpty() {
        return parameters.isEmpty();
    }

    public boolean contains(String name) {
        return parameters.containsKey(name);
    }

    public boolean contains(String name, String value) {
        return contains(new QueryParam(name, value));
    }

    public boolean contains(QueryParam parameter) {
        List<String> values = parameters.get(parameter.getName());
        return values != null && (values.isEmpty() && parameter.isEmpty() || values.contains(parameter.getValue()));
    }

    public QueryParams add(String name, String... value) {
        List<QueryParam> p = new ArrayList<QueryParam>();
        if (value.length == 0) {
            p.add(new QueryParam(name, null));
        }
        for (String v: value) {
            p.add(new QueryParam(name, v));
        }
        return add(p);
    }

    public QueryParams add(QueryParam param) {
        return add(Arrays.asList(param));
    }

    public QueryParams add(Iterable<QueryParam> params) {
        Map<String, List<String>> map = toMap(params);
        return add(map);
    }

    public QueryParams add(Map<String, List<String>> params) {
        if (params.isEmpty()) {
            return this;
        }
        LinkedHashMap<String, List<String>> copy = copy();
        copy.putAll(params);
        return new QueryParams(copy);
    }

    public QueryParams set(Map<String, List<String>> params) {
        return new QueryParams(params);
    }

    public QueryParams set(Iterable<QueryParam> params) {
        Map<String, List<String>> map = toMap(params);
        return new QueryParams(map);
    }

    public QueryParams set(String name, String value) {
        LinkedHashMap<String, List<String>> copy = copy();
        copy.remove(name);
        if (value != null) {
            ArrayList<String> list = new ArrayList<String>();
            list.add(value);
            copy.put(name, list);
        }
        return new QueryParams(copy);
    }

    public QueryParams set(String name, List<String> value) {
        LinkedHashMap<String, List<String>> copy = copy();
        copy.remove(name);
        if (!value.isEmpty()) {
            ArrayList<String> list = new ArrayList<String>();
            list.addAll(value);
            copy.put(name, list);
        }
        return new QueryParams(copy);
    }

    public List<String> get(String name) {
        List<String> list = parameters.get(name);
        if (list != null) {
            return Collections.unmodifiableList(list);
        }
        return Collections.emptyList();
    }

    public String getFirst(String name) {
        List<String> values = get(name);
        if (!values.isEmpty()) return values.get(0);
        return null;
    }

    public QueryParams remove(String name) {
        if (!parameters.containsKey(name)) {
            return this;
        }
        LinkedHashMap<String, List<String>> copy = copy();
        copy.remove(name);
        return new QueryParams(copy);
    }

    public List<QueryParam> asList() {
        List<QueryParam> list = new ArrayList<QueryParam>();
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            if (entry.getValue().isEmpty()) {
                list.add(new QueryParam(entry.getKey(), ""));
            }
            for (String value : entry.getValue()) {
                list.add(new QueryParam(entry.getKey(), value));
            }
        }
        return Collections.unmodifiableList(list);
    }

    public Map<String, List<String>> asMap() {
        return Collections.unmodifiableMap(parameters);
    }

    public String toQuery(boolean sort) {
        StringBuilder builder = new StringBuilder();
        List<QueryParam> params = new ArrayList<QueryParam>(asList());
        if (sort) {
            Collections.sort(params, new Comparator<QueryParam>() {
                @Override
                public int compare(QueryParam o1, QueryParam o2) {
                    return Collator.getInstance(Locale.ENGLISH).compare(o1.getName(), o2.getName());
                }
            });
        }
        for (QueryParam parameter : params) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URIEncoder.encodeUTF8(parameter.getName()));
            if(!parameter.isEmpty()) {
                builder.append("=").append(URIEncoder.encodeUTF8(parameter.getValue()));
            }
        }
        if (builder.length() == 0) {
            return null;
        }
        return builder.toString();
    }

    private LinkedHashMap<String, List<String>> copy() {
        return new LinkedHashMap<String, List<String>>(this.parameters);
    }

    @Override
    public Iterator<QueryParam> iterator() {
        return asList().iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryParams that = (QueryParams) o;

        if (!parameters.equals(that.parameters)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return parameters.hashCode();
    }


    public static QueryParams parse(String query) {
        Map<String, List<String>> map = new LinkedHashMap<String, List<String>>();
        if (query != null) {
            String[] parts = query.split("&");
            for (String part : parts) {
                String[] equalParts = part.trim().split("=");
                String name = null;
                String value = null;
                if (equalParts.length == 1) {
                    name = equalParts[0].trim();
                }
                else if (equalParts.length == 2) {
                    name = equalParts[0].trim();
                    value = equalParts[1].trim();
                }
                if (name != null) {
                    addToQueryMap(map, URIDecoder.decodeUTF8(name), URIDecoder.decodeUTF8(value));
                }
            }
        }

        return new QueryParams(map);
    }

    private static Map<String, List<String>> toMap(Iterable<QueryParam> parameters) {
        Map<String, List<String>> map = new LinkedHashMap<String, List<String>>();
        for (QueryParam parameter : parameters) {
            addToQueryMap(map, parameter.getName(), parameter.getValue());
        }
        return map;
    }

    private static void addToQueryMap(Map<String, List<String>> map, String name, String value) {
        List<String> list = map.get(name);
        if (list == null) {
            list = new ArrayList<String>();
        }
        if (value != null) {
            list.add(value);
        }
        map.put(name, list);
    }
}
