package com.tomtom.buster

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class ExampleMockkTest {
    class ClassForMocking {
        fun functionToMock() = Random(0).nextInt(0, 100)
    }

    class ClassForTesting(val classForMocking: ClassForMocking) {
        fun functionToTest() = classForMocking.functionToMock() + classForMocking.functionToMock()
    }

    @Test
    fun mockkTest() {
        val mock = mockk<ClassForMocking>()
        every { mock.functionToMock() } returns 4
        val test = ClassForTesting(mock)
        assertEquals(test.functionToTest(), 8)
    }
}
