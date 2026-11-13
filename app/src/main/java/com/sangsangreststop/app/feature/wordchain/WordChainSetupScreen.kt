package com.sangsangreststop.app.feature.wordchain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sangsangreststop.app.R
import com.sangsangreststop.app.ui.theme.SangsangRestStopBlue
import com.sangsangreststop.app.ui.theme.SangsangRestStopNavy
import com.sangsangreststop.app.ui.theme.SangsangRestStopTheme

enum class WordChainDifficulty(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
) {
    Easy(
        iconRes = R.drawable.sr_icon_wordchain_easy,
        labelRes = R.string.wordchain_difficulty_easy,
        descriptionRes = R.string.wordchain_difficulty_easy_description,
    ),
    Normal(
        iconRes = R.drawable.sr_icon_wordchain_normal,
        labelRes = R.string.wordchain_difficulty_normal,
        descriptionRes = R.string.wordchain_difficulty_normal_description,
    ),
    Hard(
        iconRes = R.drawable.sr_icon_wordchain_hard,
        labelRes = R.string.wordchain_difficulty_hard,
        descriptionRes = R.string.wordchain_difficulty_hard_description,
    ),
}

@Immutable
data class WordChainSettings(
    val difficulty: WordChainDifficulty,
    val hintsEnabled: Boolean,
)

@Composable
fun WordChainSetupScreen(
    onBack: () -> Unit,
    onStart: (WordChainSettings) -> Unit,
    modifier: Modifier = Modifier,
    // 저장된 설정(없으면 쉬움·힌트 켬)으로 시작하고, 바뀔 때마다 알린다.
    initialSettings: WordChainSettings = WordChainSettings(WordChainDifficulty.Easy, hintsEnabled = true),
    onSettingsChange: (WordChainSettings) -> Unit = {},
) {
    var difficulty by rememberSaveable(initialSettings) { mutableStateOf(initialSettings.difficulty) }
    var hintsEnabled by rememberSaveable(initialSettings) { mutableStateOf(initialSettings.hintsEnabled) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFE4F5FF),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val compact = (maxWidth < 900.dp) || (maxHeight < 680.dp)
            val horizontalPadding = (maxWidth * 0.018f).coerceIn(18.dp, 32.dp)
            val cardGap = (maxWidth * 0.014f).coerceIn(16.dp, 26.dp)
            val actionHeight = (maxHeight * 0.09f).coerceIn(76.dp, 96.dp)
            val actionBottomPadding = (maxHeight * 0.025f).coerceIn(18.dp, 30.dp)
            val heroHeight = (maxHeight * 0.43f).coerceIn(270.dp, 440.dp)
            val cardOverlap = (heroHeight * 0.075f).coerceIn(20.dp, 34.dp)
            val settingsTop = heroHeight - cardOverlap
            val actionGap = (maxHeight * 0.025f).coerceIn(22.dp, 34.dp)
            val desiredSettingsHeight = if (compact) {
                (maxHeight * 0.46f).coerceIn(300.dp, 400.dp)
            } else {
                (maxHeight * 0.38f).coerceIn(360.dp, 460.dp)
            }
            val availableSettingsHeight =
                maxHeight - settingsTop - actionHeight - actionGap - actionBottomPadding
            val settingsHeight = minOf(
                desiredSettingsHeight,
                availableSettingsHeight,
            ).coerceAtLeast(if (compact) 280.dp else 340.dp)
            val settingsBottom = settingsTop + settingsHeight
            val bottomAlignedActionTop = maxHeight - actionHeight - actionBottomPadding
            val actionTop = minOf(
                settingsBottom + actionGap,
                bottomAlignedActionTop,
            )

            Image(
                painter = painterResource(R.drawable.sr_bg_wordchain_settings),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            WordChainHero(
                onBack = onBack,
                compact = compact,
                width = maxWidth,
                height = heroHeight,
                cardTop = settingsTop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .zIndex(1f),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(settingsHeight)
                    .offset(y = settingsTop)
                    .padding(horizontal = horizontalPadding)
                    .zIndex(2f),
                horizontalArrangement = Arrangement.spacedBy(cardGap),
            ) {
                DifficultyCard(
                    selectedDifficulty = difficulty,
                    onDifficultySelected = {
                        difficulty = it
                        onSettingsChange(WordChainSettings(difficulty, hintsEnabled))
                    },
                    compact = compact,
                    modifier = Modifier
                        .weight(if (compact) 0.55f else 0.58f)
                        .fillMaxHeight(),
                )
                HintCard(
                    hintsEnabled = hintsEnabled,
                    onHintsEnabledChange = {
                        hintsEnabled = it
                        onSettingsChange(WordChainSettings(difficulty, hintsEnabled))
                    },
                    compact = compact,
                    modifier = Modifier
                        .weight(if (compact) 0.45f else 0.42f)
                        .fillMaxHeight(),
                )
            }

            SetupActions(
                onBack = onBack,
                onStart = {
                    onStart(
                        WordChainSettings(
                            difficulty = difficulty,
                            hintsEnabled = hintsEnabled,
                        ),
                    )
                },
                actionHeight = actionHeight,
                compact = compact,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = actionTop)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .zIndex(3f),
            )
        }
    }
}

