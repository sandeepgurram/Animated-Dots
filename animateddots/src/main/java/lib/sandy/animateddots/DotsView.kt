package lib.sandy.animateddots

import android.animation.Animator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import kotlin.math.min
import kotlin.reflect.KProperty

class DotsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        View(context, attrs, defStyleAttr) {

    private val LOG_TAG = "anim-dots"

    private val translationTime: Long = 400
    private val attributes = context.theme.obtainStyledAttributes(
        attrs,
        R.styleable.DotsView,
        0, 0
    )

    var dotsCount: Int by PropertyDelegate(
        attributes.getInteger(
            R.styleable.DotsView_maxDots,
            10
        )
    )

    var dotSize: Float by PropertyDelegate(
        attributes.getDimension(
            R.styleable.DotsView_dotSize,
            20f
        )
    )

    var smallDotSize: Float by PropertyDelegate(
        attributes.getDimension(
            R.styleable.DotsView_dotsSpacing,
            dotSize * 3 / 4
        )
    )
    var dotSpacing: Float by PropertyDelegate(
        attributes.getDimension(
            R.styleable.DotsView_dotsSpacing,
            10f
        )
    )

    var visibleDots: Int by PropertyDelegate(
        attributes.getInteger(
            R.styleable.DotsView_visibleDots,
            10
        )
    )

    private val dotsDrawn
        get() = min(visibleDots, dotsCount)

    private var activeDots = 50
        set(value) {
            if (dotsCount > activeDots) {
                Log.d(LOG_TAG, "active dots count is greater than dots count, setting to active dots to dotsCount")
                field = dotsCount
            }
            field = value
        }

    private val isTailShown
        get() = visibleDots < dotsCount

    private val dotsList: List<Dot> by lazy {
        prepareDotsList()
    }

    fun prepareDotsList(): List<Dot> {
        val list = arrayListOf<Dot>()
        for (i in 1..dotsDrawn) {

            val paint = if (isTailShown) {

                if (activeDots < dotsDrawn) {
                    if (activeDots >= i) activePaint else inActivePaint
                } else if (activeDots < dotsCount) {
                    if (dotsDrawn != i) activePaint else inActivePaint
                } else if (activeDots == dotsCount) {
                    activePaint
                } else {
                    inActivePaint
                }

            } else {
                if (activeDots >= i) activePaint else inActivePaint
            }

            val size = if (isTailShown) {

                when {
                    activeDots < dotsDrawn -> {
                        if (i == dotsDrawn) smallDotSize else dotSize
                    }
                    activeDots == dotsCount -> {
                        if (i == 1) smallDotSize else dotSize
                    }
                    activeDots >= dotsDrawn -> {
                        if (i == dotsDrawn || i == 1) smallDotSize else dotSize
                    }
                    else -> dotSize
                }
            } else {
                dotSize
            }

            list.add(Dot(size = size, paint = paint))
        }
        return list
    }

    fun updatedDotsList() {
        dotsList.forEachIndexed { i, dot ->

            dot.size = if (isTailShown) {
                when {
                    activeDots < dotsDrawn -> {
                        if (i == dotsDrawn - 1) smallDotSize else dotSize
                    }
                    activeDots == dotsCount -> {
                        if (i == 0) smallDotSize else dotSize
                    }
                    activeDots >= dotsDrawn -> {
                        if (i == dotsDrawn - 1 || i == 0) smallDotSize else dotSize
                    }
                    else -> dotSize
                }
            } else {
                dotSize
            }
        }
    }

    private val inActiveDots
        get() = dotsCount - activeDots

    private val desiredWidth: Float
        get() = (dotSize * dotsDrawn) + ((dotsDrawn + 1) * dotSpacing)

    val desiredHeight
        get() = dotSize * 4

    val activeColor: Int by PropertyDelegate(
        attributes.getColor(
            R.styleable.DotsView_activeColor,
            ContextCompat.getColor(context, R.color.active)
        )
    )
    val inActiveColor: Int by PropertyDelegate(
        attributes.getColor(
            R.styleable.DotsView_inActiveColor,
            ContextCompat.getColor(context, R.color.active)
        )
    )
    val removeColor: Int by PropertyDelegate(
        attributes.getColor(
            R.styleable.DotsView_removeColor,
            ContextCompat.getColor(context, R.color.active)
        )
    )

