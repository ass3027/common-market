package com.helpme.commonmarket.kotlin

import org.junit.jupiter.api.Test
import java.util.Locale

class LanguageTest {

    @Test
    fun toTest(){
        val testData = 1..31
        val associate2 = testData.associate { it.toLong() to getOrdinal(it) }
        println(testData)
        println(associate2)
        println(associate2.toString().toSlug())
    }

}

private fun getOrdinal(n: Int) = when {
    n in 11..13 -> "${n}th"
    n % 10 == 1 -> "${n}st"
    n % 10 == 2 -> "${n}nd"
    n % 10 == 3 -> "${n}rd"
    else -> "${n}th"
}

fun String.toSlug() = lowercase(Locale.getDefault())
    .replace("\n", " ")
    .replace("[^a-z\\d\\s]".toRegex(), " ")
    .split(" ")
    .joinToString("-")
    .replace("-+".toRegex(), "-")