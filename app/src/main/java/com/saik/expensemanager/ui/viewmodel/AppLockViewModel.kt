package com.saik.expensemanager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockViewModel(application: Application) : AndroidViewModel(application) {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked = _isUnlocked.asStateFlow()
    
    private var hasBeenUnlockedInSession = false

    fun unlock() { 
        _isUnlocked.value = true 
        hasBeenUnlockedInSession = true
    }
    
    fun lock() { 
        _isUnlocked.value = false 
        hasBeenUnlockedInSession = false
    }
    
    fun initUnlockState(appLockEnabled: Boolean) {
        if (!appLockEnabled) {
            _isUnlocked.value = true
        } else if (!hasBeenUnlockedInSession) {
            _isUnlocked.value = false
        }
    }
}
