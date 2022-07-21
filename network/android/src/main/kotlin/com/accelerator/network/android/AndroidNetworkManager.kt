package com.accelerator.network.android

import com.accelerator.network.android.engine.network.AndroidNetworkEngine
import com.accelerator.network.android.engine.network.AndroidTrustManager
import com.yml.network.core.NetworkManagerBuilder
import com.yml.network.core.parser.BasicDataParserFactory
import com.yml.network.core.parser.DataParser
import com.yml.network.core.parser.DataParserFactory

object AndroidNetworkManager {

    fun createBuilder(jsonDataParser: DataParser, androidTrustManager: AndroidTrustManager? = null) =
        createBuilder(BasicDataParserFactory.json(jsonDataParser), androidTrustManager)

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
