package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MigrationOption
import com.example.ui.MigrationUiState
import com.example.ui.theme.DjezzyRed

@Composable
fun MigrationScreen(
    state: MigrationUiState,
    onLoadOptions: () -> Unit,
    onExecuteMigration: (MigrationOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOptionForConfirm by remember { mutableStateOf<MigrationOption?>(null) }

    LaunchedEffect(Unit) {
        if (state.options.isEmpty()) {
            onLoadOptions()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📶 تحويل نوع الشريحة (Migration)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(onClick = onLoadOptions, enabled = !state.isLoading) {
                    Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = DjezzyRed)
                }
            }

            Text(
                text = "اختر العرض الجديد الذي تريد تحويل خطك إليه",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            state.message?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DjezzyRed)
                }
            } else if (state.options.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد خيارات تحويل شريحة متاحة لخطك حالياً")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.options) { option ->
                        val fromName = option.subscriptionTypeFrom?.name?.ar ?: option.subscriptionTypeFrom?.name?.fr ?: "العرض الحالي"
                        val toName = option.subscriptionTypeTo?.name?.ar ?: option.subscriptionTypeTo?.name?.fr ?: "العرض الجديد"
                        val desc = option.description?.ar ?: option.description?.fr ?: ""

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = DjezzyRed)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "من $fromName ➔ إلى $toName",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                if (desc.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { selectedOptionForConfirm = option },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("migrate_button_${option.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DjezzyRed),
                                    enabled = !state.isExecuting
                                ) {
                                    Text("تحويل الخط إلى هذا العرض", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Confirmation Dialog
        selectedOptionForConfirm?.let { option ->
            val toName = option.subscriptionTypeTo?.name?.ar ?: option.subscriptionTypeTo?.name?.fr ?: "العرض الجديد"
            AlertDialog(
                onDismissRequest = { if (!state.isExecuting) selectedOptionForConfirm = null },
                title = { Text("تأكيد تحويل الخط") },
                text = { Text("هل أنت تأكد من تحويل شريحتك إلى $toName؟ قد يستغرق التطبيق بضع دقائق.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onExecuteMigration(option)
                            selectedOptionForConfirm = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DjezzyRed),
                        enabled = !state.isExecuting
                    ) {
                        Text("تأكيد")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { selectedOptionForConfirm = null },
                        enabled = !state.isExecuting
                    ) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}
