package io.github.proify.lyricon.app.gesture

import android.app.Application
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.View
import android.widget.FrameLayout
import io.github.proify.lyricon.lyric.style.LyricStyle
import io.github.proify.lyricon.statusbarlyric.StatusBarLyric
import io.github.proify.lyricon.statusbarlyric.StatusBarLyric.GestureType
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StatusBarLyricGestureTest {
    private val received = mutableListOf<GestureType>()

    private fun view(vararg enabled: GestureType) = StatusBarLyric(RuntimeEnvironment.getApplication(), LyricStyle(), null).apply {
        enabledGestures = enabled.toSet()
        gestureEnabled = true
        hapticEnabled = false
        gestureListener = { received += it }
    }

    private fun touch(view: StatusBarLyric, action: Int, down: Long, x: Float, time: Long = SystemClock.uptimeMillis()): Boolean {
        val event = MotionEvent.obtain(down, time, action, x, 0f, 0)
        return try { view.onTouchEvent(event) } finally { event.recycle() }
    }

    private fun swipe(view: StatusBarLyric, dx: Float) {
        val down = SystemClock.uptimeMillis()
        touch(view, MotionEvent.ACTION_DOWN, down, 200f)
        touch(view, MotionEvent.ACTION_MOVE, down, 200f + dx, down + 50)
        touch(view, MotionEvent.ACTION_UP, down, 200f + dx, down + 100)
    }

    @Test fun `no action on both swipes produces no drag or callback`() {
        val view = view(GestureType.TAP, GestureType.LONG_PRESS)
        for (dx in listOf(-120f, 120f)) {
            val down = SystemClock.uptimeMillis()
            touch(view, MotionEvent.ACTION_DOWN, down, 200f)
            touch(view, MotionEvent.ACTION_MOVE, down, 200f + dx, down + 50)
            assertEquals(0f, view.translationX, 0f)
            touch(view, MotionEvent.ACTION_UP, down, 200f + dx, down + 100)
        }
        assertTrue(received.isEmpty())
    }

    @Test fun `one enabled swipe still works while the opposite direction is ignored`() {
        val view = view(GestureType.SWIPE_RIGHT)
        swipe(view, -120f)
        assertTrue(received.isEmpty())
        swipe(view, 120f)
        assertEquals(listOf(GestureType.SWIPE_RIGHT), received)
    }

    @Test fun `tap remains available when both swipes have no action`() {
        val view = view(GestureType.TAP, GestureType.LONG_PRESS)
        val down = SystemClock.uptimeMillis()
        touch(view, MotionEvent.ACTION_DOWN, down, 200f)
        touch(view, MotionEvent.ACTION_UP, down, 200f, down + 100)
        assertEquals(listOf(GestureType.TAP), received)
    }

    @Test fun `long press remains available when both swipes have no action`() {
        val view = view(GestureType.TAP, GestureType.LONG_PRESS)
        val down = SystemClock.uptimeMillis()
        touch(view, MotionEvent.ACTION_DOWN, down, 200f)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getLongPressTimeout().toLong() + 100))
        touch(view, MotionEvent.ACTION_UP, down, 200f)
        assertEquals(listOf(GestureType.LONG_PRESS), received)
    }

    @Test fun `disabled tap does not trigger a callback`() {
        val view = view(GestureType.SWIPE_RIGHT)
        val down = SystemClock.uptimeMillis()
        touch(view, MotionEvent.ACTION_DOWN, down, 200f)
        touch(view, MotionEvent.ACTION_UP, down, 200f, down + 100)
        assertTrue(received.isEmpty())
    }

    @Test fun `all actions disabled leaves the view non clickable and declines down`() {
        val view = view()
        assertFalse(view.isClickable)
        val down = SystemClock.uptimeMillis()
        assertFalse(touch(view, MotionEvent.ACTION_DOWN, down, 200f))
        assertTrue(received.isEmpty())
    }

    @Test fun `system replay bypasses the lyric and delivers the whole gesture to its parent`() {
        val context = RuntimeEnvironment.getApplication()
        val systemEvents = mutableListOf<Int>()
        val parent = object : FrameLayout(context) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                systemEvents += event.actionMasked
                return true
            }
        }
        val view = view(GestureType.TAP, GestureType.LONG_PRESS).apply { visibility = View.VISIBLE }
        parent.addView(view, FrameLayout.LayoutParams(400, 100))
        parent.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        parent.layout(0, 0, 400, 100)
        val down = SystemClock.uptimeMillis()
        for ((action, x, elapsed) in listOf(Triple(MotionEvent.ACTION_DOWN, 200f, 0L),
            Triple(MotionEvent.ACTION_MOVE, 80f, 50L), Triple(MotionEvent.ACTION_UP, 80f, 100L))) {
            val event = MotionEvent.obtain(down, down + elapsed, action, x, 40f, 0)
            try {
                view.withTouchPassThrough { assertTrue(parent.dispatchTouchEvent(event)) }
            } finally { event.recycle() }
        }
        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP), systemEvents)
        assertTrue(received.isEmpty())
        assertTrue(touch(view, MotionEvent.ACTION_DOWN, SystemClock.uptimeMillis(), 200f))
    }
}
