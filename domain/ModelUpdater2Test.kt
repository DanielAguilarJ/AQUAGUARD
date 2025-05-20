package com.tuempresa.fugas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelUpdater2Test {
    @Test
    fun `compareVersions returns positive when first greater`() {
        assertEquals(1, ModelUpdater2.compareVersions("1.2.0", "1.1.9") > 0, true)
    }

    @Test
    fun `compareVersions returns negative when first lower`() {
        assertEquals(-1, ModelUpdater2.compareVersions("1.0.1", "1.0.2") < 0, true)
    }

    @Test
    fun `compareVersions returns zero for equal versions`() {
        assertEquals(0, ModelUpdater2.compareVersions("2.0.0", "2.0.0"))
    }

    @Test
    fun `compareVersions handles different length parts`() {
        assertEquals(0, ModelUpdater2.compareVersions("1.0", "1.0.0"))
    }

    @Test
    fun `extractVersion returns correct version string`() {
        val url = "https://example.com/models/model_v1.2.3.tflite"
        assertEquals("1.2.3", ModelUpdater2.extractVersion(url))
    }

    @Test
    fun `extractVersion returns null when no version present`() {
        val url = "https://example.com/models/model.tflite"
        assertNull(ModelUpdater2.extractVersion(url))
    }
}
