package de.zweistein2.barcoder.qrcode

import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.reserved
import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.values
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

internal class MaskUtilTest {
    private val matrix = MatrixUtil.initiateMatrix(1)

    @BeforeEach
    fun setUp() {
        // 1-Q "HELLO WORLD"
        val finalPayload = mutableListOf(32, 91, 11, 120, 209, 114, 220, 77, 67, 64, 236, 17, 236, 168, 72, 22, 82, 217, 54, 156, 0, 46, 15, 180, 122, 16)

        MatrixUtil.placePayloadInMatrix(matrix, finalPayload)
    }

    @Test
    fun determineMaskPenaltyTest() {
        assertEquals(644, MaskUtil.determineMaskPenalty(matrix.values(), matrix.reserved(), MaskPattern.PATTERN_1))
        assertEquals(498, MaskUtil.determineMaskPenalty(matrix.values(), matrix.reserved(), MaskPattern.PATTERN_2))
        assertEquals(618, MaskUtil.determineMaskPenalty(matrix.values(), matrix.reserved(), MaskPattern.PATTERN_3))
        assertEquals(553, MaskUtil.determineMaskPenalty(matrix.values(), matrix.reserved(), MaskPattern.PATTERN_4))
        assertEquals(675, MaskUtil.determineMaskPenalty(matrix.values(), matrix.reserved(), MaskPattern.PATTERN_5))
        assertEquals(564, MaskUtil.determineMaskPenalty(matrix.values(), matrix.reserved(), MaskPattern.PATTERN_6))
        assertEquals(432, MaskUtil.determineMaskPenalty(matrix.values(), matrix.reserved(), MaskPattern.PATTERN_7))
        assertEquals(694, MaskUtil.determineMaskPenalty(matrix.values(), matrix.reserved(), MaskPattern.PATTERN_8))
    }
}