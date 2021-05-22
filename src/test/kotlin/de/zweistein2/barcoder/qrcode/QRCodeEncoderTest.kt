package de.zweistein2.barcoder.qrcode

import de.zweistein2.barcoder.BarcodeEncoder.BarcodeEncoderFactory.getEncoder
import de.zweistein2.barcoder.BarcodeFormat
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class QRCodeEncoderTest {
    private val encoder = BarcodeFormat.QR_CODE.getEncoder() as QRCodeEncoder

    @Test
    fun getGeneratorPolynomialTest() {
        val generatorPolynomialFor5 = encoder.getGeneratorPolynomial(5)
        val generatorPolynomialFor14 = encoder.getGeneratorPolynomial(14)
        val generatorPolynomialFor7 = encoder.getGeneratorPolynomial(7)
        val generatorPolynomialFor21 = encoder.getGeneratorPolynomial(21)

        assertEquals("x^5 + 31x^4 + 198x^3 + 63x^2 + 147x + 116", generatorPolynomialFor5.toString())
        assertEquals("x^14 + 14x^13 + 54x^12 + 114x^11 + 70x^10 + 174x^9 + 151x^8 + 43x^7 + 158x^6 + 195x^5 + " +
                "127x^4 + 166x^3 + 210x^2 + 234x + 163", generatorPolynomialFor14.toString())
        assertEquals("x^7 + 127x^6 + 122x^5 + 154x^4 + 164x^3 + 11x^2 + 68x + 117", generatorPolynomialFor7.toString())
        assertEquals("x^21 + 44x^20 + 243x^19 + 13x^18 + 131x^17 + 49x^16 + 132x^15 + 194x^14 + 67x^13 + 214x^12 " +
                "+ 28x^11 + 89x^10 + 124x^9 + 82x^8 + 158x^7 + 244x^6 + 37x^5 + 236x^4 + 142x^3 + 82x^2 + 255x + 89", generatorPolynomialFor21.toString())
    }

    @Test
    fun getErrorCorrectionCodesPerBlockTest() {
         assertEquals(18, encoder.getErrorCorrectionCodesPerBlock(6, ErrorCorrectionLevel.L))
        assertEquals(26, encoder.getErrorCorrectionCodesPerBlock(21, ErrorCorrectionLevel.M))
        assertEquals(30, encoder.getErrorCorrectionCodesPerBlock(32, ErrorCorrectionLevel.Q))
        assertEquals(28, encoder.getErrorCorrectionCodesPerBlock(18, ErrorCorrectionLevel.H))
    }

    @Test
    fun getDataBlockCountForGroupsTest() {
        assertEquals(Pair(2, null), encoder.getDataBlockCountForGroups(6, ErrorCorrectionLevel.L))
        assertEquals(Pair(17, null), encoder.getDataBlockCountForGroups(21, ErrorCorrectionLevel.M))
        assertEquals(Pair(10, 35), encoder.getDataBlockCountForGroups(32, ErrorCorrectionLevel.Q))
        assertEquals(Pair(2, 19), encoder.getDataBlockCountForGroups(18, ErrorCorrectionLevel.H))
    }

    @Test
    fun getDataCapacityForEncodingModeTest() {
        assertEquals(114, encoder.getDataCapacityForEncodingMode(4, ErrorCorrectionLevel.L , EncodingMode.ALPHANUMERIC))
        assertEquals(1035, encoder.getDataCapacityForEncodingMode(21, ErrorCorrectionLevel.M , EncodingMode.ALPHANUMERIC))
        assertEquals(1700, encoder.getDataCapacityForEncodingMode(33, ErrorCorrectionLevel.Q , EncodingMode.ALPHANUMERIC))
        assertEquals(1852, encoder.getDataCapacityForEncodingMode(40, ErrorCorrectionLevel.H , EncodingMode.ALPHANUMERIC))
        assertEquals(78, encoder.getDataCapacityForEncodingMode(4, ErrorCorrectionLevel.L , EncodingMode.BINARY))
        assertEquals(711, encoder.getDataCapacityForEncodingMode(21, ErrorCorrectionLevel.M , EncodingMode.BINARY))
        assertEquals(1168, encoder.getDataCapacityForEncodingMode(33, ErrorCorrectionLevel.Q , EncodingMode.BINARY))
        assertEquals(1273, encoder.getDataCapacityForEncodingMode(40, ErrorCorrectionLevel.H , EncodingMode.BINARY))
    }

    @Test
    fun getEncodingModeForContentTest() {
        assertEquals(EncodingMode.ALPHANUMERIC, encoder.getEncodingModeForContent("HELLO WORLD", Charset.UTF_8))
        assertEquals(EncodingMode.BINARY, encoder.getEncodingModeForContent("テスト", Charset.UTF_8))
        assertEquals(EncodingMode.KANJI, encoder.getEncodingModeForContent("テスト", Charset.SHIFT_JIS))
        assertEquals(EncodingMode.BINARY, encoder.getEncodingModeForContent("Hello World", Charset.UTF_8))
        assertEquals(EncodingMode.NUMERIC, encoder.getEncodingModeForContent("61257", Charset.UTF_8))
    }
}