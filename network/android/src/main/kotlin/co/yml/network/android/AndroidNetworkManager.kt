package co.yml.network.android

import co.yml.network.android.engine.network.AndroidNetworkEngine
import co.yml.network.android.engine.network.AndroidTrustManager
import co.yml.network.core.NetworkManagerBuilder
import co.yml.network.core.parser.BasicDataParserFactory
import co.yml.network.core.parser.DataParser
import co.yml.network.core.parser.DataParserFactory
object AndroidNetworkManager {


    @JvmName("createBuilder")
    fun createBuilder(jsonDataParser: DataParser, androidTrustManager: AndroidTrustManager? = null) =
        createBuilder(BasicDataParserFactory.json(jsonDataParser), androidTrustManager)
    @JvmName("createBuilder1")
    fun createBuilder(
        dataParserFactory: DataParserFactory,
        androidTrustManager: AndroidTrustManager? = null
    ) = createManagerBuilder(
        dataParserFactory,
        androidTrustManager
    )

    private fun createManagerBuilder(
        dataParserFactory: DataParserFactory,
        androidTrustManager: AndroidTrustManager?,
    ) = NetworkManagerBuilder(
        networkEngine = AndroidNetworkEngine(androidTrustManager),
        dataParserFactory = dataParserFactory
    )
}
