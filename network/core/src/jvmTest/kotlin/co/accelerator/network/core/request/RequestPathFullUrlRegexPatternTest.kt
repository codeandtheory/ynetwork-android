package co.accelerator.network.core.request

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

private val validProtocols = listOf("", "http://", "https://")
private val invalidProtocols =
    listOf("ftp", "ws", "wss", "http", "https").map { listOf("$it:", "$it:/") }.flatten() +
            listOf("ftp://", "ws://", "wss://")

private val validDomains = listOf(
    "www.google.com",
    "google.com",
    "google.co.in",
    "demo.google.co.in",
    "g.dev",    // Official Google developers website
    "www1.my-custom-website.com",
    "www2.my-custom-website.com",
    "www3.my-custom-website.com",
    "demo123.com",
    "www.demo123.com",
    "123demo.com",
    "www.123demo.com"
)
private val invalidDomains = listOf("", "www", "www..google.com")

private val validUrlPaths =
    listOf("/", "/users", "/users/", "/ugc_123", "/ugc_123/", "/#/users", "/#/users/", "/123/")
private val invalidUrlPaths = listOf("//")

private val validAnchors = listOf("", "#abc", "#abc123", "#123", "#abc_123", "#abc-123")
private val invalidAnchors = listOf("# ", "#abc def")

private val validQueryParams =
    listOf("", "?abc=123", "?abc=def", "?ab=cd&de=fg", "?ab=cd%20ef", "?ab=cd_ef")
private val invalidQueryParams = listOf("?abc", "?abc=def=ghi", "?abc = cd ed", "?abc=cd ef")

class RequestPathFullUrlRegexPatternTest {

    companion object {
        @JvmStatic
        fun validUrlData(): Stream<Arguments> {
            val urlDataList = mutableListOf<Arguments>()
            addProtocolDomainUrls(urlDataList, validProtocols, validDomains)
            addUrlData(
                urlDataList,
                validProtocols,
                validDomains,
                validUrlPaths,
                validQueryParams,
                validAnchors
            )
            return urlDataList.stream()
        }

        @JvmStatic
        fun invalidUrlData(): Stream<Arguments> {
            val urlDataList = mutableListOf<Arguments>()
            addProtocolDomainUrls(urlDataList, invalidProtocols, validDomains)
            addProtocolDomainUrls(urlDataList, validProtocols, invalidDomains)
            addProtocolDomainUrls(urlDataList, invalidProtocols, invalidDomains)
            addUrlData(
                urlDataList,
                invalidProtocols,
                invalidDomains,
                invalidUrlPaths,
                invalidQueryParams,
                invalidAnchors
            )
            addUrlData(
                urlDataList,
                invalidProtocols,
                validDomains,
                validUrlPaths,
                validQueryParams,
                validAnchors
            )
            addUrlData(
                urlDataList,
                validProtocols,
                invalidDomains,
                validUrlPaths,
                validQueryParams,
                validAnchors
            )
            addUrlData(
                urlDataList,
                validProtocols,
                validDomains,
                invalidUrlPaths,
                validQueryParams,
                validAnchors
            )
            addUrlData(
                urlDataList,
                validProtocols,
                validDomains,
                validUrlPaths,
                invalidQueryParams,
                validAnchors
            )
            addUrlData(
                urlDataList,
                validProtocols,
                validDomains,
                validUrlPaths,
                validQueryParams,
                invalidAnchors
            )
            return urlDataList.stream()
        }

        @JvmStatic
        fun validQueryParams(): Stream<Arguments> =
            validQueryParams.filter { it !== "" }.map { Arguments.of(it) }.stream()

        @JvmStatic
        fun invalidQueryParams(): Stream<Arguments> =
            invalidQueryParams.map { Arguments.of(it) }.stream()

        private fun addProtocolDomainUrls(
            urlDataList: MutableList<Arguments>,
            protocols: List<String>,
            domains: List<String>
        ) {
            protocols.forEach { protocol ->
                domains.forEach { domain ->
                    urlDataList.add(Arguments.of("$protocol$domain"))
                }
            }
        }

        private fun addUrlData(
            urlDataList: MutableList<Arguments>,
            protocols: List<String>,
            domains: List<String>,
            urlPaths: List<String>,
            queryParams: List<String>,
            anchors: List<String>
        ) {
            protocols.getOrNull(0)?.let { protocol ->
                domains.getOrNull(0)?.let { domain ->
                    urlPaths.forEach { path ->
                        queryParams.forEach { queryParam ->
                            anchors.forEach { anchor ->
                                urlDataList.add(Arguments.of("$protocol$domain$path$anchor$queryParam"))
                                urlDataList.add(Arguments.of("$protocol$domain$path$queryParam$anchor"))
                            }
                        }
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("validUrlData")
    fun verifyValidUrls(url: String) {
        assertThat(
            "Valid URL : $url is marked as invalid URL.",
            url.matches(co.yml.network.core.request.fullURLRegexPattern),
            `is`(true)
        )
    }

    @ParameterizedTest
    @MethodSource("invalidUrlData")
    fun verifyInvalidUrls(url: String) {
        assertThat(
            "Invalid URL : $url is marked as valid URL.",
            url.matches(co.yml.network.core.request.fullURLRegexPattern),
            `is`(false)
        )
    }

    @ParameterizedTest
    @MethodSource("validQueryParams")
    fun verifyValidQueryParams(queryParam: String) {
        assertThat(
            "Valid Query param : $queryParam is marked as invalid Query param.",
            queryParam.matches(co.yml.network.core.request.queryParamsRegexPattern),
            `is`(true)
        )
    }

    @ParameterizedTest
    @MethodSource("invalidQueryParams")
    fun verifyInvalidQueryParams(queryParam: String) {
        assertThat(
            "Invalid Query param : $queryParam is marked as valid Query param.",
            queryParam.matches(co.yml.network.core.request.queryParamsRegexPattern),
            `is`(false)
        )
    }
}
