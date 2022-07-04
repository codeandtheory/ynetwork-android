package com.accelerator.network.android

import com.accelerator.network.android.engine.network.AndroidNetworkEngine
import com.accelerator.network.android.engine.network.AndroidTrustManager
import com.accelerator.network.core.NetworkManagerBuilder
import com.accelerator.network.core.parser.BasicDataParserFactory
import com.accelerator.network.core.parser.DataParser
import com.accelerator.network.core.parser.DataParserFactory

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
