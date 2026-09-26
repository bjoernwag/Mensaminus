package com.bjwag.mensaminus.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser

private const val STAR_PATH = "M480,932L346,800L160,800L160,614L28,480L160,346L160,160L346,160L480,28L614,160L800,160L800,346L932,480L800,614L800,800L614,800L480,932Z"
private const val PLATE_PATH = "M480,820L560,740L620,720L720,720L720,580L820,480L720,380L720,240L580,240L480,140L380,240L240,240L240,380L140,480L240,580L240,720L340,720L400,740L480,820Z"
private const val SPOON_PATH = "M560,740L560,592Q534,577 517,541.5Q500,506 500,460Q500,402 526,361Q552,320 590,320Q627,320 653.5,361Q680,402 680,460Q680,507 663,542.5Q646,578 620,592L620,720Z"
private const val FORK_PATH = "M340,720L340,560Q314,554 297,532.5Q280,511 280,483L280,320L320,320L320,471L350,471L350,320L390,320L390,471L420,471L420,320L460,320L460,483Q460,511 443,532.5Q426,554 400,560L400,740Z"

@Composable
fun MensaminusLogo(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    color: Color = Color.White,
    lineProgress: Float = 1f,
    fillAlpha: Float = 1f,
    cutleryAlpha: Float = 1f,
    forkOffsetX: Float = 0f,
    spoonOffsetX: Float = 0f,
    strokeWidth: Float = 8f
) {
    val starPath = remember { PathParser.createPathFromPathData(STAR_PATH).asComposePath() }
    val platePath = remember { PathParser.createPathFromPathData(PLATE_PATH).asComposePath() }
    val forkPath = remember { PathParser.createPathFromPathData(FORK_PATH).asComposePath() }
    val spoonPath = remember { PathParser.createPathFromPathData(SPOON_PATH).asComposePath() }

    val framePath = remember(starPath, platePath) {
        Path().apply {
            addPath(starPath)
            addPath(platePath)
            fillType = PathFillType.EvenOdd
        }
    }

    val composePathMeasure = remember { PathMeasure() }
    val pathLength = remember(starPath) {
        composePathMeasure.setPath(starPath, false)
        composePathMeasure.length
    }
    val dashIntervals = remember(pathLength) { floatArrayOf(pathLength, pathLength) }

    Canvas(modifier = modifier.size(size)) {
        val canvasScale = this.size.width / 960f

        withTransform({
            scale(canvasScale, canvasScale, Offset.Zero)
        }) {
            // 1. Draw the Star-with-a-Hole Frame (fill) FIRST
            if (fillAlpha > 0f) {
                drawPath(path = framePath, color = color, alpha = fillAlpha)
            }

            // 2. Draw Cutlery filling the hole
            if (cutleryAlpha > 0f) {
                translate(left = forkOffsetX) {
                    drawPath(
                        path = forkPath,
                        color = color,
                        alpha = cutleryAlpha
                    )
                }
                translate(left = spoonOffsetX) {
                    drawPath(
                        path = spoonPath,
                        color = color,
                        alpha = cutleryAlpha
                    )
                }
            }

            // 3. Draw the stroke outline LAST so it's on top
            if (lineProgress > 0f) {
                drawPath(
                    path = starPath,
                    color = color,
                    style = Stroke(
                        width = strokeWidth / canvasScale,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = dashIntervals,
                            phase = pathLength * (1f - lineProgress)
                        )
                    )
                )
            }
        }
    }
}
