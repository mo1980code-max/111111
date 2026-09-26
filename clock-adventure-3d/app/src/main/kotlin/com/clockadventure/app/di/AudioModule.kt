package com.clockadventure.app.di

import com.clockadventure.app.audio.AppAudioController
import com.clockadventure.domain.repository.AudioController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the Android audio implementation to the domain interface. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindAudioController(impl: AppAudioController): AudioController
}
