package com.sangsangreststop.app.feature.dashboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sangsangreststop.app.R
import com.sangsangreststop.app.ui.theme.SangsangRestStopBlue
import com.sangsangreststop.app.ui.theme.SangsangRestStopNavy
import com.sangsangreststop.app.ui.theme.SangsangRestStopTheme

enum class DashboardGame(@param:StringRes val titleRes: Int) {
    WordChain(R.string.dashboard_word_pair_title),
    RevealWho(R.string.dashboard_reveal_who_title),
    SoundQuiz(R.string.dashboard_sound_quiz_title),
}

@Immutable
private data class DashboardCardModel(
    val game: DashboardGame,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:DrawableRes val backgroundRes: Int,
    @param:DrawableRes val artworkRes: Int,
    val titleColor: Color,
    val accentColor: Color,
    val artworkWidthFraction: Float,
    val enabled: Boolean,
)

@Composable
fun DashboardScreen(
    onGameSelected: (DashboardGame) -> Unit,
    onOpenVoicePoc: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cards = dashboardCards()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardFallbackColor),
    ) {
        Image(
            painter = painterResource(R.drawable.sr_bg_dashboard_139),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val horizontalPadding = (maxWidth * 0.018f).coerceIn(20.dp, 32.dp)
            val cardSpacing = (maxWidth * 0.012f).coerceIn(16.dp, 24.dp)
            val bottomPadding = (maxHeight * 0.024f).coerceIn(16.dp, 30.dp)
            val voiceBarHeight = (maxHeight * 0.078f).coerceIn(70.dp, 94.dp)
            val voiceBarTop = maxHeight - bottomPadding - voiceBarHeight
            val cardToVoiceGap = (maxHeight * 0.022f).coerceIn(18.dp, 30.dp)
            val wideLayout = maxWidth >= 900.dp
            val cardWidth = if (wideLayout) {
                (
                    (maxWidth - (horizontalPadding * 2f) - (cardSpacing * 2f)) / 3f
                    ).coerceAtMost(540.dp)
            } else {
                (maxWidth * 0.78f).coerceIn(280.dp, 420.dp)
            }
            val minimumHeroHeight = (maxHeight * 0.30f).coerceAtLeast(180.dp)
            val maximumCardHeight = (
                voiceBarTop - minimumHeroHeight - cardToVoiceGap
                ).coerceAtLeast(260.dp)
            val cardHeight = (
                cardWidth / if (wideLayout) 1.08f else 0.96f
                ).coerceIn(260.dp, maximumCardHeight)
            val cardTop = voiceBarTop - cardToVoiceGap - cardHeight
            val voiceBarWidth = if (wideLayout) {
                (maxWidth * 0.68f).coerceAtMost(1_100.dp)
            } else {
                maxWidth - (horizontalPadding * 2f)
            }

            DashboardHero(
                onOpenVoicePoc = onOpenVoicePoc,
                wideLayout = wideLayout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardTop),
            )

            if (wideLayout) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = cardTop)
                        .padding(horizontal = horizontalPadding)
                        .fillMaxWidth()
                        .height(cardHeight),
                    horizontalArrangement = Arrangement.spacedBy(cardSpacing),
                ) {
                    cards.forEach { card ->
                        DashboardGameCard(
                            model = card,
                            onClick = { onGameSelected(card.game) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .offset(y = cardTop),
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(cardSpacing),
                ) {
                    items(
                        items = cards,
                        key = DashboardCardModel::titleRes,
                    ) { card ->
                        DashboardGameCard(
                            model = card,
                            onClick = { onGameSelected(card.game) },
                            modifier = Modifier
                                .width(cardWidth)
                                .fillMaxHeight(),
                        )
                    }
                }
            }

            VoiceStatusBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding)
                    .width(voiceBarWidth)
                    .height(voiceBarHeight),
            )
        }
    }
}

