/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.demo.validation.ui.activities

import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.data.CardReaderApi
import org.calypsonet.keyple.demo.validation.data.LocationFileManager

/** @author youssefamrani */
abstract class BaseActivity : DaggerAppCompatActivity() {

  @Inject lateinit var cardReaderApi: CardReaderApi

  @Inject lateinit var locationFileManager: LocationFileManager
}
