package com.sangsangreststop.app.feature.revealwho

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sangsangreststop.app.R
import com.sangsangreststop.app.ui.theme.SangsangRestStopNavy
import com.sangsangreststop.app.ui.theme.SangsangRestStopTheme

/**
 * 「짜잔! 누구?」 진행 화면.
 *
 * 문제 번호·힌트 단계·힌트 문구·음성 상태는 [uiState]를 그대로 그린다.
 * 게임 진행과 음성 입출력은 RevealWhoViewModel이 맡고, 화면은 이벤트만 전달한다.
 */
@Composable
fun RevealWhoPlayScreen(
    settings: RevealWhoSettings,
    onBackClick: () -> Unit,
    onReplayClick: () -> Unit,
    onNextHintClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: RevealWhoPlayUiState = RevealWhoPlayUiState(),
    onMicClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFBDEAFF),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val compact = (maxWidth < 1_100.dp) || (maxHeight < 760.dp)
            val horizontalPadding = (maxWidth * 0.018f).coerceIn(18.dp, 32.dp)
            val headerTop = if (compact) 14.dp else 22.dp
            val headerHeight = if (compact) 60.dp else 76.dp
            val answerTop = maxHeight * if (compact) 0.52f else 0.54f
            val answerHeight = (maxHeight * if (compact) 0.265f else 0.27f)
                .coerceIn(194.dp, 290.dp)
            val actionHeight = if (compact) 72.dp else 88.dp
            val actionBottom = (maxHeight * 0.025f).coerceIn(18.dp, 30.dp)

            val girlWidth = minOf(maxWidth * 0.245f, maxHeight * 0.35f)
                .coerceIn(245.dp, 380.dp)
            val girlHeight = girlWidth / 0.80f
            val rabbitWidth = minOf(maxWidth * 0.14f, maxHeight * 0.19f)
                .coerceIn(140.dp, 210.dp)
            val rabbitHeight = rabbitWidth / 0.76f
            val boyWidth = minOf(maxWidth * 0.235f, maxHeight * 0.34f)
                .coerceIn(240.dp, 370.dp)
            val boyHeight = boyWidth / 0.74f

            val hintWidth = (maxWidth * 0.56f).coerceIn(520.dp, 820.dp)
            val hintHeight = hintWidth / (1320f / 670f)
            val hintTop = (maxHeight * if (compact) 0.16f else 0.17f)
                .coerceIn(105.dp, 190.dp)

            Image(
                painter = painterResource(R.drawable.sr_who_bg_play),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            PlayHeader(
                onBackClick = onBackClick,
                compact = compact,
                modifier = Modifier
                    .offset(x = horizontalPadding, y = headerTop)
                    .height(headerHeight)
                    .zIndex(6f),
            )

            ProgressPill(
                uiState = uiState,
                compact = compact,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -horizontalPadding, y = headerTop)
                    .height(headerHeight)
                    .width(if (compact) 330.dp else 410.dp)
                    .zIndex(6f),
            )

            Image(
                painter = painterResource(R.drawable.sr_who_deco_sparkle),
                contentDescription = null,
                modifier = Modifier
                    .offset(
                        x = girlWidth * 0.65f,
                        y = (answerTop - (girlHeight * 0.82f)) - (girlHeight * 0.035f),
                    )
                    .size(if (compact) 55.dp else 72.dp)
                    .zIndex(2f),
                contentScale = ContentScale.Fit,
            )
            Image(
                painter = painterResource(R.drawable.sr_who_char_girl),
                contentDescription = null,
                modifier = Modifier
                    .offset(
                        x = horizontalPadding + (maxWidth * 0.025f),
                        y = answerTop - (girlHeight * 0.82f),
                    )
                    .width(girlWidth)
                    .height(girlHeight)
                    .zIndex(2f),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
            )
            Image(
                painter = painterResource(R.drawable.sr_who_char_rabbit),
                contentDescription = null,
                modifier = Modifier
                    .offset(
                        x = horizontalPadding + (maxWidth * 0.20f),
                        y = answerTop - (rabbitHeight * 0.70f),
                    )
                    .width(rabbitWidth)
                    .height(rabbitHeight)
                    .zIndex(3f),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
            )
            Image(
                painter = painterResource(R.drawable.sr_who_char_boy),
                contentDescription = null,
                modifier = Modifier
                    .offset(
                        x = maxWidth - boyWidth - horizontalPadding - (maxWidth * 0.035f),
                        y = answerTop - (boyHeight * 0.80f),
                    )
                    .width(boyWidth)
                    .height(boyHeight)
                    .zIndex(2f),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
            )
            Image(
                painter = painterResource(R.drawable.sr_who_deco_cloud),
                contentDescription = null,
                modifier = Modifier
                    .offset(
                        x = maxWidth - boyWidth * 0.52f - horizontalPadding,
                        y = answerTop - (boyHeight * 0.74f),
                    )
                    .size(if (compact) 92.dp else 124.dp)
                    .zIndex(2f),
                contentScale = ContentScale.Fit,
            )
            Image(
                painter = painterResource(R.drawable.sr_who_deco_star),
                contentDescription = null,
                modifier = Modifier
                    .offset(
                        x = maxWidth - horizontalPadding - if (compact) 72.dp else 92.dp,
                        y = answerTop - if (compact) 54.dp else 72.dp,
                    )
                    .size(if (compact) 48.dp else 62.dp)
                    .zIndex(2f),
                contentScale = ContentScale.Fit,
            )

            HintBubble(
                hintText = uiState.hintText,
                compact = compact,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = hintTop)
                    .width(hintWidth)
                    .height(hintHeight)
                    .zIndex(4f),
            )

            AnswerPanel(
                uiState = uiState,
                onMicClick = onMicClick,
                compact = compact,
                modifier = Modifier
                    .offset(x = horizontalPadding, y = answerTop)
                    .fillMaxWidth()
                    .padding(end = horizontalPadding * 2f)
                    .height(answerHeight)
                    .zIndex(5f),
            )

            PlayActions(
                onReplayClick = onReplayClick,
                onNextHintClick = onNextHintClick,
                onStopClick = onStopClick,
                compact = compact,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        bottom = actionBottom,
                    )
                    .fillMaxWidth()
                    .height(actionHeight)
                    .zIndex(6f),
            )
        }
    }
}

