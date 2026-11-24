package com.shelfie.app.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.domain.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** State of the app shell around the screens: for now the household badge on the family tab. */
@HiltViewModel
class ShellViewModel @Inject constructor(householdRepository: HouseholdRepository) : ViewModel() {

    /** What the rest of the household did since the user last looked at the feed. */
    val unseenActivity: StateFlow<Int> = householdRepository.unseenActivityCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
