package com.sangsangreststop.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sangsangreststop.app.ui.components.popup.CommonPopupAction
import com.sangsangreststop.app.ui.components.popup.CommonPopupErrorType
import com.sangsangreststop.app.ui.components.popup.CommonPopupOverlay
import com.sangsangreststop.app.ui.components.popup.CommonPopupType
import com.sangsangreststop.app.ui.components.popup.commonPopupModel
import com.sangsangreststop.app.ui.theme.SangsangRestStopTheme

class PopupGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SangsangRestStopTheme {
                PopupGalleryScreen()
            }
        }
    }
}

private data class GalleryPopup(
    val type: CommonPopupType,
    val errorType: CommonPopupErrorType = CommonPopupErrorType.SPEECH_RECOGNITION,
)

@Composable
private fun PopupGalleryScreen() {
    var selectedPopup by remember { mutableStateOf<GalleryPopup?>(null) }
    var lastAction by remember { mutableStateOf("아직 선택한 팝업 액션이 없습니다.") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF2F8FF),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFE5F6FF),
                            Color(0xFFFFEDF4),
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 40.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "SangsangRestStop Debug Popup Gallery",
                    color = Color(0xFF102A5A),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "게임·전화·음성 기능은 호출하지 않고 UI 콜백만 확인합니다.",
                    color = Color(0xFF40597A),
                    fontSize = 20.sp,
                )
                Spacer(Modifier.height(22.dp))

                GalleryButtonRow(
                    firstLabel = "전화 수신",
                    onFirstClick = {
                        selectedPopup = GalleryPopup(CommonPopupType.PHONE)
                    },
                    secondLabel = "내비게이션 안내",
                    onSecondClick = {
                        selectedPopup = GalleryPopup(CommonPopupType.NAVIGATION)
                    },
                )
                Spacer(Modifier.height(14.dp))
                GalleryButtonRow(
                    firstLabel = "음성인식 오류",
                    onFirstClick = {
                        selectedPopup = GalleryPopup(
                            type = CommonPopupType.ERROR,
                            errorType = CommonPopupErrorType.SPEECH_RECOGNITION,
                        )
                    },
                    secondLabel = "네트워크 오류",
                    onSecondClick = {
                        selectedPopup = GalleryPopup(
                            type = CommonPopupType.ERROR,
                            errorType = CommonPopupErrorType.NETWORK,
                        )
                    },
                )
                Spacer(Modifier.height(14.dp))
                GalleryButtonRow(
                    firstLabel = "게임 종료",
                    onFirstClick = {
                        selectedPopup = GalleryPopup(CommonPopupType.GAME_END)
                    },
                    secondLabel = "게임 일시정지",
                    onSecondClick = {
                        selectedPopup = GalleryPopup(CommonPopupType.GAME_PAUSE)
                    },
                )

                Spacer(Modifier.height(28.dp))
                Text(
                    text = "최근 액션: $lastAction",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            selectedPopup?.let { selection ->
                CommonPopupOverlay(
                    model = commonPopupModel(
                        type = selection.type,
                        errorType = selection.errorType,
                    ),
                    onAction = { action ->
                        lastAction = action.galleryLabel()
                        selectedPopup = null
                    },
                    onDismissRequest = {
                        lastAction = "팝업 닫기"
                        selectedPopup = null
                    },
                    dismissOnBackdrop = false,
                )
            }
        }
    }
}

@Composable
private fun GalleryButtonRow(
    firstLabel: String,
    onFirstClick: () -> Unit,
    secondLabel: String,
    onSecondClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(0.72f),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Button(
            onClick = onFirstClick,
            modifier = Modifier
                .weight(1f)
                .height(68.dp),
        ) {
            Text(
                text = firstLabel,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Button(
            onClick = onSecondClick,
            modifier = Modifier
                .weight(1f)
                .height(68.dp),
        ) {
            Text(
                text = secondLabel,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun CommonPopupAction.galleryLabel(): String = when (this) {
    CommonPopupAction.CALL -> "통화하기"
    CommonPopupAction.CONFIRM -> "확인"
    CommonPopupAction.RETRY -> "다시 시도"
    CommonPopupAction.HOME -> "홈으로 가기"
    CommonPopupAction.STOP -> "그만할래"
    CommonPopupAction.RESUME -> "조금만 더 놀게요"
    CommonPopupAction.VOICE_SETTINGS -> "목소리 설정하기"
    CommonPopupAction.REPLAY -> "다시 놀기"
}
