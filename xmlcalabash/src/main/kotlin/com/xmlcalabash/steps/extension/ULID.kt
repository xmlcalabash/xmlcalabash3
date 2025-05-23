package com.xmlcalabash.steps.extension

import java.security.SecureRandom
import kotlin.math.floor

// Inspired by https://github.com/Lewiscowles1986/jULID but reimplemented to avoid a dependency

class ULID {
    companion object {
        private val encoding = listOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
            'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X',
            'Y', 'Z')
        private var lastTime = 0L

        fun next(): String {
            synchronized(encoding) {
                val encoded = CharArray(26)
                val random = SecureRandom()
                for (pos in 25 downTo 10) {
                    val rand = floor(random.nextDouble() * encoding.size).toInt()
                    encoded[pos] = encoding[rand]
                }

                val time = System.currentTimeMillis()
                if (time > lastTime) {
                    lastTime = time
                } else {
                    lastTime++
                }

                var instant = lastTime
                for (pos in 9 downTo 0) {
                    val mod = (instant % encoding.size).toInt()
                    encoded[pos] = encoding[mod]
                    instant = (instant - mod) / encoding.size
                }

                return String(encoded)
            }
        }
    }
}