@Composable
private fun PlayHeader(
    onBackClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 20.dp),
    ) {
        val backDescription = stringResource(R.string.wordchain_back_content_description)
        Surface(
            onClick = onBackClick,
            modifier = Modifier
                .size(if (compact) 54.dp else 66.dp)
                .semantics { contentDescription = backDescription },
            color = Color.White.copy(alpha = 0.96f),
            shape = CircleShape,
            shadowElevation = 6.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "←",
                    color = SangsangRestStopNavy,
                    fontSize = if (compact) 31.sp else 38.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = stringResource(R.string.reveal_who_play_title),
            color = SangsangRestStopNavy,
            fontSize = if (compact) 38.sp else 50.sp,
            lineHeight = if (compact) 43.sp else 56.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProgressPill(
    uiState: RevealWhoPlayUiState,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.94f),
        shape = RoundedCornerShape(40.dp),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 20.dp else 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                text = "●",
                color = Color(0xFF5A78BE),
                fontSize = if (compact) 18.sp else 24.sp,
            )
            Text(
                text = stringResource(R.string.reveal_who_play_problem, uiState.questionNumber),
                color = SangsangRestStopNavy,
                fontSize = if (compact) 23.sp else 29.sp,
                fontWeight = FontWeight.Black,
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(if (compact) 32.dp else 42.dp)
                    .background(Color(0xFFD4DFEE)),
            )
            Image(
                painter = painterResource(R.drawable.sr_who_icon_hint),
                contentDescription = null,
                modifier = Modifier.size(if (compact) 33.dp else 43.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(
                    R.string.reveal_who_play_hint_progress,
                    uiState.hintNumber,
                    uiState.hintCount,
                ),
                color = SangsangRestStopNavy,
                fontSize = if (compact) 23.sp else 29.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun HintBubble(
    hintText: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.sr_who_deco_hint_bubble),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (compact) 50.dp else 60.dp,
                    end = if (compact) 50.dp else 60.dp,
                    top = if (compact) 28.dp else 38.dp,
                    bottom = if (compact) 48.dp else 62.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.sr_who_icon_speaker),
                contentDescription = null,
                modifier = Modifier.size(if (compact) 54.dp else 64.dp),
                contentScale = ContentScale.Fit,
            )
            Image(
                painter = painterResource(R.drawable.sr_who_deco_soundwave),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = if (compact) 10.dp else 14.dp)
                    .width(if (compact) 72.dp else 84.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = hintText,
                color = SangsangRestStopNavy,
                fontSize = if (compact) 33.sp else 40.sp,
                lineHeight = if (compact) 39.sp else 46.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun AnswerPanel(
    uiState: RevealWhoPlayUiState,
    onMicClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(38.dp)),
        color = Color(0xFFFFF1F6).copy(alpha = 0.96f),
        shape = RoundedCornerShape(38.dp),
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.9f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = if (compact) 42.dp else 52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 26.dp else 42.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.sr_who_deco_soundwave),
                    contentDescription = null,
                    modifier = Modifier.width(if (compact) 105.dp else 145.dp),
                    contentScale = ContentScale.Fit,
                )
                Surface(
                    onClick = onMicClick,
                    modifier = Modifier.size(if (compact) 116.dp else 150.dp),
                    color = Color.White.copy(alpha = 0.62f),
                    shape = CircleShape,
                    border = BorderStroke(5.dp, Color(0xFFFFB7D1)),
                    shadowElevation = 5.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.sr_who_icon_microphone),
                            contentDescription = null,
                            modifier = Modifier.size(if (compact) 62.dp else 84.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Image(
                    painter = painterResource(R.drawable.sr_who_deco_soundwave),
                    contentDescription = null,
                    modifier = Modifier.width(if (compact) 105.dp else 145.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                text = stringResource(
                    when {
                        uiState.isFinished -> R.string.reveal_who_play_finished_prompt
                        uiState.voiceState == RevealWhoVoiceState.SPEAKING -> R.string.reveal_who_play_speaking_prompt
                        uiState.voiceState == RevealWhoVoiceState.LISTENING -> R.string.reveal_who_play_answer_prompt
                        else -> R.string.reveal_who_play_mic_prompt
                    },
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (compact) 18.dp else 25.dp),
                color = SangsangRestStopNavy,
                fontSize = if (compact) 28.sp else 38.sp,
                lineHeight = if (compact) 33.sp else 44.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun PlayActions(
    onReplayClick: () -> Unit,
    onNextHintClick: () -> Unit,
    onStopClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 20.dp),
    ) {
        PlayActionButton(
            label = stringResource(R.string.reveal_who_play_replay),
            iconRes = R.drawable.sr_who_icon_speaker,
            onClick = onReplayClick,
            compact = compact,
            containerColor = Color(0xFFFFFEF5),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        PlayActionButton(
            label = stringResource(R.string.reveal_who_play_next_hint),
            iconRes = R.drawable.sr_who_icon_hint,
            onClick = onNextHintClick,
            compact = compact,
            containerColor = Color(0xFFF0F8FF),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        PlayActionButton(
            label = stringResource(R.string.reveal_who_play_stop),
            iconText = "✕",
            onClick = onStopClick,
            compact = compact,
            containerColor = Color(0xFFFFF2F6),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun PlayActionButton(
    label: String,
    onClick: () -> Unit,
    compact: Boolean,
    containerColor: Color,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    iconText: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = containerColor,
        contentColor = SangsangRestStopNavy,
        shape = RoundedCornerShape(42.dp),
        border = BorderStroke(2.dp, Color.White),
        shadowElevation = 7.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 16.dp else 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            iconRes?.let {
                Image(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 40.dp else 50.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            iconText?.let {
                Text(
                    text = it,
                    color = Color(0xFFFF2E68),
                    fontSize = if (compact) 36.sp else 46.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = label,
                modifier = Modifier.padding(start = if (compact) 12.dp else 18.dp),
                fontSize = if (compact) 26.sp else 34.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Preview(
    name = "Reveal Who Play Pleos",
    widthDp = 1390,
    heightDp = 1000,
    showBackground = true,
)
@Composable
private fun RevealWhoPlayPleosPreview() {
    SangsangRestStopTheme {
        RevealWhoPlayScreen(
            settings = RevealWhoSettings(),
            onBackClick = {},
            onReplayClick = {},
            onNextHintClick = {},
            onStopClick = {},
        )
    }
}

@Preview(
    name = "Reveal Who Play 16:9",
    widthDp = 1280,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun RevealWhoPlayLandscapePreview() {
    SangsangRestStopTheme {
        RevealWhoPlayScreen(
            settings = RevealWhoSettings(),
            onBackClick = {},
            onReplayClick = {},
            onNextHintClick = {},
            onStopClick = {},
        )
    }
}