    private val ripplePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeColor
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
    }

    private val activePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeColor
        }
    }

    private val zoomAnimPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeColor
        }
    }

    private val inActivePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inActiveColor
        }
    }

    private val removingPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = removeColor
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width: Int = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> min(desiredWidth.toInt(), MeasureSpec.getSize(widthMeasureSpec))
            else -> desiredWidth.toInt()
        }

        val height: Int = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> min(desiredHeight.toInt(), MeasureSpec.getSize(widthMeasureSpec))
            else -> desiredHeight.toInt()
        }

        Log.d("onMeasure", "calculated width - $width")
        Log.d("onMeasure", "calculated height - $height")

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        var xPos: Float = dotSpacing + dotSize / 2
        val list = dotsList
        list.forEachIndexed { i, dot ->
            Log.d(LOG_TAG, "UI state - ${dot.dotUIState}")
            when (dot.dotUIState) {
                DotUIState.removing -> {
                    val initialY = desiredHeight / 2 - animatedYValue
                    canvas?.drawCircle(xPos.minus(animatedXValue), initialY, dot.size / 2, dot.paint)
                }
                DotUIState.adding -> {
                    canvas?.drawCircle(xPos, desiredHeight / 2, dot.size / 2, zoomAnimPaint)
                }
                DotUIState.ripple -> {
                    canvas?.drawCircle(xPos, desiredHeight / 2, rippleSize / 2, ripplePaint)
                    canvas?.drawCircle(xPos, desiredHeight / 2, dot.size / 2, dot.paint)
                }
                else -> canvas?.drawCircle(xPos - xTranslation, desiredHeight / 2, dot.size / 2, dot.paint)
            }

            xPos += dotSpacing + dotSize
        }

        if (isInAddAnimationState) {
            canvas?.drawCircle(xPos - xTranslation, desiredHeight / 2, smallDotSize / 2, inActivePaint)
        }

    }

    private var propertyAlpha = PropertyValuesHolder.ofInt("PROPERTY_ALPHA", 0, 255)
    private var propertyRadius = PropertyValuesHolder.ofFloat("PROPERTY_RADIUS", dotSize * 3, dotSize)
    private var propertyRipple = PropertyValuesHolder.ofFloat("PROPERTY_RIPPLE", dotSize, dotSize * 4)
    private var propertyRippleAlpha = PropertyValuesHolder.ofInt("PROPERTY_RIPPLE_ALPHA", 200, 0)
    private var propertyXTranslationRight = PropertyValuesHolder.ofFloat("PROPERTY_X", 0f, dotSpacing + dotSize)
    private var propertyLastDotSize = PropertyValuesHolder.ofFloat("PROPERTY_ENTRY_DOT_SIZE", smallDotSize, dotSize)
    private var propertyExitDotSize = PropertyValuesHolder.ofFloat("PROPERTY_EXIT_DOT_SIZE", dotSize, smallDotSize)

    private var animatedYValue = 0
    private var animatedXValue = 0
    private var xTranslation = 0f
    private var isInAddAnimationState = false
    private var rippleSize = dotSize
