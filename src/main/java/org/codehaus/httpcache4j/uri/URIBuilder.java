/*
 * Copyright (c) 2009. The Codehaus. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.codehaus.httpcache4j.uri;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Immutable URI builder.
 * Paths in this URI builder will be UTF-8 {@link org.codehaus.httpcache4j.uri.URIEncoder URIEncoded}.
 *
 * All methods return a NEW instance of the URI builder, meaning you can create a ROOT uri builder and use it
 * to your heart's content, as the instance will never change.
 *
 * @author <a href="mailto:hamnis@codehaus.org">Erlend Hamnaberg</a>
 */
public final class URIBuilder {
    public static AtomicReference<URISchemeDefaults> schemeDefaults = new AtomicReference<>(new URISchemeDefaults());

    private final Optional<String> scheme;
    private final Optional<String> host;
    private final Optional<Integer> port;
    private final List<Path> path;
    private final Optional<String> fragment;
    private final QueryParams parameters;
    private final boolean wasPathAbsolute;
    private final boolean endsWithSlash;
    private final Optional<String> schemeSpecificPart;

    private URIBuilder(Optional<String> scheme, Optional<String> schemeSpecificPart, Optional<String> host, Optional<Integer> port, List<Path> path, Optional<String> fragment, QueryParams parameters, boolean wasPathAbsolute, boolean endsWithSlash) {
        this.scheme = scheme;
        this.schemeSpecificPart = schemeSpecificPart;
        this.host = host;
        this.port = port;
        this.path = path;
        this.fragment = fragment;
        this.parameters = parameters;
        this.wasPathAbsolute = wasPathAbsolute;
        this.endsWithSlash = endsWithSlash;
    }

    public URIBuilder withHost(String host) {
        return new URIBuilder(scheme, schemeSpecificPart, Optional.ofNullable(host), port, path, fragment, parameters, wasPathAbsolute, endsWithSlash);
    }

    /**
     * Sets the port. This is not required to set if you are using default ports for 'http' or 'https'
     * @param port the port to set
     * @return a new URIBuilder with the port set
     */
    public URIBuilder withPort(int port) {
        Optional<Integer> defaultPort = scheme.flatMap(s -> schemeDefaults.get().getPort(s));

        if (exists(defaultPort, p -> p == port)) {
            defaultPort = Optional.empty();
        }
        else {
            defaultPort = Optional.of(port);
        }
        return new URIBuilder(scheme, schemeSpecificPart, host, defaultPort, path, fragment, parameters, wasPathAbsolute, endsWithSlash);
    }

    private <T> boolean exists(Optional<T> opt, Predicate<T> p) {
        return opt.filter(p).isPresent();
    }

    private boolean isURN() {
        return exists(scheme, s -> s.startsWith("urn"));
    }


    /**
     * This is the scheme to use. Usually 'http' or 'https'.
     * @param scheme the scheme
     * @return a new URIBuilder with the new scheme set.
     */
    public URIBuilder withScheme(String scheme) {
        return new URIBuilder(Optional.ofNullable(scheme), schemeSpecificPart, host, port, path, fragment, parameters, wasPathAbsolute, endsWithSlash);
    }

    /**
     * Adds a raw path to the URI.
     * @param path a path which may contain '/'
     * @return a new URI builder which contains the added path.
     */
    public URIBuilder addRawPath(String path) {
        boolean pathAbsolute = wasPathAbsolute || this.path.isEmpty() && path.startsWith("/");
        boolean endsWithSlash = this.endsWithSlash || this.path.isEmpty() && path.endsWith("/");
        List<Path> currentPath = Stream.concat(this.path.stream(), toPathParts(path).stream()).collect(Collectors.toList());
        return pathInternal(currentPath, pathAbsolute, endsWithSlash);
    }

    /**
     * Appends the path part to the URI.
     * We do not expect the path separator '/' to appear here, as each element will be URLEncoded.
     * If the '/' character do appear it will be URLEncoded with the rest of the path.
     *
     * @param path path elements.
     * @return a new URI builder which contains the new path.
     */
    public URIBuilder addPath(List<String> path) {
        List<Path> currentPath = Stream.concat(this.path.stream(), path.stream().map(stringToPath)).collect(Collectors.toList());
        return pathInternal(currentPath, wasPathAbsolute, false);
    }

    /**
     * @see #addPath(java.util.List)
     *
     * @param path path elements
     * @return a new URI builder which contains the new path.
     */
    public URIBuilder addPath(String... path) {
        return addPath(Arrays.asList(path));
    }

