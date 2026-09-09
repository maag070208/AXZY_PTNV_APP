package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppColors

@Composable
fun StatusChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleSmall,
        color = AppColors.TextMuted,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppColors.EmeraldPrimary)
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = AppColors.Danger,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
                modifier = Modifier.padding(top = 10.dp, bottom = 14.dp),
            )
            if (onRetry != null) {
                OutlinedButton(onClick = onRetry) {
                    Text("Reintentar")
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Inbox,
                contentDescription = null,
                tint = AppColors.TextFaint,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextFaint,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
fun EmptyRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text, color = AppColors.TextFaint, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    color: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    var cardModifier = modifier
        .background(AppColors.Surface, RoundedCornerShape(16.dp))
    if (onClick != null) {
        cardModifier = cardModifier.clickable(onClick = onClick)
    }
    cardModifier = cardModifier.padding(14.dp)

    Row(
        modifier = cardModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        }
    }
}

@Composable
fun SimpleDropdownField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = options.firstOrNull { it.first == value }?.second ?: value
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
                Text(displayLabel, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (optValue, optLabel) ->
                DropdownMenuItem(
                    text = { Text(optLabel) },
                    onClick = {
                        onSelect(optValue)
                        expanded = false
                    },
                )
            }
        }
    }
}
