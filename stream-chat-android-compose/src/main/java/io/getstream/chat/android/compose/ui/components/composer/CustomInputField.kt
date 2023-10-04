package io.getstream.chat.android.compose.ui.components.composer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.chat.android.compose.R
import io.getstream.chat.android.core.internal.exhaustive
import io.getstream.chat.android.models.Attachment
import io.getstream.chat.android.ui.common.state.messages.composer.ValidationError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
public fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    border: BorderStroke = BorderStroke(1.dp, Color(0xFF9C9497).copy(0.2f)),
    shape: Shape = RoundedCornerShape(12.dp),
    innerPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
    attachments: List<Attachment>,
    validationErrors: List<ValidationError>,
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }

    // Workaround to move cursor to the end after selecting a suggestion
    val selection = if (textFieldValueState.isCursorAtTheEnd()) {
        TextRange(value.length)
    } else {
        textFieldValueState.selection
    }

    val textFieldValue = textFieldValueState.copy(
        text = value,
        selection = selection,
    )

    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()


    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ){
        Box(
            modifier = Modifier
            .height(48.dp)
            .weight(1f, true)
            .border(border,shape)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF9C9497).copy(0.05f)),
            contentAlignment = Alignment.Center
        ){
            BasicTextField(
                modifier = Modifier.padding(start = 15.dp).focusRequester(focusRequester),
                value = textFieldValue,
                enabled = enabled,
                textStyle = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily(Font(R.font.monteserrat)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                onValueChange = {
                    textFieldValueState = it
                    if (value != it.text) {
                        onValueChange(it.text)
                    }
                },
                decorationBox = { innerTextField ->
                    decorationBox(innerTextField)
                },
                cursorBrush = SolidColor(
                    Color(0xFFD9E1E1).copy(0.4f)
                ),
                maxLines = maxLines,
                singleLine = maxLines == 1,
                keyboardOptions =   KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                )

            )
        }

        if(!(enabled && (value.isNotBlank() || attachments.isNotEmpty()) && validationErrors.isEmpty())){
            IconButton(
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp),
                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.smiley),
                        contentDescription = null,
                        tint = Color.White,
                    )
                },
                onClick = {
                    scope.launch {
                        focusRequester.requestFocus()
                    }
                },
            )
        }



    }
}

private fun TextFieldValue.isCursorAtTheEnd(): Boolean {
    val textLength = text.length
    val selectionStart = selection.start
    val selectionEnd = selection.end

    return textLength == selectionStart && textLength == selectionEnd
}