private fun dashboardCards(): List<DashboardCardModel> = listOf(
    DashboardCardModel(
        game = DashboardGame.WordChain,
        titleRes = R.string.dashboard_word_pair_title,
        descriptionRes = R.string.dashboard_word_pair_description,
        backgroundRes = R.drawable.sr_card_word_bg,
        artworkRes = R.drawable.sr_card_word_art,
        titleColor = Color(0xFF9D1457),
        accentColor = Color(0xFFFF3F87),
        artworkWidthFraction = 0.88f,
        enabled = true,
    ),
    DashboardCardModel(
        game = DashboardGame.RevealWho,
        titleRes = R.string.dashboard_reveal_who_title,
        descriptionRes = R.string.dashboard_reveal_who_description,
        backgroundRes = R.drawable.sr_card_who_bg,
        artworkRes = R.drawable.sr_card_who_art,
        titleColor = Color(0xFF10529A),
        accentColor = Color(0xFF2E87F4),
        artworkWidthFraction = 0.88f,
        enabled = false,
    ),
    DashboardCardModel(
        game = DashboardGame.SoundQuiz,
        titleRes = R.string.dashboard_sound_quiz_title,
        descriptionRes = R.string.dashboard_sound_quiz_description,
        backgroundRes = R.drawable.sr_card_sound_bg,
        artworkRes = R.drawable.sr_card_sound_art,
        titleColor = Color(0xFF11763C),
        accentColor = Color(0xFF10AF62),
        artworkWidthFraction = 0.76f,
        enabled = false,
    ),
)

@Composable
private fun DashboardHero(
    onOpenVoicePoc: () -> Unit,
    wideLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val logoWidth = if (wideLayout) {
            (maxWidth * 0.25f).coerceIn(320.dp, 390.dp)
        } else {
            (maxWidth * 0.28f).coerceIn(190.dp, 260.dp)
        }
        val logoTop = if (wideLayout) 72.dp else 58.dp
        val logoLeft = if (wideLayout) 34.dp else 20.dp
        val logoHeight = logoWidth / LogoAspectRatio

        BrandPill(
            modifier = Modifier
                .offset(x = 24.dp, y = 16.dp),
        )

        Image(
            painter = painterResource(R.drawable.sr_logo_main),
            contentDescription = stringResource(R.string.dashboard_brand),
            modifier = Modifier
                .offset(x = logoLeft, y = logoTop)
                .width(logoWidth)
                .height(logoHeight),
            contentScale = ContentScale.Fit,
        )

        Surface(
            modifier = Modifier
                .offset(
                    x = logoLeft + (logoWidth * 0.05f),
                    y = (logoTop + logoHeight) - 10.dp,
                )
                .width(logoWidth * 0.90f),
            color = Color.White.copy(alpha = 0.90f),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 3.dp,
        ) {
            Text(
                text = stringResource(R.string.dashboard_subtitle),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                color = SangsangRestStopNavy,
                fontSize = if (wideLayout) 18.sp else 14.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }

        if (wideLayout) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = 44.dp, y = 30.dp),
                color = Color.White.copy(alpha = 0.94f),
                shape = RoundedCornerShape(50),
                shadowElevation = 5.dp,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_speech_bubble),
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                    color = Color(0xFF6E241A),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        VoiceProfileCard(
            onOpenVoicePoc = onOpenVoicePoc,
            compact = !wideLayout,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 24.dp)
                .width(if (wideLayout) 300.dp else 220.dp),
        )
    }
}

