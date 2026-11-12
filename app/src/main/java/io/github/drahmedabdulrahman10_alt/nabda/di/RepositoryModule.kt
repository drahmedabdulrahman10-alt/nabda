package io.github.drahmedabdulrahman10_alt.nabda.di

import android.content.Context
import io.github.drahmedabdulrahman10_alt.nabda.data.repository.LocalNabdaRepository
import io.github.drahmedabdulrahman10_alt.nabda.data.repository.NabdaRepository

/** Holds the single repository instance shared by every screen. */
object RepositoryModule {

    @Volatile
    private var nabdaRepository: NabdaRepository? = null

    fun provideNabdaRepository(context: Context): NabdaRepository {
        return nabdaRepository ?: synchronized(this) {
            nabdaRepository ?: LocalNabdaRepository(context.applicationContext).also { nabdaRepository = it }
        }
    }
}
