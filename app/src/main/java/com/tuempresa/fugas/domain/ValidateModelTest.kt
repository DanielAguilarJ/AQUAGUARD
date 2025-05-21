package com.tuempresa.fugas.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class ValidateModelTest {
    @Test
    fun `validateModel returns false for small file`() {
        val temp = File.createTempFile("test", null)
        FileOutputStream(temp).use { it.write(ByteArray(10)) }
        assertFalse(ModelUpdater2.validateModel(temp, null))
        temp.delete()
    }

    @Test
    fun `validateModel returns true for valid file without checksum`() {
        val temp = File.createTempFile("test", null)
        FileOutputStream(temp).use { it.write(ByteArray(2048)) }
        assertTrue(ModelUpdater2.validateModel(temp, null))
        temp.delete()
    }

    @Test
    fun `validateModel returns false for checksum mismatch`() {
        val temp = File.createTempFile("test", null)
        FileOutputStream(temp).use { it.write("hello".toByteArray()) }
        // MD5 of "hello" is 5d41402abc4b2a76b9719d911017c592
        assertFalse(ModelUpdater2.validateModel(temp, "invalidchecksum"))
        temp.delete()
    }

    @Test
    fun `validateModel returns true for checksum match`() {
        val temp = File.createTempFile("test", null)
        val content = "abc".toByteArray()
        FileOutputStream(temp).use { it.write(content) }
        val expected = ModelUpdater2.computeMD5(temp)
        assertTrue(ModelUpdater2.validateModel(temp, expected))
        temp.delete()
    }
}
