package com.wonddak.fonthelper.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.wonddak.fonthelper.model.FontType
import com.wonddak.fonthelper.util.FontUtil

@Composable
fun FontTable(
    normalFontList: List<FontType.Normal?>,
    italicFontList: List<FontType.Italic?>,
    updateNormalFontList: (Int, String) -> Unit,
    updateItalicFontList: (Int, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val labelModifier = Modifier.width(96.dp)
        val contentModifier = Modifier.width(240.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = labelModifier
            )
            Text(
                text = FontUtil.NORMAL,
                modifier = contentModifier,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Normal,
                style = MaterialTheme.typography.subtitle2
            )
            Text(
                text = FontUtil.ITALIC,
                modifier = contentModifier,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.subtitle2

            )
        }
        repeat(9) { index ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = FontUtil.getWeightTextByIndex(index),
                    modifier = labelModifier,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight((index + 1) * 100),
                    style = MaterialTheme.typography.body2
                )
                FontBox(
                    modifier = contentModifier,
                    path = normalFontList[index]?.path ?: "",
                    onNewPath = {
                        updateNormalFontList(index, it)
                    }
                )
                FontBox(
                    modifier = contentModifier,
                    path = italicFontList[index]?.path ?: "",
                    onNewPath = {
                        updateItalicFontList(index, it)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun FontBox(
    modifier: Modifier,
    path: String,
    onNewPath: (path: String) -> Unit
) {
    var showFilePicker by remember { mutableStateOf(false) }

    val fileType = listOf("ttf", "otf")
    FilePicker(
        show = showFilePicker,
        fileExtensions = fileType,
    ) { platformFile ->
        showFilePicker = false
        // do something with the file
        platformFile?.let {
            onNewPath(it.path)
        }
    }

    val callback = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                // Parse received data
                val data = event.dragData()
                if (data is DragData.FilesList) {
                    val filePaths = data.readFiles()
                    val filePath = filePaths.firstOrNull() ?: return false
                    if (filePath.isSupportedFontFile()) {
                        onNewPath(filePath.normalizeDroppedPath())
                        return true
                    }
                }
                return false
            }
        }
    }

    TextField(
        value = path,
        onValueChange = {},
        readOnly = true,
        modifier = modifier
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    return@dragAndDropTarget true
                }, target = callback
            ),
        singleLine = true,
        trailingIcon = {
            if (path.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onNewPath("")
                    }
                ) {
                    Icon(Icons.Default.Clear, null)
                }
            } else {
                IconButton(
                    onClick = {
                        showFilePicker = true
                    }
                ) {
                    Icon(Icons.Default.FolderOpen, null)
                }
            }
        }
    )
}
