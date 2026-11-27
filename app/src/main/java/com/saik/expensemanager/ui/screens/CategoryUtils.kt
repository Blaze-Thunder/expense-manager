package com.saik.expensemanager.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.material.icons.automirrored.filled.*

object CategoryUtils {
    val iconGroups = mapOf(
        "Food & Drink" to listOf(
            "Restaurant", "LocalDining", "LocalCafe", "LocalBar", "LocalPizza", "Icecream", "BakeryDining", "LunchDining", "LocalGroceryStore"
        ),
        "Shopping" to listOf(
            "ShoppingCart", "ShoppingBag", "Storefront", "Sell", "LocalMall", "CardGiftcard", "Redeem"
        ),
        "Transport & Travel" to listOf(
            "DirectionsCar", "DirectionsBus", "DirectionsBike", "LocalGasStation", "Flight", "Train", "Commute", "Map", "Tram"
        ),
        "Home & Utilities" to listOf(
            "Home", "Bolt", "WaterDrop", "Wifi", "Phone", "EvStation", "CleaningServices", "Kitchen", "Bed", "Router", "Construction"
        ),
        "Finance" to listOf(
            "Paid", "Payments", "Savings", "AccountBalance", "AccountBalanceWallet", "MonetizationOn", "CurrencyExchange", "ShowChart", "Calculate", "ReceiptLong"
        ),
        "Entertainment" to listOf(
            "Movie", "TheaterComedy", "MusicNote", "SportsEsports", "CameraAlt", "Brush", "Palette", "Subscriptions", "Stadium"
        ),
        "Health & Fitness" to listOf(
            "Favorite", "MedicalServices", "HealthAndSafety", "FitnessCenter", "SelfImprovement", "SportsSoccer", "LocalPharmacy"
        ),
        "Work & Education" to listOf(
            "Work", "School", "Book", "Computer", "Laptop", "Analytics", "Assignment"
        ),
        "Others" to listOf(
            "Label", "Star", "Info", "Help", "Public", "Pets", "ChildCare", "Policy", "Flag", "Cake"
        )
    )

    val iconList = iconGroups.values.flatten().distinct()

    fun getCategoryIcon(name: String): ImageVector {
        return when (name) {
            "Restaurant" -> Icons.Default.Restaurant
            "LocalDining" -> Icons.Default.LocalDining
            "LocalCafe" -> Icons.Default.LocalCafe
            "LocalBar" -> Icons.Default.LocalBar
            "LocalPizza" -> Icons.Default.LocalPizza
            "Icecream" -> Icons.Default.Icecream
            "BakeryDining" -> Icons.Default.BakeryDining
            "LunchDining" -> Icons.Default.LunchDining
            "LocalGroceryStore" -> Icons.Default.LocalGroceryStore
            
            "ShoppingCart" -> Icons.Default.ShoppingCart
            "ShoppingBag" -> Icons.Default.ShoppingBag
            "Storefront" -> Icons.Default.Storefront
            "Sell" -> Icons.Default.Sell
            "LocalMall" -> Icons.Default.LocalMall
            "CardGiftcard" -> Icons.Default.CardGiftcard
            "Redeem" -> Icons.Default.Redeem
            
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "DirectionsBus" -> Icons.Default.DirectionsBus
            "DirectionsBike" -> Icons.Default.DirectionsBike
            "LocalGasStation" -> Icons.Default.LocalGasStation
            "Flight" -> Icons.Default.Flight
            "Train" -> Icons.Default.Train
            "Commute" -> Icons.Default.Commute
            "Map" -> Icons.Default.Map
            "Tram" -> Icons.Default.Tram
            
            "Home" -> Icons.Default.Home
            "Bolt" -> Icons.Default.Bolt
            "WaterDrop" -> Icons.Default.WaterDrop
            "Wifi" -> Icons.Default.Wifi
            "Phone" -> Icons.Default.Phone
            "EvStation" -> Icons.Default.EvStation
            "CleaningServices" -> Icons.Default.CleaningServices
            "Kitchen" -> Icons.Default.Kitchen
            "Bed" -> Icons.Default.Bed
            "Router" -> Icons.Default.Router
            "Construction" -> Icons.Default.Construction
            
            "Paid" -> Icons.Default.Paid
            "Payments" -> Icons.Default.Payments
            "Savings" -> Icons.Default.Savings
            "AccountBalance" -> Icons.Default.AccountBalance
            "AccountBalanceWallet" -> Icons.Default.AccountBalanceWallet
            "MonetizationOn" -> Icons.Default.MonetizationOn
            "CurrencyExchange" -> Icons.Default.CurrencyExchange
            "ShowChart" -> Icons.AutoMirrored.Filled.ShowChart
            "Calculate" -> Icons.Default.Calculate
            "ReceiptLong" -> Icons.AutoMirrored.Filled.ReceiptLong
            
            "Movie" -> Icons.Default.Movie
            "TheaterComedy" -> Icons.Default.TheaterComedy
            "MusicNote" -> Icons.Default.MusicNote
            "SportsEsports" -> Icons.Default.SportsEsports
            "CameraAlt" -> Icons.Default.CameraAlt
            "Brush" -> Icons.Default.Brush
            "Palette" -> Icons.Default.Palette
            "Subscriptions" -> Icons.Default.Subscriptions
            "Stadium" -> Icons.Default.Stadium
            
            "Favorite" -> Icons.Default.Favorite
            "MedicalServices" -> Icons.Default.MedicalServices
            "HealthAndSafety" -> Icons.Default.HealthAndSafety
            "FitnessCenter" -> Icons.Default.FitnessCenter
            "SelfImprovement" -> Icons.Default.SelfImprovement
            "SportsSoccer" -> Icons.Default.SportsSoccer
            "LocalPharmacy" -> Icons.Default.LocalPharmacy
            
            "Work" -> Icons.Default.Work
            "School" -> Icons.Default.School
            "Book" -> Icons.Default.Book
            "Computer" -> Icons.Default.Computer
            "Laptop" -> Icons.Default.Laptop
            "Analytics" -> Icons.Default.Analytics
            "Assignment" -> Icons.AutoMirrored.Filled.Assignment
            
            "Star" -> Icons.Default.Star
            "Info" -> Icons.Default.Info
            "Help" -> Icons.Default.Help
            "Public" -> Icons.Default.Public
            "Pets" -> Icons.Default.Pets
            "ChildCare" -> Icons.Default.ChildCare
            "Policy" -> Icons.Default.Policy
            "Flag" -> Icons.Default.Flag
            "Cake" -> Icons.Default.Cake
            
            else -> Icons.Default.Label
        }
    }

    val emojiList = listOf(
        "🍔", "🍕", "🍦", "🍺", "☕", "🍎", "🥦", "🍣",
        "🛒", "🛍️", "🎁", "💄", "👕", "👟", "📱", "💻",
        "🚗", "🚲", "⛽", "✈️", "🚂", "🚕", "🚌", "🚤",
        "🏠", "⚡", "💧", "📶", "📞", "🧹", "🛌", "🛋️",
        "💰", "💳", "🏦", "📈", "💵", "💸", "🧧", "🏧",
        "🎬", "🎮", "🎵", "🎨", "🎭", "🎪", "🎤", "🎧",
        "❤️", "💊", "🏥", "💪", "🧘", "⚽", "🏀", "🎾",
        "💼", "🎓", "📚", "🖊️", "📅", "📐", "🔬", "💻",
        "🐶", "🐱", "👶", "🌟", "🔥", "🌈", "🎈", "🔒"
    )
}
