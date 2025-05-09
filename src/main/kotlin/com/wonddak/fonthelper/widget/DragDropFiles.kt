package com.wonddak.fonthelper.widget

import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.unit.dp
import com.wonddak.fonthelper.util.FontUtil
import com.wonddak.fonthelper.util.FontUtil.FontTypeConst

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DragDropFiles(
    updateNormalFontList: (Int, String) -> Unit,
    updateItalicFontList: (Int, String) -> Unit,
) {
    val fontWeightMap = mapOf(
        FontTypeConst.EXTRA_LIGHT.lowercase() to 1,
        FontTypeConst.EXTRA_BOLD.lowercase() to 7,
        FontTypeConst.SEMI_BOLD.lowercase() to 5,
        FontTypeConst.THIN.lowercase() to 0,
        FontTypeConst.LIGHT.lowercase() to 2,
        FontTypeConst.NORMAL.lowercase() to 3,
        FontTypeConst.REGULAR.lowercase() to 3,
        FontTypeConst.MEDIUM.lowercase() to 4,
        FontTypeConst.BOLD.lowercase() to 6,
        FontTypeConst.BLACK.lowercase() to 8
    )

    val callback = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                // Parse received data
                val data = event.dragData()
                if (data is DragData.FilesList) {
                    val filePaths = data.readFiles()
                    for (filePath in filePaths) {
                        if (!filePath.endsWith(".ttf") && !filePath.endsWith(".otf")) continue

                        val fileName = filePath.trimEnd('/').substringAfterLast('/').lowercase()
                        val isItalic = fileName.contains(FontUtil.ITALIC.lowercase())

                        val matchedEntry = fontWeightMap.entries.firstOrNull { (type, _) ->
                            fileName.contains(type)
                        }

                        matchedEntry?.let { (_, weight) ->
                            if (isItalic) updateItalicFontList(weight, filePath)
                            else updateNormalFontList(weight, filePath)
                        }
                    }
                }
                return false
            }
        }
    }
    Column(
        modifier = Modifier
            .size(200.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.primary,
                shape = RoundedCornerShape(10.dp),
            )
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    return@dragAndDropTarget true
                }, target = callback
            )
    ) {
        Text(
            text = "Drop files here!",
        )
    }
}