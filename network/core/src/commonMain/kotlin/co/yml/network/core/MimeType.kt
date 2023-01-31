package co.yml.network.core

data class MimeType(private val value: String) {

    override fun toString(): String = value

    companion object {
        // region common MIME types

        // Ref: https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
        val JSON = MimeType("application/json")
        val APPLICATION_XML = MimeType("application/xml")
        val TEXT_PLAIN = MimeType("text/plain")
        val TEXT_XML = MimeType("text/xml")

        val OCTET_STREAM = MimeType("application/octet-stream") // Any kind of Binary data

        /**
         * The media-type multipart/form-data follows the rules of all multipart MIME data streams as
         * outlined in RFC 2046. In forms, there are a series of fields to be supplied by the user who
         * fills out the form. Each field has a name. Within a given form, the names are unique.
         */
        val MULTIPART_FORM = MimeType("multipart/form-data")

        /**
         * The "mixed" subtype of "multipart" is intended for use when the body parts are independent
         * and need to be bundled in a particular order. Any "multipart" subtypes that an implementation
         * does not recognize must be treated as being of subtype "mixed".
         */
        val MULTIPART_MIXED = MimeType("multipart/mixed")

        /**
         * The "multipart/alternative" type is syntactically identical to "multipart/mixed", but the
         * semantics are different. In particular, each of the body parts is an "alternative" version of
         * the same information.
         */
        val MULTIPART_ALTERNATIVE = MimeType("multipart/alternative")

        /**
         * This type is syntactically identical to "multipart/mixed", but the semantics are different.
         * In particular, in a digest, the default `Content-Type` value for a body part is changed from
         * "text/plain" to "message/rfc822".
         */
        val MULTIPART_DIGEST = MimeType("multipart/digest")

        /**
         * This type is syntactically identical to "multipart/mixed", but the semantics are different.
         * In particular, in a parallel entity, the order of body parts is not significant.
         */
        val MULTIPART_PARALLEL = MimeType("multipart/parallel")
        // endregion
    }
}
