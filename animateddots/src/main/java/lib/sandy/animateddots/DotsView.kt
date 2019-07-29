package lib.sandy.animateddots

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
import androidx.core.content.ContextCompat
import kotlin.math.min
import kotlin.reflect.KProperty

class DotsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    private val LOG_TAG = "dots"

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

    private var activeDots = 0
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

            when (dot.dotUIState) {
                DotUIState.removing -> {
                    val initialY = desiredHeight / 2 - animatedYValue
                    canvas?.drawCircle(xPos.minus(animatedXValue), initialY, dot.size / 2, dot.paint)
                }
                DotUIState.adding -> {
                    canvas?.drawCircle(xPos, desiredHeight / 2, dot.size / 2, zoomAnimPaint)
                }
                else -> canvas?.drawCircle(xPos - xTranslation, desiredHeight / 2, dot.size / 2, dot.paint)
            }

            xPos += dotSpacing + dotSize
        }

    }

    private var propertyAlpha = PropertyValuesHolder.ofInt("PROPERTY_ALPHA", 0, 255)
    private var propertyRadius = PropertyValuesHolder.ofFloat("PROPERTY_RADIUS", dotSize * 3, dotSize)
    private var propertyXTranslationRight = PropertyValuesHolder.ofFloat("PROPERTY_X", 0f, dotSpacing + dotSize)

    private var animatedYValue = 0
    private var animatedXValue = 0
    private var xTranslation = 0f


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
            setValues(propertyRadius, propertyXTranslationRight)
            duration = 400
            doOnCancel {
                xTranslation = 0f
                invalidate()
            }
        }

        val zoomAnimation = ValueAnimator().apply {
            setValues(propertyRadius, propertyAlpha)
            duration = 300

        }

        zoomAnimation.apply {
            addUpdateListener { animation ->
                dotToTranslate.dotUIState = DotUIState.adding
                dotToTranslate.size = animation.getAnimatedValue("PROPERTY_RADIUS") as Float
                zoomAnimPaint.alpha = animation.getAnimatedValue("PROPERTY_ALPHA") as Int
                invalidate()
            }
            doOnCancel {
                dotToTranslate.dotUIState = DotUIState.normal
                zoomAnimPaint.alpha
                dotToTranslate.size = dotSize
            }
            doOnEnd {
                dotToTranslate.dotUIState = DotUIState.normal
                zoomAnimPaint.alpha
                dotToTranslate.size = dotSize
            }
        }

        transitionAnimationRight.apply {
            addUpdateListener { animation ->
                //            dotToTranslate.size = animation.getAnimatedValue("PROPERTY_RADIUS") as Float
                xTranslation = animation.getAnimatedValue("PROPERTY_X") as Float
                invalidate()
            }
            doOnEnd {
                xTranslation = 0f
                zoomAnimation.start()
                invalidate()
            }
        }

        invalidate()
        if (isTailShown && (activeDots >= dotsDrawn))
            transitionAnimationRight.start()
        else
            zoomAnimation.start()
    }

    val maxVibrationRight: Int = (dotSpacing / 2).toInt()
    val maxVibrationLeft = -1 * (dotSpacing / 2).toInt()
    val maxTopValue = 30
    val maxBottomValue = -30

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
        0
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
        0
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
            setValues(propertyRadius, propertyXTranslationRight)
            duration = 400
            doOnCancel {
                xTranslation = 0f
                invalidate()
            }
        }

        transitionAnimationLeft.apply {
            addUpdateListener { animation ->
                //            dotToTranslate.size = animation.getAnimatedValue("PROPERTY_RADIUS") as Float
                xTranslation = -1 * animation.getAnimatedValue("PROPERTY_X") as Float
                invalidate()
            }
            doOnEnd {
                dotToTranslate.paint = activePaint
                xTranslation = 0f
                invalidate()
            }
            doOnCancel {
                dotToTranslate.paint = activePaint
                xTranslation = 0f
                invalidate()
            }
        }


        val removeAnimation = ValueAnimator().apply {
            setValues(propertyXPosition, propertyYPosition)
            duration = 600
            addUpdateListener { animation ->
                dotToTranslate.dotUIState = DotUIState.removing
                dotToTranslate.paint = removingPaint
                animatedYValue = animation.getAnimatedValue("PROPERTY_POSITION_Y") as Int
                animatedXValue = animation.getAnimatedValue("PROPERTY_POSITION_X") as Int
                invalidate()
            }
            doOnEnd {
                dotToTranslate.paint = inActivePaint
                dotToTranslate.dotUIState = DotUIState.normal
                if (isTailShown && (activeDots >= dotsDrawn))
                    transitionAnimationLeft.start()
                invalidate()
            }
            doOnCancel {
                dotToTranslate.paint = inActivePaint
                dotToTranslate.dotUIState = DotUIState.normal
                invalidate()
            }
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


data class Dot(
    var size: Float,
    var alpha: Float = 1f,
    var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG),

    var dotUIState: DotUIState = DotUIState.normal
)


enum class DotUIState {
    normal, removing, adding
}
