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

import java.util.Locale

enum class Status(private val status: String) {
  LOADING("loading"),
  SUCCESS("Success"),
  INVALID_CARD("Invalid card"),
  EMPTY_CARD("Empty card"),
  ERROR("error");

  override fun toString(): String {
    return status
  }

  companion object {
    @JvmStatic
    fun getStatus(name: String): Status {
      return try {
        valueOf(name.toUpperCase(Locale.ENGLISH))
      } catch (e: Exception) {
        // If the given state does not exist, return the default value.
        ERROR
      }
    }
  }
}
