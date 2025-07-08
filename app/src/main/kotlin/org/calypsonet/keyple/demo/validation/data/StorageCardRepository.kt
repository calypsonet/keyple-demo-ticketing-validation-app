/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the
 * terms of the BSD 3-Clause License which is available at
 * https://opensource.org/licenses/BSD-3-Clause.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */
package org.calypsonet.keyple.demo.validation.data

import android.content.Context
import java.time.LocalDate
import java.time.LocalDateTime
import org.calypsonet.keyple.card.storagecard.StorageCardExtensionService
import org.calypsonet.keyple.demo.common.constant.CardConstant
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.common.parser.SCContractStructureParser
import org.calypsonet.keyple.demo.common.parser.SCEnvironmentHolderStructureParser
import org.calypsonet.keyple.demo.common.parser.SCEventStructureParser
import org.calypsonet.keyple.demo.validation.R
import org.calypsonet.keyple.demo.validation.data.model.AppSettings
import org.calypsonet.keyple.demo.validation.data.model.CardReaderResponse
import org.calypsonet.keyple.demo.validation.data.model.Location
import org.calypsonet.keyple.demo.validation.data.model.Status
import org.calypsonet.keyple.demo.validation.data.model.Validation
import org.calypsonet.keyple.demo.validation.data.model.mapper.ValidationMapper
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.storagecard.card.StorageCard
import org.eclipse.keypop.storagecard.transaction.ChannelControl
import timber.log.Timber

class StorageCardRepository {

