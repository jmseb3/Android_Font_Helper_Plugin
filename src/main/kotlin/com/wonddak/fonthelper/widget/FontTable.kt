package com.wonddak.fonthelper.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
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
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 640.dp
        val gap = 12.dp

        if (!compact) {
            val labelWidth = 92.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(labelWidth))
                Text(
                    text = FontUtil.NORMAL,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Normal,
                    style = MaterialTheme.typography.subtitle2
                )
                Text(
                    text = FontUtil.ITALIC,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.subtitle2
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(9) { index ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = FontUtil.getWeightTextByIndex(index),
                            modifier = Modifier.width(labelWidth),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight((index + 1) * 100),
                            style = MaterialTheme.typography.body2
                        )
                        FontBox(
                            modifier = Modifier.weight(1f),
                            path = normalFontList[index]?.path ?: "",
                            onNewPath = { updateNormalFontList(index, it) }
                        )
                        FontBox(
                            modifier = Modifier.weight(1f),
                            path = italicFontList[index]?.path ?: "",
                            onNewPath = { updateItalicFontList(index, it) }
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(9) { index ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        elevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = FontUtil.getWeightTextByIndex(index),
                                fontWeight = FontWeight((index + 1) * 100),
                                style = MaterialTheme.typography.body2
                            )
                            Text(
                                text = FontUtil.NORMAL,
                                style = MaterialTheme.typography.caption
                            )
                            FontBox(
                                modifier = Modifier.fillMaxWidth(),
                                path = normalFontList[index]?.path ?: "",
                                onNewPath = { updateNormalFontList(index, it) }
                            )
                            Text(
                                text = FontUtil.ITALIC,
                                style = MaterialTheme.typography.caption
                            )
                            FontBox(
                                modifier = Modifier.fillMaxWidth(),
                                path = italicFontList[index]?.path ?: "",
                                onNewPath = { updateItalicFontList(index, it) }
                            )
                        }
                    }
                }
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
        platformFile?.let {
            val newPath = it.path
            if (!isSameFontFile(path, newPath)) {
                onNewPath(newPath)
            }
        }
    }

    val callback = remember(path) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                // Parse received data
                val data = event.dragData()
                if (data is DragData.FilesList) {
                    val filePaths = data.readFiles()
                    val filePath = filePaths.firstOrNull() ?: return false
                    if (filePath.isSupportedFontFile()) {
                        val newPath = filePath.normalizeDroppedPath()
                        if (!isSameFontFile(path, newPath)) {
                            onNewPath(newPath)
                        }
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

private fun isSameFontFile(currentPath: String, newPath: String): Boolean {
    if (newPath.isBlank()) return true
    if (currentPath == newPath) return true
    val currentName = currentPath.trim().substringAfterLast('/').substringAfterLast('\\')
    val newName = newPath.trim().substringAfterLast('/').substringAfterLast('\\')
    return currentName.isNotBlank() && currentName.equals(newName, ignoreCase = true)
}
