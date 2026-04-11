package com.mocharealm.gaze.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object SFIcons {

    fun buildVector(name: String, pathData: String, fillType: PathFillType): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024.0f,
            viewportHeight = 1024.0f
        ).apply {
            path(fill = SolidColor(Color.Black), pathFillType = fillType) {
                parsePathData(pathData)
            }
        }.build()
    }

    private fun PathBuilder.parsePathData(data: String) {
        // Kotlin 兼容支持科学计数法的正则表达式
        val regex = "([A-Za-z])|([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?)".toRegex()
        val matches = regex.findAll(data).toList()
        var i = 0
        while (i < matches.size) {
            val cmd = matches[i].value
            if (cmd[0].isLetter() && cmd.length == 1) { // 避免误把 e 当命令
                val isRelative = cmd[0].isLowerCase()
                val command = cmd.uppercase()
                i++
                val args = mutableListOf<Float>()
                while (i < matches.size && !(matches[i].value[0].isLetter() && matches[i].value.length == 1)) {
                    args.add(matches[i].value.toFloat())
                    i++
                }
                execute(command, isRelative, args)
            } else { i++ }
        }
    }

    private fun PathBuilder.execute(cmd: String, isRelative: Boolean, args: List<Float>) {
        when (cmd) {
            "M" -> for (j in args.indices step 2) if (isRelative) moveToRelative(args[j], args[j+1]) else moveTo(args[j], args[j+1])
            "L" -> for (j in args.indices step 2) if (isRelative) lineToRelative(args[j], args[j+1]) else lineTo(args[j], args[j+1])
            "H" -> for (j in args.indices) if (isRelative) horizontalLineToRelative(args[j]) else horizontalLineTo(args[j])
            "V" -> for (j in args.indices) if (isRelative) verticalLineToRelative(args[j]) else verticalLineTo(args[j])
            "C" -> for (j in args.indices step 6) if (isRelative) curveToRelative(args[j], args[j+1], args[j+2], args[j+3], args[j+4], args[j+5]) else curveTo(args[j], args[j+1], args[j+2], args[j+3], args[j+4], args[j+5])
            "S" -> for (j in args.indices step 4) if (isRelative) reflectiveCurveToRelative(args[j], args[j+1], args[j+2], args[j+3]) else reflectiveCurveTo(args[j], args[j+1], args[j+2], args[j+3])
            "Q" -> for (j in args.indices step 4) if (isRelative) quadToRelative(args[j], args[j+1], args[j+2], args[j+3]) else quadTo(args[j], args[j+1], args[j+2], args[j+3])
            "T" -> for (j in args.indices step 2) if (isRelative) reflectiveQuadToRelative(args[j], args[j+1]) else reflectiveQuadTo(args[j], args[j+1])
            "A" -> for (j in args.indices step 7) {
                if (isRelative) arcToRelative(args[j], args[j+1], args[j+2], args[j+3]!=0f, args[j+4]!=0f, args[j+5], args[j+6])
                else arcTo(args[j], args[j+1], args[j+2], args[j+3]!=0f, args[j+4]!=0f, args[j+5], args[j+6])
            }
            "Z" -> close()
        }
    }
}

