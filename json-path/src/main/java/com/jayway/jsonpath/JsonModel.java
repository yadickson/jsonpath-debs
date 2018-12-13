/*
 * Copyright 2011 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.jsonpath;

import com.jayway.jsonpath.internal.JsonFormatter;
import com.jayway.jsonpath.internal.JsonReader;
import com.jayway.jsonpath.internal.PathToken;
import com.jayway.jsonpath.internal.Utils;
import com.jayway.jsonpath.spi.JsonProvider;
import com.jayway.jsonpath.spi.JsonProviderFactory;
import com.jayway.jsonpath.spi.MappingProviderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static java.util.Arrays.asList;
import static com.jayway.jsonpath.internal.Utils.*;

/**
 * A JsonModel holds a parsed JSON document and provides easy read and write operations. In contrast to the
 * static read operations provided by {@link JsonPath} a JsonModel will only parse the document once.
 *
 * @author Kalle Stenflo
 */
public class JsonModel {


    private static final JsonPath JSON_PATH_ROOT = JsonPath.compile("$");
    private Object jsonObject;
    private Configuration configuration;

    // --------------------------------------------------------
    //
    // Constructors
    //
    // --------------------------------------------------------
    private JsonModel(Object jsonObject, Configuration configuration) {
        notNull(jsonObject, "json can not be null");
        notNull(configuration, "configuration can not be null");

        if (!configuration.getProvider().isContainer(jsonObject)) {
            throw new IllegalArgumentException("Invalid container object");
        }
        this.configuration = configuration;
        this.jsonObject = jsonObject;
    }





    /**
     * Check if this JsonModel is holding a JSON array as to object
     *
     * @return true if root is an array
     */
    public boolean isList() {
        return configuration.getProvider().isArray(jsonObject);
    }

    /**
     * Check if this JsonModel is holding a JSON object as to object
     *
     * @return true if root is an object
     */
    public boolean isMap() {
        return configuration.getProvider().isMap(jsonObject);
    }

    /**
     * Prints this JsonModel to standard out
     */
    public void print() {
        String json = toJson();
        System.out.println(JsonFormatter.prettyPrint(json));
    }

    /**
     * Check if this JsonModel has the given definite path
     *
     * @param jsonPath path to check
     * @return true if model contains path
     * @see com.jayway.jsonpath.JsonPath#isPathDefinite()
     */
    public boolean hasPath(String jsonPath) {
        return hasPath(JsonPath.compile(jsonPath));
    }

    /**
     * Check if this JsonModel has the given definite path
     *
     * @param jsonPath path to check
     * @return true if model contains path
     * @see com.jayway.jsonpath.JsonPath#isPathDefinite()
     */
    public boolean hasPath(JsonPath jsonPath) {

        isTrue(jsonPath.isPathDefinite(), "hasPath can only be used for definite paths");

        try {
            jsonPath.read(jsonObject, configuration.options(Option.THROW_ON_MISSING_PROPERTY));
        } catch (PathNotFoundException e) {
            return false;
        }
        return true;
    }

    // --------------------------------------------------------
    //
    // Getters
    //
    // --------------------------------------------------------

    /**
     * Returns the root object of this JsonModel
     *
     * @return returns the root object
     */
    public Object getJsonObject() {
        return this.jsonObject;
    }

    // --------------------------------------------------------
    //
    // Model readers
    //
    // --------------------------------------------------------

    /**
     * Reads the given path from this JsonModel. Filters is a way to problematically filter the contents of a list.
     * Instead of writing the filter criteria directly inside the JsonPath expression the filter is indicated and
     * provided as an argument.
     * <p/>
     * All three statements below are equivalent
     * <p/>
     * <code>
     * JsonModel model = JsonModel.model(myJson);
     * <p/>
     * //A
     * List<String> books = model.read("$store.book[?(@author == 'Nigel Rees')]");
     * <p/>
     * //B
     * List<String> books = model.read("$store.book[?]", filter(where("author").is("Nigel Rees"));
     * <p/>
     * //C
     * JsonPath path = JsonPath.compile("$store.book[?]", filter(where("author").is("Nigel Rees"));
     * <p/>
     * List<String> books = model.read(path);
     * <p/>
     * </code>
     * <p/>
     * The filters are applied in the order they are provided. If a path contains multiple [?] filter markers
     * the filters must be passed in the correct order.
     *
     * @param jsonPath the path to read
     * @param filters  filters to use in the path
     * @param <T>      expected return type
     * @return the json path result
     * @see Filter
     * @see Criteria
     */
    @SuppressWarnings({"unchecked"})
    public <T> T get(String jsonPath, Filter... filters) {
        return get(JsonPath.compile(jsonPath, filters));
    }

