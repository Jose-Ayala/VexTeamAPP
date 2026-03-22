package com.jayala.vexapp

private val TEAM_NUMBER_PATTERN = Regex("^(\\d+)(.*)$")

// Natural team-number ordering puts 2A before 10A and keeps alpha suffixes stable.
fun compareTeamNumbersNatural(left: String, right: String): Int {
    val leftMatch = TEAM_NUMBER_PATTERN.matchEntire(left.trim())
    val rightMatch = TEAM_NUMBER_PATTERN.matchEntire(right.trim())

    if (leftMatch != null && rightMatch != null) {
        val leftNumber = leftMatch.groupValues[1].toLongOrNull() ?: Long.MAX_VALUE
        val rightNumber = rightMatch.groupValues[1].toLongOrNull() ?: Long.MAX_VALUE

        val numberCompare = leftNumber.compareTo(rightNumber)
        if (numberCompare != 0) return numberCompare

        val suffixCompare = leftMatch.groupValues[2].compareTo(rightMatch.groupValues[2], ignoreCase = true)
        if (suffixCompare != 0) return suffixCompare
    } else if (leftMatch != null) {
        return -1
    } else if (rightMatch != null) {
        return 1
    }

    return left.compareTo(right, ignoreCase = true)
}

