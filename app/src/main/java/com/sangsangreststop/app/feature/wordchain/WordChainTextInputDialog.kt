package com.sangsangreststop.app.feature.wordchain

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.sangsangreststop.app.R

/**
 * '직접 입력' 버튼용 글자 입력 창. 음성 인식이 연결되기 전에는 마이크 버튼도 이 창을 연다.
 * 짜잔! 누구?도 음성 인식을 쓸 수 없을 때 [title]·[placeholder]만 바꿔 재사용한다.
 */
@Composable
fun WordChainTextInputDialog(
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.wordchain_input_title),
    placeholder: String = stringResource(R.string.wordchain_input_placeholder),
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // 키보드를 먼저 닫아야 창이 닫힌 직후의 첫 터치가 게임 화면에 전달된다 (Pleos 에뮬레이터에서 확인)
    val close: (() -> Unit) -> Unit = { action ->
        focusManager.clearFocus()
        keyboard?.hide()
        action()
    }
    val submit = {
        if (text.isNotBlank()) close { onSubmit(text.trim()) }
    }

    AlertDialog(
        onDismissRequest = { close(onDismiss) },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(placeholder) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.wordchain_input_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = { close(onDismiss) }) {
                Text(stringResource(R.string.wordchain_input_cancel))
            }
        },
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
}
