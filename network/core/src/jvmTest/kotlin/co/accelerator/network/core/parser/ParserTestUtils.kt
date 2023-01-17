package co.accelerator.network.core.parser

import co.yml.network.core.MimeType
import co.yml.network.core.parser.DataParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Base64
import kotlin.reflect.KClass
import kotlin.reflect.cast

class MockObjectParser : DataParser {
    override fun <DATA : Any> serialize(data: DATA): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val outputStream = ObjectOutputStream(byteArrayOutputStream)
        outputStream.writeObject(data)
        outputStream.close()
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }

    override fun <DATA : Any> deserialize(data: String, kClass: KClass<DATA>): DATA {
        val bytes: ByteArray = Base64.getDecoder().decode(data)
        val objectInputStream = ObjectInputStream(ByteArrayInputStream(bytes))
        val parsedData = kClass.cast(objectInputStream.readObject())
        objectInputStream.close()
        return parsedData
    }
}

val mockParserFactoryList = mapOf(MimeType.JSON.toString() to MockObjectParser())