    /**
     * Reads the given path from this JsonModel.
     *
     * @param jsonPath the path to read
     * @param <T>      expected return type
     * @return the json path result
     */
    @SuppressWarnings({"unchecked"})
    public <T> T get(JsonPath jsonPath) {
        notNull(jsonPath, "jsonPath can not be null");

        return jsonPath.read(jsonObject, configuration);
    }

    // --------------------------------------------------------
    //
    // Model writers
    //
    // --------------------------------------------------------

    /**
     * Gets an {@link ArrayOps} for this JsonModel. Note that the root element of this model
     * must be a json array.
     *
     * @return array operations for this JsonModel
     */
    public ArrayOps opsForArray() {
        isTrue(configuration.getProvider().isArray(jsonObject), "This JsonModel is not a JSON array");
        return opsForArray(JSON_PATH_ROOT);
    }

    /**
     * Gets an {@link ArrayOps} for the array inside this JsonModel identified by the given JsonPath. The path must
     * be definite ({@link com.jayway.jsonpath.JsonPath#isPathDefinite()}).
     * <p/>
     * Note that the element returned by the given path must be a json array.
     *
     * @param jsonPath definite path to array to perform operations on
     * @return array operations for the targeted array
     */
    public ArrayOps opsForArray(String jsonPath) {
        return opsForArray(JsonPath.compile(jsonPath));
    }

    /**
     * Gets an {@link ArrayOps} for the array inside this JsonModel identified by the given JsonPath. The path must
     * be definite ({@link com.jayway.jsonpath.JsonPath#isPathDefinite()}).
     * <p/>
     * Note that the element returned by the given path must be a json array.
     *
     * @param jsonPath definite path to array to perform operations on
     * @return array operations for the targeted array
     */
    public ArrayOps opsForArray(JsonPath jsonPath) {
        notNull(jsonPath, "jsonPath can not be null");
        return new DefaultArrayOps(jsonPath);
    }

    /**
     * Gets an {@link ObjectOps} for this JsonModel. Note that the root element of this model
     * must be a json object.
     *
     * @return object operations for this JsonModel
     */
    public ObjectOps opsForObject() {
        return opsForObject(JSON_PATH_ROOT);
    }

    /**
     * Gets an {@link ObjectOps} for the object inside this JsonModel identified by the given JsonPath. The path must
     * be definite ({@link com.jayway.jsonpath.JsonPath#isPathDefinite()}).
     * <p/>
     * Note that the element returned by the given path must be a json object.
     *
     * @param jsonPath definite path to object to perform operations on
     * @return object operations for the targeted object
     */
    public ObjectOps opsForObject(String jsonPath) {
        return opsForObject(JsonPath.compile(jsonPath));
    }

    /**
     * Gets an {@link ObjectOps} for the object inside this JsonModel identified by the given JsonPath. The path must
     * be definite ({@link com.jayway.jsonpath.JsonPath#isPathDefinite()}).
     * <p/>
     * Note that the element returned by the given path must be a json object.
     *
     * @param jsonPath definite path to object to perform operations on
     * @return object operations for the targeted object
     */
    public ObjectOps opsForObject(JsonPath jsonPath) {
        notNull(jsonPath, "jsonPath can not be null");
        return new DefaultObjectOps(jsonPath);
    }

    // --------------------------------------------------------
    //
    // JSON extractors
    //
    // --------------------------------------------------------

    /**
     * Creates a JSON representation of this JsonModel
     *
     * @return model as Json
     */
    public String toJson() {
        return toJson(false);
    }


    /**
     * Creates a JSON representation of this JsonModel
     *
     * @param prettyPrint if the model should be pretty printed
     * @return
     */
    public String toJson(boolean prettyPrint) {
        String json = configuration.getProvider().toJson(jsonObject);
        if (prettyPrint)
            return JsonFormatter.prettyPrint(json);
        else
            return json;
    }

