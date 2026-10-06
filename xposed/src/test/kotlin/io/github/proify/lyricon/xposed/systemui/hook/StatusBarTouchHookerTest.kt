package io.github.proify.lyricon.xposed.systemui.hook

import android.app.Application
import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import io.github.libxposed.api.XposedInterface
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, application = Application::class)
class StatusBarTouchHookerTest {
    private class Window(context: Context) : FrameLayout(context) {
        var virtualDispatches = 0
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            virtualDispatches++
            error("Virtual dispatch re-enters the hooked method instead of continuing the chain")
        }
    }

    private fun replay(actions: List<Int>, originalResult: Boolean) {
        val window = Window(RuntimeEnvironment.getApplication())
        val continuedActions = mutableListOf<Int>()
        val replayDepth = StatusBarTouchHooker::class.java.getDeclaredField("redispatchDepth").apply { isAccessible = true }
        val handle = StatusBarTouchHooker::class.java.getDeclaredMethod("handleDispatchTouchEvent",
            XposedInterface.Chain::class.java, ViewGroup::class.java).apply { isAccessible = true }
        try {
            replayDepth.setInt(null, 1)
            actions.forEachIndexed { index, action ->
                val event = MotionEvent.obtain(0, index * 50L, action, 100f, 20f, 0)
                try {
                    val chain = Proxy.newProxyInstance(XposedInterface.Chain::class.java.classLoader,
                        arrayOf(XposedInterface.Chain::class.java)) { _, method, _ ->
                        when (method.name) {
                            "proceed" -> { continuedActions += event.actionMasked; originalResult }
                            "getArgs" -> listOf(event)
                            "getThisObject" -> window
                            else -> error("Unexpected chain method: ${method.name}")
                        }
                    } as XposedInterface.Chain
                    assertEquals(originalResult, handle.invoke(StatusBarTouchHooker, chain, window))
                } finally { event.recycle() }
            }
        } finally { replayDepth.setInt(null, 0) }
        assertEquals(actions, continuedActions)
        assertEquals(0, window.virtualDispatches)
    }

    @Test fun `system replay continues the real hook chain once for each event`() {
        replay(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP), true)
    }

    @Test fun `system replay preserves an unhandled result without virtual redispatch`() {
        replay(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_CANCEL), false)
    }
}
