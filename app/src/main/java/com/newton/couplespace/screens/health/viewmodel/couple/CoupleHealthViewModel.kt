package com.newton.couplespace.screens.health.viewmodel.couple

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newton.couplespace.screens.health.data.models.HealthMetric
import com.newton.couplespace.screens.health.data.repository.CoupleHealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * ViewModel responsible for managing partner health data and sharing health metrics
 */
@HiltViewModel
class CoupleHealthViewModel @Inject constructor(
    application: Application,
    private val coupleHealthRepository: CoupleHealthRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "CoupleHealthViewModel"
    }

    // Partner health metrics for the current day
    private val _partnerHealthMetrics = MutableStateFlow<List<HealthMetric>>(emptyList())
    val partnerHealthMetrics: StateFlow<List<HealthMetric>> = _partnerHealthMetrics.asStateFlow()
    
    // Flag to indicate if partner data is available
    private val _hasPartnerData = MutableStateFlow(false)
    val hasPartnerData: StateFlow<Boolean> = _hasPartnerData.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    /**
     * Updates the selected date and loads partner data for that date
     */
    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        loadPartnerHealthData()
    }

    /**
     * Loads partner health data if available
     */
    fun loadPartnerHealthData() {
        viewModelScope.launch {
            try {
                // Check if there's a partner connected in the couple space
                val hasPartner = coupleHealthRepository.hasConnectedPartner()
                if (!hasPartner) {
                    _hasPartnerData.value = false
                    return@launch
                }
                
                Log.d(TAG, "Loading partner health data for ${_selectedDate.value}")
                
                // Get partner metrics from the couple health repository
                coupleHealthRepository.getPartnerHealthMetrics(_selectedDate.value).collect { partnerMetrics ->
                    if (partnerMetrics.isNotEmpty()) {
                        _partnerHealthMetrics.value = partnerMetrics
                        _hasPartnerData.value = true
                        Log.d(TAG, "Successfully loaded ${partnerMetrics.size} partner health metrics")
                    } else {
                        _hasPartnerData.value = false
                        Log.d(TAG, "No partner health metrics available for this date")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading partner health data", e)
                _hasPartnerData.value = false
            }
        }
    }

    /**
     * Shares a health metric with the partner through the couple health repository
     */
    fun shareHealthMetric(metric: HealthMetric) {
        viewModelScope.launch {
            try {
                coupleHealthRepository.shareHealthMetric(metric)
                Log.d(TAG, "Successfully shared health metric: ${metric.type}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sharing health metric", e)
            }
        }
    }

    /**
     * Shares all health metrics with the partner
     */
    fun shareAllHealthMetrics(metrics: List<HealthMetric>) {
        viewModelScope.launch {
            try {
                metrics.forEach { metric ->
                    coupleHealthRepository.shareHealthMetric(metric)
                }
                Log.d(TAG, "Successfully shared ${metrics.size} health metrics with partner")
            } catch (e: Exception) {
                Log.e(TAG, "Error sharing health metrics with partner", e)
            }
        }
    }
}
