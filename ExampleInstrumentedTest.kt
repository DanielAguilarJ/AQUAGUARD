package com.tuempresa.fugas

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Contexto de la app bajo prueba.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.tuempresa.fugas", appContext.packageName)
    }
}
