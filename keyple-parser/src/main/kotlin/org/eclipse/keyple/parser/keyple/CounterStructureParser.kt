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
package org.eclipse.keyple.parser.keyple

import fr.devnied.bitlib.BitUtils
import java.math.BigInteger
import org.eclipse.keyple.parser.IParser
import org.eclipse.keyple.parser.model.CounterStructureDto

class CounterStructureParser :
    IParser<CounterStructureDto> {

    fun parseAllCounters(content: ByteArray, nbCounters: Int): List<CounterStructureDto> {
        val resultList = mutableListOf<CounterStructureDto>()

        val bitUtils = BitUtils(content)
        bitUtils.currentBitIndex = 0

        for (i in 0 until nbCounters) {
            val counterValue = bitUtils.getNextInteger(COUNTER_SIZE)
            resultList.add(CounterStructureDto(counterValue = counterValue))
        }

        return resultList
    }

    override fun parse(content: ByteArray): CounterStructureDto {

        val bitUtils = BitUtils(content)
        bitUtils.currentBitIndex = 0

        /*
         * counterValue
         */
        val counterValue = bitUtils.getNextInteger(COUNTER_SIZE)

        return CounterStructureDto(
            counterValue = counterValue
        )
    }

    override fun generate(content: CounterStructureDto): ByteArray {

        val bitUtils = BitUtils(COUNTER_SIZE)
        bitUtils.currentBitIndex = 0

        /*
         * counterValue
         */
        bitUtils.setNextByte(
            BigInteger.valueOf(content.counterValue.toLong()).toByteArray(),
            COUNTER_SIZE
        )

        return bitUtils.data
    }

    companion object {
        const val COUNTER_SIZE = 24
    }
}
