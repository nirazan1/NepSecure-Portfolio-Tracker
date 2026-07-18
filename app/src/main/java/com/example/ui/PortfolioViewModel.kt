package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.CurrentHolding
import com.example.data.database.PortfolioDatabase
import com.example.data.database.PortfolioHistory
import com.example.data.database.StockItem
import com.example.data.database.WatchStock
import com.example.data.repository.PortfolioRepository
import com.example.data.api.MarketCandle
import com.example.data.api.MarketDataClient
import com.example.data.api.MarketNewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SyncState {
    object Idle : SyncState
    object Syncing : SyncState
    object Success : SyncState
    data class Error(val message: String) : SyncState
}

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PortfolioDatabase.getDatabase(application)
    private val repository = PortfolioRepository(application, database.portfolioDao())

    val currentHoldings: StateFlow<List<CurrentHolding>> = repository.currentHoldings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val stockList: StateFlow<List<StockItem>> = repository.stockList
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val portfolioHistory: StateFlow<List<PortfolioHistory>> = repository.portfolioHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val watchList: StateFlow<List<WatchStock>> = repository.watchList
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _spreadsheetId = MutableStateFlow(repository.getSpreadsheetId())
    val spreadsheetId: StateFlow<String> = _spreadsheetId.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(repository.getLastSyncTime())
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _googleAccountEmail = MutableStateFlow(repository.getGoogleAccountEmail())
    val googleAccountEmail: StateFlow<String?> = _googleAccountEmail.asStateFlow()

    private val _e5Value = MutableStateFlow<Double?>(repository.getE5Value())
    val e5Value: StateFlow<Double?> = _e5Value.asStateFlow()

    private val _o5Value = MutableStateFlow<Double?>(repository.getO5Value())
    val o5Value: StateFlow<Double?> = _o5Value.asStateFlow()

    fun updateGoogleAccount(email: String?) {
        _googleAccountEmail.value = email
        repository.saveGoogleAccountEmail(email)
    }

    fun signOutGoogle() {
        _googleAccountEmail.value = null
        repository.saveGoogleAccountEmail(null)
    }

    private val _nepseIndexValue = MutableStateFlow<Double?>(null)
    val nepseIndexValue: StateFlow<Double?> = _nepseIndexValue.asStateFlow()

    private val _nepseIndexChange = MutableStateFlow<Double?>(null)
    val nepseIndexChange: StateFlow<Double?> = _nepseIndexChange.asStateFlow()

    private val _nepseIndexPercent = MutableStateFlow<Double?>(null)
    val nepseIndexPercent: StateFlow<Double?> = _nepseIndexPercent.asStateFlow()

    private val _isNepseLoading = MutableStateFlow(false)
    val isNepseLoading: StateFlow<Boolean> = _isNepseLoading.asStateFlow()

    private val _newsList = MutableStateFlow<List<MarketNewsItem>>(emptyList())
    val newsList: StateFlow<List<MarketNewsItem>> = _newsList.asStateFlow()

    private val _isNewsLoading = MutableStateFlow(false)
    val isNewsLoading: StateFlow<Boolean> = _isNewsLoading.asStateFlow()

    private val _newsError = MutableStateFlow<String?>(null)
    val newsError: StateFlow<String?> = _newsError.asStateFlow()

    init {
        viewModelScope.launch {
            if (repository.isDatabaseEmpty()) {
                Log.d("ViewModel", "Database is empty, loading demo data")
                repository.loadDemoData()
                _lastSyncTime.value = repository.getLastSyncTime()
            }
            _e5Value.value = repository.getE5Value()
            _o5Value.value = repository.getO5Value()
            fetchNepseIndex()
            fetchNews()
        }
    }

    fun fetchNepseIndex() {
        viewModelScope.launch {
            _isNepseLoading.value = true
            try {
                val data = withContext(Dispatchers.IO) {
                    MarketDataClient.fetchCandles("NEPSE")
                }
                if (data.size >= 2) {
                    val latest = data.last()
                    val prev = data[data.size - 2]
                    _nepseIndexValue.value = latest.close
                    val diff = latest.close - prev.close
                    _nepseIndexChange.value = diff
                    _nepseIndexPercent.value = (diff / prev.close) * 100.0
                } else if (data.isNotEmpty()) {
                    _nepseIndexValue.value = data.last().close
                    _nepseIndexChange.value = 0.0
                    _nepseIndexPercent.value = 0.0
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to fetch NEPSE index", e)
            } finally {
                _isNepseLoading.value = false
            }
        }
    }

    fun fetchNews() {
        viewModelScope.launch {
            _isNewsLoading.value = true
            _newsError.value = null
            try {
                val news = withContext(Dispatchers.IO) {
                    MarketDataClient.fetchNews(page = 1, size = 30)
                }
                _newsList.value = news
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to fetch news", e)
                _newsError.value = e.message ?: "Failed to fetch news."
            } finally {
                _isNewsLoading.value = false
            }
        }
    }

    fun updateSpreadsheetId(id: String) {
        _spreadsheetId.value = id
        repository.saveSpreadsheetId(id)
    }

    fun refreshFromSheets() {
        val id = _spreadsheetId.value
        if (id.isBlank()) {
            _syncState.value = SyncState.Error("Google Spreadsheet ID cannot be blank.")
            return
        }

        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                val success = repository.refreshFromGoogleSheets()
                if (success) {
                    _syncState.value = SyncState.Success
                    _lastSyncTime.value = repository.getLastSyncTime()
                    _e5Value.value = repository.getE5Value()
                    _o5Value.value = repository.getO5Value()
                    fetchNepseIndex()
                    fetchNews()
                } else {
                    _syncState.value = SyncState.Error("Parsed spreadsheet but found no records. Please check sheet names match exactly: 'Current', 'Stocks', 'PortfolioHistory', 'Watch List'.")
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Sync failed", e)
                _syncState.value = SyncState.Error(e.message ?: "Unknown sync error. Verify sheet sharing and connection.")
            }
        }
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }

    fun loadDemoData() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                repository.loadDemoData()
                _lastSyncTime.value = repository.getLastSyncTime()
                _e5Value.value = repository.getE5Value()
                _o5Value.value = repository.getO5Value()
                fetchNepseIndex()
                fetchNews()
                _syncState.value = SyncState.Success
            } catch (e: Exception) {
                _syncState.value = SyncState.Error("Failed to load demo data: ${e.message}")
            }
        }
    }

    private val _selectedSymbol = MutableStateFlow<String?>(null)
    val selectedSymbol: StateFlow<String?> = _selectedSymbol.asStateFlow()

    private val _candleData = MutableStateFlow<List<MarketCandle>>(emptyList())
    val candleData: StateFlow<List<MarketCandle>> = _candleData.asStateFlow()

    private val _isCandleLoading = MutableStateFlow(false)
    val isCandleLoading: StateFlow<Boolean> = _isCandleLoading.asStateFlow()

    private val _candleError = MutableStateFlow<String?>(null)
    val candleError: StateFlow<String?> = _candleError.asStateFlow()

    fun selectSymbol(symbol: String?) {
        _selectedSymbol.value = symbol
        if (symbol == null) {
            _candleData.value = emptyList()
            _candleError.value = null
            return
        }

        viewModelScope.launch {
            _isCandleLoading.value = true
            _candleError.value = null
            try {
                val data = withContext(Dispatchers.IO) {
                    MarketDataClient.fetchCandles(symbol)
                }
                if (data.isNotEmpty()) {
                    _candleData.value = data
                } else {
                    _candleError.value = "No historical candle data found for $symbol."
                }
            } catch (e: Exception) {
                _candleError.value = "Failed to load data: ${e.message}"
            } finally {
                _isCandleLoading.value = false
            }
        }
    }

}
