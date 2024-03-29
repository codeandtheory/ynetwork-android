package co.yml.network.android.engine.cache.disklrucache

import co.yml.network.android.engine.cache.disklrucache.DiskLruCacheException
import co.yml.network.android.engine.cache.disklrucache.ErrorCode
import co.yml.network.android.engine.cache.disklrucache.StrictLineReader
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

const val streamData = """What is Lorem Ipsum?
Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.

Where does it come from?
Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words, consectetur, from a Lorem Ipsum passage, and going through the cites of the word in classical literature, discovered the undoubtable source. Lorem Ipsum comes from sections 1.10.32 and 1.10.33 of "de Finibus Bonorum et Malorum" (The Extremes of Good and Evil) by Cicero, written in 45 BC. This book is a treatise on the theory of ethics, very popular during the Renaissance. The first line of Lorem Ipsum, "Lorem ipsum dolor sit amet..", comes from a line in section 1.10.32.

The standard chunk of Lorem Ipsum used since the 1500s is reproduced below for those interested. Sections 1.10.32 and 1.10.33 from "de Finibus Bonorum et Malorum" by Cicero are also reproduced in their exact original form, accompanied by English versions from the 1914 translation by H. Rackham.
"""
val lines = streamData.split("\n")

class StrictLineReaderTest {

    @Test
    fun verifyReadLine() {
        val inputStream = ByteArrayInputStream(streamData.toByteArray())
        val reader = StrictLineReader(inputStream, StandardCharsets.US_ASCII)

        val firstLine = reader.readLine()
        val secondLine = reader.readLine()
        assertThat(firstLine, `is`(lines[0]))
        assertThat(secondLine, `is`(lines[1]))
    }

    @Test
    fun verifyReadLineWithLowBufferSize() {
        val inputStream = ByteArrayInputStream(streamData.toByteArray())
        val reader = StrictLineReader(inputStream, StandardCharsets.US_ASCII, 20 /* Bytes */)

        val firstLine = reader.readLine()
        val secondLine = reader.readLine()
        assertThat(firstLine, `is`(lines[0]))
        assertThat(secondLine, `is`(lines[1]))
    }

    @Test
    fun verifyReadLineAfterClosing() {
        val inputStream = ByteArrayInputStream(streamData.toByteArray())
        val reader = StrictLineReader(inputStream, StandardCharsets.US_ASCII, 20 /* Bytes */)

        reader.close()
        val exception = assertThrows<DiskLruCacheException> { reader.readLine() }
        assertThat(exception.errorCode, `is`(ErrorCode.LINEREADER_CLOSED))
        assertThat(
            exception.message,
            `is`("${ErrorCode.LINEREADER_CLOSED.type} - LineReader is closed")
        )
    }
}
