package com.mobile_ui.injection

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton

import com.mobile_ui.SnapshotApplication
import com.mobile_ui.injection.module.*

@Singleton
@Component(modules = [AndroidInjectionModule::class, ApplicationModule::class, UiModule::class])
internal interface ApplicationComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(app: Application?): Builder
        fun build(): ApplicationComponent
    }

    fun inject(app: SnapshotApplication)
}
