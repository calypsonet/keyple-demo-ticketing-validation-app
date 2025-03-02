/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the
 * terms of the MIT License which is available at
 * https://opensource.org/licenses/MIT.
 *
 * SPDX-License-Identifier: MIT
 ****************************************************************************** */
package org.calypsonet.keyple.demo.validation.di

import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import org.calypsonet.keyple.demo.validation.Application
import org.calypsonet.keyple.demo.validation.di.scope.AppScoped

@AppScoped
@Component(
    modules =
        [
            AppModule::class,
            UIModule::class,
            AndroidSupportInjectionModule::class,
            ReaderModule::class,
            LocationModule::class])
interface AppComponent : AndroidInjector<Application?> {
  @Component.Builder
  interface Builder {
    @BindsInstance fun application(application: Application): Builder

    fun build(): AppComponent
  }
}
