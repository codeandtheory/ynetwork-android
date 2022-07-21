package com.yml.network.core.parser

import kotlin.reflect.KClass

/**
 * Class/Interface used for serializing/deserializing the data.
 */
interface DataParser {
    /**
     * Serialize a POJO data into String.
     *
     * @param data POJO object containing the data to serialize
     *
     * @return serialized string of the passed [data] object.
     * @throws [Exception] in case the serialization operation is failed.
     */
    fun <DATA : Any> serialize(data: DATA): String

    /**
     * Deserialize the String into specified POJO class object.
     *
     * @param data string data to deserialize into POJO object
     * @param kClass class reference to which the [data] needs to be deserialized.
     *
     * @return deserialized data from the [data] with the type of [kClass].
     * @throws [Exception] in case the deserialization operation is failed.
     */
    fun <DATA : Any> deserialize(data: String, kClass: KClass<DATA>): DATA
}
