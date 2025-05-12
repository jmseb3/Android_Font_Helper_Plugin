package com.wonddak.fonthelper.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wonddak.fonthelper.setting.FontMatchSettingsService
import com.wonddak.fonthelper.setting.FontMatchSettingsState

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DragDropFiles(
    updateNormalFontList: (Int, String) -> Unit,
    updateItalicFontList: (Int, String) -> Unit,
) {
    var isHover by remember {
        mutableStateOf(false)
    }

    val service = FontMatchSettingsService.getInstance()
    val settings: FontMatchSettingsState = service.state

    val callback = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                super.onStarted(event)
                isHover = true
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                // Parse received data
                val data = event.dragData()
                if (data is DragData.FilesList) {
                    val filePaths = data.readFiles()
                    for (filePath in filePaths) {
                        if (!filePath.endsWith(".ttf") && !filePath.endsWith(".otf")) continue

                        val fileName = filePath.trimEnd('/').substringAfterLast('/').lowercase()
                        settings.checkType(fileName)?.let { (isItalic, weight) ->
                            if (isItalic) updateItalicFontList(weight, filePath)
                            else updateNormalFontList(weight, filePath)
                        }
                    }
                }
                return false
            }

            override fun onEnded(event: DragAndDropEvent) {
                super.onEnded(event)
                isHover = false
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .heightIn(min = 150.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.primary,
                shape = RoundedCornerShape(10.dp),
            )
            .background(
                color = when {
                    isHover -> MaterialTheme.colors.primary.copy(alpha = 0.05f)
                    else -> Color.Transparent
                },
                shape = MaterialTheme.shapes.small,
            )
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    return@dragAndDropTarget true
                }, target = callback
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.FileUpload,
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = "Drag & drop Font Files",
            )
        }
    }
}