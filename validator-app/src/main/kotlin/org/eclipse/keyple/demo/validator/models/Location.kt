/*
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.keyple.demo.validator.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 *  @author youssefamrani
 */
@Parcelize
data class Location(val id: Int, val name: String) : Parcelable {

    override fun toString(): String {
        return "$id - $name"
    }
}