@Composable
private fun BrandPill(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.94f),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 18.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.sr_logo_symbol),
                contentDescription = null,
                modifier = Modifier
                    .width(38.dp)
                    .height(44.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(R.string.dashboard_brand),
                color = SangsangRestStopNavy,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "›",
                color = SangsangRestStopNavy,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun VoiceProfileCard(
    onOpenVoicePoc: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onOpenVoicePoc),
        color = Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.sr_avatar_girl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(if (compact) 48.dp else 66.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFE1ED)),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dashboard_current_voice),
                        color = SangsangRestStopNavy.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_voice_name),
                        color = SangsangRestStopNavy,
                        style = if (compact) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                }
                Text(
                    text = "›",
                    color = SangsangRestStopNavy,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (!compact) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFE1F0FF),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text(
                        text = "⚙  " + stringResource(R.string.dashboard_voice_test),
                        modifier = Modifier.padding(vertical = 11.dp),
                        color = SangsangRestStopNavy,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardGameCard(
    model: DashboardCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(model.titleRes)
    val description = stringResource(model.descriptionRes)
    val shape = RoundedCornerShape(42.dp)

    Surface(
        modifier = modifier.clickable(
            enabled = model.enabled,
            onClick = onClick,
        ),
        color = Color.Transparent,
        shape = shape,
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.92f)),
        shadowElevation = 10.dp,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cardWidth = maxWidth
            val cardHeight = maxHeight

            Image(
                painter = painterResource(model.backgroundRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
            )

            Image(
                painter = painterResource(model.artworkRes),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(cardWidth * model.artworkWidthFraction)
                    .height(cardHeight * 0.62f),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = (cardWidth * 0.075f).coerceIn(20.dp, 34.dp),
                        top = (cardHeight * 0.065f).coerceIn(18.dp, 32.dp),
                        end = 18.dp,
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        color = model.titleColor,
                        fontSize = if (cardWidth >= 430.dp) 42.sp else 30.sp,
                        lineHeight = if (cardWidth >= 430.dp) 48.sp else 36.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                    Text(
                        text = "›",
                        color = model.titleColor,
                        fontSize = if (cardWidth >= 430.dp) 44.sp else 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = description,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .width(cardWidth * 0.70f),
                    color = SangsangRestStopNavy,
                    fontSize = if (cardWidth >= 430.dp) 24.sp else 19.sp,
                    lineHeight = if (cardWidth >= 430.dp) 31.sp else 25.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )
                if (!model.enabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_coming_soon_status),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            color = model.titleColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = (cardWidth * 0.055f).coerceIn(16.dp, 28.dp),
                        bottom = (cardHeight * 0.055f).coerceIn(16.dp, 28.dp),
                    )
                    .size((cardWidth * 0.15f).coerceIn(58.dp, 78.dp)),
                color = model.accentColor.copy(alpha = if (model.enabled) 1f else 0.70f),
                contentColor = Color.White,
                shape = CircleShape,
                border = BorderStroke(3.dp, Color.White),
                shadowElevation = 5.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (model.enabled) "→" else "—",
                        fontSize = if (cardWidth >= 430.dp) 40.sp else 32.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceStatusBar(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.94f),
        shape = RoundedCornerShape(48.dp),
        border = BorderStroke(2.dp, SangsangRestStopBlue.copy(alpha = 0.18f)),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                color = SangsangRestStopBlue,
                contentColor = Color.White,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    MicrophoneIcon(
                        modifier = Modifier.size(29.dp),
                        color = Color.White,
                    )
                }
            }
            Text(
                text = stringResource(R.string.dashboard_voice_hint),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 22.dp),
                color = SangsangRestStopNavy,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            VoiceWaveform(
                modifier = Modifier
                    .width(66.dp)
                    .height(34.dp),
                color = SangsangRestStopBlue,
            )
            Text(
                text = "›",
                modifier = Modifier.padding(start = 12.dp),
                color = SangsangRestStopNavy,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MicrophoneIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.10f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.34f, size.height * 0.08f),
            size = Size(size.width * 0.32f, size.height * 0.52f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.16f),
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
private fun VoiceWaveform(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val heights = listOf(0.30f, 0.60f, 0.96f, 0.58f, 0.34f)
        val step = size.width / heights.size
        heights.forEachIndexed { index, heightFraction ->
            val x = (step * index) + (step / 2f)
            val halfHeight = (size.height * heightFraction) / 2f
            drawLine(
                color = color,
                start = Offset(x, (size.height / 2f) - halfHeight),
                end = Offset(x, (size.height / 2f) + halfHeight),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val LogoAspectRatio = 502f / 308f
private val DashboardFallbackColor = Color(0xFF8ED4F5)

@Preview(
    name = "Pleos Connect dashboard",
    widthDp = 1682,
    heightDp = 1210,
    showBackground = true,
)
@Composable
private fun DashboardPleosPreview() {
    SangsangRestStopTheme {
        DashboardScreen(
            onGameSelected = {},
            onOpenVoicePoc = {},
        )
    }
}
