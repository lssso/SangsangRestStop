package com.sangsangreststop.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sangsangreststop.app.R

enum class SangsangRestStopGameCardStyle {
    WordChain,
    TwentyQuestions,
    Imagination,
    ComingSoon,
}

@Composable
fun SangsangRestStopGameCard(
    title: String,
    description: String,
    style: SangsangRestStopGameCardStyle,
    startColor: Color,
    endColor: Color,
    titleColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTwentyQuestionsDisabled =
        (style == SangsangRestStopGameCardStyle.TwentyQuestions) && !enabled
    val outlineModifier = if (style == SangsangRestStopGameCardStyle.ComingSoon) {
        Modifier.drawWithContent {
            drawContent()
            drawRoundRect(
                color = Color(0xFFB7B1D7),
                cornerRadius = CornerRadius(30.dp.toPx()),
                style = Stroke(
                    width = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(12.dp.toPx(), 8.dp.toPx()),
                    ),
                ),
            )
        }
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$title. $description"
                if (!enabled) disabled()
            }
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .alpha(
                when {
                    enabled -> 1f
                    isTwentyQuestionsDisabled -> 1f
                    else -> 0.88f
                },
            )
            .then(outlineModifier),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled) 10.dp else 3.dp,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(startColor, endColor),
                    ),
                ),
        ) {
            val titleFontSize = when {
                maxWidth >= 440.dp -> 34.sp
                maxWidth >= 320.dp -> 30.sp
                else -> 26.sp
            }
            val descriptionFontSize = when {
                maxWidth >= 440.dp -> 20.sp
                maxWidth >= 320.dp -> 18.sp
                else -> 16.sp
            }
            val horizontalTextPadding = if (maxWidth >= 400.dp) 26.dp else 20.dp
            val contentColor = if (isTwentyQuestionsDisabled) {
                Color(0xFF43505A)
            } else {
                titleColor
            }

            GameCardArtwork(
                style = style,
                grayscale = isTwentyQuestionsDisabled,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.86f)
                    .padding(
                        start = horizontalTextPadding,
                        top = 24.dp,
                        bottom = 20.dp,
                    ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = contentColor,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = titleFontSize,
                        lineHeight = (titleFontSize.value + 6f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "›",
                        modifier = Modifier.padding(start = 8.dp),
                        color = contentColor,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = titleFontSize,
                        lineHeight = (titleFontSize.value + 6f).sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Text(
                    text = description,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = descriptionFontSize,
                    lineHeight = (descriptionFontSize.value + 7f).sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                if (style == SangsangRestStopGameCardStyle.ComingSoon) {
                    Text(
                        text = "준비중",
                        color = titleColor.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (isTwentyQuestionsDisabled) {
                TwentyQuestionsComingSoonOverlay(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(
                            top = (maxHeight * 0.24f).coerceIn(76.dp, 148.dp),
                        )
                        .fillMaxWidth(0.60f),
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .size(62.dp),
                color = if (enabled) {
                    titleColor
                } else {
                    Color(0xFF8B9298)
                },
                contentColor = Color.White,
                shape = CircleShape,
                shadowElevation = if (enabled) 5.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (enabled) "→" else "—",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCardArtwork(
    style: SangsangRestStopGameCardStyle,
    grayscale: Boolean,
    modifier: Modifier = Modifier,
) {
    val drawableRes = when (style) {
        SangsangRestStopGameCardStyle.WordChain -> R.drawable.sr_card_wordchain
        SangsangRestStopGameCardStyle.TwentyQuestions -> R.drawable.sr_card_twenty
        SangsangRestStopGameCardStyle.Imagination -> R.drawable.sr_card_imagination
        SangsangRestStopGameCardStyle.ComingSoon -> R.drawable.sr_card_coming_soon
    }

    Image(
        painter = painterResource(drawableRes),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        colorFilter = if (grayscale) {
            ColorFilter.colorMatrix(
                ColorMatrix().apply { setToSaturation(0.08f) },
            )
        } else {
            null
        },
    )
}

@Composable
private fun TwentyQuestionsComingSoonOverlay(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.88f),
        contentColor = Color(0xFF43505A),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 5.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LockIcon(
                modifier = Modifier.size(30.dp),
                color = Color(0xFF59656E),
            )
            Text(
                text = "준비중",
                color = Color(0xFF43505A),
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LockIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.13f
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(
                x = size.width * 0.22f,
                y = size.height * 0.05f,
            ),
            size = androidx.compose.ui.geometry.Size(
                width = size.width * 0.56f,
                height = size.height * 0.58f,
            ),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(
                x = size.width * 0.10f,
                y = size.height * 0.42f,
            ),
            size = androidx.compose.ui.geometry.Size(
                width = size.width * 0.80f,
                height = size.height * 0.52f,
            ),
            cornerRadius = CornerRadius(
                x = size.width * 0.12f,
                y = size.width * 0.12f,
            ),
        )
    }
}
