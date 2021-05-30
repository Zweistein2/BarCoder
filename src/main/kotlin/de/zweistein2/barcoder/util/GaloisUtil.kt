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

import kotlin.math.pow

/**
 * A utility class for all galoisfield-related functions
 */
object GaloisUtil {
    /**
     * This method generates powers of 2 with the given exponent inside the galois field GF(256)
     *
     * @param exponent The exponent to be used in the calculation
     * @return The resulting number (in the range of 0 - 255)
     */
    fun powerGalois(exponent: Int): Int {
        var result: Int

        when {
            exponent < 8 -> { return 2.0.pow(exponent).toInt() }
            exponent == 8 -> { return 2.0.pow(exponent).toInt() xor 285 }
            else -> {
                val tempExp = exponent - 8
                result = 2.0.pow(8).toInt() xor 285

                for(i in 1..tempExp) {
                    result *= 2

                    if(result > 256) {
                        result = result xor 285
                    }
                }
            }
        }

        return result
    }

    /**
     * This method performs a multiplication inside the galois field GF(256)
     *
     * @param a The first multiplicand
     * @param a The second multiplicand
     * @return The resulting number (in the range of 0 - 255)
     */
    fun multiplyGalois(a: Int, b: Int): Int {
        var newExponent = getGaloisExponent(a) + getGaloisExponent(b)

        while(newExponent > 256) {
            newExponent %= 255
        }

        return powerGalois(newExponent)
    }

    /**
     * This method gets the corresponding exponent for the provided number (as it can be represented by a power of 2)
     *
     * @param number The number for which the exponent shall be returned
     * @return The corresponding exponent used to calculate the number
     */
    fun getGaloisExponent(number: Int): Int {
        for(i in 0..256) {
            if(powerGalois(i) == number) {
                return i
            }
        }

        return -1
    }

    /**
     * This method performs an addition inside the galois field GF(256)
     *
     * @param a The first addend
     * @param b The second addend
     * @return The sum of both addends (in the range of 0 - 255)
     */
    fun addGalois(a: Int, b: Int): Int {
        return a xor b
    }
}