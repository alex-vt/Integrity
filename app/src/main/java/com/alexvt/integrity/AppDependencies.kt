/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity

import android.content.Context
import com.alexvt.integrity.core.IntegrityCoreDependenciesModule
import com.alexvt.integrity.ui.UiDependenciesModule
import dagger.*
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton
import dagger.android.AndroidInjector

object AppDependencies {

    fun createDependencyGraph(app: App) {
        DaggerAppComponent.builder()
                .appContext(app)
                .create(app)
                .inject(app)
    }

}

@Singleton
@Component(modules = [
    AndroidInjectionModule::class,
    IntegrityCoreDependenciesModule::class,
    UiDependenciesModule::class
])
interface AppComponent : AndroidInjector<App> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<App>() {
        @BindsInstance abstract fun appContext(appContext: Context): Builder
    }
}



