package app.pulse.acr.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun testFormatDuration() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:05", formatDuration(5000))
        assertEquals("1:00", formatDuration(60000))
        assertEquals("1:05", formatDuration(65000))
        assertEquals("60:00", formatDuration(3600000))
        
        // Coercing negative values to 0
        assertEquals("0:00", formatDuration(-1000))
    }

    @Test
    fun testFormatBytes() {
        assertEquals("0 B", formatBytes(0L))
        assertEquals("500 B", formatBytes(500L))
        assertEquals("1.0 KB", formatBytes(1024L))
        assertEquals("1.5 KB", formatBytes(1536L))
        assertEquals("1.0 MB", formatBytes(1048576L))
        assertEquals("1.5 MB", formatBytes(1572864L))
    }
}
