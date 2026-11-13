package com.sangsangreststop.app.feature.wordchain

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sangsangreststop.app.R
import com.sangsangreststop.app.ui.theme.SangsangRestStopBlue
import com.sangsangreststop.app.ui.theme.SangsangRestStopNavy
import com.sangsangreststop.app.ui.theme.SangsangRestStopTheme

enum class WordChainTurn {
    CHILD,
    AI,
}

enum class WordChainVoiceInputState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
}

enum class WordChainPauseCause {
    MANUAL,
    PHONE,
    NAVIGATION,
}

data class WordChainPlayUiState(
    val currentTurn: WordChainTurn = WordChainTurn.CHILD,
    val previousWord: String = "사과",
    val nextSyllable: String = "과",
    val guidance: String = "다음은 이 글자로 시작하는 단어예요!",
    val voiceState: WordChainVoiceInputState = WordChainVoiceInputState.IDLE,
    val difficulty: WordChainDifficulty = WordChainDifficulty.Easy,
    val hintsEnabled: Boolean = true,
    val isPaused: Boolean = false,
    val pauseCause: WordChainPauseCause? = null,
    val errorMessage: String? = null,
)

@Composable
fun WordChainPlayScreen(
    uiState: WordChainPlayUiState,
    onMicClick: () -> Unit,
    onStopClick: () -> Unit,
    onReplayClick: () -> Unit,
    onHintClick: () -> Unit,
    onExampleClick: () -> Unit,
    onTextInputClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFDDF3FF),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val compact = (maxWidth < 1_100.dp) || (maxHeight < 760.dp)
            val horizontalPadding = (maxWidth * 0.018f).coerceIn(18.dp, 32.dp)
            val tableSurfaceTop = maxHeight * if (compact) 0.64f else 0.66f
            val tableImageHeight = maxWidth / 2.667f
            val tableImageTop = tableSurfaceTop - (tableImageHeight * 0.43f)
            // The table PNG has transparent scenery above its visible yellow edge.
            // Anchor foreground elements to that visible edge rather than the image bounds.
            val tableVisibleTop = tableImageTop + (tableImageHeight * 0.74f)

            // The reference is much wider than the Pleos content window. Size the artwork
            // against both axes so a tall window does not make the center panel oversized
            // or leave the characters visually too small.
            val girlWidth = minOf(maxWidth * 0.27f, maxHeight * 0.38f)
                .coerceIn(280.dp, 460.dp)
            val girlHeight = girlWidth / 0.803f
            val rabbitWidth = minOf(maxWidth * 0.125f, maxHeight * 0.17f)
                .coerceIn(140.dp, 230.dp)
            val rabbitHeight = rabbitWidth / 0.765f
            val boyWidth = minOf(maxWidth * 0.255f, maxHeight * 0.355f)
                .coerceIn(270.dp, 430.dp)
            val boyHeight = boyWidth / 0.743f

            val panelWidth = minOf(maxWidth * 0.34f, maxHeight * 0.53f)
                .coerceIn(450.dp, 590.dp)
            val panelHeight = panelWidth / 1.42f
            val panelTop = (maxHeight * if (compact) 0.22f else 0.245f)
                .coerceIn(170.dp, 300.dp)
            val panelLeft = (maxWidth - panelWidth) / 2f
            val previousWordOverlap = if (compact) 48.dp else 56.dp

            val micSize = minOf(maxWidth * 0.14f, maxHeight * 0.20f)
                .coerceIn(176.dp, 230.dp)
            val actionHeight = if (compact) 72.dp else 82.dp
            val actionBottomPadding = (maxHeight * 0.032f).coerceIn(24.dp, 38.dp)
            val centerActionClearance = micSize + if (compact) 56.dp else 72.dp
            val actionSideSpace = (
                (maxWidth - centerActionClearance - (horizontalPadding * 2f)) / 2f
                ).coerceAtLeast(280.dp)
            val leftActionsWidth = (actionSideSpace * 0.86f)
                .coerceIn(if (compact) 300.dp else 380.dp, if (compact) 420.dp else 540.dp)
            val rightActionsWidth = actionSideSpace
                .coerceIn(if (compact) 360.dp else 440.dp, if (compact) 500.dp else 620.dp)

            val rabbitTransparentBottomInset = (maxHeight * 0.045f).coerceIn(45.dp, 58.dp)
            val childTransparentBottomInset = (maxHeight * 0.016f).coerceIn(16.dp, 22.dp)

            // Preserve the reference centers while allowing each character to grow with
            // the available height. Keep their visible groups outside the center panel.
            val sideArtworkGap = 12.dp
            val preferredGirlX = (maxWidth * 0.24f) - (girlWidth / 2f)
            val girlX = minOf(
                preferredGirlX,
                panelLeft - girlWidth - sideArtworkGap,
            ).coerceAtLeast(horizontalPadding)
            val girlY = (tableVisibleTop - girlHeight) - childTransparentBottomInset
            val rabbitX = ((maxWidth * 0.065f) - (rabbitWidth / 2f))
                .coerceAtLeast(0.dp)
            val rabbitY = (tableVisibleTop - rabbitHeight) - rabbitTransparentBottomInset
            val preferredBoyX = (maxWidth * 0.79f) - (boyWidth / 2f)
            val boyX = maxOf(
                preferredBoyX,
                panelLeft + panelWidth + sideArtworkGap,
            ).coerceAtMost(maxWidth - boyWidth - horizontalPadding)
            val boyY = (tableVisibleTop - boyHeight) - childTransparentBottomInset

            // Bubble dimensions follow the corresponding character rather than the screen.
            // The artwork itself compensates for transparent PNG margins below.
            val bubblePanelGap = sideArtworkGap
            val bubbleHeadGap = (maxWidth * 0.007f).coerceIn(8.dp, 16.dp)

            val bubbleAspectRatio = 4f / 3f
            val girlBubbleWidth = (girlWidth * 1.0575f).coerceIn(382.dp, 515.dp)
            val girlBubbleHeight = girlBubbleWidth / bubbleAspectRatio
            val girlHeadAnchorX = girlX + (girlWidth * 0.56f)
            val girlTailX = girlBubbleWidth * 0.20f
            val girlBubbleX = minOf(
                girlHeadAnchorX - girlTailX,
                panelLeft - girlBubbleWidth - bubblePanelGap,
            ).coerceAtLeast(0.dp)
            val girlVisibleHeadTop = girlY + (girlHeight * 0.05f)
            val girlBubbleY =
                (girlVisibleHeadTop - (girlBubbleHeight * 0.94f)) + bubbleHeadGap +
                    if (compact) 18.dp else 16.dp

            val boyBubbleWidth = (boyWidth * 1.1456f).coerceIn(411.dp, 529.dp)
            val boyBubbleHeight = boyBubbleWidth / bubbleAspectRatio
            val boyHeadAnchorX = boyX + (boyWidth * 0.44f)
            val boyTailX = boyBubbleWidth * 0.80f
            val boyBubbleX = maxOf(
                boyHeadAnchorX - boyTailX,
                panelLeft + panelWidth + bubblePanelGap,
            ).coerceAtMost(maxWidth - boyBubbleWidth)
            val boyVisibleHeadTop = boyY + (boyHeight * 0.105f)
            val boyBubbleY =
                (boyVisibleHeadTop - (boyBubbleHeight * 0.94f)) + bubbleHeadGap

            Image(
                painter = painterResource(R.drawable.sr_bg_wordchain_play),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // Characters are anchored to the visible table edge. The table is drawn later,
            // so their transparent PNG bottoms are naturally hidden behind it.
            Image(
                painter = painterResource(R.drawable.sr_char_wordchain_play_rabbit),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = rabbitX, y = rabbitY)
                    .width(rabbitWidth)
                    .height(rabbitHeight),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
            )
            Image(
                painter = painterResource(R.drawable.sr_char_wordchain_play_girl),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = girlX, y = girlY)
                    .width(girlWidth)
                    .height(girlHeight),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
            )
            Image(
                painter = painterResource(R.drawable.sr_char_wordchain_play_bo),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = boyX, y = boyY)
                    .width(boyWidth)
                    .height(boyHeight),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
            )

            Image(
                painter = painterResource(R.drawable.sr_deco_wordchain_play_panel),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = panelLeft, y = panelTop)
                    .width(panelWidth)
                    .height(panelHeight),
                contentScale = ContentScale.Fit,
            )

            CharacterSpeechBubble(
                text = stringResource(R.string.wordchain_play_girl_bubble),
                imageRes = R.drawable.sr_deco_wordchain_play_bubble,
                compact = compact,
                modifier = Modifier
                    .offset(x = girlBubbleX, y = girlBubbleY)
                    .width(girlBubbleWidth)
                    .height(girlBubbleHeight),
            )
            CharacterSpeechBubble(
                text = stringResource(R.string.wordchain_play_boy_bubble),
                imageRes = R.drawable.sr_deco_wordchain_play_bubble_boy,
                compact = compact,
                modifier = Modifier
                    .offset(x = boyBubbleX, y = boyBubbleY)
                    .width(boyBubbleWidth)
                    .height(boyBubbleHeight),
            )
            Image(
                painter = painterResource(R.drawable.sr_deco_wordchain_play_stars),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = if (compact) 100.dp else 124.dp,
                        end = if (compact) 32.dp else 52.dp,
                    )
                    .size(if (compact) 100.dp else 132.dp),
                contentScale = ContentScale.Fit,
            )

            val tableExtensionTop = tableSurfaceTop + if (compact) 82.dp else 104.dp
            Surface(
                modifier = Modifier
                    .offset(y = tableExtensionTop)
                    .fillMaxWidth()
                    .height((maxHeight - tableExtensionTop).coerceAtLeast(1.dp)),
                color = Color(0xFFFFD98C),
            ) {}

            Image(
                painter = painterResource(R.drawable.sr_deco_wordchain_play_table),
                contentDescription = null,
                modifier = Modifier
                    .offset(y = tableImageTop)
                    .fillMaxWidth()
                    .height(tableImageHeight),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
            )

            WordChainPlayHeader(
                uiState = uiState,
                compact = compact,
                onBackClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = if (compact) 14.dp else 20.dp),
            )

            PreviousWordPanel(
                previousWord = uiState.previousWord,
                compact = compact,
                modifier = Modifier
                    .offset(
                        x = panelLeft + (panelWidth * 0.09f),
                        y = panelTop - previousWordOverlap,
                    )
                    .width(panelWidth * 0.82f)
                    .height(if (compact) 112.dp else 132.dp),
            )

            WordChainCenterStatus(
                uiState = uiState,
                compact = compact,
                modifier = Modifier
                    .offset(
                        x = panelLeft + (panelWidth * 0.09f),
                        y = panelTop + (panelHeight * 0.24f),
                    )
                    .width(panelWidth * 0.82f)
                    .height(panelHeight * 0.62f),
            )

            WordChainBottomActions(
                uiState = uiState,
                onMicClick = onMicClick,
                onStopClick = onStopClick,
                onReplayClick = onReplayClick,
                onHintClick = onHintClick,
                onExampleClick = onExampleClick,
                onTextInputClick = onTextInputClick,
                compact = compact,
                micSize = micSize,
                tableVisibleTop = tableVisibleTop,
                actionHeight = actionHeight,
                bottomPadding = actionBottomPadding,
                horizontalPadding = horizontalPadding,
                leftActionsWidth = leftActionsWidth,
                rightActionsWidth = rightActionsWidth,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun WordChainPlayHeader(
    uiState: WordChainPlayUiState,
    compact: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backDescription = stringResource(R.string.wordchain_back_content_description)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier
                .size(if (compact) 56.dp else 68.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = backDescription
                }
                .clickable(onClick = onBackClick),
            color = Color.White.copy(alpha = 0.95f),
            shape = CircleShape,
            shadowElevation = 6.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                PlayChevronIcon(
                    pointsRight = false,
                    color = SangsangRestStopNavy,
                    modifier = Modifier.size(if (compact) 24.dp else 30.dp),
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = if (compact) 16.dp else 22.dp),
        ) {
            Text(
                text = stringResource(R.string.wordchain_setup_title),
                color = SangsangRestStopNavy,
                fontSize = if (compact) 40.sp else 50.sp,
                lineHeight = if (compact) 46.sp else 56.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = stringResource(R.string.wordchain_play_description),
                color = SangsangRestStopNavy.copy(alpha = 0.78f),
                fontSize = if (compact) 23.sp else 28.sp,
                lineHeight = if (compact) 30.sp else 35.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.weight(1f))

        TurnIndicator(
            currentTurn = uiState.currentTurn,
            compact = compact,
            modifier = Modifier
                .width(if (compact) 360.dp else 460.dp)
                .height(if (compact) 72.dp else 88.dp),
        )
    }
}