    /**
     * @see #withPath(java.util.List)
     *
     * @param path path elements.
     * @return a new URI builder which contains the new path.
     */
    public URIBuilder withPath(String... path) {
        return withPath(Arrays.asList(path));
    }

    /**
     * Sets the path of the uri.
     * We do not expect the path separator '/' to appear here, as each element will be URLEncoded.
     * If the '/' character do appear it will be URLEncoded with the rest of the path.
     *
     * @param pathList path elements.
     * @return a new URI builder which contains the new path.
     */
    public URIBuilder withPath(List<String> pathList) {
        List<Path> paths = pathList.stream().map(stringToPath).collect(Collectors.toList());
        return pathInternal(paths, false, false);
    }

    /**
     * @see #withPath(java.util.List)
     *
     * @param path path elements.
     * @return a new URI builder which contains the new path.
     */
    public URIBuilder withRawPath(String path) {
        boolean pathAbsoulute = path.startsWith("/");
        boolean endsWithSlash = path.endsWith("/");
        List<Path> parts = toPathParts(path);
        return pathInternal(parts, pathAbsoulute, endsWithSlash);
    }

    private URIBuilder pathInternal(List<Path> pathList, boolean pathAbsolute, boolean endsWithSlash) {
        return new URIBuilder(scheme, schemeSpecificPart, host, port, pathList, fragment, parameters, pathAbsolute, endsWithSlash);
    }

    public URIBuilder withFragment(String fragment) {
        return new URIBuilder(scheme, schemeSpecificPart, host, port, path, Optional.ofNullable(fragment), parameters, wasPathAbsolute, endsWithSlash);
    }

    /**
     * Creates a new URIBuilder with no parameters, but all other values retained.
     * @return new URIBuilder with no parameters.
     */
    public URIBuilder noParameters() {
        return withParameters(QueryParams.empty());
    }

    /**
     * Sets a list of parameters. This will clear out all previously set parameters in the new instance.
     * @param parameters the list of parameters
     * @return new URIBuilder with parameters.
     */
    public URIBuilder withParameters(Iterable<QueryParam> parameters) {
        QueryParams updated = this.parameters.set(parameters);
        return withParameters(updated);
    }

    public URIBuilder withParameters(Map<String, List<String>> params) {
        QueryParams updated = this.parameters.set(params);
        return withParameters(updated);
    }

    public URIBuilder withParameters(QueryParams params) {
        return new URIBuilder(scheme, schemeSpecificPart, host, port, path, fragment, params, wasPathAbsolute, endsWithSlash);
    }


    /**
     * Adds a new Parameter to the collection of parameters
     * @param name the parameter name
     * @param value the parameter value
     * @return a new instance of the URIBuilder
     */
    public URIBuilder addParameter(String name, String value) {
        return addParameter(new QueryParam(name, value));
    }

    /**
     * Adds a new Parameter to the collection of parameters
     * @param parameter the parameter
     * @return a new instance of the URIBuilder
     */
    public URIBuilder addParameter(QueryParam parameter) {
        return addParameters(Arrays.asList(parameter));
    }

    /**
     * Adds Parameters to the collection of parameters
     * @return a new instance of the URIBuilder
     */
    public URIBuilder addParameters(Iterable<QueryParam> newParams) {
        if (!newParams.iterator().hasNext()) {
            return this;
        }
        QueryParams updated = this.parameters.add(newParams);
        return withParameters(updated);
    }

    public URIBuilder addParameters(String name, String... values) {
        QueryParams updated = this.parameters.add(name, values);
        return withParameters(updated);
    }

    public URIBuilder addParameters(Map<String, List<String>> newParams) {
        if (newParams.isEmpty()) {
            return this;
        }
        QueryParams updated = this.parameters.add(newParams);
        return withParameters(updated);
    }

    public URIBuilder removeParameters(String name) {
        return withParameters(this.parameters.remove(name));
    }

    public URIBuilder replaceParameter(String name, String value) {
        return withParameters(this.parameters.set(name, value));
    }

