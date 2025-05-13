package com.wonddak.fonthelper.widget

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable

@Composable
fun InputRow(
    title: String,
    text: String,
    onValueChange: (text: String) -> Unit,
    otherContent: @Composable RowScope.() -> Unit = {}
) {
    LabelContent(
        title = title
    ) {
        TextField(
            value = text,
            onValueChange = onValueChange,
            placeholder = {
                Text(title)
            },
            trailingIcon = if (text.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = {
                                onValueChange("")
                            }
                        ) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                } else {
                    null
                }
        )
        otherContent()
    }
}