    /**
     * Creates a JSON representation of the result of the provided JsonPath
     *
     * @return path result as Json
     */
    public String toJson(String jsonPath, Filter... filters) {
        return toJson(JsonPath.compile(jsonPath, filters));
    }

    /**
     * Creates a JSON representation of the result of the provided JsonPath
     *
     * @return path result as Json
     */
    public String toJson(JsonPath jsonPath) {
        notNull(jsonPath, "jsonPath can not be null");

        return configuration.getProvider().toJson(get(jsonPath));
    }

    // --------------------------------------------------------
    //
    // Sub model readers
    //
    // --------------------------------------------------------

    /**
     * Returns a sub model from this JsonModel. A sub model can be any JSON object or JSON array
     * addressed by a definite path. In contrast to a detached model changes on the sub model
     * will be applied on the source model (the JsonModel from which the sub model was created)
     *
     * @param jsonPath the absolute path to extract a JsonModel for
     * @return the new JsonModel
     * @see com.jayway.jsonpath.JsonPath#isPathDefinite()
     */
    public JsonModel getSubModel(String jsonPath) {
        return getSubModel(JsonPath.compile(jsonPath));
    }

    /**
     * Returns a sub model from this JsonModel. A sub model can be any JSON object or JSON array
     * addressed by a definite path. In contrast to a detached model changes on the sub model
     * will be applied on the source model (the JsonModel from which the sub model was created)
     *
     * @param jsonPath the absolute path to extract a JsonModel for
     * @return the new JsonModel
     * @see com.jayway.jsonpath.JsonPath#isPathDefinite()
     */
    public JsonModel getSubModel(JsonPath jsonPath) {
        notNull(jsonPath, "jsonPath can not be null");

        isTrue(jsonPath.isPathDefinite(), "You can only get subModels with a definite path. Use getDetachedModel if path is not definite.");

        Object subModel = jsonPath.read(jsonObject);

        if (!configuration.getProvider().isContainer(subModel)) {
            throw new InvalidModelException("The path " + jsonPath.getPath() + " returned an invalid model " + (subModel != null ? subModel.getClass() : "null"));
        }

        return new JsonSubModel(subModel, this.configuration, this, jsonPath);
    }
    // --------------------------------------------------------
    //
    // Detached sub model readers
    //
    // --------------------------------------------------------

    /**
     * Creates a detached sub model from this JsonModel. A detached sub model does not have
     * to be created using a definite path. Changes on a detached sub model will not be reflected on the
     * source model (the JsonModel from which the sub model was created).
     *
     * @param jsonPath the absolute path to extract a JsonModel for
     * @param filters  filters to expand the path
     * @return a detached JsonModel
     */
    public JsonModel getSubModelDetached(String jsonPath, Filter... filters) {
        return getSubModelDetached(JsonPath.compile(jsonPath, filters));
    }

    /**
     * Creates a detached sub model from this JsonModel. A detached sub model does not have
     * to be created using a definite path. Changes on a detached sub model will not be reflected on the
     * source model (the JsonModel from which the sub model was created).
     *
     * @param jsonPath the absolute path to extract a JsonModel for
     * @return a detached JsonModel
     */
    public JsonModel getSubModelDetached(JsonPath jsonPath) {
        notNull(jsonPath, "jsonPath can not be null");

        Object subModel = jsonPath.read(jsonObject, configuration);

        if (!configuration.getProvider().isContainer(subModel)) {
            throw new InvalidModelException("The path " + jsonPath.getPath() + " returned an invalid model " + (subModel != null ? subModel.getClass() : "null"));
        }

        subModel = configuration.getProvider().clone(subModel);

        return new JsonModel(subModel, configuration);
    }

    // --------------------------------------------------------
    //
    // Mapping model readers
    //
    // --------------------------------------------------------

    /**
     * Returns a {@link MappingModelReader} for this JsonModel. Note that to use this functionality you need
     * an optional dependencies on your classpath (jackson-mapper-asl ver >= 1.9.5)
     *
     * @return a object mapper
     */
    public MappingModelReader map() {
        return new DefaultMappingModelReader(this.jsonObject);
    }

    /**
     * Returns a {@link MappingModelReader} for the JsonModel targeted by the provided {@link JsonPath}. Note that to use this functionality you need
     * an optional dependencies on your classpath (jackson-mapper-asl ver >= 1.9.5)
     *
     * @return a object mapper
     */
    public MappingModelReader map(String jsonPath, Filter... filters) {
        return map(JsonPath.compile(jsonPath, filters));
    }

