package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DjezzyOffer
import com.example.data.model.PaidOffers
import com.example.ui.theme.DjezzyCoral
import com.example.ui.theme.DjezzyRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    selectedCategory: String,
    searchQuery: String,
    isActivating: Boolean,
    activatingOfferName: String?,
    resultMessage: String?,
    onCategorySelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onActivateOffer: (DjezzyOffer) -> Unit,
    onDismissResultDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOfferForConfirm by remember { mutableStateOf<DjezzyOffer?>(null) }

    val categories = listOf("الكل", "يومي", "أسبوعي", "شهري")

    val filteredOffers = remember(selectedCategory, searchQuery) {
        PaidOffers.list.filter { offer ->
            val matchesCategory = (selectedCategory == "الكل" || offer.category == selectedCategory)
            val matchesQuery = searchQuery.isBlank() ||
                    offer.label.contains(searchQuery, ignoreCase = true) ||
                    offer.amount.contains(searchQuery, ignoreCase = true) ||
                    offer.priceDzd.toString().contains(searchQuery)
            matchesCategory && matchesQuery
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
            Text(
                text = "💰 عروض جيزي المدفوعة (13 عرض)",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_offers_input"),
                placeholder = { Text("بحث عن عرض (مثلاً: 100, 4GB, 190...)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelected(category) },
                        label = { Text(category, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DjezzyRed,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Offers List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredOffers) { offer ->
                    OfferCard(
                        offer = offer,
                        onActivateClick = { selectedOfferForConfirm = offer }
                    )
                }
            }
        }

        // Confirmation Dialog
        selectedOfferForConfirm?.let { offer ->
            AlertDialog(
                onDismissRequest = { if (!isActivating) selectedOfferForConfirm = null },
                title = { Text("تأكيد تفعيل العرض") },
                text = {
                    Column {
                        Text(text = offer.label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "حجم النت: ${offer.amount}")
                        Text(text = "السعر: ${offer.priceDzd} دج")
                        Text(text = "الصلاحية: ${offer.duration}")
                        Text(text = "النوع: ${if (offer.type == "shake") "عرض الشيك (SHAKE)" else "عرض مباشر"}")

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "سيتم خصم ${offer.priceDzd} دج من رصيدك الرئيسي.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onActivateOffer(offer)
                            selectedOfferForConfirm = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DjezzyRed),
                        enabled = !isActivating
                    ) {
                        Text("تأكيد التفعيل")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { selectedOfferForConfirm = null },
                        enabled = !isActivating
                    ) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Activation Loading Overlay
        if (isActivating) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = DjezzyRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "جاري تفعيل ${activatingOfferName ?: "العرض"}...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "يرجى الانتظار، السيرفر يتواصل مع جيزي",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Result Message Dialog
        resultMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = onDismissResultDialog,
                title = { Text("نتيجة التفعيل") },
                text = { Text(msg) },
                confirmButton = {
                    Button(
                        onClick = onDismissResultDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = DjezzyRed)
                    ) {
                        Text("حسناً")
                    }
                }
            )
        }
    }
}

@Composable
fun OfferCard(
    offer: DjezzyOffer,
    onActivateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (offer.type == "shake") DjezzyCoral.copy(alpha = 0.15f) else DjezzyRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (offer.type == "shake") "SHAKE ⚡" else "عرض مباشر",
                        color = if (offer.type == "shake") DjezzyCoral else DjezzyRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${offer.priceDzd} دج",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = offer.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الحجم: ${offer.amount}  •  الصلاحية: ${offer.duration}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onActivateClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DjezzyRed),
                    modifier = Modifier.testTag("activate_offer_${offer.id}")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تفعيل", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
