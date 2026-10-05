/*
 * Copyright 2026 Proify, Tomakino
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.proify.lyricon.statusbarlyric

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * 状态栏歌词播放进度条
 * 极细高度，绘制在歌词下方，支持单色跟随和彩虹渐变
 */
class LyricProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        /** 颜色模式：跟随状态栏颜色 */
        const val COLOR_MODE_FOLLOW = 0

        /** 颜色模式：彩虹渐变 */
        const val COLOR_MODE_RAINBOW = 1

        /** 预设彩虹渐变色（暗色模式） */
        private val RAINBOW_COLORS = intArrayOf(
            0xFFFF4D4F.toInt(),
            0xFFFF9F43.toInt(),
            0xFFFFD93D.toInt(),
            0xFF2ED573.toInt(),
            0xFF1E90FF.toInt(),
            0xFF5352ED.toInt(),
            0xFFA55EEA.toInt()
        )
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 进度 0f ~ 1f */
    var progress: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (field == clamped) return
            field = clamped
            invalidate()
        }

    /** 进度条高度（像素） */
    var barHeight: Float = 2f
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
        }

    /** 进度条圆角半径 */
    var cornerRadius: Float = 1f

    /** 颜色模式：0=跟随状态栏，1=彩虹渐变 */
    var colorMode: Int = COLOR_MODE_RAINBOW
        set(value) {
            if (field == value) return
            field = value
            progressPaint.shader = null
            invalidate()
        }

    /** 单色模式下的进度颜色 */
    private var singleProgressColor: Int = 0xFFFFFFFF.toInt()

    init {
        setWillNotDraw(false)
    }

    fun setColors(backgroundColor: Int, progressColor: Int) {
        backgroundPaint.color = backgroundColor
        singleProgressColor = progressColor
        if (colorMode == COLOR_MODE_FOLLOW) {
            progressPaint.color = progressColor
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (colorMode == COLOR_MODE_RAINBOW && w > 0) {
            progressPaint.shader = LinearGradient(
                0f, 0f, w.toFloat(), 0f,
                RAINBOW_COLORS, null, Shader.TileMode.CLAMP
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = barHeight.toInt() + paddingTop + paddingBottom
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(suggestedMinimumWidth),
            height
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val top = paddingTop.toFloat()
        val bottom = top + barHeight
        val right = width.toFloat()

        // 彩虹模式：确保进度 shader 已设置
        if (colorMode == COLOR_MODE_RAINBOW) {
            if (progressPaint.shader == null && width > 0) {
                progressPaint.shader = LinearGradient(
                    0f, 0f, right, 0f,
                    RAINBOW_COLORS, null, Shader.TileMode.CLAMP
                )
            }
        } else {
            progressPaint.shader = null
            progressPaint.color = singleProgressColor
        }

        // 已播放进度
        val progressWidth = right * progress
        if (progressWidth > 0) {
            if (cornerRadius > 0) {
                canvas.drawRoundRect(
                    0f, top, progressWidth, bottom,
                    cornerRadius, cornerRadius, progressPaint
                )
            } else {
                canvas.drawRect(0f, top, progressWidth, bottom, progressPaint)
            }
        }
    }
}
