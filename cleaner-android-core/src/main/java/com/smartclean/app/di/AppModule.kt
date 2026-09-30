package com.smartclean.app.di

import android.content.Context
import com.smartclean.app.data.repository.StorageScannerRepositoryImpl
import com.smartclean.app.data.scanner.DuplicateFileScanner
import com.smartclean.app.data.scanner.EmptyFolderScanner
import com.smartclean.app.data.scanner.SimilarImageScanner
import com.smartclean.app.domain.repository.StorageScannerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEmptyFolderScanner(): EmptyFolderScanner = EmptyFolderScanner()

    @Provides
    @Singleton
    fun provideDuplicateFileScanner(): DuplicateFileScanner = DuplicateFileScanner()

    @Provides
    @Singleton
    fun provideSimilarImageScanner(): SimilarImageScanner = SimilarImageScanner()

    @Provides
    @Singleton
    fun provideStorageScannerRepository(
        @ApplicationContext context: Context,
        emptyFolderScanner: EmptyFolderScanner,
        duplicateScanner: DuplicateFileScanner,
        similarImageScanner: SimilarImageScanner
    ): StorageScannerRepository {
        return StorageScannerRepositoryImpl(
            context = context,
            emptyFolderScanner = emptyFolderScanner,
            duplicateScanner = duplicateScanner,
            similarImageScanner = similarImageScanner
        )
    }
}
