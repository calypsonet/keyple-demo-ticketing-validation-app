/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the BSD 3-Clause License which is available at
 * https://opensource.org/licenses/BSD-3-Clause.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */
package org.calypsonet.keyple.demo.validation.data.model.mapper

import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.validation.data.model.Location
import org.calypsonet.keyple.demo.validation.data.model.Validation

object ValidationMapper {
  fun map(event: EventStructure, locations: List<Location>): Validation {
    return Validation(
        name = "Event name",
        dateTime = event.eventDatetime,
        location = LocationMapper.map(locations, event),
        destination = null,
        provider = null)
  }
}