@Composable
private fun TurnIndicator(
    currentTurn: WordChainTurn,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.94f),
        shape = RoundedCornerShape(50),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 14.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
        ) {
            TurnChip(
                label = stringResource(R.string.wordchain_play_child_turn),
                selected = currentTurn == WordChainTurn.CHILD,
                color = SangsangRestStopBlue,
                modifier = Modifier.weight(1f),
            )
            TurnChip(
                label = stringResource(R.string.wordchain_play_ai_turn),
                selected = currentTurn == WordChainTurn.AI,
                color = Color(0xFF9AA5AD),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TurnChip(
    label: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(0.72f),
        color = if (selected) color.copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(28.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) color else SangsangRestStopNavy.copy(alpha = 0.38f),
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CharacterSpeechBubble(
    text: String,
    imageRes: Int,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = text,
            modifier = Modifier.padding(
                start = if (compact) 28.dp else 34.dp,
                top = if (compact) 22.dp else 26.dp,
                end = if (compact) 28.dp else 34.dp,
                bottom = if (compact) 34.dp else 40.dp,
            ),
            color = SangsangRestStopNavy,
            fontSize = if (compact) 34.sp else 42.sp,
            lineHeight = if (compact) 43.sp else 52.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun PreviousWordPanel(
    previousWord: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF52A9F5).copy(alpha = 0.94f),
        shape = RoundedCornerShape(if (compact) 28.dp else 36.dp),
        shadowElevation = 7.dp,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 26.dp else 34.dp,
                vertical = if (compact) 12.dp else 15.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.wordchain_play_previous_word),
                color = Color.White,
                fontSize = if (compact) 21.sp else 24.sp,
                lineHeight = if (compact) 26.sp else 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                color = Color.White.copy(alpha = 0.96f),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text(
                    text = previousWord,
                    modifier = Modifier.padding(vertical = if (compact) 8.dp else 10.dp),
                    color = SangsangRestStopNavy,
                    fontSize = if (compact) 38.sp else 46.sp,
                    lineHeight = if (compact) 44.sp else 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WordChainCenterStatus(
    uiState: WordChainPlayUiState,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val pausedMessage = when (uiState.pauseCause) {
        WordChainPauseCause.PHONE -> stringResource(R.string.wordchain_play_paused_phone)
        WordChainPauseCause.NAVIGATION -> stringResource(R.string.wordchain_play_paused_navigation)
        WordChainPauseCause.MANUAL,
        null,
        -> stringResource(R.string.wordchain_play_paused)
    }
    val statusMessage = when {
        uiState.errorMessage != null -> uiState.errorMessage
        uiState.isPaused -> pausedMessage
        else -> uiState.guidance
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = statusMessage,
            color = SangsangRestStopNavy.copy(alpha = 0.80f),
            fontSize = if (compact) 24.sp else 28.sp,
            lineHeight = if (compact) 31.sp else 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Surface(
            modifier = Modifier
                .padding(top = if (compact) 12.dp else 18.dp)
                .size(if (compact) 130.dp else 160.dp),
            color = Color(0xFFE8F1FF),
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.nextSyllable,
                    color = Color(0xFF235FE8),
                    fontSize = if (compact) 72.sp else 90.sp,
                    lineHeight = if (compact) 78.sp else 96.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun WordChainBottomActions(
    uiState: WordChainPlayUiState,
    onMicClick: () -> Unit,
    onStopClick: () -> Unit,
    onReplayClick: () -> Unit,
    onHintClick: () -> Unit,
    onExampleClick: () -> Unit,
    onTextInputClick: () -> Unit,
    compact: Boolean,
    micSize: Dp,
    tableVisibleTop: Dp,
    actionHeight: Dp,
    bottomPadding: Dp,
    horizontalPadding: Dp,
    leftActionsWidth: Dp,
    rightActionsWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val interactionEnabled = !uiState.isPaused && uiState.errorMessage == null
    val voiceLabel = when (uiState.voiceState) {
        WordChainVoiceInputState.IDLE -> stringResource(R.string.wordchain_play_voice_idle)
        WordChainVoiceInputState.LISTENING -> stringResource(R.string.wordchain_play_voice_listening)
        WordChainVoiceInputState.PROCESSING -> stringResource(R.string.wordchain_play_voice_processing)
        WordChainVoiceInputState.SPEAKING -> stringResource(R.string.wordchain_play_voice_speaking)
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = horizontalPadding, bottom = bottomPadding)
                .width(leftActionsWidth),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
        ) {
            PlayActionButton(
                label = stringResource(R.string.wordchain_play_stop),
                icon = PlayActionIcon.STOP,
                tint = Color(0xFF5E6C87),
                enabled = true,
                onClick = onStopClick,
                compact = compact,
                modifier = Modifier
                    .weight(1f)
                    .height(actionHeight),
            )
            PlayActionButton(
                label = stringResource(R.string.wordchain_play_replay),
                icon = PlayActionIcon.REPLAY,
                tint = Color(0xFF5E6C87),
                enabled = interactionEnabled,
                onClick = onReplayClick,
                compact = compact,
                modifier = Modifier
                    .weight(1f)
                    .height(actionHeight),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = horizontalPadding, bottom = bottomPadding)
                .width(rightActionsWidth),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        ) {
            PlayActionButton(
                label = stringResource(R.string.wordchain_play_hint),
                icon = PlayActionIcon.HINT,
                tint = Color(0xFFF24982),
                enabled = interactionEnabled && uiState.hintsEnabled,
                onClick = onHintClick,
                compact = compact,
                modifier = Modifier
                    .weight(0.92f)
                    .height(actionHeight),
            )
            PlayActionButton(
                label = stringResource(R.string.wordchain_play_example),
                icon = PlayActionIcon.EXAMPLE,
                tint = Color(0xFF12A97B),
                enabled = interactionEnabled,
                onClick = onExampleClick,
                compact = compact,
                modifier = Modifier
                    .weight(1.08f)
                    .height(actionHeight),
            )
            PlayActionButton(
                label = stringResource(R.string.wordchain_play_text_input),
                icon = PlayActionIcon.KEYBOARD,
                tint = Color(0xFF2F7DEB),
                enabled = interactionEnabled,
                onClick = onTextInputClick,
                compact = compact,
                modifier = Modifier
                    .weight(0.90f)
                    .height(actionHeight),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    y = tableVisibleTop - (micSize * 0.62f) -
                        if (compact) 38.dp else 48.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier
                    .size(micSize)
                    .shadow(14.dp, CircleShape)
                    .border(6.dp, Color.White.copy(alpha = 0.82f), CircleShape)
                    .semantics {
                        role = Role.Button
                        contentDescription = voiceLabel
                        if (!interactionEnabled) disabled()
                    }
                    .clickable(
                        enabled = interactionEnabled,
                        onClick = onMicClick,
                    ),
                color = if (interactionEnabled) SangsangRestStopBlue else Color(0xFF9AA7B5),
                contentColor = Color.White,
                shape = CircleShape,
                shadowElevation = 10.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PlayMicrophoneIcon(
                        color = Color.White,
                        modifier = Modifier.size(micSize * 0.46f),
                    )
                }
            }
            Text(
                text = voiceLabel,
                modifier = Modifier.padding(top = 10.dp),
                color = SangsangRestStopNavy,
                fontSize = if (compact) 27.sp else 33.sp,
                lineHeight = if (compact) 34.sp else 41.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private enum class PlayActionIcon {
    STOP,
    REPLAY,
    HINT,
    EXAMPLE,
    KEYBOARD,
}

@Composable
private fun PlayActionButton(
    label: String,
    icon: PlayActionIcon,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.55f)
            .semantics {
                role = Role.Button
                contentDescription = label
                if (!enabled) disabled()
            }
            .clickable(enabled = enabled, onClick = onClick),
        color = Color.White.copy(alpha = 0.94f),
        shape = RoundedCornerShape(if (compact) 26.dp else 32.dp),
        shadowElevation = if (enabled) 5.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            PlayActionIcon(
                icon = icon,
                color = tint,
                modifier = Modifier.size(if (compact) 28.dp else 34.dp),
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = if (compact) 6.dp else 8.dp),
                color = tint,
                fontSize = if (compact) 23.sp else 28.sp,
                lineHeight = if (compact) 29.sp else 35.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun PlayActionIcon(
    icon: PlayActionIcon,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.09f
        when (icon) {
            PlayActionIcon.STOP -> {
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.24f, size.height * 0.24f),
                    end = Offset(size.width * 0.76f, size.height * 0.76f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.76f, size.height * 0.24f),
                    end = Offset(size.width * 0.24f, size.height * 0.76f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            PlayActionIcon.REPLAY -> {
                drawArc(
                    color = color,
                    startAngle = -55f,
                    sweepAngle = 285f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.16f),
                    size = Size(size.width * 0.68f, size.height * 0.68f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                val arrow = Path().apply {
                    moveTo(size.width * 0.15f, size.height * 0.20f)
                    lineTo(size.width * 0.42f, size.height * 0.18f)
                    lineTo(size.width * 0.25f, size.height * 0.39f)
                    close()
                }
                drawPath(arrow, color)
            }

            PlayActionIcon.HINT -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.25f,
                    center = Offset(size.width * 0.50f, size.height * 0.40f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.40f, size.height * 0.70f),
                    end = Offset(size.width * 0.60f, size.height * 0.70f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.44f, size.height * 0.84f),
                    end = Offset(size.width * 0.56f, size.height * 0.84f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            PlayActionIcon.EXAMPLE -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.36f,
                    center = center,
                    style = Stroke(width = stroke),
                )
                drawCircle(
                    color = color,
                    radius = stroke * 0.55f,
                    center = Offset(size.width * 0.38f, size.height * 0.43f),
                )
                drawCircle(
                    color = color,
                    radius = stroke * 0.55f,
                    center = Offset(size.width * 0.62f, size.height * 0.43f),
                )
                drawArc(
                    color = color,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.32f, size.height * 0.40f),
                    size = Size(size.width * 0.36f, size.height * 0.34f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }

            PlayActionIcon.KEYBOARD -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.10f, size.height * 0.22f),
                    size = Size(size.width * 0.80f, size.height * 0.58f),
                    cornerRadius = CornerRadius(size.width * 0.08f),
                    style = Stroke(width = stroke),
                )
                val keyRadius = stroke * 0.45f
                listOf(0.30f, 0.50f, 0.70f).forEach { x ->
                    drawCircle(color, keyRadius, Offset(size.width * x, size.height * 0.43f))
                }
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.30f, size.height * 0.64f),
                    end = Offset(size.width * 0.70f, size.height * 0.64f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun PlayMicrophoneIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.10f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.34f, size.height * 0.08f),
            size = Size(size.width * 0.32f, size.height * 0.52f),
            cornerRadius = CornerRadius(size.width * 0.16f),
            style = Stroke(width = strokeWidth),
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.20f, size.height * 0.30f),
            size = Size(size.width * 0.60f, size.height * 0.46f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.50f, size.height * 0.76f),
            end = Offset(size.width * 0.50f, size.height * 0.92f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.34f, size.height * 0.92f),
            end = Offset(size.width * 0.66f, size.height * 0.92f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun PlayChevronIcon(
    pointsRight: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.14f
        val upper = Offset(
            x = size.width * if (pointsRight) 0.36f else 0.64f,
            y = size.height * 0.20f,
        )
        val middle = Offset(
            x = size.width * if (pointsRight) 0.64f else 0.36f,
            y = size.height * 0.50f,
        )
        val lower = Offset(upper.x, size.height * 0.80f)
        drawLine(color, upper, middle, strokeWidth, StrokeCap.Round)
        drawLine(color, middle, lower, strokeWidth, StrokeCap.Round)
    }
}

@Preview(device = "spec:width=1682px,height=1210px,dpi=160", showBackground = true)
@Composable
private fun WordChainPlayPreview() {
    SangsangRestStopTheme {
        WordChainPlayScreen(
            uiState = WordChainPlayUiState(),
            onMicClick = {},
            onStopClick = {},
            onReplayClick = {},
            onHintClick = {},
            onExampleClick = {},
            onTextInputClick = {},
            onBackClick = {},
        )
    }
}
