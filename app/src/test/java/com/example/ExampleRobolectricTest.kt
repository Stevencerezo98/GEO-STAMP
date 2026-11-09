package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.util.CollageConfig
import com.example.util.CollageLayout
import com.example.util.CollageMaker
import com.example.util.ImageStamper
import com.example.util.StampConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("StampCam", appName)
    }

    @Test
    fun `apply stamp to bitmap produces valid stamped image`() {
        val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val config = StampConfig(
            dateTime = "06/11/2026 10:00:00",
            latitude = 19.4326,
            longitude = -99.1332,
            address = "Av. Reforma 100",
            project = "Proyecto Test",
            notes = "Nota de prueba"
        )
        val stamped = ImageStamper.applyStamp(bitmap, config)
        assertNotNull(stamped)
        assertEquals(400, stamped.width)
        assertEquals(400, stamped.height)
    }

    @Test
    fun `collage maker generates collage bitmap`() {
        val bmp1 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val bmp2 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val config = CollageConfig(
            title = "Test Collage",
            layoutType = CollageLayout.HORIZONTAL_2,
            targetWidth = 600,
            targetHeight = 400
        )
        val collage = CollageMaker.createCollage(listOf(bmp1, bmp2), config)
        assertNotNull(collage)
        assertEquals(600, collage.width)
        assertEquals(400, collage.height)
    }
}
