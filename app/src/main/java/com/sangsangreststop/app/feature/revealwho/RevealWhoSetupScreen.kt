package com.sangsangreststop.app.feature.revealwho

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
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

enum class RevealWhoDifficulty(
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Easy(
        R.string.reveal_who_difficulty_easy,
        R.string.reveal_who_difficulty_easy_description,
        R.drawable.sr_who_icon_easy,
    ),
    Normal(
        R.string.reveal_who_difficulty_normal,
        R.string.reveal_who_difficulty_normal_description,
        R.drawable.sr_who_icon_normal,
    ),
    Hard(
        R.string.reveal_who_difficulty_hard,
        R.string.reveal_who_difficulty_hard_description,
        R.drawable.sr_who_icon_hard,
    ),
}

enum class RevealWhoCategory(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Animal(R.string.reveal_who_category_animal, R.drawable.sr_who_icon_animal),
    Vehicle(R.string.reveal_who_category_vehicle, R.drawable.sr_who_icon_vehicle),
    Object(R.string.reveal_who_category_object, R.drawable.sr_who_icon_object),
}

data class RevealWhoSettings(
    val difficulty: RevealWhoDifficulty = RevealWhoDifficulty.Easy,
    val category: RevealWhoCategory = RevealWhoCategory.Animal,
)

