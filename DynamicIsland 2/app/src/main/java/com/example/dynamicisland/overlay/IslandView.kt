package com.example.dynamicisland.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.events.EventManager
import com.example.dynamicisland.events.IslandEvent

/**
 * The visual Island. This composable is hosted inside a ComposeView added directly
 * to WindowManager by IslandOverlayManager — it does NOT live inside MainActivity.
 */
@Composable
fun IslandView(
    eventManager: EventManager,
    expanded: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val event by eventManager.currentEvent.collectAsState()
    val isExpanded = expanded && event != null

    val width by animateDpAsState(
        targetValue = if (isExpanded) IslandAnimator.EXPANDED_WIDTH_DP.dp else IslandAnimator.COLLAPSED_WIDTH_DP.dp,
        animationSpec = IslandAnimator.springSpec(),
        label = "width"
    )
    val height by animateDpAsState(
        targetValue = if (isExpanded) IslandAnimator.EXPANDED_HEIGHT_DP.dp else IslandAnimator.COLLAPSED_HEIGHT_DP.dp,
        animationSpec = IslandAnimator.springSpec(),
        label = "height"
    )
    val corner by animateDpAsState(
        targetValue = if (isExpanded) IslandAnimator.CORNER_EXPANDED_DP.dp else IslandAnimator.CORNER_COLLAPSED_DP.dp,
        animationSpec = IslandAnimator.springSpec(),
        label = "corner"
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(Color(0xFF0A0A0A), RoundedCornerShape(corner))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(targetState = event, label = "content") { current ->
            when {
                current == null -> CollapsedIdle()
                !isExpanded -> CollapsedPreview(current)
                else -> ExpandedContent(current)
            }
        }
    }
}

@Composable
private fun CollapsedIdle() {
    // Empty pill — nothing happening.
}

@Composable
private fun CollapsedPreview(event: IslandEvent) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val (icon, label) = when (event) {
            is IslandEvent.Call -> Icons.Filled.Call to (if (event.isIncoming) "Incoming" else formatDuration(event.durationSeconds))
            is IslandEvent.Alarm -> Icons.Filled.Notifications to event.label
            is IslandEvent.Timer -> Icons.Filled.Notifications to formatDuration(event.remainingSeconds)
            is IslandEvent.Media -> Icons.Filled.PlayArrow to event.title
            is IslandEvent.Notification -> Icons.Filled.Notifications to event.appName
            is IslandEvent.Charging -> Icons.Filled.Star to "${event.percent}%"
            is IslandEvent.Clear -> Icons.Filled.Star to ""
        }
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(label, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ExpandedContent(event: IslandEvent) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (event) {
            is IslandEvent.Media -> {
                Text(event.title, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(event.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = Color.White)
                    Icon(if (event.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White)
                    Icon(Icons.Filled.SkipNext, null, tint = Color.White)
                }
            }
            is IslandEvent.Call -> {
                Text(event.callerName, color = Color.White, fontSize = 15.sp)
                Text(if (event.isIncoming) "Incoming call" else formatDuration(event.durationSeconds), color = Color.Gray, fontSize = 12.sp)
            }
            is IslandEvent.Timer -> {
                Text(event.label, color = Color.White, fontSize = 15.sp)
                Text(formatDuration(event.remainingSeconds), color = Color.Gray, fontSize = 12.sp)
            }
            is IslandEvent.Alarm -> {
                Text(event.label, color = Color.White, fontSize = 15.sp)
            }
            is IslandEvent.Notification -> {
                Text(event.appName, color = Color.White, fontSize = 13.sp)
                Text(event.title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(event.text, color = Color.Gray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            is IslandEvent.Charging -> {
                Text(if (event.isFull) "Fully Charged" else "Charging", color = Color.White, fontSize = 14.sp)
                Text("${event.percent}%", color = Color.Gray, fontSize = 12.sp)
            }
            is IslandEvent.Clear -> {}
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
