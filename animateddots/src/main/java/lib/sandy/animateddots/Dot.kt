package lib.sandy.animateddots

import android.graphics.Paint

data class Dot(
    var size: Float,
    var alpha: Float = 1f,
    var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG),

    var dotUIState: DotUIState = DotUIState.normal
)

enum class DotUIState {
    normal, removing, adding, ripple
}
