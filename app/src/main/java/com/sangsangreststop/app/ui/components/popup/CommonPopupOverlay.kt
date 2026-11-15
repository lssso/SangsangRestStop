package com.sangsangreststop.app.ui.components.popup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PopupNavy = Color(0xFF102A5A)
private val PopupPink = Color(0xFFFF2B67)
private val PopupBlue = Color(0xFF1E73F1)
private val PopupPurple = Color(0xFF7448E8)
private val PopupMint = Color(0xFF00B986)
private val PopupGray = Color(0xFF5D6674)

@Composable
fun CommonPopupOverlay(
    model: CommonPopupUiModel,
    onAction: (CommonPopupAction) -> Unit,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    dismissOnBackdrop: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07162E).copy(alpha = 0.68f)),
    ) {
        if (dismissOnBackdrop) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val compact = maxWidth < 1_150.dp || maxHeight < 760.dp
            val artwork = commonPopupArtwork(model)
            val isStandard = model.type == CommonPopupType.PHONE ||
                model.type == CommonPopupType.NAVIGATION ||
                model.type == CommonPopupType.ERROR
            val widthFraction = if (compact) 0.90f else 0.74f
            val heightFraction = if (isStandard) {
                if (compact) 0.86f else 0.82f
            } else {
                if (compact) 0.82f else 0.74f
            }
            val maxPanelWidth = if (isStandard) 1_180.dp else 1_220.dp
            val panelWidth = minOf(
                maxWidth * widthFraction,
                maxHeight * heightFraction * artwork.cloudAspectRatio,
                maxPanelWidth,
            )
            val panelHeight = panelWidth / artwork.cloudAspectRatio

            PopupPanel(
                model = model,
                artwork = artwork,
                panelWidth = panelWidth,
                panelHeight = panelHeight,
                compact = compact,
                onAction = onAction,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun PopupPanel(
    model: CommonPopupUiModel,
    artwork: CommonPopupArtwork,
    panelWidth: Dp,
    panelHeight: Dp,
    compact: Boolean,
    onAction: (CommonPopupAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isStandard = model.type == CommonPopupType.PHONE ||
        model.type == CommonPopupType.NAVIGATION ||
        model.type == CommonPopupType.ERROR
    val charactersWidthFraction = if (isStandard) 0.38f else 0.34f
    val characterAspectRatio = if (isStandard) 1.5f else 4f / 3f
    val characterTopOffset = if (isStandard) -0.03f else -0.055f
    val contentTopFraction = if (isStandard) 0.32f else 0.40f
    val contentWidthFraction = if (isStandard) 0.68f else 0.66f
    val actionBottomFraction = if (isStandard) 0.080f else 0.090f
    val actionHeight = if (compact) 104.dp else 130.dp

    Box(
        modifier = modifier.size(panelWidth, panelHeight),
    ) {
        Image(
            painter = painterResource(artwork.cloudRes),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(panelWidth, panelHeight),
            contentScale = ContentScale.Fit,
        )

        when (model.type) {
            CommonPopupType.PHONE,
            CommonPopupType.NAVIGATION,
            CommonPopupType.ERROR,
            -> {
                Image(
                    painter = painterResource(artwork.decorationRes),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = panelWidth * 0.075f,
                            y = panelHeight * 0.25f,
                        )
                        .size(panelWidth * 0.16f),
                    contentScale = ContentScale.Fit,
                )
                Image(
                    painter = painterResource(artwork.decorationRes),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = -(panelWidth * 0.055f),
                            y = panelHeight * 0.28f,
                        )
                        .size(panelWidth * 0.10f),
                    contentScale = ContentScale.Fit,
                )
            }

            CommonPopupType.GAME_END -> {
                Image(
                    painter = painterResource(artwork.decorationRes),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(
                            width = panelWidth * 0.94f,
                            height = panelHeight * 0.94f,
                        ),
                    contentScale = ContentScale.Fit,
                )
            }

            CommonPopupType.GAME_PAUSE -> {
                Image(
                    painter = painterResource(artwork.decorationRes),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(
                            x = panelWidth * 0.08f,
                            y = -(panelHeight * 0.03f),
                        )
                        .size(panelWidth * 0.14f),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Image(
            painter = painterResource(artwork.charactersRes),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = panelHeight * characterTopOffset)
                .width(panelWidth * charactersWidthFraction)
                .aspectRatio(characterAspectRatio),
            contentScale = ContentScale.Fit,
        )

        artwork.secondaryDecorationRes?.let { decorationRes ->
            when (model.type) {
                CommonPopupType.GAME_PAUSE -> {
                    Image(
                        painter = painterResource(decorationRes),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(
                                x = panelWidth * 0.13f,
                                y = panelHeight * 0.01f,
                            )
                            .size(panelWidth * 0.095f),
                        contentScale = ContentScale.Fit,
                    )
                }

                else -> {
                    Image(
                        painter = painterResource(decorationRes),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(
                                x = panelWidth * 0.11f,
                                y = panelHeight * 0.14f,
                            )
                            .size(panelWidth * 0.075f),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }

        PopupTextContent(
            model = model,
            artwork = artwork,
            compact = compact,
            panelWidth = panelWidth,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = panelHeight * contentTopFraction)
                .width(panelWidth * contentWidthFraction),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -(panelHeight * actionBottomFraction))
                .width(panelWidth * 0.86f)
                .height(actionHeight),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            model.actions.forEach { button ->
                PopupActionButton(
                    popupType = model.type,
                    model = button,
                    compact = compact,
                    onClick = { onAction(button.action) },
                    modifier = Modifier
                        .weight(1f)
                        .height(actionHeight),
                )
            }
        }
    }
}

@Composable
private fun PopupTextContent(
    model: CommonPopupUiModel,
    artwork: CommonPopupArtwork,
    compact: Boolean,
    panelWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val isStandard = model.type == CommonPopupType.PHONE ||
        model.type == CommonPopupType.NAVIGATION ||
        model.type == CommonPopupType.ERROR
    val titleSize = if (compact) 36.sp else 52.sp
    val descriptionSize = if (isStandard) {
        if (compact) 27.sp else 38.sp
    } else {
        if (compact) 25.sp else 36.sp
    }
    val supportingSize = if (compact) 22.sp else 30.sp
    val reassuranceSize = if (compact) 20.sp else 26.sp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (model.type == CommonPopupType.GAME_END) {
            artwork.mainIconRes?.let { crownRes ->
                Image(
                    painter = painterResource(crownRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 44.dp else 58.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(if (compact) 2.dp else 4.dp))
            }
        }

        Text(
            text = popupTitle(model),
            color = PopupNavy,
            fontSize = titleSize,
            lineHeight = titleSize * 1.12f,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )

        if (isStandard) {
            artwork.mainIconRes?.let { iconRes ->
                Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(
                        width = panelWidth * if (model.type == CommonPopupType.NAVIGATION) 0.20f else 0.13f,
                        height = if (compact) 72.dp else 100.dp,
                    ),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Spacer(
            Modifier.height(
                if (isStandard) {
                    if (compact) 12.dp else 18.dp
                } else {
                    if (compact) 10.dp else 15.dp
                },
            ),
        )
        Text(
            text = model.description,
            color = PopupNavy,
            fontSize = descriptionSize,
            lineHeight = descriptionSize * 1.24f,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )

        model.supportingText?.let { supporting ->
            Spacer(Modifier.height(if (compact) 3.dp else 5.dp))
            Text(
                text = supporting,
                color = PopupBlue,
                fontSize = supportingSize,
                lineHeight = supportingSize * 1.22f,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }

        model.reassurance?.let { reassurance ->
            Spacer(Modifier.height(if (compact) 6.dp else 9.dp))
            Text(
                text = reassurance,
                color = PopupNavy,
                fontSize = reassuranceSize,
                lineHeight = reassuranceSize * 1.18f,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private fun popupTitle(model: CommonPopupUiModel) = buildAnnotatedString {
    append(model.titleStart)
    model.titleAccent?.let { accent ->
        withStyle(SpanStyle(color = PopupPink)) {
            append(accent)
        }
    }
    append(model.titleEnd)
}

@Composable
private fun PopupActionButton(
    popupType: CommonPopupType,
    model: CommonPopupButtonModel,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artwork = commonPopupButtonArtwork(popupType, model)
    val showSupportingText = popupType != CommonPopupType.GAME_END &&
        popupType != CommonPopupType.GAME_PAUSE
    val isStandardStop = model.action == CommonPopupAction.STOP &&
        (popupType == CommonPopupType.PHONE ||
            popupType == CommonPopupType.NAVIGATION ||
            popupType == CommonPopupType.ERROR)
    val iconSize = when {
        isStandardStop -> if (compact) 56.dp else 76.dp
        model.action == CommonPopupAction.STOP -> if (compact) 60.dp else 82.dp
        else -> if (compact) 52.dp else 70.dp
    }
    val contentColor = when (model.tone) {
        CommonPopupButtonTone.MINT -> PopupMint
        CommonPopupButtonTone.BLUE -> PopupBlue
        CommonPopupButtonTone.PINK -> PopupPink
        CommonPopupButtonTone.PURPLE -> PopupPurple
        CommonPopupButtonTone.GRAY -> PopupGray
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(artwork.backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (compact) 8.dp else 14.dp,
                    vertical = if (compact) 7.dp else 10.dp,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isStandardStop) {
                Box(
                    modifier = Modifier.size(if (compact) 52.dp else 70.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(artwork.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        contentScale = ContentScale.Fit,
                    )
                }
            } else {
                Image(
                    painter = painterResource(artwork.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.width(if (compact) 8.dp else 11.dp))
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = model.label,
                    color = contentColor,
                    fontSize = if (compact) 24.sp else 34.sp,
                    lineHeight = if (compact) 28.sp else 39.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
                if (showSupportingText) {
                    model.supportingText?.let { supporting ->
                        Text(
                            text = supporting,
                            color = PopupNavy.copy(alpha = 0.72f),
                            fontSize = if (compact) 15.sp else 22.sp,
                            lineHeight = if (compact) 18.sp else 25.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
