package com.zjutjh.qaq

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class MessageRecycleView : RecyclerView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var onTouchAction: (v: View) -> Boolean = fun(_): Boolean {
        return false
    }

    fun setOnTouchAction(action: (v: View) -> Boolean) {
        onTouchAction = action
    }

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        super.onTouchEvent(e)

        if (e != null) {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> return true
                MotionEvent.ACTION_UP -> {
                    performClick()
                    return true
                }
            }
        }

        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return onTouchAction(this)
    }

}