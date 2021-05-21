package de.zweistein2.barcoder.qrcode

enum class MaskPattern(val bitSequence: String, val formula: (Int, Int) -> Boolean) {
    PATTERN_1("000", { row, col -> (row + col) % 2 == 0 } ),
    PATTERN_2("001", { row, _ -> row % 2 == 0 }),
    PATTERN_3("010", { _, col -> col % 3 == 0 }),
    PATTERN_4("011", { row, col -> (row + col) % 3 == 0 }),
    PATTERN_5("100", { row, col -> (row / 2 + col / 3) % 2 == 0 }),
    PATTERN_6("101", { row, col -> (row * col) % 2 + (row * col) % 3 == 0 }),
    PATTERN_7("110", { row, col -> ((row * col) % 3 + row * col) % 2 == 0 }),
    PATTERN_8("111", { row, col -> ((row * col) % 3 + row + col) % 2 == 0 });
}