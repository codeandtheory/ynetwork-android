package co.yml.chat.data.parser

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.util.LinkedHashMap

/**
 * This class is copy of GSON-Extra RuntimeTypeAdapterFactory, as GSON-Extra are not provided in the maven repository, we have to copy the required classes manually.
 *
 * Class Ref: https://github.com/google/gson/blob/master/extras/src/main/java/com/google/gson/typeadapters/RuntimeTypeAdapterFactory.java
 */

/**
 * Adapts values whose runtime type may differ from their declaration type. This
 * is necessary when a field's type is not the same type that GSON should create
 * when deserializing that field. For example, consider these types:
 * <pre>   `abstract class Shape {
 * int x;
 * int y;
 * }
 * class Circle extends Shape {
 * int radius;
 * }
 * class Rectangle extends Shape {
 * int width;
 * int height;
 * }
 * class Diamond extends Shape {
 * int width;
 * int height;
 * }
 * class Drawing {
 * Shape bottomShape;
 * Shape topShape;
 * }
`</pre> *
 *
 * Without additional type information, the serialized JSON is ambiguous. Is
 * the bottom shape in this drawing a rectangle or a diamond? <pre>   `{
 * "bottomShape": {
 * "width": 10,
 * "height": 5,
 * "x": 0,
 * "y": 0
 * },
 * "topShape": {
 * "radius": 2,
 * "x": 4,
 * "y": 1
 * }
 * }`</pre>
 * This class addresses this problem by adding type information to the
 * serialized JSON and honoring that type information when the JSON is
 * deserialized: <pre>   `{
 * "bottomShape": {
 * "type": "Diamond",
 * "width": 10,
 * "height": 5,
 * "x": 0,
 * "y": 0
 * },
 * "topShape": {
 * "type": "Circle",
 * "radius": 2,
 * "x": 4,
 * "y": 1
 * }
 * }`</pre>
 * Both the type field name (`"type"`) and the type labels (`"Rectangle"`) are configurable.
 *
 * <h3>Registering Types</h3>
 * Create a `RuntimeTypeAdapterFactory` by passing the base type and type field
 * name to the [.of] factory method. If you don't supply an explicit type
 * field name, `"type"` will be used. <pre>   `RuntimeTypeAdapterFactory<Shape> shapeAdapterFactory
 * = RuntimeTypeAdapterFactory.of(Shape.class, "type");
`</pre> *
 * Next register all of your subtypes. Every subtype must be explicitly
 * registered. This protects your application from injection attacks. If you
 * don't supply an explicit type label, the type's simple name will be used.
 * <pre>   `shapeAdapterFactory.registerSubtype(Rectangle.class, "Rectangle");
 * shapeAdapterFactory.registerSubtype(Circle.class, "Circle");
 * shapeAdapterFactory.registerSubtype(Diamond.class, "Diamond");
`</pre> *
 * Finally, register the type adapter factory in your application's GSON builder:
 * <pre>   `Gson gson = new GsonBuilder()
 * .registerTypeAdapterFactory(shapeAdapterFactory)
 * .create();
`</pre> *
 * Like `GsonBuilder`, this API supports chaining: <pre>   `RuntimeTypeAdapterFactory<Shape> shapeAdapterFactory = RuntimeTypeAdapterFactory.of(Shape.class)
 * .registerSubtype(Rectangle.class)
 * .registerSubtype(Circle.class)
 * .registerSubtype(Diamond.class);
`</pre> *
 *
 * <h3>Serialization and deserialization</h3>
 * In order to serialize and deserialize a polymorphic object,
 * you must specify the base type explicitly.
 * <pre>   `Diamond diamond = new Diamond();
 * String json = gson.toJson(diamond, Shape.class);
`</pre> *
 * And then:
 * <pre>   `Shape shape = gson.fromJson(json, Shape.class);
`</pre> *
 */
