package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ml.WaterMLEngine
import com.example.model.WaterParameters
import com.example.model.WaterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    assertEquals("Hydro AI", appName)
  }

  @Test
  fun `test WaterMLEngine classifies Type A Freshwater`() {
    val params = WaterParameters(ph = 7.3, tds = 250.0, ec = 380.0, turbidity = 0.8, hardness = 90.0, organicCarbon = 1.0)
    val result = WaterMLEngine.classifyAndPredict(params, 10000.0)
    assertEquals(WaterType.TYPE_A, result.waterType)
    assertTrue(result.predictedYieldPercent >= 94.0)
    assertTrue(result.convertibilityScore >= 90)
  }

  @Test
  fun `test WaterMLEngine classifies Type B Brackish Water`() {
    val params = WaterParameters(ph = 7.7, tds = 2800.0, ec = 4200.0, turbidity = 1.2, hardness = 420.0, organicCarbon = 1.8)
    val result = WaterMLEngine.classifyAndPredict(params, 10000.0)
    assertEquals(WaterType.TYPE_B, result.waterType)
    assertTrue(result.predictedYieldPercent in 70.0..82.0)
  }

  @Test
  fun `test WaterMLEngine classifies Type C Agri Runoff`() {
    val params = WaterParameters(ph = 7.5, tds = 1200.0, ec = 1800.0, turbidity = 22.0, hardness = 200.0, organicCarbon = 8.5)
    val result = WaterMLEngine.classifyAndPredict(params, 10000.0)
    assertEquals(WaterType.TYPE_C, result.waterType)
    assertTrue(result.predictedYieldPercent in 84.0..92.0)
  }

  @Test
  fun `test WaterMLEngine classifies Type D Acidic Mine Drainage`() {
    val params = WaterParameters(ph = 4.1, tds = 3800.0, ec = 6000.0, turbidity = 11.0, hardness = 320.0, organicCarbon = 2.0)
    val result = WaterMLEngine.classifyAndPredict(params, 10000.0)
    assertEquals(WaterType.TYPE_D, result.waterType)
    assertEquals(90.0, result.predictedYieldPercent, 0.1)
  }

  @Test
  fun `test WaterMLEngine classifies Type E Seawater`() {
    val params = WaterParameters(ph = 8.1, tds = 35000.0, ec = 52000.0, turbidity = 2.0, hardness = 1400.0, organicCarbon = 2.5)
    val result = WaterMLEngine.classifyAndPredict(params, 10000.0)
    assertEquals(WaterType.TYPE_E, result.waterType)
    assertTrue(result.predictedYieldPercent in 34.0..46.0)
  }
}