    /**
     * Returns a {@link MappingModelReader} for the JsonModel targeted by the provided {@link JsonPath}. Note that to use this functionality you need
     * an optional dependencies on your classpath (jackson-mapper-asl ver >= 1.9.5)
     *
     * @return a object mapper
     */
    public MappingModelReader map(JsonPath jsonPath) {
        notNull(jsonPath, "jsonPath can not be null");

        return new DefaultMappingModelReader(JsonModel.this.get(jsonPath));
    }

    // --------------------------------------------------------
    //
    // Static factory methods
    //
    // --------------------------------------------------------

    /**
     * Creates a JsonModel
     *
     * @param jsonProvider JsonProvider to use
     * @param json         json string
     * @return a new JsonModel
     */
    public static JsonModel model(JsonProvider jsonProvider, String json) {
        notNull(jsonProvider, "jsonProvider can not be null");
        notNull(json, "jsonObject can not be null");
        return new JsonModel(new JsonReader(jsonProvider).parse(json).json(), Configuration.builder().jsonProvider(jsonProvider).build());
    }

    /**
     * Creates a JsonModel
     *
     * @param json json string
     * @return a new JsonModel
     */
    public static JsonModel model(String json) {
        return model(JsonProviderFactory.createProvider(), json);
    }

    public static JsonModel create(String json) {
        return model(json);
    }

    /**
     * Creates a JsonModel
     *
     * @param jsonProvider JsonProvider to use
     * @param jsonObject   a json container (a {@link Map} or a {@link List})
     * @return a new JsonModel
     */
    public static JsonModel model(JsonProvider jsonProvider, Object jsonObject) {
        notNull(jsonProvider, "jsonProvider can not be null");
        notNull(jsonObject, "jsonObject can not be null");
        return new JsonModel(jsonObject, Configuration.builder().jsonProvider(jsonProvider).build());
    }


    /**
     * Creates a JsonModel
     *
     * @param jsonObject a json container (a {@link Map} or a {@link List})
     * @return a new JsonModel
     */
    public static JsonModel model(Object jsonObject) {
        return model(JsonProviderFactory.createProvider(), jsonObject);
    }

    public static JsonModel create(Object jsonObject) {
        return model(jsonObject);
    }

    /**
     * Creates a JsonModel
     *
     * @param jsonProvider JsonProvider to use
     * @param url pointing to a Json document
     * @return a new JsonModel
     */
    public static JsonModel model(JsonProvider jsonProvider, URL url) throws IOException {
        notNull(jsonProvider, "jsonProvider can not be null");
        notNull(url, "url can not be null");

        return new JsonModel(new JsonReader(jsonProvider).parse(url).json(), Configuration.builder().jsonProvider(jsonProvider).build());
    }

    /**
     * Creates a JsonModel
     *
     * @param url pointing to a Json document
     * @return a new JsonModel
     */
    public static JsonModel model(URL url) throws IOException {
        return model(JsonProviderFactory.createProvider(), url);
    }

    public static JsonModel create(URL url) throws IOException {
        return model(url);
    }

    /**
     * Creates a JsonModel
     *
     * @param jsonProvider JsonProvider to use
     * @param jsonInputStream json document stream
     * @return a new JsonModel
     */
    public static JsonModel model(JsonProvider jsonProvider, InputStream jsonInputStream) throws IOException {
        notNull(jsonProvider, "jsonProvider can not be null");
        notNull(jsonInputStream, "jsonInputStream can not be null");

        return new JsonModel(new JsonReader(jsonProvider).parse(jsonInputStream).json(), Configuration.builder().jsonProvider(jsonProvider).build());
    }

    /**
     * Creates a JsonModel
     *
     * @param jsonInputStream json document stream
     * @return a new JsonModel
     */
    public static JsonModel model(InputStream jsonInputStream) throws IOException {
        return model(JsonProviderFactory.createProvider(), jsonInputStream);
    }

    public static JsonModel create(InputStream jsonInputStream) throws IOException {
        return model(jsonInputStream);
    }

    // --------------------------------------------------------
    //
    // Private helpers
    //
    // --------------------------------------------------------