//    private var lastDotSize = smallDotSize

    fun addCounter() {
        if (activeDots > dotsCount) {
            Log.e(LOG_TAG, "Adding more dots than expected, ignoring added dots")
            return
        }

        activeDots++

        val dotToTranslate = if (activeDots < dotsList.size - 1) {
            dotsList[activeDots - 1]
        } else if (activeDots == dotsCount) {
            dotsList[dotsList.size - 1]
        } else {
            dotsList[dotsList.size - 2]
        }

        dotToTranslate.paint = activePaint
        updatedDotsList()

        val transitionAnimationRight = ValueAnimator().apply {
            setValues(propertyRadius, propertyXTranslationRight, propertyLastDotSize, propertyExitDotSize)
            duration = translationTime
            addListener(object : Animator.AnimatorListener {

                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {}

                override fun onAnimationCancel(animation: Animator?) {
                    isInAddAnimationState = false
                    xTranslation = 0f
                    invalidate()
                }

                override fun onAnimationStart(animation: Animator?) {}

            })
        }

        val rippleAnimation = ValueAnimator().apply {
            setValues(propertyRipple, propertyRippleAlpha)
            duration = 500
            startDelay = 150
            addUpdateListener { animation ->
                dotToTranslate.dotUIState = DotUIState.ripple
                rippleSize = animation.getAnimatedValue("PROPERTY_RIPPLE") as Float
                ripplePaint.alpha = animation.getAnimatedValue("PROPERTY_RIPPLE_ALPHA") as Int
                invalidate()
            }
            addListener(object : Animator.AnimatorListener {

                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    dotToTranslate.dotUIState = DotUIState.normal
                    ripplePaint.alpha = 0
                    rippleSize = 0f
                }

                override fun onAnimationCancel(animation: Animator?) {
                    dotToTranslate.dotUIState = DotUIState.normal
                    ripplePaint.alpha = 0
                    rippleSize = 0f
                }

                override fun onAnimationStart(animation: Animator?) {}

            })
        }

        val zoomAnimation = ValueAnimator().apply {
            setValues(propertyRadius, propertyAlpha)
            duration = 450
            addUpdateListener { animation ->
                val alpha = animation.getAnimatedValue("PROPERTY_ALPHA") as Int
                isInAddAnimationState = false
                dotToTranslate.dotUIState = DotUIState.adding
                if (alpha < 100f) {
                    dotToTranslate.size = dotSize
                    zoomAnimPaint.color = inActiveColor
                    zoomAnimPaint.alpha = 255
                } else {
                    dotToTranslate.size = animation.getAnimatedValue("PROPERTY_RADIUS") as Float
                    zoomAnimPaint.color = activeColor
                    zoomAnimPaint.alpha = alpha
                }
                invalidate()
            }
            addListener(object : Animator.AnimatorListener {

                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    rippleAnimation.start()
                    dotToTranslate.paint = activePaint
                    dotToTranslate.dotUIState = DotUIState.normal
                    zoomAnimPaint.alpha = 255
                    dotToTranslate.size = dotSize
                }

                override fun onAnimationCancel(animation: Animator?) {
                    dotToTranslate.paint = activePaint
                    dotToTranslate.dotUIState = DotUIState.normal
                    zoomAnimPaint.alpha = 255
                    dotToTranslate.size = dotSize
                }

                override fun onAnimationStart(animation: Animator?) {}

            })

        }


        transitionAnimationRight.apply {
            addUpdateListener { animation ->
                //            dotToTranslate.size = animation.getAnimatedValue("PROPERTY_RADIUS") as Float
                xTranslation = animation.getAnimatedValue("PROPERTY_X") as Float

                dotsList.last().size = animation.getAnimatedValue("PROPERTY_ENTRY_DOT_SIZE") as Float
                dotsList[1].size = animation.getAnimatedValue("PROPERTY_EXIT_DOT_SIZE") as Float
                isInAddAnimationState = true
                invalidate()
            }
            addListener(object : Animator.AnimatorListener {

                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    dotsList.last().size = smallDotSize
                    dotsList[1].size = dotSize
                    dotToTranslate.dotUIState = DotUIState.normal
                    isInAddAnimationState = false
                    xTranslation = 0f
                    zoomAnimation.start()
                    invalidate()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    dotsList.last().size = smallDotSize
                    dotsList[1].size = dotSize
                    xTranslation = 0f
                    dotToTranslate.dotUIState = DotUIState.normal
                    isInAddAnimationState = false
                    invalidate()
                }

                override fun onAnimationStart(animation: Animator?) {}

            })
        }

        invalidate()
        if (isTailShown && (activeDots >= dotsDrawn))
            transitionAnimationRight.start()
        else
            zoomAnimation.start()
    }

    private val maxVibrationRight: Int = (dotSpacing / 2).toInt()
    private val maxVibrationLeft = -1 * (dotSpacing / 2).toInt()
    private val maxTopValue = 30
    private val maxBottomValue = -30

    private val propertyXPosition = PropertyValuesHolder.ofInt(
        "PROPERTY_POSITION_X",
        0,
        maxVibrationRight,
        maxVibrationLeft,
        maxVibrationRight,
        maxVibrationLeft,
        0,
        0,
        0,
        0,
        0, 0

    )
    private val propertyYPosition = PropertyValuesHolder.ofInt(
        "PROPERTY_POSITION_Y",
        0,
        0,
        0,
        0,
        0,
        0,
        maxTopValue,
        maxBottomValue,
        maxBottomValue,
        maxBottomValue,
        maxBottomValue
    )

    private val propertyReappearSize = PropertyValuesHolder.ofFloat(
        "PROPERTY_REAPPEAR_SIZE",
        0f,
        0f,
        0f,
        0f,
        0f,
        0f,
        0f,
        0f,
        0f,
        1f,
        dotSize
    )

    fun removeCounter() {
        if (activeDots <= 0)
            return

        val dotToTranslate = if (activeDots < dotsList.size - 1) {
            dotsList[activeDots - 1]
        } else if (activeDots == dotsCount) {
            dotsList.last()
        } else {
            dotsList[dotsList.size - 2]
        }

        invalidate()
        val transitionAnimationLeft = ValueAnimator().apply {
            setValues(propertyRadius, propertyXTranslationRight, propertyLastDotSize, propertyExitDotSize)
            duration = translationTime
            addUpdateListener { animation ->
                //            dotToTranslate.size = animation.getAnimatedValue("PROPERTY_RADIUS") as Float
                xTranslation = -1 * animation.getAnimatedValue("PROPERTY_X") as Float
                dotsList.first().size = animation.getAnimatedValue("PROPERTY_ENTRY_DOT_SIZE") as Float
                dotsList[dotsList.size - 2].size = animation.getAnimatedValue("PROPERTY_EXIT_DOT_SIZE") as Float
                invalidate()
            }
            addListener(object : Animator.AnimatorListener {

                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    dotsList.first().size = smallDotSize
                    dotsList[dotsList.size - 2].size = dotSize
                    dotToTranslate.paint = activePaint
                    xTranslation = 0f
                    invalidate()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    dotsList[dotsList.size - 2].size = dotSize
                    dotsList.first().size = smallDotSize
                    dotToTranslate.paint = activePaint
                    xTranslation = 0f
                    invalidate()
                }

                override fun onAnimationStart(animation: Animator?) {
                    dotToTranslate.paint.alpha = 255
                    dotToTranslate.paint = inActivePaint
                    dotToTranslate.dotUIState = DotUIState.normal
                }

            })
        }

        val removeAnimation = ValueAnimator().apply {
            setValues(propertyXPosition, propertyYPosition, propertyReappearSize)
            duration = 1200
            addUpdateListener { animation ->

                dotToTranslate.dotUIState = DotUIState.removing
                removingPaint.alpha = 255
                dotToTranslate.paint = removingPaint

                animatedYValue = animation.getAnimatedValue("PROPERTY_POSITION_Y") as Int
                animatedXValue = animation.getAnimatedValue("PROPERTY_POSITION_X") as Int
                val reAppearSize = animation.getAnimatedValue("PROPERTY_REAPPEAR_SIZE") as Float
                if (reAppearSize != 0f) {
                    animatedYValue = 0
                    dotToTranslate.size = reAppearSize
                    dotToTranslate.paint = inActivePaint
                }
                invalidate()
            }
            addListener(object : Animator.AnimatorListener {

                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    dotToTranslate.paint.alpha = 255
                    dotToTranslate.paint = inActivePaint
                    dotToTranslate.dotUIState = DotUIState.normal
                    if (isTailShown && (activeDots >= dotsDrawn)) {
                        transitionAnimationLeft.start()
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {
                    dotToTranslate.paint.alpha = 255
                    dotToTranslate.paint = inActivePaint
                    dotToTranslate.dotUIState = DotUIState.normal
                }

                override fun onAnimationStart(animation: Animator?) {
                    dotToTranslate.dotUIState = DotUIState.removing
                    removingPaint.alpha = 255
                    dotToTranslate.paint = removingPaint
                }

            })
        }

        activeDots--
        if (activeDots < dotsDrawn)
            updatedDotsList()
        removeAnimation.start()

    }

    val currentSelectedDots
        get() = activeDots


    /**
     * Delegate Property used to invalidate on value set after executing a custom function
     */
    inner class PropertyDelegate<T>(private var field: T, private inline var func: () -> Unit = {}) {
        operator fun setValue(thisRef: Any?, p: KProperty<*>, v: T) {
            field = v
            func()
            invalidate()
        }

        operator fun getValue(thisRef: Any?, p: KProperty<*>): T {
            return field
        }
    }
}
