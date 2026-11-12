package io.github.dimitrysaf.asko.data.importing

/**
 * Rows of a CSV file: fields split on [delimiter], quoted fields ("a, b", with "" for a quote)
 * kept whole, even across lines. Blank lines are skipped.
 */
internal fun parseCsv(text: String, delimiter: Char): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var i = 0
    fun endField() {
        row += field.toString()
        field.clear()
    }
    fun endRow() {
        endField()
        if (row.any { it.isNotBlank() }) rows += row
        row = mutableListOf()
    }
    while (i < text.length) {
        val c = text[i]
        when {
            quoted && c == '"' && text.getOrNull(i + 1) == '"' -> {
                field.append('"')
                i++
            }
            c == '"' -> quoted = !quoted
            !quoted && c == delimiter -> endField()
            !quoted && (c == '\n' || c == '\r') -> {
                if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                endRow()
            }
            else -> field.append(c)
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) endRow()
    return rows
}

/** The delimiter a CSV's header line uses: Strong's older exports use semicolons. */
internal fun delimiterOf(text: String): Char {
    val header = text.lineSequence().firstOrNull().orEmpty()
    return if (header.count { it == ';' } > header.count { it == ',' }) ';' else ','
}
