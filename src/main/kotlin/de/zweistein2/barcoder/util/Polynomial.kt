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

import de.zweistein2.barcoder.util.GaloisUtil.addGalois
import de.zweistein2.barcoder.util.GaloisUtil.multiplyGalois

/**
 * This class represents a polynomial
 *
 * @param degree The (highest) degree the polynomial should have
 * @param coefficients The list of coefficients of the polynomial (if there is no x^n at a specific position the corresponding coefficient should be 0 not "null")
 */
class Polynomial(var degree: Int, val coefficients: MutableList<Int>) {

    init {
        require(degree == coefficients.size - 1) {"number of coefficients doesn't match required polynomial degree"}

        while(coefficients[0] == 0) {
            coefficients.removeFirst()
            degree -= 1
        }
    }

    /**
     * This method multiplies the current polynomial with another
     *
     * @param other The other polynomial needed for the multiplication
     * @return The resulting polynomial
     */
    fun multiplyWith(other: Polynomial): Polynomial {
        val aCoefficients = coefficients
        val aLength = aCoefficients.size
        val bCoefficients = other.coefficients
        val bLength = bCoefficients.size
        val product = MutableList(aLength + bLength - 1) { 0 }

        for (i in 0 until aLength) {
            val aCoeff = aCoefficients[i]

            for (j in 0 until bLength) {
                product[i + j] = addGalois(product[i + j], multiplyGalois(aCoeff, bCoefficients[j]))
            }
        }

        return Polynomial(product.size - 1, product)
    }

    /**
     * This method multiplies the current polynomial by a monomial (x^n)
     *
     * @param degree The degree of the monomial (n)
     * @return The resulting polynomial
     */
    fun multiplyByMonomial(degree: Int): Polynomial {
        val product = MutableList(coefficients.size + degree) { 0 }

        for(i in 0 until coefficients.size) {
            product[i] = coefficients[i]
        }

        return Polynomial(product.size - 1, product)
    }

    /**
     * This method multiplies the current polynomial by a scalar (n)
     *
     * @param scalar The scalar (n)
     * @return The resulting polynomial
     */
    fun multiplyBy(scalar: Int): Polynomial {
        val product = MutableList(coefficients.size) { 0 }

        for (i in 0 until coefficients.size) {
            product[i] = multiplyGalois(coefficients[i], scalar)
        }

        return Polynomial(product.size - 1, product)
    }

    /**
     * This method evaluates the polynomial for a given x
     *
     * @param x The value x should have for the evaluation
     * @return The result
     */
    fun evaluate(x: Int): Int {
        var y = coefficients[0]

        for(i in 1 until coefficients.size) {
            y = multiplyGalois(y, x) xor coefficients[i]
        }

        return y
    }

    /**
     * This method divides the current polynomial by another
     *
     * @param other The other polynomial needed for the division
     * @return The resulting polynomial
     */
    fun divideBy(other: Polynomial): Polynomial {
        val product = MutableList(coefficients.size) { 0 }

        for(i in 0 until 1) {
            val coeff = coefficients[i]

            if(coeff != 0) {
                for (j in 0 until other.coefficients.size) {
                    if(other.coefficients[j] != 0) {
                        product[i + j] = product[i + j] xor multiplyGalois(other.coefficients[j], coeff)
                    }
                }
            }
        }

        return Polynomial(product.size - 1, product)
    }

    /**
     * This method xors the current polynomial with another
     *
     * @param other The other polynomial needed for the xor-operation
     * @return The resulting polynomial
     */
    fun xorWith(other: Polynomial): Polynomial {
        val product = MutableList(coefficients.size) { 0 }

        for(i in 0 until coefficients.size) {
            product[i] = coefficients[i] xor other.coefficients[i]
        }

        return Polynomial(product.size - 1, product)
    }

    /**
     * This method adds another polynomial to the the current one
     *
     * @param other The other polynomial needed for the addition
     * @return The resulting polynomial
     */
    fun add(other: Polynomial): Polynomial {
        val aCoefficients = coefficients
        val aLength = aCoefficients.size
        val bCoefficients = other.coefficients
        val bLength = bCoefficients.size

        val product = MutableList(maxOf(aLength, bLength)) { 0 }

        for (i in 0 until aLength) {
            product[i + product.size - aLength] = aCoefficients[i]
        }
        for (i in 0 until bLength) {
            product[i + product.size - bLength] = product[i + product.size - bLength] xor bCoefficients[i]
        }

        return Polynomial(product.size - 1, product)
    }

    /**
     * This method generates a string representation of the current polynomial
     *
     * @return The polynomial represented by a string
     */
    override fun toString(): String {
        val result: StringBuilder = StringBuilder()

        for (i in degree downTo 0) {
            var coefficient: Int = coefficients.reversed()[i]

            if (coefficient != 0) {
                if (coefficient < 0) {
                    if (i == degree) {
                        result.append("-")
                    } else {
                        result.append(" - ")
                    }
                    coefficient = -coefficient
                } else {
                    if (result.isNotEmpty()) {
                        result.append(" + ")
                    }
                }

                if (i == 0 || coefficient != 1) {
                    result.append(coefficient)
                }

                if (i == 1) {
                    result.append('x')
                } else if(i > 1) {
                    result.append("x^")
                    result.append(i)
                }
            }
        }
        return result.toString()
    }
}