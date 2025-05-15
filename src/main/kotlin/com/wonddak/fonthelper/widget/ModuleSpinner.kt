package com.wonddak.fonthelper.widget


import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wonddak.fonthelper.model.ModuleData

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ModuleSpinner(
    moduleList: List<ModuleData>,
    selectedModule: ModuleData?,
    updateModule: (module: ModuleData) -> Unit,
) {
    LabelContent(
        "Select Module"
    ) {
        var expandStatus by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expandStatus,
            onExpandedChange = {
                expandStatus = !expandStatus
            },
            modifier = Modifier
        ) {
            TextField(
                readOnly = true,
                value = selectedModule?.name ?: "",
                onValueChange = {},
                modifier = Modifier,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandStatus) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(
                    focusedLabelColor = Color.Transparent,
                    unfocusedLabelColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            ExposedDropdownMenu(
                expanded = expandStatus,
                onDismissRequest = { expandStatus = false }) {
                moduleList.forEach { module ->
                    DropdownMenuItem(
                        onClick = {
                            updateModule(module)
                            expandStatus = false
                        },
                        content = {
                            Text(text = module.name)
                        }
                    )
                }
            }
        }
    }
}