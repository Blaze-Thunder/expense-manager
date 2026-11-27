package com.saik.expensemanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

enum class GiftCardBrand(val displayName: String) {
    AMAZON_PAY("Amazon Pay"),
    FLIPKART("Flipkart"),
    MYNTRA("Myntra"),
    GOOGLE_PLAY("Google Play"),
    PAYTM("Paytm"),
    OTHER("Other")
}

@Entity(
    tableName = "gift_cards",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["linkedAccountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("linkedAccountId")]
)
data class GiftCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val linkedAccountId: Long,
    val brand: String,
    val customBrandName: String? = null,  // used when brand == "OTHER"
    val code: String,
    val amount: Double,
    val purchaseDate: Long,
    val validityDays: Int,
    val isUsed: Boolean = false,
    val usedDate: Long? = null,
    val note: String? = null,
    val entryTransactionId: Long? = null
)

data class GiftCardData(
    val brand: String,
    val customBrandName: String? = null,
    val code: String,
    val amount: Double,
    val validityDays: Int,
    val note: String? = null
)

fun GiftCard.expiryDate(): Long = purchaseDate + (validityDays * 24L * 60 * 60 * 1000)
fun GiftCard.isExpired(): Boolean = !isUsed && System.currentTimeMillis() > expiryDate()
fun GiftCard.daysUntilExpiry(): Long = (expiryDate() - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
fun GiftCard.displayBrandName(): String {
    if (brand == "OTHER" && !customBrandName.isNullOrBlank()) return customBrandName
    return try {
        GiftCardBrand.valueOf(brand).displayName
    } catch (e: Exception) {
        brand // Fallback to raw string if not a valid enum name
    }
}

fun GiftCardBrand.colorAndIcon(): Pair<Color, ImageVector> = when (this) {
    GiftCardBrand.AMAZON_PAY -> Color(0xFFFF9900) to Icons.Default.ShoppingCart
    GiftCardBrand.FLIPKART -> Color(0xFF2874F0) to Icons.Default.ShoppingBag
    GiftCardBrand.MYNTRA -> Color(0xFFE91E63) to Icons.Default.Checkroom
    GiftCardBrand.GOOGLE_PLAY -> Color(0xFF34A853) to Icons.Default.PlayArrow
    GiftCardBrand.PAYTM -> Color(0xFF00BAF2) to Icons.Default.AccountBalanceWallet
    GiftCardBrand.OTHER -> Color.Gray to Icons.Default.CardGiftcard
}
