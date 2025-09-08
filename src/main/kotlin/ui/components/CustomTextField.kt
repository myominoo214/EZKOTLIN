package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle(fontSize = 14.sp),
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    shape: Shape = RoundedCornerShape(8.dp),
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color.Black,
    focusedBorderColor: Color = Color.Black,
    backgroundColor: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val currentBorderColor = when {
        isError -> Color.Red
        isFocused -> focusedBorderColor
        else -> borderColor
    }
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = currentBorderColor,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusRequester?.requestFocus()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Box(
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { mod ->
                            if (focusRequester != null) {
                                mod.focusRequester(focusRequester)
                            } else {
                                mod
                            }
                        }
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        },
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = textStyle,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
                
                // Label
                if (label != null && (value.isEmpty() || isFocused)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (value.isNotEmpty() || isFocused) 0.dp else 8.dp)
                    ) {
                        CompositionLocalProvider(
                            LocalTextStyle provides textStyle.copy(
                                color = if (isFocused) MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = if (value.isNotEmpty() || isFocused) 12.sp else 14.sp
                            )
                        ) {
                            label()
                        }
                    }
                }
                
                // Placeholder
                if (placeholder != null && value.isEmpty() && !isFocused) {
                    CompositionLocalProvider(
                        LocalTextStyle provides textStyle.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        placeholder()
                    }
                }
            }
            
            trailingIcon?.let {
                Spacer(modifier = Modifier.width(8.dp))
                it()
            }
        }
    }
}