package com.mobile_ui.injection.module

import com.mobile_ui.view.MainActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class UiModule {

    @ContributesAndroidInjector
    abstract fun contributesMainActivity(): MainActivity

}
