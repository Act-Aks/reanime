package com.reanime.core.model.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class ResultTest {
    private enum class TestError : Error { INVALID, MAPPED }

    @Test
    fun `success maps data without changing the error type`() {
        val result: Result<Int, TestError> = Result.Success(3)
        assertEquals(Result.Success("3"), result.map(Int::toString))
    }

    @Test
    fun `failure skips success transformation and retains the original instance`() {
        val failure: Result<Int, TestError> = Result.Failure(TestError.INVALID)
        var transformed = false
        val mapped = failure.map {
            transformed = true
            it.toString()
        }

        assertSame(failure, mapped)
        assertFalse(transformed)
    }

    @Test
    fun `error mapping preserves success`() {
        val success: Result<Int, TestError> = Result.Success(3)
        assertSame(success, success.mapError { TestError.MAPPED })
    }

    @Test
    fun `error mapping transforms only the failure`() {
        val failure: Result<Int, TestError> = Result.Failure(TestError.INVALID)
        assertEquals(Result.Failure(TestError.MAPPED), failure.mapError { TestError.MAPPED })
    }

    @Test(expected = IllegalStateException::class)
    fun `mapping does not swallow programming defects`() {
        val success: Result<Int, TestError> = Result.Success(3)
        success.map { error("programming defect") }
    }
}
