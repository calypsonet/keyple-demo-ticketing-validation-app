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
package org.calypsonet.keyple.demo.validation.service.ticketing.model.mapper

import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.validation.service.ticketing.model.Location
import org.calypsonet.keyple.demo.validation.service.ticketing.model.Validation

object ValidationMapper {
  fun map(event: EventStructure, locations: List<Location>): Validation {
    return Validation(
        name = "Event name",
        date = event.eventDatetime,
        location = LocationMapper.map(locations, event),
        destination = null,
        provider = null)
  }
}