    private <T> T getTargetObject(JsonPath jsonPath, Class<T> clazz) {
        notNull(jsonPath, "jsonPath can not be null");

        if (!jsonPath.isPathDefinite()) {
            throw new IndefinitePathException(jsonPath.getPath());
        }

        Object modelRef = jsonObject;

        if (jsonPath.getTokenizer().size() == 1) {
            PathToken onlyToken = jsonPath.getTokenizer().iterator().next();
            if ("$".equals(onlyToken.getFragment())) {
                return clazz.cast(modelRef);
            }
        } else {

            LinkedList<PathToken> tokens = jsonPath.getTokenizer().getPathTokens();

            PathToken currentToken;
            do {
                currentToken = tokens.poll();
                modelRef = currentToken.apply(modelRef, this.configuration);
            } while (!tokens.isEmpty());

            if (modelRef.getClass().isAssignableFrom(clazz)) {
                throw new InvalidModelException(jsonPath + " does nor refer to a Map but " + currentToken.getClass().getName());
            }
            return clazz.cast(modelRef);
        }
        throw new InvalidModelException();
    }

    private void setTargetObject(JsonPath jsonPath, Object newValue) {
        JsonPath setterPath = jsonPath.copy();
        PathToken pathToken = setterPath.getTokenizer().removeLastPathToken();


        if (pathToken.isRootToken()) {
            if (this instanceof JsonSubModel) {
                JsonSubModel thisModel = (JsonSubModel) this;

                thisModel.parent.setTargetObject(thisModel.subModelPath, newValue);
            } else {
                this.jsonObject = newValue;
            }
        } else {
            if (pathToken.isArrayIndexToken()) {
                int arrayIndex = pathToken.getArrayIndex();
                opsForArray(setterPath).set(arrayIndex, newValue);
            } else {
                opsForObject(setterPath).put(pathToken.getFragment(), newValue);
            }
        }
    }

    // --------------------------------------------------------
    //
    // Interfaces
    //
    // --------------------------------------------------------

    /**
     * Converts a {@link JsonModel} to an Object
     */
    public interface ObjectMappingModelReader {
        /**
         * Converts this JsonModel to the specified class using the configured {@link com.jayway.jsonpath.spi.MappingProvider}
         *
         * @param targetClass class to convert the {@link JsonModel} to
         * @param <T>         template class
         * @return the mapped model
         * @see MappingProviderFactory
         */
        <T> T to(Class<T> targetClass);
    }

    /**
     * Converts a {@link JsonModel} to an {@link Collection} of Objects
     */
    public interface ListMappingModelReader {
        /**
         * Converts this JsonModel to the a list of objects with the provided class using the configured {@link com.jayway.jsonpath.spi.MappingProvider}
         *
         * @param targetClass class to convert the {@link JsonModel} array items to
         * @param <T>         template class
         * @return the mapped mode
         */
        <T> List<T> of(Class<T> targetClass);

        /**
         * Syntactic sugar function to use with {@link ListMappingModelReader#of}
         */
        ListMappingModelReader toList();

        /**
         * Converts this JsonModel to the a {@link List} of objects with the provided class using the configured {@link com.jayway.jsonpath.spi.MappingProvider}
         *
         * @param targetClass class to convert the {@link JsonModel} array items to
         * @param <T>         template class
         * @return the mapped mode
         */
        <T> List<T> toListOf(Class<T> targetClass);

        /**
         * Converts this JsonModel to the a {@link Set} of objects with the provided class using the configured {@link com.jayway.jsonpath.spi.MappingProvider}
         *
         * @param targetClass class to convert the {@link JsonModel} array items to
         * @param <T>         template class
         * @return the mapped model
         */
        <T> Set<T> toSetOf(Class<T> targetClass);
    }

    /**
     * Object mapping interface used when for root object that can be either a {@link List} or a {@link Map}.
     * It's up to the invoker to know what the conversion target can be mapped to.
     */
    public interface MappingModelReader extends ListMappingModelReader, ObjectMappingModelReader {
    }

    /**
     * Operations that can be performed on Json objects ({@link Map}s)
     */
    public interface ObjectOps {

        /**
         * Returns the operation target
         *
         * @return the operation target
         */
        Map<String, Object> getTarget();

        /**
         * @see Map#containsKey(Object)
         */
        boolean containsKey(String key);

        /**
         * @see Map#put(Object, Object)
         */
        ObjectOps put(String key, Object value);

