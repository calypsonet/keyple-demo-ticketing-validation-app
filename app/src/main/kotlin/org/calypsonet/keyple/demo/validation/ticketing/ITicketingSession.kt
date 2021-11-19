/********************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.calypsonet.keyple.demo.validation.ticketing

import android.content.Context
import org.calypsonet.keyple.demo.validation.models.CardReaderResponse
import org.calypsonet.keyple.demo.validation.models.FileStructureEnum
import org.calypsonet.keyple.demo.validation.models.Location
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting
import org.calypsonet.terminal.reader.selection.CardSelectionResult
import org.calypsonet.terminal.reader.selection.ScheduledCardSelectionsResponse
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.Reader

interface ITicketingSession {
    val cardReader: Reader?
    val samReader: Reader?
    val samPluginName: String?
    val cardAid: String?
    val fileStructure: FileStructureEnum?

    fun prepareAndSetCardDefaultSelection()
    fun processDefaultSelection(selectionResponse: ScheduledCardSelectionsResponse?): CardSelectionResult
    fun checkStructure(): Boolean
    fun checkStartupInfo(): Boolean
    fun launchValidationProcedure(context: Context, locations: List<Location>): CardReaderResponse
    fun setupCardResourceService(samProfileName: String?, samPluginName: String?)
    fun getSecuritySettings(): CardSecuritySetting?
    fun getPlugin(): Plugin
}
