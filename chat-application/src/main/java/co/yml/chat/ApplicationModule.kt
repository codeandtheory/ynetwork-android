package co.yml.chat

import android.content.Context
import android.util.Log
import co.yml.network.android.engine.cache.AndroidCacheEngine
import co.yml.network.android.engine.network.AndroidNetworkEngine
import co.yml.network.core.NetworkManager
import co.yml.network.core.NetworkManagerBuilder
import co.yml.network.core.engine.cache.CacheEngine
import co.yml.network.core.engine.network.NetworkEngine
import co.yml.network.core.parser.BasicDataParserFactory
import co.yml.chat.data.parser.JsonDataParser
import co.yml.chat.logger.LoggerInterceptor
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
