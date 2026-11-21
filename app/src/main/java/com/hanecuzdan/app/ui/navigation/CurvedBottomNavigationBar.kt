package com.hanecuzdan.app.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun CurvedBottomNavigationBar(
    items: List<AppDestination>,
    currentDestination: AppDestination,
    onItemSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = items.indexOf(currentDestination)
    val hasSelection = selectedIndex >= 0
    val animatedIndex by animateFloatAsState(
        targetValue = if (hasSelection) selectedIndex.toFloat() else 0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "curvedNavIndex"
    )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val barColor = if (isDark) Color(0xFF1E1F22) else MaterialTheme.colorScheme.surfaceContainerHigh
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = if (isDark) Color(0xFFE2E2E6) else MaterialTheme.colorScheme.onSurfaceVariant

    val barHeight = 64.dp
    val circleSize = 50.dp
    val topOverhang = 22.dp
    val totalHeight = barHeight + topOverhang

    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .height(totalHeight)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val topOffsetPx = with(density) { topOverhang.toPx() }
        val totalHeightPx = with(density) { totalHeight.toPx() }
        val topCornerPx = with(density) { 22.dp.toPx() }
        val bottomCornerPx = with(density) { 14.dp.toPx() }
        val notchDepthPx = with(density) { 26.dp.toPx() }
        val circleSizePx = with(density) { circleSize.toPx() }
        val slotWidthPx = if (items.isNotEmpty()) widthPx / items.size else widthPx
        val notchWidthPx = minOf(with(density) { 92.dp.toPx() }, slotWidthPx * 1.15f)
        val centerX = (animatedIndex + 0.5f) * slotWidthPx

        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(0f, topOffsetPx + topCornerPx)
                quadraticTo(0f, topOffsetPx, topCornerPx, topOffsetPx)

                if (hasSelection && items.isNotEmpty()) {
                    val notchStart = (centerX - notchWidthPx / 2f).coerceAtLeast(topCornerPx)
                    val notchEnd = (centerX + notchWidthPx / 2f).coerceAtMost(size.width - topCornerPx)

                    lineTo(notchStart, topOffsetPx)
                    cubicTo(
                        notchStart + notchWidthPx * 0.24f, topOffsetPx,
                        centerX - notchWidthPx * 0.26f, topOffsetPx + notchDepthPx,
                        centerX, topOffsetPx + notchDepthPx
                    )
                    cubicTo(
                        centerX + notchWidthPx * 0.26f, topOffsetPx + notchDepthPx,
                        notchEnd - notchWidthPx * 0.24f, topOffsetPx,
                        notchEnd, topOffsetPx
                    )
                }

                lineTo(size.width - topCornerPx, topOffsetPx)
                quadraticTo(size.width, topOffsetPx, size.width, topOffsetPx + topCornerPx)
                lineTo(size.width, totalHeightPx - bottomCornerPx)
                quadraticTo(size.width, totalHeightPx, size.width - bottomCornerPx, totalHeightPx)
                lineTo(bottomCornerPx, totalHeightPx)
                quadraticTo(0f, totalHeightPx, 0f, totalHeightPx - bottomCornerPx)
                close()
            }
            drawPath(path = path, color = barColor)
        }

        if (hasSelection && items.isNotEmpty()) {
            val circleX = (centerX - circleSizePx / 2f).roundToInt()
            val circleY = with(density) { 2.dp.toPx() }.roundToInt()
            Box(
                modifier = Modifier
                    .offset { IntOffset(circleX, circleY) }
                    .size(circleSize)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(barColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = items[selectedIndex].icon,
                    contentDescription = items[selectedIndex].label,
                    tint = activeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemSelected(item) }
                        )
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (!isSelected) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = inactiveColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = item.label,
                        color = if (isSelected) activeColor else inactiveColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
