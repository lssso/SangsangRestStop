package com.sangsangreststop.app.ui.components.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.sangsangreststop.app.ui.theme.SangsangRestStopTheme

@Preview(
    name = "전화 수신",
    widthDp = 1682,
    heightDp = 1210,
    showBackground = true,
)
@Composable
private fun PhonePopupPreview() {
    PopupPreviewHost(CommonPopupType.PHONE)
}

@Preview(
    name = "내비게이션 안내",
    widthDp = 1682,
    heightDp = 1210,
    showBackground = true,
)
@Composable
private fun NavigationPopupPreview() {
    PopupPreviewHost(CommonPopupType.NAVIGATION)
}

@Preview(
    name = "음성인식 오류",
    widthDp = 1682,
    heightDp = 1210,
    showBackground = true,
)
@Composable
private fun SpeechErrorPopupPreview() {
    PopupPreviewHost(
        type = CommonPopupType.ERROR,
        errorType = CommonPopupErrorType.SPEECH_RECOGNITION,
    )
}

@Preview(
    name = "네트워크 오류",
    widthDp = 1682,
    heightDp = 1210,
    showBackground = true,
)
@Composable
private fun NetworkErrorPopupPreview() {
    PopupPreviewHost(
        type = CommonPopupType.ERROR,
        errorType = CommonPopupErrorType.NETWORK,
    )
}

@Preview(
    name = "게임 종료",
    widthDp = 1682,
    heightDp = 1210,
    showBackground = true,
)
@Composable
private fun GameEndPopupPreview() {
    PopupPreviewHost(CommonPopupType.GAME_END)
}

@Preview(
    name = "게임 일시정지",
    widthDp = 1682,
    heightDp = 1210,
    showBackground = true,
)
@Composable
private fun GamePausePopupPreview() {
    PopupPreviewHost(CommonPopupType.GAME_PAUSE)
}

@Composable
private fun PopupPreviewHost(
    type: CommonPopupType,
    errorType: CommonPopupErrorType = CommonPopupErrorType.SPEECH_RECOGNITION,
) {
    SangsangRestStopTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF9FD9F4),
                            Color(0xFFFCE6EE),
                        ),
                    ),
                ),
        ) {
            CommonPopupOverlay(
                model = commonPopupModel(type, errorType),
                onAction = {},
            )
        }
    }
}