    private String toPath(boolean encodepath) {
        if (path.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (Path pathElement : path) {
            if (builder.length() > 0) {
                builder.append("/");
            }
            builder.append(encodepath ? pathElement.getEncodedValue() : pathElement.getValue());
        }
        if ((wasPathAbsolute || host.isPresent()) && builder.length() > 1) {
            if (!"/".equals(builder.substring(0, 1))) {
                builder.insert(0, "/");                
            }
        }
        if (endsWithSlash) {
            builder.append("/");
        }
        return builder.toString();
    }

    public URI toURI() {
        return toURI(true, false, false);
    }

    public URI toNormalizedURI(boolean encodePath) {
        return toURI(encodePath, true, false).normalize();
    }

    public URI toNormalizedURI() {
        return toNormalizedURI(true);
    }

    /**
     * @return true if the scheme and host parts are not set.
     */
    public boolean isRelative() {
        return (!scheme.isPresent() && !host.isPresent());
    }

    public URI toAbsoluteURI() {
        return toURI(true, false, true);
    }

    private URI toURI(boolean encodePath, boolean sortQP, boolean absolutify) {
        try {
            if (isURN()) {
                return new URI(scheme.get(), schemeSpecificPart.get(), fragment.orElse(null));
            }
            StringBuilder sb = new StringBuilder();
            scheme.ifPresent(s ->{
                sb.append(s);
                sb.append("://");
            });
            host.ifPresent(sb::append);
            port.ifPresent(p -> sb.append(":").append(p));

            if (!path.isEmpty()) {
                String path = toPath(encodePath);
                if (absolutify && isRelative() && !path.startsWith("/")) {
                    path = "/" + path;
                }
                sb.append(path);
            }
            if (!parameters.isEmpty()) {
                sb.append("?");
                sb.append(parameters.toQuery(sortQP));
            }
            fragment.ifPresent(f -> {
                sb.append("#");
                sb.append(f);
            });
            return URI.create(sb.toString());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    
    public List<QueryParam> getParametersByName(final String name) {
        return parameters.getAsQueryParam(name);
    }

    public Optional<String> getFirstParameterValueByName(final String name) {
        return parameters.getFirst(name);
    }

    /**
     * Constructs a new URIBuilder from the given URI
     * @param uri the uri to use
     * @return a new URIBuilder which has the information from the URI.
     */
    public static URIBuilder fromURI(URI uri) {
        boolean pathAbsoluteness = uri.getPath() != null && uri.getPath().startsWith("/");
        boolean endsWithSlash = uri.getPath() != null && uri.getPath().endsWith("/");
        return new URIBuilder(
                Optional.ofNullable(uri.getScheme()),
                Optional.ofNullable(uri.getSchemeSpecificPart()),
                Optional.ofNullable(uri.getHost()),
                Optional.of(uri.getPort()).filter(p -> p != -1),
                toPathParts(uri.getPath()),
                Optional.ofNullable(uri.getFragment()),
                QueryParams.parse(uri.getRawQuery()),
                pathAbsoluteness,
                endsWithSlash
        );
    }

    /**
     * Constructs a new URIBuilder from the given URI
     * @param uri the uri to use
     * @return a new URIBuilder which has the information from the URI.
     */
    public static URIBuilder fromString(String uri) {
        return fromURI(URI.create(uri));
    }

    /**
     * Creates an empty URIBuilder.
     * @return an empty URIBuilder which result of {@link #toURI()} ()} will return "".
     */
    public static URIBuilder empty() {
        return new URIBuilder(Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<Integer>empty(), Collections.<Path>emptyList(), Optional.<String>empty(), new QueryParams(), false, false);
    }

    public Optional<String> getScheme() {
        return scheme;
    }

    public Optional<String> getHost() {
        return host;
    }

    public Optional<Integer> getPort() {
        return port;
    }

    public List<String> getPath() {
        return path.stream().map(pathToString).collect(Collectors.toList());
    }

    public List<String> getEncodedPath() {
        return path.stream().map(encodedPathToString).collect(Collectors.toList());
    }

    public String getCurrentPath() {
        return toPath(false);
    }

    public Optional<String> getFragment() {
        return fragment;
    }

    public QueryParams getParameters() {
        return parameters;
    }

    public Map<String, List<String>> getParametersAsMap() {
        return parameters.asMap();
    }

    private static List<Path> toPathParts(String path) {
        if (path == null) {
            return Collections.emptyList();
        }
        if (!path.contains("/")) {
            return Collections.singletonList(new Path(path));
        }
        else {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            List<String> stringList = Arrays.asList(path.split("/"));
            return stringList.stream().map(stringToPath).collect(Collectors.toList());
        }
    }

    private static Function<String, Path> stringToPath = Path::new;

    private static Function<Path, String> pathToString = Path::getValue;

    private static Function<Path, String> encodedPathToString = Path::getEncodedValue;

    private static class Path {
        private final String value;

        private Path(String value) {
            this.value = URIDecoder.decodeUTF8(value);
        }

        String getEncodedValue() {
            return URIEncoder.encodeUTF8(value);
        }

        String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
