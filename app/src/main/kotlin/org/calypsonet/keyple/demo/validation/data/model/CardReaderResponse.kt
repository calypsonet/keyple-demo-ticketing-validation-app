/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the
 * terms of the BSD 3-Clause License which is available at
 * https://opensource.org/licenses/BSD-3-Clause.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */
package org.calypsonet.keyple.demo.validation.data.model

import android.os.Parcelable
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CardReaderResponse(
    val status: Status,
    val nbTicketsLeft: Int? = null,
    val contract: String?,
    val validation: Validation?,
    val eventDateTime: LocalDateTime? = null,
    val passValidityEndDate: LocalDate? = null,
    val errorMessage: String? = null
) : Parcelable
