package org.codehaus.httpcache4j.uri;

import java.text.Collator;
import java.util.*;
import java.util.stream.*;

/**
 * @author <a href="mailto:hamnis@codehaus.org">Erlend Hamnaberg</a>
 */
public final class QueryParams implements Iterable<QueryParam> {
    private final Map<String, List<String>> parameters = new LinkedHashMap<>();

    public QueryParams() {
        this(Collections.emptyMap());
    }

    public QueryParams(Map<String, List<String>> parameters) {
        this.parameters.putAll(parameters);
    }

    public QueryParams(Iterable<QueryParam> parameters) {
        this(toMap(parameters));
    }

    public static QueryParams empty() {
        return new QueryParams();
    }

    public boolean isEmpty() {
        return parameters.isEmpty();
    }

    public int size() {
        return asList().size();
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
        List<QueryParam> p = new ArrayList<>();
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
        return add(toMap(params));
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

    public QueryParams set(String name, String... value) {
        return set(name, value != null ? Arrays.asList(value) : Collections.emptyList());
    }

    public QueryParams set(String name, List<String> value) {
        LinkedHashMap<String, List<String>> copy = copy();
        copy.remove(name);
        if (!value.isEmpty()) {
            copy.put(name, new ArrayList<>(value));
        }
        return new QueryParams(copy);
    }

    public List<String> get(String name) {
        Optional<List<String>> maybeList = Optional.ofNullable(parameters.get(name));
        return maybeList.map(Collections::unmodifiableList).orElse(Collections.emptyList());
    }

    public List<QueryParam> getAsQueryParam(String name) {
        List<String> list = parameters.get(name);
        if (list != null) {
            return list.stream().map(v -> new QueryParam(name, v)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public Optional<String> getFirst(String name) {
        List<String> values = get(name);
        return values.stream().findFirst();
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
        return parameters.entrySet().stream().flatMap(e -> {
            if (e.getValue().isEmpty()) {
                return Stream.of(new QueryParam(e.getKey(), ""));
            } else {
                return e.getValue().stream().map(v -> new QueryParam(e.getKey(), v));
            }
        }).collect(Collectors.toList());
    }

    public Map<String, List<String>> asMap() {
        return Collections.unmodifiableMap(parameters);
    }

    public String toQuery(boolean sort) {
        return toQuery(sort, Locale.ENGLISH);
    }

    public String toQuery(boolean sort, Locale locale) {
        StringBuilder builder = new StringBuilder();
        List<QueryParam> params = new ArrayList<>(asList());
        if (sort) {
            params.sort((o1, o2) -> Collator.getInstance(locale).compare(o1.getName(), o2.getName()));
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
        return new LinkedHashMap<>(this.parameters);
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
        if (query != null) {
            List<String> parts = Arrays.asList(query.split("&"));
            return new QueryParams(toMap(parts.stream().map(QueryParams::parseQP).collect(Collectors.toList())));
        }

        return new QueryParams(Collections.emptyMap());
    }

    private static QueryParam parseQP(String s) {
        String[] equalParts = s.trim().split("=");
        String name = null;
        String value = null;
        if (equalParts.length == 1) {
            name = equalParts[0].trim();
        }
        else if (equalParts.length == 2) {
            name = equalParts[0].trim();
            value = equalParts[1].trim();
        }
        return new QueryParam(URIDecoder.decodeUTF8(name), URIDecoder.decodeUTF8(value));
    }

    private static Map<String, List<String>> toMap(Iterable<QueryParam> parameters) {
        Stream<QueryParam> stream = StreamSupport.stream(parameters.spliterator(), false);
        Map<String, java.util.List<QueryParam>> nqp = stream.collect(Collectors.groupingBy(QueryParam::getName, LinkedHashMap::new, Collectors.toList()));
        LinkedHashMap<String, List<String>> map = new LinkedHashMap<>(nqp.size());
        nqp.forEach((k, v) ->
                map.put(k, v.stream().map(QueryParam::getValue).collect(Collectors.toList()))
        );
        return map;
    }
}
