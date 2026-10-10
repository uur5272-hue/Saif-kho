package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppControlAction
import com.example.util.DeviceControlHelper
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
    assertEquals("MOTO AI", appName)
  }

  @Test
  fun `test device control action parser`() {
    val action = DeviceControlHelper.parsePotentialAction("open youtube")
    assertTrue(action is AppControlAction.LaunchApp)
    assertEquals("youtube", (action as AppControlAction.LaunchApp).appName)

    val callAction = DeviceControlHelper.parsePotentialAction("call John Doe")
    assertTrue(callAction is AppControlAction.InitiateCall)
    assertEquals("John Doe", (callAction as AppControlAction.InitiateCall).contactName)
  }
}
