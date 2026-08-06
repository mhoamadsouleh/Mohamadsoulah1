package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MgmUiState
import com.example.ui.theme.DjezzyCoral
import com.example.ui.theme.DjezzyRed

@Composable
fun MgmScreen(
    state: MgmUiState,
    onFriendPhoneChanged: (String) -> Unit,
    onFriendOtpChanged: (String) -> Unit,
    onSendInvitation: () -> Unit,
    onVerifyFriendOtp: () -> Unit,
    onResetState: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🎁 دعوة صديق (MGM) - 1Go مجاناً",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Explanation Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = DjezzyRed,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "احصل على 1GB مجاناً لكل دعوة!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "ادعُ صديقاً واستخرج رمز SMS لتأكيد التفعيل والحصول على المكافأة المباشرة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (state.isInviteSent) "الخطوة 2: أدخل رمز OTP للمدعو" else "الخطوة 1: أدخل رقم الصديق",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Friend Phone Input
                OutlinedTextField(
                    value = state.friendPhoneInput,
                    onValueChange = onFriendPhoneChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("friend_phone_input"),
                    label = { Text("رقم الصديق المراد دعوته (07xxxxxxxx)") },
                    leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = DjezzyRed) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    enabled = !state.isInviteSent && !state.isLoading,
                    shape = RoundedCornerShape(12.dp)
                )

                state.message?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        fontSize = 13.sp,
                        color = if (msg.contains("نجاح") || msg.contains("تم")) DjezzyRed else MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = !state.isInviteSent) {
                    Button(
                        onClick = onSendInvitation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_mgm_invite_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DjezzyRed),
                        enabled = !state.isLoading && state.friendPhoneInput.length >= 10
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إرسال الدعوة SMS", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                AnimatedVisibility(visible = state.isInviteSent) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.friendOtpInput,
                            onValueChange = onFriendOtpChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("friend_otp_input"),
                            label = { Text("رمز SMS الوارد على رقم الصديق (6 أرقام)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = DjezzyCoral) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onVerifyFriendOtp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("verify_friend_otp_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DjezzyRed),
                            enabled = !state.isLoading && state.friendOtpInput.length == 6
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("تأكيد وتفعيل 1Go مجاناً 🎉", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = onResetState) {
                            Text("إلغاء وإعادة المحاولة")
                        }
                    }
                }
            }
        }
    }
}
