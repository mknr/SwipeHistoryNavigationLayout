package com.github.swipehistorynavigation

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EdgeEffect
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SwipeHistoryNavigationLayout : FrameLayout {
    private val leftHandleView: HandleView
    private val rightHandleView: HandleView
    private val rightEdgeEffect: EdgeEffect

    // Styleable properties
    private val iconWidth: Float = resources.getDimension(R.dimen.handle_icon_size)
    private val backgroundDrawable: Drawable?
    private val leftEdgeDrawable: Drawable?
    private val rightEdgeDrawable: Drawable?
    private val firstText: String
    private val inactiveColor: Int
    private val activeColor: Int
    // end Styleable properties

    private var leftHandleFirstPos: Float = Float.NaN
    private var rightHandleFirstPos: Float = Float.NaN

    /**
     * Left edge touch detection width.
     */
    private var leftEdgeWidth = Float.NaN

    /**
     * Right edge touch detection width.
     */
    private var rightEdgeWidth = Float.NaN

    /**
     * Swipeable width.
     */
    private var swipeableWidth = Float.NaN

    /**
     * Percentage of screen edges to be judged.
     */
    private var edgePer = 5 / 100f

    /**
     * Ratio of swipeable width to screen width..
     */
    private var swipeablePer = 15 / 100f

    private var firstTouchX: Int = Int.MIN_VALUE
    private var isSwipingLeftEdge = false
    private var isSwipingRightEdge = false

    private var lastTouchX: Float = Float.NaN
    private var oldDeltaX: Float = Float.NaN
    private var deltaX: Float = Float.NaN
    private var isSwipeReachesLimit = false

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
    ) : super(context, attrs, defStyle) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.SwipeHistoryNavigationLayout, 0, 0)
            .apply {
                backgroundDrawable =
                    getDrawable(R.styleable.SwipeHistoryNavigationLayout_handleBackground)
                leftEdgeDrawable =
                    getDrawable(R.styleable.SwipeHistoryNavigationLayout_leftHandleDrawable)
                        ?: ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.ic_baseline_arrow_back_24,
                            context.theme
                        )
                rightEdgeDrawable =
                    getDrawable(R.styleable.SwipeHistoryNavigationLayout_rightHandleDrawable)
                        ?: ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.ic_baseline_arrow_forward_24,
                            context.theme
                        )
                firstText =
                    getString(R.styleable.SwipeHistoryNavigationLayout_leftHandleLabel) ?: ""
                inactiveColor = getColor(
                    R.styleable.SwipeHistoryNavigationLayout_inactiveColor,
                    ResourcesCompat.getColor(resources, R.color.color_inactive, context.theme)
                )
                activeColor = getColor(
                    R.styleable.SwipeHistoryNavigationLayout_activeColor,
                    ResourcesCompat.getColor(resources, R.color.color_active, context.theme)
                )
            }

        leftHandleView = HandleView(
            context,
            backgroundDrawable,
            leftEdgeDrawable,
            firstText,
            inactiveColor,
            activeColor
        )
        rightHandleView = HandleView(
            context,
            backgroundDrawable,
            rightEdgeDrawable,
            "",
            inactiveColor,
            activeColor
        )
        rightEdgeEffect = EdgeEffect(context)
        setWillNotDraw(false)
    }

    @SuppressLint("RtlHardcoded")
    override fun onFinishInflate() {
        super.onFinishInflate()
        val leftParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_VERTICAL or Gravity.LEFT
        )
        addView(leftHandleView, leftParams)
        addView(
            rightHandleView, LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL
            )
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            leftHandleView.let {
                leftHandleFirstPos = -iconWidth
                it.translationX = leftHandleFirstPos
            }
            rightHandleView.let {
                rightHandleFirstPos = width + iconWidth
                it.translationX = rightHandleFirstPos
            }

            leftEdgeWidth = width.toFloat() * edgePer
            rightEdgeWidth = width - leftEdgeWidth
            swipeableWidth = width.toFloat() * swipeablePer
        }
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (isTouchedEdge(ev)) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        var needsInvalidate = false
        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isLeftEdge(ev.x) && listener.canSwipeLeftEdge()) {
                    isSwipingLeftEdge = true
                    firstTouchX = ev.x.toInt()
                    leftEdgeGrabbed()
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                } else if (isRightEdge(ev.x)) {
                    isSwipingRightEdge = true
                    firstTouchX = width
                    rightEdgeGrabbed()
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                lastTouchX = ev.x
                oldDeltaX = deltaX
                deltaX = abs(lastTouchX - firstTouchX)
                if (isSwipingLeftEdge) {
                    moveLeftHandle()
                } else if (isSwipingRightEdge) {
                    if (listener.canSwipeRightEdge()) {
                        moveRightHandle()
                    } else if (deltaX > oldDeltaX) {
                        val over = abs(deltaX - oldDeltaX)
                        rightEdgeEffect.onPull(over / width)
                        needsInvalidate = true
                    }
                }

                val rightOffset = isSwipingRightEdge.let { +iconWidth }
                if (deltaX > swipeableWidth + rightOffset) {
                    if (!isSwipeReachesLimit) {
                        isSwipeReachesLimit = true
                        swipeReachesLimit()
                    }
                } else {
                    if (isSwipeReachesLimit) {
                        isSwipeReachesLimit = false
                        leaveHandle()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                needsInvalidate = releaseSwipe()
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }

        return super.onTouchEvent(ev)
    }


    private fun isLeftEdge(x: Float) = x <= leftEdgeWidth
    private fun isRightEdge(x: Float) = x >= rightEdgeWidth

    private fun isTouchedEdge(ev: MotionEvent?): Boolean {
        return ev?.action == MotionEvent.ACTION_DOWN && (isLeftEdge(ev.x) || isRightEdge(ev.x))
    }

    private fun moveLeftHandle() {
        leftHandleView.let {
            val value = deltaX - firstTouchX - iconWidth
            it.translationX = min(value, swipeableWidth - iconWidth)
        }
    }

    private fun moveRightHandle() {
        rightHandleView.let {
            val value = firstTouchX - deltaX + iconWidth / 2
            it.translationX = max(value, width - swipeableWidth)
        }
    }

    private fun leftEdgeGrabbed() {
        leftHandleView.setText(listener.getGoBackLabel())
    }

    private fun rightEdgeGrabbed() {
    }

    private fun releaseSwipe(): Boolean {
        rightEdgeEffect.onRelease()

        if (isSwipingLeftEdge) {
            if (isSwipeReachesLimit) {
                leaveHandle()
                listener.goBack()
            }
            leftHandleView.let {
                val animator = ObjectAnimator.ofFloat(
                    it,
                    View.TRANSLATION_X,
                    it.translationX,
                    leftHandleFirstPos
                )
                animator.duration = 400
                animator.start()
            }
        } else if (isSwipingRightEdge) {
            if (isSwipeReachesLimit) {
                leaveHandle()
                listener.goForward()
            }
            rightHandleView.let {
                val animator = ObjectAnimator.ofFloat(
                    it,
                    View.TRANSLATION_X,
                    it.translationX,
                    rightHandleFirstPos
                )
                animator.duration = 400
                animator.start()
            }
        }
        isSwipingLeftEdge = false
        isSwipingRightEdge = false
        isSwipeReachesLimit = false
        return rightEdgeEffect.isFinished
    }

    private fun swipeReachesLimit() {
        if (isSwipingLeftEdge && listener.canSwipeLeftEdge()) {
            listener.leftSwipeReachesLimit()
            leftHandleView.animateActive()
            leftHandleView.animateShowText()
        } else if (isSwipingRightEdge && listener.canSwipeRightEdge()) {
            listener.rightSwipeReachesLimit()
            rightHandleView.animateActive()
            rightHandleView.animateShowText()
        }
    }

    private fun leaveHandle() {
        if (isSwipingLeftEdge) {
            leftHandleView.animateInactive()
            leftHandleView.animateHideText()
        } else if (isSwipingRightEdge) {
            rightHandleView.animateInactive()
            rightHandleView.animateHideText()
        }
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        var needsInvalidate = false
        if (overScrollMode == OVER_SCROLL_ALWAYS || overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS) {
            if (!rightEdgeEffect.isFinished) {
                canvas?.let {
                    val restoreCount: Int = canvas.save()
                    val width: Int = width
                    val height: Int = height - paddingTop - paddingBottom

                    canvas.rotate(90f)
                    canvas.translate(paddingTop.toFloat(), -width.toFloat())
                    rightEdgeEffect.setSize(height, width)
                    needsInvalidate = needsInvalidate or rightEdgeEffect.draw(canvas)
                    canvas.restoreToCount(restoreCount)
                }

            }
        } else {
            rightEdgeEffect.finish()
        }
        if (needsInvalidate) {
            // Keep animating
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }


    var listener: OnListener = object : OnListener {
        override fun canSwipeLeftEdge(): Boolean = true
        override fun canSwipeRightEdge(): Boolean = true
        override fun getGoBackLabel(): String = ""
        override fun goBack(): Boolean = true
        override fun goForward(): Boolean = true
        override fun leftSwipeReachesLimit() {}
        override fun rightSwipeReachesLimit() {}
    }

    interface OnListener {
        /**
         * Return true if left-edge swipe is to be enabled.
         */
        fun canSwipeLeftEdge(): Boolean

        /**
         * Return true if right-edge swipe is to be enabled.
         */
        fun canSwipeRightEdge(): Boolean

        /**
         * Called when you grab the left edge.
         * Text to be displayed when swiping to the limit.
         */
        fun getGoBackLabel(): String

        /**
         * Implement the page back operation.
         */
        fun goBack(): Boolean

        /**
         * Implement the page forward operation.
         */
        fun goForward(): Boolean

        /**
         * Called when the movement of the left-edge swipe reaches its limit.
         */
        fun leftSwipeReachesLimit()

        /**
         * Called when the movement of the right-edge swipe reaches its limit.
         */
        fun rightSwipeReachesLimit()
    }
}