@Composable
private fun WordChainHero(
    onBack: () -> Unit,
    compact: Boolean,
    width: Dp,
    height: Dp,
    cardTop: Dp,
    modifier: Modifier = Modifier,
) {
    val backContentDescription = stringResource(R.string.wordchain_back_content_description)
    val girlHeight = (height * 1.30f).coerceIn(390.dp, 580.dp)
    val girlCardOverlap = (cardTop * 0.46f).coerceIn(170.dp, 190.dp)
    val girlTop = ((cardTop - girlHeight) + girlCardOverlap).coerceAtLeast((-40).dp)
    val rabbitHeight = (height * 0.82f).coerceIn(220.dp, 340.dp)
    val rabbitCardOverlap = (cardTop * 0.34f).coerceIn(120.dp, 140.dp)
    val rabbitTop = ((cardTop - rabbitHeight) + rabbitCardOverlap).coerceAtLeast(48.dp)

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .padding(start = 24.dp, top = 24.dp)
                .size(if (compact) 56.dp else 68.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = backContentDescription
                }
                .clickable(onClick = onBack),
            color = Color.White.copy(alpha = 0.94f),
            shape = CircleShape,
            shadowElevation = 6.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                SangsangRestStopChevronIcon(
                    pointsRight = false,
                    color = SangsangRestStopNavy,
                    modifier = Modifier.size(if (compact) 24.dp else 30.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(
                    start = if (compact) 96.dp else 118.dp,
                    top = if (compact) 24.dp else 28.dp,
                )
                .width(if (compact) 370.dp else 480.dp),
        ) {
            Text(
                text = stringResource(R.string.wordchain_setup_title),
                color = SangsangRestStopNavy,
                fontSize = if (compact) 43.sp else 56.sp,
                lineHeight = if (compact) 49.sp else 62.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.wordchain_setup_question),
                modifier = Modifier.padding(top = 2.dp),
                color = SangsangRestStopNavy,
                fontSize = if (compact) 31.sp else 40.sp,
                lineHeight = if (compact) 37.sp else 46.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.wordchain_setup_description),
                modifier = Modifier.padding(top = if (compact) 10.dp else 14.dp),
                color = SangsangRestStopNavy.copy(alpha = 0.76f),
                fontSize = if (compact) 24.sp else 29.sp,
                lineHeight = if (compact) 31.sp else 36.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
        }

        Image(
            painter = painterResource(R.drawable.sr_char_wordchain_girl),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    x = if (compact) (-16).dp else (-30).dp,
                    y = girlTop,
                )
                .width((width * 0.39f).coerceIn(340.dp, 650.dp))
                .requiredHeight(girlHeight),
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
        )

        Image(
            painter = painterResource(R.drawable.sr_deco_wordchain_cloud),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = if (compact) 18.dp else 24.dp,
                    end = if (compact) 12.dp else 20.dp,
                )
                .width((width * 0.095f).coerceIn(130.dp, 170.dp))
                .height((height * 0.25f).coerceIn(72.dp, 104.dp))
                .alpha(0.92f),
            contentScale = ContentScale.Fit,
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(
                    x = -(width * 0.255f).coerceIn(250.dp, 430.dp),
                    y = (height * 0.10f).coerceIn(38.dp, 48.dp),
                )
                .width((width * 0.20f).coerceIn(280.dp, 340.dp))
                .height((height * 0.59f).coerceIn(220.dp, 260.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.sr_deco_wordchain_bubble),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(R.string.wordchain_setup_speech_bubble),
                modifier = Modifier.padding(
                    start = 48.dp,
                    top = 18.dp,
                    end = 40.dp,
                    bottom = 42.dp,
                ),
                color = SangsangRestStopNavy,
                fontSize = if (compact) 22.sp else 27.sp,
                lineHeight = if (compact) 27.sp else 34.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }

        Image(
            painter = painterResource(R.drawable.sr_char_wordchain_rabbit),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = if (compact) 20.dp else 34.dp)
                .offset(
                    x = -(width * 0.10f).coerceIn(110.dp, 175.dp),
                    y = rabbitTop,
                )
                .width((width * 0.16f).coerceIn(170.dp, 270.dp))
                .requiredHeight(rabbitHeight),
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
        )

        Image(
            painter = painterResource(R.drawable.sr_deco_wordchain_stars),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = if (compact) 32.dp else 42.dp,
                    end = if (compact) 142.dp else 196.dp,
                )
                .size(if (compact) 72.dp else 94.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun DifficultyCard(
    selectedDifficulty: WordChainDifficulty,
    onDifficultySelected: (WordChainDifficulty) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFFFEDF4).copy(alpha = 0.96f),
        shape = RoundedCornerShape(if (compact) 28.dp else 34.dp),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 22.dp else 30.dp),
        ) {
            Text(
                text = stringResource(R.string.wordchain_difficulty_title),
                color = SangsangRestStopNavy,
                fontSize = if (compact) 30.sp else 38.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = stringResource(R.string.wordchain_difficulty_prompt),
                modifier = Modifier.padding(top = 2.dp),
                color = SangsangRestStopNavy.copy(alpha = 0.72f),
                fontSize = if (compact) 23.sp else 27.sp,
                lineHeight = if (compact) 29.sp else 33.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 220.dp else 270.dp)
                    .padding(top = if (compact) 14.dp else 20.dp),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
            ) {
                WordChainDifficulty.entries.forEach { difficulty ->
                    DifficultyOption(
                        difficulty = difficulty,
                        selected = difficulty == selectedDifficulty,
                        onClick = { onDifficultySelected(difficulty) },
                        compact = compact,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyOption(
    difficulty: WordChainDifficulty,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val selectedColor = Color(0xFFF45B91)
    val difficultyLabel = stringResource(difficulty.labelRes)

    Surface(
        modifier = modifier
            .semantics {
                role = Role.Button
                contentDescription = difficultyLabel
            }
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = if (selected) 0.98f else 0.82f),
        shape = RoundedCornerShape(if (compact) 22.dp else 26.dp),
        border = BorderStroke(
            width = if (selected) 3.dp else 1.dp,
            color = if (selected) {
                selectedColor
            } else {
                Color.White.copy(alpha = 0.82f)
            },
        ),
        shadowElevation = if (selected) 5.dp else 1.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (selected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(if (compact) 30.dp else 36.dp),
                    color = selectedColor,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SangsangRestStopCheckIcon(
                            color = Color.White,
                            modifier = Modifier.size(if (compact) 17.dp else 21.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(difficulty.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 80.dp else 104.dp),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    text = stringResource(difficulty.labelRes),
                    modifier = Modifier.padding(top = 6.dp),
                    color = SangsangRestStopNavy,
                    fontSize = if (compact) 28.sp else 35.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = stringResource(difficulty.descriptionRes),
                    modifier = Modifier.padding(top = 4.dp),
                    color = SangsangRestStopNavy.copy(alpha = 0.74f),
                    fontSize = if (compact) 20.sp else 23.sp,
                    lineHeight = if (compact) 24.sp else 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HintCard(
    hintsEnabled: Boolean,
    onHintsEnabledChange: (Boolean) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFE8FFF8).copy(alpha = 0.96f),
        shape = RoundedCornerShape(if (compact) 28.dp else 34.dp),
        shadowElevation = 8.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.sr_deco_wordchain_hint_cloud),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp)
                    .size(if (compact) 96.dp else 124.dp)
                    .alpha(0.94f),
                contentScale = ContentScale.Fit,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 22.dp else 30.dp),
            ) {
                Text(
                    text = stringResource(R.string.wordchain_hint_title),
                    modifier = Modifier.padding(end = if (compact) 78.dp else 108.dp),
                    color = SangsangRestStopNavy,
                    fontSize = if (compact) 30.sp else 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = stringResource(R.string.wordchain_hint_prompt),
                    modifier = Modifier.padding(
                        top = 2.dp,
                        end = if (compact) 72.dp else 104.dp,
                    ),
                    color = SangsangRestStopNavy.copy(alpha = 0.72f),
                    fontSize = if (compact) 23.sp else 27.sp,
                    lineHeight = if (compact) 29.sp else 33.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (compact) 16.dp else 22.dp)
                        .clickable { onHintsEnabledChange(!hintsEnabled) },
                    color = Color.White.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 3.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = if (compact) 18.dp else 24.dp,
                            vertical = if (compact) 20.dp else 24.dp,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Switch(
                            checked = hintsEnabled,
                            onCheckedChange = onHintsEnabledChange,
                            modifier = Modifier.size(
                                width = if (compact) 60.dp else 68.dp,
                                height = if (compact) 38.dp else 44.dp,
                            ),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SangsangRestStopBlue,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFB6C9C4),
                            ),
                        )
                        Column(
                            modifier = Modifier.padding(start = if (compact) 14.dp else 18.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (hintsEnabled) {
                                        R.string.wordchain_hint_enabled
                                    } else {
                                        R.string.wordchain_hint_disabled
                                    },
                                ),
                                color = SangsangRestStopNavy,
                                fontSize = if (compact) 29.sp else 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = stringResource(
                                    if (hintsEnabled) {
                                        R.string.wordchain_hint_enabled_description
                                    } else {
                                        R.string.wordchain_hint_disabled_description
                                    },
                                ),
                                color = SangsangRestStopNavy.copy(alpha = 0.70f),
                                fontSize = if (compact) 21.sp else 24.sp,
                                lineHeight = if (compact) 25.sp else 29.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(if (compact) 18.dp else 24.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFDDF7F2).copy(alpha = 0.88f),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text(
                        text = stringResource(R.string.wordchain_hint_info),
                        modifier = Modifier.padding(
                            horizontal = if (compact) 18.dp else 24.dp,
                            vertical = if (compact) 16.dp else 20.dp,
                        ),
                        color = SangsangRestStopNavy.copy(alpha = 0.76f),
                        fontSize = if (compact) 21.sp else 24.sp,
                        lineHeight = if (compact) 26.sp else 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupActions(
    onBack: () -> Unit,
    onStart: () -> Unit,
    actionHeight: Dp,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val previousContentDescription = stringResource(R.string.wordchain_previous)

    Row(
        modifier = modifier.height(actionHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .width(if (compact) 230.dp else 300.dp)
                .fillMaxHeight()
                .semantics {
                    role = Role.Button
                    contentDescription = previousContentDescription
                }
                .clickable(onClick = onBack),
            color = Color.White.copy(alpha = 0.96f),
            shape = RoundedCornerShape(40.dp),
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = if (compact) 12.dp else 16.dp,
                    alignment = Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SangsangRestStopChevronIcon(
                    pointsRight = false,
                    color = SangsangRestStopNavy,
                    modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                )
                Text(
                    text = stringResource(R.string.wordchain_previous),
                    color = SangsangRestStopNavy,
                    fontSize = if (compact) 24.sp else 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .width(if (compact) 440.dp else 600.dp)
                .fillMaxHeight(),
            shape = RoundedCornerShape(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SangsangRestStopBlue,
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 7.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    space = if (compact) 12.dp else 16.dp,
                    alignment = Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.wordchain_start),
                    fontSize = if (compact) 25.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
                SangsangRestStopChevronIcon(
                    pointsRight = true,
                    color = Color.White,
                    modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                )
            }
        }
    }
}

@Composable
private fun SangsangRestStopChevronIcon(
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
        val lower = Offset(
            x = upper.x,
            y = size.height * 0.80f,
        )
        drawLine(
            color = color,
            start = upper,
            end = middle,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = middle,
            end = lower,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SangsangRestStopCheckIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.16f
        val middle = Offset(size.width * 0.42f, size.height * 0.68f)
        drawLine(
            color = color,
            start = Offset(size.width * 0.16f, size.height * 0.50f),
            end = middle,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = middle,
            end = Offset(size.width * 0.84f, size.height * 0.24f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Preview(
    name = "WordChain Setup Pleos",
    widthDp = 1390,
    heightDp = 1000,
    showBackground = true,
)
@Composable
private fun WordChainSetupPreview() {
    SangsangRestStopTheme {
        WordChainSetupScreen(
            onBack = {},
            onStart = {},
        )
    }
}
