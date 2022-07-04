package com.yml.chat

import android.content.Context
import android.util.Log
import com.accelerator.network.android.engine.cache.AndroidCacheEngine
import com.accelerator.network.android.engine.network.AndroidNetworkEngine
import com.accelerator.network.core.NetworkManager
import com.accelerator.network.core.NetworkManagerBuilder
import com.accelerator.network.core.engine.cache.CacheEngine
import com.accelerator.network.core.engine.network.NetworkEngine
import com.accelerator.network.core.parser.BasicDataParserFactory
import com.yml.chat.data.parser.JsonDataParser
import com.yml.chat.logger.LoggerInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.UUID
import javax.inject.Singleton

const val CACHE_SIZE = 10L * 1024 * 1024 // 10 MB
const val BASE_URL = "https://accelerator-theia.herokuapp.com/"

val TAG = ApplicationModule::class.simpleName

@InstallIn(SingletonComponent::class)
@Module
object ApplicationModule {

    /**
     * Object containing all the dependencies that will be replaced with fake/mock implementation
     * during the tests.
     */
    @InstallIn(SingletonComponent::class)
    @Module
    object Fakes {
        @Provides
        @Singleton
        fun provideNetworkEngine(): NetworkEngine = AndroidNetworkEngine()

        @Provides
        @Singleton
        fun provideCacheEngine(@ApplicationContext appContext: Context): CacheEngine =
            AndroidCacheEngine(
                CACHE_SIZE,
                File(appContext.cacheDir, "httpCache"),
                1
            )

        @Provides
        @Singleton
        fun provideUniqueIdGenerator(): UniqueIdGenerator = object : UniqueIdGenerator {
            override fun generateId(): String = UUID.randomUUID().toString()
        }
    }

    @Provides
    @Singleton
    fun getJsonDataParser(): JsonDataParser = JsonDataParser()

    @Provides
    @Singleton
    fun provideNetworkManager(
        networkEngine: NetworkEngine,
        cacheEngine: CacheEngine,
        jsonDataParser: JsonDataParser,
        fileTransferManager: FileTransferManager
    ): NetworkManager {
        return NetworkManagerBuilder(networkEngine, BasicDataParserFactory.json(jsonDataParser))
            .setCacheEngine(cacheEngine)
            .setBasePath(BASE_URL).setInterceptors(listOf(LoggerInterceptor()))
            .setFileTransferProgressCallback { fileInfo, bytesTransferredResource ->
                Log.d(TAG, "$fileInfo $bytesTransferredResource")
                fileTransferManager.onUpdate(fileInfo, bytesTransferredResource)
            }
            .build()
    }
}
