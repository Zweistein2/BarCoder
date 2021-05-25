/*
 * Copyright 2021 Fabian Karolat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.zweistein2.barcoder.util

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class PolynomialTest {
    private val firstPolynomial = Polynomial(7, mutableListOf(32, 91, 11, 120, 209, 114, 220, 77))
    private val secondPolynomial = Polynomial(7, mutableListOf(1, 87, 229, 146, 149, 238, 102, 21))
    private val thirdPolynomial = Polynomial(15, mutableListOf(32, 91, 11, 120, 209, 114, 220, 77, 67, 64, 236, 17, 236, 17, 236, 17)).multiplyByMonomial(10)
    private val fourthPolynomial = Polynomial(10, mutableListOf(1, 216, 194, 159, 111, 199, 94, 95, 113, 157, 193)).multiplyByMonomial(15)

    @Test
    fun multiplyWithTest() {
        val firstPoly = Polynomial(1, mutableListOf(1, Polynomial.powerGalois(0)))
        val secondPoly = Polynomial(1, mutableListOf(1, Polynomial.powerGalois(1)))

        assertEquals("x^2 + 3x + 2", firstPoly.multiplyWith(secondPoly).toString())
    }

    @Test
    fun multiplyByMonomialTest() {
        assertEquals("32x^7 + 91x^6 + 11x^5 + 120x^4 + 209x^3 + 114x^2 + 220x + 77", firstPolynomial.toString())
        assertEquals("32x^14 + 91x^13 + 11x^12 + 120x^11 + 209x^10 + 114x^9 + 220x^8 + 77x^7", firstPolynomial.multiplyByMonomial(7).toString())
    }

    @Test
    fun multiplyByTest() {
        assertEquals("32x^7 + 91x^6 + 11x^5 + 120x^4 + 209x^3 + 114x^2 + 220x + 77", firstPolynomial.toString())
        assertEquals("93x^7 + 84x^6 + 78x^5 + 23x^4 + 121x^3 + 83x^2 + 11x + 200", firstPolynomial.multiplyBy(10).toString())
    }

    @Test
    fun evaluateTest() {
        assertEquals(138, firstPolynomial.evaluate(0))
        assertEquals(58, firstPolynomial.evaluate(1))
        assertEquals(47, firstPolynomial.evaluate(5))
    }

    @Test
    fun divideByTest() {
        assertEquals("32x^25 + 2x^24 + 101x^23 + 10x^22 + 97x^21 + 197x^20 + 15x^19 + 47x^18 + 134x^17 + 74x^16 + 5x^15", thirdPolynomial.divideBy(fourthPolynomial).toString())
    }

    @Test
    fun xorWithTest() {
        assertEquals("33x^7 + 12x^6 + 238x^5 + 234x^4 + 68x^3 + 156x^2 + 186x + 88", firstPolynomial.xorWith(secondPolynomial).toString())
    }

    @Test
    fun addTest() {
        assertEquals("33x^7 + 12x^6 + 238x^5 + 234x^4 + 68x^3 + 156x^2 + 186x + 88", firstPolynomial.add(secondPolynomial).toString())
    }
}