@Composable
fun RevealWhoSetupScreen(
    onBack: () -> Unit,
    onStart: (RevealWhoSettings) -> Unit,
    modifier: Modifier = Modifier,
    initialSettings: RevealWhoSettings = RevealWhoSettings(),
) {
    var difficulty by rememberSaveable { mutableStateOf(initialSettings.difficulty) }
    var category by rememberSaveable { mutableStateOf(initialSettings.category) }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RevealWhoSky),
    ) {
        Image(
            painter = painterResource(R.drawable.sr_who_bg_setup),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val wideLayout = maxWidth >= 900.dp
            val compact = (maxWidth < 900.dp) || (maxHeight < 680.dp)
            val horizontalPadding = (maxWidth * 0.018f).coerceIn(18.dp, 32.dp)

            if (wideLayout) {
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

                RevealWhoHero(
                    onBack = onBack,
                    compact = compact,
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
                    DifficultyPanel(
                        selected = difficulty,
                        onSelected = { difficulty = it },
                        compact = compact,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    CategoryPanel(
                        selected = category,
                        onSelected = { category = it },
                        compact = compact,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }

                SetupFooter(
                    onBack = onBack,
                    onStart = {
                        onStart(
                            RevealWhoSettings(
                                difficulty = difficulty,
                                category = category,
                            ),
                        )
                    },
                    compact = false,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = actionTop)
                        .fillMaxWidth()
                        .height(actionHeight)
                        .padding(horizontal = horizontalPadding)
                        .zIndex(3f),
                )
            } else {
                val verticalPadding = (maxHeight * 0.024f).coerceIn(14.dp, 24.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = maxHeight)
                        .verticalScroll(scrollState)
                        .padding(
                            horizontal = horizontalPadding,
                            vertical = verticalPadding,
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RevealWhoHero(
                        onBack = onBack,
                        compact = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp),
                    )
                    DifficultyPanel(
                        selected = difficulty,
                        onSelected = { difficulty = it },
                        compact = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CategoryPanel(
                        selected = category,
                        onSelected = { category = it },
                        compact = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SetupFooter(
                        onBack = onBack,
                        onStart = {
                            onStart(
                                RevealWhoSettings(
                                    difficulty = difficulty,
                                    category = category,
                                ),
                            )
                        },
                        compact = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RevealWhoHero(
    onBack: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val backContentDescription = stringResource(R.string.wordchain_back_content_description)

    Box(modifier = modifier) {
        Surface(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 24.dp, top = 24.dp)
                .size(if (compact) 56.dp else 68.dp)
                .semantics {
                    contentDescription = backContentDescription
                },
            color = Color.White.copy(alpha = 0.96f),
            shape = androidx.compose.foundation.shape.CircleShape,
            shadowElevation = 6.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "←",
                    color = SangsangRestStopNavy,
                    fontSize = if (compact) 32.sp else 40.sp,
                    fontWeight = FontWeight.Bold,
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
                text = stringResource(R.string.reveal_who_setup_title),
                color = SangsangRestStopNavy,
                fontSize = if (compact) 43.sp else 56.sp,
                lineHeight = if (compact) 49.sp else 62.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.reveal_who_setup_subtitle),
                modifier = Modifier.padding(top = if (compact) 6.dp else 10.dp),
                color = SangsangRestStopNavy.copy(alpha = 0.86f),
                fontSize = if (compact) 24.sp else 29.sp,
                lineHeight = if (compact) 31.sp else 36.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        HeroCharacters(
            compact = compact,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(0.55f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun HeroCharacters(
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val childHeight = (maxHeight * 1.14f).coerceIn(300.dp, 500.dp)
        val childWidth = (maxWidth * 0.38f).coerceIn(230.dp, 350.dp)
        val rabbitHeight = (childHeight * 0.72f).coerceIn(215.dp, 340.dp)
        val rabbitWidth = (childWidth * 0.72f).coerceIn(165.dp, 250.dp)

        Image(
            painter = painterResource(R.drawable.sr_who_deco_hint_bubble),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = if (compact) 16.dp else 24.dp)
                .width(if (compact) 270.dp else 340.dp)
                .aspectRatio(1320f / 670f),
            contentScale = ContentScale.Fit,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = if (compact) 16.dp else 24.dp)
                .width(if (compact) 270.dp else 340.dp)
                .aspectRatio(1320f / 670f)
                .padding(
                    start = if (compact) 22.dp else 28.dp,
                    end = if (compact) 22.dp else 28.dp,
                    bottom = if (compact) 16.dp else 22.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.reveal_who_hint_bubble),
                color = SangsangRestStopNavy,
                fontSize = if (compact) 22.sp else 27.sp,
                lineHeight = if (compact) 27.sp else 33.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
        Image(
            painter = painterResource(R.drawable.sr_who_deco_sparkle),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = childWidth * 0.04f,
                    y = (maxHeight - childHeight) + (childHeight * 0.22f),
                )
                .size(if (compact) 58.dp else 76.dp),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.sr_who_deco_cloud),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(
                    x = -(childWidth * 0.08f),
                    y = (maxHeight - childHeight) + (childHeight * 0.12f),
                )
                .size(if (compact) 110.dp else 138.dp),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.sr_who_char_girl),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = if (compact) 0.dp else (-8).dp)
                .width(childWidth)
                .requiredHeight(childHeight),
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
        )
        Image(
            painter = painterResource(R.drawable.sr_who_char_rabbit),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = if (compact) 8.dp else 12.dp)
                .width(rabbitWidth)
                .requiredHeight(rabbitHeight),
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
        )
        Image(
            painter = painterResource(R.drawable.sr_who_char_boy),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = if (compact) 0.dp else 8.dp)
                .width(childWidth)
                .requiredHeight(childHeight),
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
        )
        Image(
            painter = painterResource(R.drawable.sr_who_deco_star),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = -childWidth, y = (-8).dp)
                .size(if (compact) 34.dp else 46.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun DifficultyPanel(
    selected: RevealWhoDifficulty,
    onSelected: (RevealWhoDifficulty) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    SelectionPanel(
        title = stringResource(R.string.reveal_who_difficulty_title),
        prompt = stringResource(R.string.reveal_who_difficulty_prompt),
        headerIcon = R.drawable.sr_who_icon_difficulty,
        containerColor = Color(0xFFFFF1F6).copy(alpha = 0.94f),
        compact = compact,
        modifier = modifier,
    ) { useRow ->
        if (useRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                RevealWhoDifficulty.entries.forEach { option ->
                    SelectionCard(
                        title = stringResource(option.labelRes),
                        description = stringResource(option.descriptionRes),
                        iconRes = option.iconRes,
                        selected = selected == option,
                        accentColor = RevealWhoPink,
                        compact = compact,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.08f),
                        onClick = { onSelected(option) },
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RevealWhoDifficulty.entries.forEach { option ->
                    SelectionCard(
                        title = stringResource(option.labelRes),
                        description = stringResource(option.descriptionRes),
                        iconRes = option.iconRes,
                        selected = selected == option,
                        accentColor = RevealWhoPink,
                        compact = true,
                        horizontal = true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelected(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPanel(
    selected: RevealWhoCategory,
    onSelected: (RevealWhoCategory) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    SelectionPanel(
        title = stringResource(R.string.reveal_who_category_title),
        prompt = stringResource(R.string.reveal_who_category_prompt),
        headerIcon = R.drawable.sr_who_icon_category,
        containerColor = Color(0xFFEEF8FF).copy(alpha = 0.94f),
        compact = compact,
        modifier = modifier,
    ) { useRow ->
        if (useRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                RevealWhoCategory.entries.forEach { option ->
                    SelectionCard(
                        title = stringResource(option.labelRes),
                        description = null,
                        iconRes = option.iconRes,
                        selected = selected == option,
                        accentColor = RevealWhoBlue,
                        compact = compact,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.08f),
                        onClick = { onSelected(option) },
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RevealWhoCategory.entries.forEach { option ->
                    SelectionCard(
                        title = stringResource(option.labelRes),
                        description = null,
                        iconRes = option.iconRes,
                        selected = selected == option,
                        accentColor = RevealWhoBlue,
                        compact = true,
                        horizontal = true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelected(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionPanel(
    title: String,
    prompt: String,
    @DrawableRes headerIcon: Int,
    containerColor: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (useRow: Boolean) -> Unit,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.92f)),
        shadowElevation = 9.dp,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 18.dp else 24.dp,
                vertical = if (compact) 16.dp else 22.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Image(
                    painter = painterResource(headerIcon),
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 48.dp else 60.dp),
                    contentScale = ContentScale.Fit,
                )
                Column {
                    Text(
                        text = title,
                        color = SangsangRestStopNavy,
                        fontSize = if (compact) 27.sp else 36.sp,
                        lineHeight = if (compact) 32.sp else 42.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = prompt,
                        color = SangsangRestStopNavy.copy(alpha = 0.70f),
                        fontSize = if (compact) 16.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopStart,
            ) {
                content(maxWidth >= 400.dp)
            }
        }
    }
}

@Composable
private fun SelectionCard(
    title: String,
    description: String?,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    accentColor: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
    onClick: () -> Unit,
) {
    val sizedModifier = if (horizontal) {
        modifier.height(112.dp)
    } else {
        modifier
    }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    Box(
        modifier = sizedModifier
            .clip(shape)
            .background(
                color = if (selected) {
                    accentColor.copy(alpha = 0.07f)
                } else {
                    Color.White.copy(alpha = 0.90f)
                },
            )
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) accentColor else Color.White,
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics {
                this.selected = selected
                contentDescription = if (selected) "$title, 선택됨" else title
            },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (horizontal) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = SangsangRestStopNavy,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        description?.let {
                            Text(
                                text = it,
                                color = SangsangRestStopNavy.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = if (compact) 4.dp else 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 44.dp else 82.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        text = title,
                        modifier = Modifier.padding(top = if (compact) 4.dp else 5.dp),
                        color = SangsangRestStopNavy,
                        fontSize = if (compact) 23.sp else 31.sp,
                        lineHeight = if (compact) 27.sp else 36.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    description?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(top = 2.dp),
                            color = SangsangRestStopNavy.copy(alpha = 0.74f),
                            fontSize = if (compact) 14.sp else 18.sp,
                            lineHeight = if (compact) 17.sp else 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }

            if (selected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(if (compact) 48.dp else 60.dp),
                    color = accentColor,
                    contentColor = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    shadowElevation = 4.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "✓",
                            fontSize = if (compact) 23.sp else 27.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupFooter(
    onBack: () -> Unit,
    onStart: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    if (compact) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StartButton(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp),
            )
            PreviousButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PreviousButton(
                onClick = onBack,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
            )
            Spacer(modifier = Modifier.weight(1f))
            StartButton(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth(0.54f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun PreviousButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color.White.copy(alpha = 0.96f),
        contentColor = SangsangRestStopNavy,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
        border = BorderStroke(2.dp, Color.White),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.CenterHorizontally,
            ),
        ) {
            RevealWhoChevronIcon(
                modifier = Modifier.size(22.dp),
                color = SangsangRestStopNavy,
            )
            Text(
                text = stringResource(R.string.reveal_who_previous),
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun RevealWhoChevronIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.14f
        val upper = Offset(x = size.width * 0.64f, y = size.height * 0.20f)
        val middle = Offset(x = size.width * 0.36f, y = size.height * 0.50f)
        val lower = Offset(x = size.width * 0.64f, y = size.height * 0.80f)
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
private fun StartButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val startContentDescription = stringResource(R.string.reveal_who_start)
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(44.dp)
    Box(
        modifier = modifier
            .shadow(9.dp, shape)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF2DA4FF), Color(0xFF0A63F6)),
                ),
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = startContentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "▶",
                color = Color.White,
                fontSize = 32.sp,
            )
            Text(
                text = stringResource(R.string.reveal_who_start),
                modifier = Modifier.padding(start = 18.dp),
                color = Color.White,
                fontSize = 31.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private val RevealWhoSky = Color(0xFFBDEAFF)
private val RevealWhoPink = Color(0xFFFF3F88)
private val RevealWhoBlue = Color(0xFF1677F2)

@Preview(
    name = "Reveal Who Setup Pleos",
    widthDp = 1390,
    heightDp = 1000,
    showBackground = true,
)
@Composable
private fun RevealWhoSetupPleosPreview() {
    SangsangRestStopTheme {
        RevealWhoSetupScreen(
            onBack = {},
            onStart = {},
        )
    }
}

@Preview(
    name = "Reveal Who Setup Compact Landscape",
    widthDp = 960,
    heightDp = 600,
    showBackground = true,
)
@Composable
private fun RevealWhoSetupCompactPreview() {
    SangsangRestStopTheme {
        RevealWhoSetupScreen(
            onBack = {},
            onStart = {},
        )
    }
}
