/*
 * Copyright (C) 2021 Tony Guyot
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.tonyguyot.minipaint

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat

private const val STROKE_WIDTH = 12.0f // has to be a float

class MyCanvasView(context: Context) : View(context) {
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    private val backgroundColor =
        ResourcesCompat.getColor(resources, R.color.colorBackground, null)
    private val drawColor =
        ResourcesCompat.getColor(resources, R.color.colorPaint, null)

    private var motionTouchEventX = 0.0f
    private var motionTouchEventY = 0.0f

    private var currentX = 0.0f
    private var currentY = 0.0f

    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    // Set up the paint with which to draw
    private val paint = Paint().apply {
        color = drawColor
        // Smooth out edges of what is drawn without affecting shape
        isAntiAlias = true
        // Dithering affects how colors with higher precision than the device are down-sampled
        isDither = true
        style = Paint.Style.STROKE // default is FILL
        strokeJoin = Paint.Join.ROUND // default is MITER
        strokeCap = Paint.Cap.ROUND // default is BUTT
        strokeWidth = STROKE_WIDTH // default is hairline-width (very thin)
    }

    private val path = Path()

    private lateinit var frame: Rect

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)

        // Calculate a rectangular frame around the picture
        val inset = 40
        frame = Rect(inset, inset, w - inset, h - inset)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(extraBitmap, 0.0f, 0.0f, null)

        // Draw a frame around the canvas
        canvas?.drawRect(frame, paint)
    }

    private fun touchStart() {
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    private fun touchMove() {
        val dx = Math.abs(motionTouchEventX - currentX)
        val dy = Math.abs(motionTouchEventY - currentY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // QuadTo() adds a quadratic bezier from the last point,
            // approaching control point (x1, y1), and ending at (x2, y2)
            path.quadTo(currentX, currentY, (motionTouchEventX + currentX) / 2,
                (motionTouchEventY + currentY) / 2)
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            // Draw the path in the extra bitmap to cache it
            extraCanvas.drawPath(path, paint)
        }
        invalidate()
    }

    private fun touchUp() {
        // Reset the path so it doesn't get drawn again
        path.reset()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }
        return true
    }
}