        /**
         * Adds the value to the target map if it is not already present
         *
         * @param key   the key
         * @param value the value
         * @return this {@link ObjectOps}
         */
        ObjectOps putIfAbsent(String key, Object value);

        /**
         * @see Map#get(Object)
         */
        Object get(String key);

        /**
         * Tries to convert the value associated with the key to an {@link Integer}
         *
         * @param key the key
         * @return converted value
         */
        Integer getInteger(String key);

        /**
         * Tries to convert the value associated with the key to an {@link Long}
         *
         * @param key the key
         * @return converted value
         */
        Long getLong(String key);

        /**
         * Tries to convert the value associated with the key to an {@link Double}
         *
         * @param key the key
         * @return converted value
         */
        Double getDouble(String key);

        /**
         * Tries to convert the value associated with the key to an {@link String}
         *
         * @param key the key
         * @return converted value
         */
        String getString(String key);

        /**
         * @see Map#putAll(java.util.Map)
         */
        ObjectOps putAll(Map<String, Object> map);

        /**
         * @see Map#remove(Object)
         */
        ObjectOps remove(String key);

        /**
         * Allows transformations of the target object. the target for this {@link ObjectOps} will be be replaced
         * with the {@link Object} returned by the {@link Transformer#transform(Object, Configuration)}
         *
         * @param transformer the transformer to use
         * @return this {@link ObjectOps}
         */
        ObjectOps transform(Transformer<Map<String, Object>> transformer);

        /**
         * Map the target of this {@link ObjectOps} to the provided class
         *
         * @param targetClass class to convert the target object to
         * @param <T>         template class
         * @return the mapped model
         */
        <T> T to(Class<T> targetClass);
    }

    /**
     * Operations that can be performed on Json arrays ({@link List}s)
     */
    public interface ArrayOps {

        /**
         * Returns the operation target
         *
         * @return the operation target
         */
        List<Object> getTarget();

        /**
         * @see List#add(Object)
         */
        ArrayOps add(Object o);

        /**
         * @see List#addAll(java.util.Collection)
         */
        ArrayOps addAll(Collection<Object> collection);

        /**
         * @see List#remove(int)
         */
        ArrayOps remove(Object o);

        /**
         * @see java.util.List#size()
         */
        int size();

        /**
         * @see List#set(int, Object)
         */
        ArrayOps set(int index, Object value);

        /**
         * Allows transformations of the target list. The target for this {@link ArrayOps} will be be replaced
         * with the {@link Object} returned by the {@link Transformer#transform(Object, Configuration)}
         *
         * @param transformer the transformer to use
         * @return this {@link ArrayOps}
         */
        ArrayOps transform(Transformer<List<Object>> transformer);


        ArrayOps each(Transformer<Object> transformer);

        /**
         * @see ListMappingModelReader
         */
        ListMappingModelReader toList();

        /**
         * @see ListMappingModelReader
         */
        <T> List<T> toListOf(Class<T> targetClass);

        /**
         * @see ListMappingModelReader
         */
        <T> Set<T> toSetOf(Class<T> targetClass);
    }

    private class DefaultObjectOps implements ObjectOps {

        private JsonPath jsonPath;

        private DefaultObjectOps(JsonPath jsonPath) {
            this.jsonPath = jsonPath;
        }

        @Override
        public Map<String, Object> getTarget() {
            return getTargetObject(jsonPath, Map.class);
        }

        @Override
        public boolean containsKey(String key) {
            return getTargetObject(jsonPath, Map.class).containsKey(key);
        }

        @Override
        public ObjectOps put(String key, Object value) {
            getTargetObject(jsonPath, Map.class).put(key, value);
            return this;
        }

        @Override
        public ObjectOps putIfAbsent(String key, Object value) {
            Map targetObject = getTargetObject(jsonPath, Map.class);
            if (!targetObject.containsKey(key)) {
                targetObject.put(key, value);
            }
            return this;
        }

        @Override
        public Object get(String key) {
            return getTargetObject(jsonPath, Map.class).get(key);
        }

        @Override
        public Integer getInteger(String key) {
            return Utils.toInt(get(key));
        }

        @Override
        public Long getLong(String key) {
            return Utils.toLong(get(key));
        }

        @Override
        public Double getDouble(String key) {
            return Utils.toDouble(get(key));
        }

        @Override
        public String getString(String key) {
            return Utils.toString(get(key));
        }

