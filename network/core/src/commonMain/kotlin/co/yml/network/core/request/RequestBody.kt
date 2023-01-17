package co.yml.network.core.request

import co.yml.network.core.Headers
import co.yml.network.core.MimeType
import co.yml.network.core.constants.HeadersConstants
import co.yml.network.core.request.FormRequestBody.FormBodyEntry
import co.yml.network.core.request.MultiPartRequestBody.Part

/**
 * Class responsible for holding Network request body
 */
sealed class RequestBody

sealed class DataRequestBody : RequestBody()

/**
 * Class responsible for holding Network request body data and mimeType.
 *
 * @param data A POJO object holding the request data.
 * @param mimeType Optional MimeType
 *
 */
data class BasicRequestBody<DATA>(
    val data: DATA,
    val mimeType: MimeType = MimeType.JSON
) : DataRequestBody() {

    /**
     * @constructor
     *
     * @param data A POJO object holding the request data.
     * @param mimeType MimeType in string format
     */
    constructor(data: DATA, mimeType: String) : this(data, MimeType(mimeType))
}

/**
 * Class responsible for holding the Network request data for file upload
 *
 * @param filePath path to the file
 * @param filename original name of the file
 * @param mimeType mimeType of the file.
 * @param fileTransferProgressCallback Callback for getting file upload progress.
 */
data class FileRequestBody(
    val filePath: String,
    val filename: String? = null,
    val mimeType: MimeType? = null,
    val fileTransferProgressCallback: FileTransferProgressCallback? = null
) :
    DataRequestBody()

/**
 * Class responsible for holding form encoding data.
 *
 * @constructor
 * @param entries List of [FormBodyEntry] containing the form data.
 */
class FormRequestBody private constructor(val entries: List<FormBodyEntry>) : RequestBody() {
    data class FormBodyEntry(val name: String, val value: String, val isEncoded: Boolean)

    class Builder {
        private val entries = mutableListOf<FormBodyEntry>()

        fun add(name: String, value: String) = apply {
            entries.add(FormBodyEntry(name, value, isEncoded = false))
        }

        fun addEncoded(name: String, value: String) = apply {
            entries.add(FormBodyEntry(name, value, isEncoded = true))
        }

        fun build() = FormRequestBody(entries)
    }
}

/**
 * Class responsible for holding the Network request data for Multipart request.
 *
 * @constructor
 * @param parts List of [Part] containing data of individual part of multipart request.
 * @param type [MimeType] of the multipart request
 */
class MultiPartRequestBody private constructor(val parts: List<Part>, val type: MimeType) :
    RequestBody() {

    data class Part(val body: DataRequestBody, val headers: Headers?)

    class Builder(private val type: MimeType) {
        private val parts = mutableListOf<Part>()

        fun addPart(part: Part): Builder = apply { parts.add(part) }

        fun addPart(body: DataRequestBody, headers: Headers? = null): Builder {
            require(headers?.get(HeadersConstants.CONTENT_TYPE) === null) { "Unexpected header: Content-Type" }
            require(headers?.get(HeadersConstants.CONTENT_LENGTH) == null) { "Unexpected header: Content-Length" }
            return addPart(Part(body, headers))
        }

        fun addFormData(name: String, value: String) =
            addFormData(name, null, BasicRequestBody(value, MimeType.TEXT_PLAIN))

        fun addFormData(name: String, filename: String?, body: DataRequestBody): Builder {
            val disposition = buildString {
                append("form-data; name=")
                appendQuotedString(this, name)

                if (filename != null) {
                    append("; filename=")
                    appendQuotedString(this, filename)
                }
            }

            val headers = Headers().add(HeadersConstants.CONTENT_DISPOSITION, disposition)
            return addPart(body, headers)
        }

        fun build() = MultiPartRequestBody(parts, type)

        /**
         * Append the quoted strings by encoding them.
         *
         * @see <a href="https://www.w3.org/TR/html4/interact/forms.html#h-17.13.4">https://www.w3.org/TR/html4/interact/forms.html#h-17.13.4</a>
         */
        private fun appendQuotedString(builder: StringBuilder, key: String) {
            builder.append('"')
            for (ch in key) {
                when (ch) {
                    '\n' -> builder.append("%0A")
                    '\r' -> builder.append("%0D")
                    '"' -> builder.append("%22")
                    else -> builder.append(ch)
                }
            }
            builder.append('"')
        }
    }
}
