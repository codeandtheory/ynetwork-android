package co.yml.chat

import co.yml.network.core.engine.cache.CacheEngine
import co.yml.network.core.engine.cache.demo.DemoCacheEngine
import co.yml.network.core.engine.network.NetworkEngine
import co.yml.network.core.engine.network.demo.DemoNetworkEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module(includes = [TestApplicationModule.TestDependencies::class])
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ApplicationModule.Fakes::class]
)
abstract class TestApplicationModule {

    @Module
    @InstallIn(SingletonComponent::class)
    object TestDependencies {

        @Singleton
        @Provides
        fun provideDemoNetworkEngine(): DemoNetworkEngine =
            DemoNetworkEngine().setBasePath("https://accelerator-theia.herokuapp.com/")

        @Singleton
        @Provides
        fun provideDemoCacheEngine(): DemoCacheEngine = DemoCacheEngine()

        @Singleton
        @Provides
        fun provideUniqueIdGenerator(): UniqueIdGenerator = object : UniqueIdGenerator {
            override fun generateId(): String = "UniqueId"
        }
    }

    @Singleton
    @Binds
    abstract fun provideNetworkEngine(demoNetworkEngine: DemoNetworkEngine): NetworkEngine

    @Singleton
    @Binds
    abstract fun provideCacheEngine(demoCacheEngine: DemoCacheEngine): CacheEngine
}
