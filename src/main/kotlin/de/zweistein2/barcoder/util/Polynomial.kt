package de.zweistein2.barcoder.util

import kotlin.math.pow

class Polynomial(var degree: Int, val coefficients: MutableList<Int>) {
    companion object {
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
    }

    init {
        require(degree == coefficients.size - 1) {"number of coefficients doesn't match required polynomial degree"}

        while(coefficients[0] == 0) {
            coefficients.removeFirst()
            degree -= 1
        }
    }

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

    fun multiplyByMonomial(degree: Int): Polynomial {
        val product = MutableList(coefficients.size + degree) { 0 }

        for(i in 0 until coefficients.size) {
            product[i] = coefficients[i]
        }

        return Polynomial(product.size - 1, product)
    }

    fun multiplyBy(scalar: Int): Polynomial {
        val product = MutableList(coefficients.size) { 0 }

        for (i in 0 until coefficients.size) {
            product[i] = multiplyGalois(coefficients[i], scalar)
        }

        return Polynomial(product.size - 1, product)
    }

    fun evaluate(x: Int): Int {
        var y = coefficients[0]

        for(i in 1 until coefficients.size) {
            y = multiplyGalois(y, x) xor coefficients[i]
        }

        return y
    }

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

    fun xorWith(other: Polynomial): Polynomial {
        val product = MutableList(coefficients.size) { 0 }

        for(i in 0 until coefficients.size) {
            product[i] = coefficients[i] xor other.coefficients[i]
        }

        return Polynomial(product.size - 1, product)
    }

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

    private fun multiplyGalois(exponentA: Int, exponentB: Int): Int {
        var newExponent = getGaloisExponent(exponentA) + getGaloisExponent(exponentB)

        while(newExponent > 256) {
            newExponent %= 255
        }

        return powerGalois(newExponent)
    }

    private fun getGaloisExponent(intValue: Int): Int {
        for(i in 0..256) {
            if(powerGalois(i) == intValue) {
                return i
            }
        }

        return -1
    }

    private fun addGalois(a: Int, b: Int): Int {
        return a xor b
    }

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