        @Override
        public ObjectOps putAll(Map<String, Object> map) {
            getTargetObject(jsonPath, Map.class).putAll(map);
            return this;
        }

        @Override
        public ObjectOps remove(String key) {
            getTargetObject(jsonPath, Map.class).remove(key);
            return this;
        }

        @Override
        public ObjectOps transform(Transformer<Map<String, Object>> transformer) {
            Map targetObject = getTargetObject(jsonPath, Map.class);
            Object transformed = transformer.transform(targetObject, configuration);
            setTargetObject(jsonPath, transformed);
            return this;
        }

        @Override
        public <T> T to(Class<T> targetClass) {
            Map targetObject = getTargetObject(jsonPath, Map.class);
            return new DefaultMappingModelReader(targetObject).to(targetClass);
        }
    }


    private class DefaultArrayOps implements ArrayOps {

        private JsonPath jsonPath;

        private DefaultArrayOps(JsonPath jsonPath) {
            this.jsonPath = jsonPath;
        }

        @Override
        public List<Object> getTarget() {
            return getTargetObject(jsonPath, List.class);
        }

        @Override
        public ArrayOps add(Object o) {
            getTargetObject(jsonPath, List.class).add(o);
            return this;
        }

        @Override
        public ArrayOps addAll(Collection<Object> collection) {
            getTargetObject(jsonPath, List.class).addAll(collection);
            return this;
        }

        @Override
        public ArrayOps remove(Object o) {
            getTargetObject(jsonPath, List.class).remove(o);
            return this;
        }

        @Override
        public int size() {
            return getTargetObject(jsonPath, List.class).size();
        }

        @Override
        public ArrayOps set(int index, Object value) {
            getTargetObject(jsonPath, List.class).set(index, value);
            return this;
        }

        @Override
        public ListMappingModelReader toList() {
            return new DefaultMappingModelReader(getTargetObject(jsonPath, List.class));
        }

        @Override
        public ArrayOps transform(Transformer<List<Object>> transformer) {
            Object transformed = transformer.transform(getTargetObject(jsonPath, List.class), configuration);
            setTargetObject(jsonPath, transformed);
            return this;
        }

        @Override
        public ArrayOps each(Transformer<Object> transformer) {

            List targetObject = getTargetObject(jsonPath, List.class);
            for (int i = 0; i < targetObject.size(); i++) {
                targetObject.set(i, transformer.transform(targetObject.get(i), configuration));
            }
            return this;
        }

        @Override
        public <T> List<T> toListOf(Class<T> targetClass) {
            return new DefaultMappingModelReader(getTargetObject(jsonPath, List.class)).toListOf(targetClass);
        }

        @Override
        public <T> Set<T> toSetOf(Class<T> targetClass) {
            return new DefaultMappingModelReader(getTargetObject(jsonPath, List.class)).toSetOf(targetClass);
        }
    }

    private static class DefaultMappingModelReader implements MappingModelReader {
        private Object model;

        private DefaultMappingModelReader(Object model) {
            this.model = model;
        }

        @Override
        public ListMappingModelReader toList() {
            return this;
        }

        @Override
        public <T> List<T> of(Class<T> targetClass) {
            return toListOf(targetClass);
        }

        @Override
        public <T> List<T> toListOf(Class<T> targetClass) {
            Object modelRef = model;
            if (!(modelRef instanceof List)) {
                modelRef = asList(modelRef);
            }
            return MappingProviderFactory.createProvider().convertValue(modelRef, List.class, targetClass);
        }

        @Override
        public <T> Set<T> toSetOf(Class<T> targetClass) {
            Object modelRef = model;
            if (!(modelRef instanceof List)) {
                Set setModel = new HashSet();
                setModel.add(model);
                modelRef = setModel;
            }
            return MappingProviderFactory.createProvider().convertValue(modelRef, Set.class, targetClass);
        }

        @Override
        public <T> T to(Class<T> targetClass) {
            return MappingProviderFactory.createProvider().convertValue(model, targetClass);
        }
    }

    private static class JsonSubModel extends JsonModel {

        private final JsonModel parent;
        private final JsonPath subModelPath;

        private JsonSubModel(Object jsonObject, Configuration configuration, JsonModel parent, JsonPath subModelPath) {
            super(jsonObject, configuration);
            this.parent = parent;
            this.subModelPath = subModelPath;
        }
    }
}
