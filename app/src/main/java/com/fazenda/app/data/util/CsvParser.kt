package com.fazenda.app.data.util

import java.io.BufferedReader

object CsvParser {
    /**
     * Parses a line from a CSV file, respecting quoted values with commas
     */
    fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]

            when {
                char == '"' -> {
                    inQuotes = !inQuotes
                    i++
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                    i++
                }
                else -> {
                    current.append(char)
                    i++
                }
            }
        }

        result.add(current.toString().trim())
        return result
    }

    /**
     * Reads all lines from a CSV file, skipping the header
     */
    fun readAllLines(reader: BufferedReader): List<String> {
        val lines = mutableListOf<String>()
        reader.useLines { sequence ->
            sequence.filter { it.isNotBlank() }
                    .drop(1)  // Skip header
                    .forEach { lines.add(it) }
        }
        return lines
    }
}
