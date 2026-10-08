package com.icyamire.valut.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icyamire.valut.viewmodel.WizardStep

@Composable
fun WizardProgressBar(
    currentStep: WizardStep,
    modifier: Modifier = Modifier,
) {
    val steps = listOf(
        WizardStep.MEDIA_SELECTION to "1. Media",
        WizardStep.PIN_SETUP to "2. PIN",
        WizardStep.PROCESSING to "3. Process",
        WizardStep.RESULT to "4. Keys",
    )

    val currentStepIndex = currentStep.ordinal
    val progressTarget = (currentStepIndex + 1) / steps.size.toFloat()
    val animatedProgress by animateFloatAsState(targetValue = progressTarget, label = "wizard_progress")

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                steps.forEachIndexed { index, (_, title) ->
                    val isCompleted = index < currentStepIndex
                    val isCurrent = index == currentStepIndex

                    val circleBgColor by animateColorAsState(
                        targetValue = when {
                            isCompleted || isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        label = "circle_bg",
                    )

                    val circleTextColor by animateColorAsState(
                        targetValue = when {
                            isCompleted || isCurrent -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "circle_text",
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(circleBgColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Completed",
                                    tint = circleTextColor,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Text(
                                    text = (index + 1).toString(),
                                    color = circleTextColor,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    ),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = title.substringAfter(". "),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}