class RuntimeTypeAdapterFactory<T> private constructor(
    private val baseType: Class<T>,
    private val typeFieldName: String,
    private val maintainType: Boolean
) : TypeAdapterFactory {
    private val labelToSubtype: MutableMap<String, Class<out T>> = LinkedHashMap()
    private val subtypeToLabel: MutableMap<Class<out T>, String> = LinkedHashMap()


    /**
     * Registers `type` identified by `label`. Labels are case
     * sensitive.
     *
     * @throws IllegalArgumentException if either `type` or `label`
     * have already been registered on this type adapter.
     */
    /**
     * Registers `type` identified by its [simple][Class.getSimpleName]. Labels are case sensitive.
     *
     * @throws IllegalArgumentException if either `type` or its simple name
     * have already been registered on this type adapter.
     */
    fun registerSubtype(
        type: Class<out T>,
        label: String = type.simpleName
    ): RuntimeTypeAdapterFactory<T> {
        if (subtypeToLabel.containsKey(type) || labelToSubtype.containsKey(label)) {
            throw IllegalArgumentException("types and labels must be unique")
        }
        labelToSubtype[label] = type
        subtypeToLabel[type] = label
        return this
    }

    override fun <R> create(gson: Gson, type: TypeToken<R>): TypeAdapter<R>? {
        if (type.rawType != baseType) {
            return null
        }
        val jsonElementAdapter = gson.getAdapter(
            JsonElement::class.java
        )
        val labelToDelegate: MutableMap<String, TypeAdapter<*>> = LinkedHashMap()
        val subtypeToDelegate: MutableMap<Class<*>, TypeAdapter<*>> = LinkedHashMap()
        for (entry: Map.Entry<String, Class<*>> in labelToSubtype.entries) {
            val delegate = gson.getDelegateAdapter(this, TypeToken.get(entry.value))
            labelToDelegate[entry.key] = delegate
            subtypeToDelegate[entry.value] = delegate
        }
        return object : TypeAdapter<R>() {

            @Throws(IOException::class)
            override fun read(reader: JsonReader): R {
                val jsonElement = jsonElementAdapter.read(reader)
                val labelJsonElement: JsonElement? =
                    if (maintainType) jsonElement.asJsonObject[typeFieldName]
                    else jsonElement.asJsonObject.remove(typeFieldName)

                if (labelJsonElement == null) {
                    throw JsonParseException("cannot deserialize $baseType because it does not define a field named $typeFieldName")
                }
                val label = labelJsonElement.asString
                val delegate = labelToDelegate[label] as TypeAdapter<R>?
                    ?: throw JsonParseException("cannot deserialize $baseType subtype named $label; did you forget to register a subtype?")
                return delegate.fromJsonTree(jsonElement)
            }

            @Throws(IOException::class)
            override fun write(out: JsonWriter, value: R) {
                val srcType: Class<*> = value!!::class.java
                val label = subtypeToLabel[srcType]
                val delegate = subtypeToDelegate[srcType] as TypeAdapter<R>?
                    ?: throw JsonParseException("cannot serialize ${srcType.name}; did you forget to register a subtype?")

                val jsonObject = delegate.toJsonTree(value).asJsonObject
                if (maintainType) {
                    jsonElementAdapter.write(out, jsonObject)
                    return
                }
                val clone = JsonObject()
                if (jsonObject.has(typeFieldName)) {
                    throw JsonParseException("cannot serialize ${srcType.name} because it already defines a field named $typeFieldName")
                }
                clone.add(typeFieldName, JsonPrimitive(label))
                for (e: Map.Entry<String?, JsonElement?> in jsonObject.entrySet()) {
                    clone.add(e.key, e.value)
                }
                jsonElementAdapter.write(out, clone)
            }
        }.nullSafe()
    }

    companion object {
        /**
         * Creates a new runtime type adapter using for `baseType` using `typeFieldName` as the type field name. Type field names are case sensitive.
         * `maintainType` flag decide if the type will be stored in pojo or not.
         */
        fun <T> of(
            baseType: Class<T>,
            typeFieldName: String = "type",
            maintainType: Boolean = false
        ): RuntimeTypeAdapterFactory<T> {
            return RuntimeTypeAdapterFactory(baseType, typeFieldName, maintainType)
        }
    }
}