  fun executeValidationProcedure(
      validationDateTime: LocalDateTime,
      context: Context,
      validationAmount: Int,
      cardReader: CardReader,
      storageCard: StorageCard,
      locations: List<Location>
  ): CardReaderResponse {
    var status: Status = Status.LOADING
    var errorMessage: String? = null
    var passValidityEndDate: LocalDate? = null
    var nbTicketsLeft: Int? = null
    var validation: Validation? = null

    val storageCardExtension = StorageCardExtensionService.getInstance()

    // Create a card transaction for validation
    val cardTransaction =
        try {
          storageCardExtension.createStorageCardTransactionManager(cardReader, storageCard)
        } catch (e: Exception) {
          Timber.w(e)
          status = Status.ERROR
          errorMessage = e.message
          null
        }

    if (cardTransaction != null) {
      try {
        // ***************** Event and Environment Analysis
        // Step 1 - Read the environment and event data
        cardTransaction
            .prepareReadBlocks(
                CardConstant.SC_ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
                CardConstant.SC_ENVIRONMENT_AND_HOLDER_LAST_BLOCK)
            .prepareReadBlocks(CardConstant.SC_EVENT_FIRST_BLOCK, CardConstant.SC_EVENT_LAST_BLOCK)
            .prepareReadBlocks(
                CardConstant.SC_CONTRACT_FIRST_BLOCK, CardConstant.SC_COUNTER_LAST_BLOCK)
            .processCommands(ChannelControl.KEEP_OPEN)

        // Step 2 - Unpack environment structure
        val environmentContent =
            storageCard.getBlocks(
                CardConstant.SC_ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
                CardConstant.SC_ENVIRONMENT_AND_HOLDER_LAST_BLOCK)
        val environment = SCEnvironmentHolderStructureParser().parse(environmentContent)

        // Step 3 - Validate environment version
        if (environment.envVersionNumber != VersionNumber.CURRENT_VERSION) {
          status = Status.INVALID_CARD
          throw RuntimeException("Environment error: wrong version number")
        }

        // Step 4 - Validate environment end date
        if (environment.envEndDate.getDate().isBefore(validationDateTime.toLocalDate())) {
          status = Status.INVALID_CARD
          throw RuntimeException("Environment error: end date expired")
        }

        // Step 5 - Read and unpack the event record
        val eventContent =
            storageCard.getBlocks(CardConstant.SC_EVENT_FIRST_BLOCK, CardConstant.SC_EVENT_LAST_BLOCK)
        val event = SCEventStructureParser().parse(eventContent)

        // Step 6 - Validate event version
        val eventVersionNumber = event.eventVersionNumber
        if (eventVersionNumber != VersionNumber.CURRENT_VERSION) {
          if (eventVersionNumber == VersionNumber.UNDEFINED) {
            status = Status.EMPTY_CARD
            throw RuntimeException("No valid title detected")
          } else {
            status = Status.INVALID_CARD
            throw RuntimeException("Event error: wrong version number")
          }
        }

        // Step 7 - Read and unpack the contract record
        val contractContent =
            storageCard.getBlocks(
                CardConstant.SC_CONTRACT_FIRST_BLOCK, CardConstant.SC_COUNTER_LAST_BLOCK)
        val contract = SCContractStructureParser().parse(contractContent)

        // Validate contract version
        if (contract.contractVersionNumber != VersionNumber.CURRENT_VERSION) {
          status = Status.INVALID_CARD
          throw RuntimeException("Contract Version Number error (!= CURRENT_VERSION)")
        }

        // Check contract validity
        if (contract.contractValidityEndDate.getDate().isBefore(validationDateTime.toLocalDate())) {
          status = Status.EMPTY_CARD
          errorMessage = context.getString(R.string.expired_title)
          throw RuntimeException("Contract expired")
        }

        // Determine contract priority from contract tariff
        val contractPriority = contract.contractTariff

        var writeEvent: Boolean
        val contractUsed = 1 // For storage card, we only have one contract

        when (contractPriority) {
          PriorityCode.MULTI_TRIP -> {
            // Check if there are trips left
            val counterValue = contract.counterValue ?: 0
            if (counterValue == 0) {
              status = Status.EMPTY_CARD
              errorMessage = context.getString(R.string.no_trips_left)
              throw RuntimeException("No trips left")
            }

            // Decrement counter
            val newCounterValue = counterValue - SINGLE_VALIDATION_AMOUNT
            contract.counterValue = newCounterValue
            nbTicketsLeft = newCounterValue

            // Update contract data
            val updatedContractContent = SCContractStructureParser().generate(contract)
            cardTransaction
                .prepareWriteBlocks(CardConstant.SC_CONTRACT_FIRST_BLOCK, updatedContractContent)
                .processCommands(ChannelControl.KEEP_OPEN)

            writeEvent = true
          }
          PriorityCode.STORED_VALUE -> {
            // Check if there's enough value
            val counterValue = contract.counterValue ?: 0
            if (counterValue < validationAmount) {
              status = Status.EMPTY_CARD
              errorMessage = context.getString(R.string.no_trips_left)
              throw RuntimeException("Insufficient stored value")
            }

            // Decrement counter by validation amount
            val newCounterValue = counterValue - validationAmount
            contract.counterValue = newCounterValue
            nbTicketsLeft = newCounterValue

            // Update contract data
            val updatedContractContent = SCContractStructureParser().generate(contract)
            cardTransaction
                .prepareWriteBlocks(CardConstant.SC_CONTRACT_FIRST_BLOCK, updatedContractContent)
                .processCommands(ChannelControl.KEEP_OPEN)

            writeEvent = true
          }
          PriorityCode.SEASON_PASS -> {
            passValidityEndDate = contract.contractValidityEndDate.getDate()
            writeEvent = true
          }
          PriorityCode.FORBIDDEN,
          PriorityCode.EXPIRED,
          PriorityCode.UNKNOWN -> {
            status = Status.EMPTY_CARD
            errorMessage = context.getString(R.string.no_valid_title_detected)
            throw RuntimeException("Contract is forbidden or expired")
          }
        }

        if (writeEvent) {
          // Create a new validation event
          val eventToWrite =
              EventStructure(
                  eventVersionNumber = VersionNumber.CURRENT_VERSION,
                  eventDateStamp = DateCompact(validationDateTime.toLocalDate()),
                  eventTimeStamp = TimeCompact(validationDateTime),
                  eventLocation = AppSettings.location.id,
                  eventContractUsed = contractUsed,
                  contractPriority1 = contractPriority,
                  contractPriority2 = PriorityCode.FORBIDDEN,
                  contractPriority3 = PriorityCode.FORBIDDEN,
                  contractPriority4 = PriorityCode.FORBIDDEN)

          validation = ValidationMapper.map(eventToWrite, locations)

          // Write the event
          val eventBytesToWrite = SCEventStructureParser().generate(eventToWrite)
          cardTransaction
              .prepareWriteBlocks(CardConstant.SC_EVENT_FIRST_BLOCK, eventBytesToWrite)
              .processCommands(ChannelControl.KEEP_OPEN)

          Timber.i("Validation procedure result: SUCCESS")
          status = Status.SUCCESS
          errorMessage = null
        } else {
          Timber.i("Validation procedure result: Failed - No valid contract found")
          errorMessage = context.getString(R.string.no_valid_title_detected)
        }
      } catch (e: Exception) {
        Timber.e(e)
        errorMessage = e.message
        if (status == Status.LOADING) {
          status = Status.ERROR
        }
      } finally {
        // Close the transaction
        try {
          cardTransaction.processCommands(ChannelControl.CLOSE_AFTER)
        } catch (e: Exception) {
          Timber.e(e)
          if (status == Status.LOADING) {
            status = Status.ERROR
          }
          if (errorMessage.isNullOrEmpty()) {
            errorMessage = e.message
          }
        }
      }
    }

    return CardReaderResponse(
        status = status,
        cardType = storageCard.productType.name,
        nbTicketsLeft = nbTicketsLeft,
        contract = "",
        validation = validation,
        errorMessage = errorMessage,
        passValidityEndDate = passValidityEndDate,
        eventDateTime = validationDateTime)
  }

  companion object {
    const val SINGLE_VALIDATION_AMOUNT = 1
  }
}
