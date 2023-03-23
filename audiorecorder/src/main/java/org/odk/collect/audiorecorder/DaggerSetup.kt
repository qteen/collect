package org.odk.collect.audiorecorder

import android.app.Application
import android.content.Context
import android.media.MediaRecorder
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import org.odk.collect.audiorecorder.recorder.MediaRecorderRecorder
import org.odk.collect.audiorecorder.recorder.RealMediaRecorderWrapper
import org.odk.collect.audiorecorder.recorder.Recorder
import org.odk.collect.audiorecorder.recording.AudioRecorderActivity
import org.odk.collect.audiorecorder.recording.AudioRecorderService
import org.odk.collect.audiorecorder.recording.RecordingRepository
import java.lang.IllegalStateException
import javax.inject.Singleton

private var _component: AudioRecorderDependencyComponent? = null

internal fun Context.getComponent(): AudioRecorderDependencyComponent {
    return _component.let {
        if (it == null && applicationContext is TestApplication) {
            throw IllegalStateException("Dependencies not specified!")
        }

        if (it == null) {
            val newComponent = DaggerAudioRecorderDependencyComponent.builder()
                .application(applicationContext as Application)
                .build()

            _component = newComponent
            newComponent
        } else {
            it
        }
    }
}

@Component(modules = [AudioRecorderDependencyModule::class])
@Singleton
internal interface AudioRecorderDependencyComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun dependencyModule(audioRecorderDependencyModule: AudioRecorderDependencyModule): Builder

        fun build(): AudioRecorderDependencyComponent
    }

    fun inject(activity: AudioRecorderActivity)
    fun inject(activity: AudioRecorderService)
}

@Module
internal open class AudioRecorderDependencyModule {

    @Provides
    open fun providesRecorder(application: Application): Recorder {
        return MediaRecorderRecorder(application.cacheDir) { RealMediaRecorderWrapper(MediaRecorder()) }
    }

    @Provides
    @Singleton
    open fun providesRecordingRepository(): RecordingRepository {
        return RecordingRepository()
    }
}

internal fun TestApplication.clearDependencies() {
    _component = null
}

internal fun TestApplication.setupDependencies(module: AudioRecorderDependencyModule) {
    _component = DaggerAudioRecorderDependencyComponent.builder()
        .application(this)
        .dependencyModule(module)
        .build()
}
