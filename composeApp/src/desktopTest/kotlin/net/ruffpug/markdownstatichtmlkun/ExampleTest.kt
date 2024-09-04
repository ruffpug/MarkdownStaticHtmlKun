package net.ruffpug.markdownstatichtmlkun

import kotlin.test.assertEquals
import org.junit.Test

internal class ExampleTest {

    @Test
    fun example() {
        fun add(x: Int, y: Int): Int {
            return x + y
        }

        assertEquals(expected = 3, actual = add(1, 2))
    }
}
