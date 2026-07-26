package com.medbill.tracker.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medbill.tracker.data.model.Bill
import com.medbill.tracker.data.model.BillCreateRequest
import com.medbill.tracker.data.model.Dashboard
import com.medbill.tracker.data.model.DocumentRecord
import com.medbill.tracker.data.model.Provider
import com.medbill.tracker.data.repository.BillsRepository
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class BillsState(
    val loading: Boolean = false,
    val dashboard: Dashboard? = null,
    val bills: List<Bill> = emptyList(),
    val selectedBill: Bill? = null,
    val providerLookupLoading: Boolean = false,
    val providerMatches: List<Provider> = emptyList(),
    val providerLookupError: String? = null,
    val documents: List<DocumentRecord> = emptyList(),
    val paymentRedirectUrl: String? = null,
    val error: String? = null
)

class BillsViewModel : ViewModel() {
    private val repository = BillsRepository()

    private val _state = MutableStateFlow(BillsState(loading = true))
    val state: StateFlow<BillsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val dashboard = repository.getDashboard()
                val bills = repository.getBills()
                _state.value = _state.value.copy(loading = false, dashboard = dashboard, bills = bills)
            }.onFailure {
                _state.value = BillsState(loading = false, error = "Unable to load billing data")
            }
        }
    }

    fun selectBill(billId: String) {
        viewModelScope.launch {
            runCatching {
                val bill = repository.getBill(billId)
                val docs = repository.getDocuments(billId)
                _state.value = _state.value.copy(selectedBill = bill, documents = docs)
            }.onFailure {
                _state.value = _state.value.copy(error = "Unable to load bill details")
            }
        }
    }

    fun addBill(request: BillCreateRequest, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                repository.createBill(request)
                val dashboard = repository.getDashboard()
                val bills = repository.getBills()
                _state.value = _state.value.copy(loading = false, dashboard = dashboard, bills = bills)
                onResult(true)
            }.onFailure { throwable ->
                _state.value = _state.value.copy(loading = false, error = mapAddBillError(throwable))
                onResult(false)
            }
        }
    }

    private fun mapAddBillError(throwable: Throwable): String {
        if (throwable is HttpException) {
            val code = throwable.code()
            val raw = runCatching { throwable.response()?.errorBody()?.string() }.getOrNull().orEmpty()

            if (code == 400) {
                return "Invalid bill input. Check Provider ID is a valid GUID, invoice is filled, and dates/amounts are valid."
            }

            if (code == 401 || code == 403) {
                return "Session expired or unauthorized. Sign out and sign in again."
            }

            if (raw.isNotBlank()) {
                return "Unable to add bill (HTTP $code): $raw"
            }

            return "Unable to add bill (HTTP $code)."
        }

        return throwable.message ?: "Unable to add bill"
    }

    fun createPaymentSession(amount: BigDecimal) {
        val bill = _state.value.selectedBill ?: return
        viewModelScope.launch {
            runCatching {
                val result = repository.createPaymentSession(bill.billId, amount)
                _state.value = _state.value.copy(paymentRedirectUrl = result.redirectUrl)
            }.onFailure {
                _state.value = _state.value.copy(error = "Unable to create payment session")
            }
        }
    }

    fun lookupProvidersByName(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _state.value = _state.value.copy(providerMatches = emptyList(), providerLookupLoading = false, providerLookupError = null)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(providerLookupLoading = true, providerLookupError = null)
            runCatching {
                val providers = repository.lookupProvidersByName(trimmed)
                _state.value = _state.value.copy(providerMatches = providers, providerLookupLoading = false, providerLookupError = null)
            }.onFailure { throwable ->
                _state.value = _state.value.copy(
                    providerMatches = emptyList(),
                    providerLookupLoading = false,
                    providerLookupError = mapProviderLookupError(throwable)
                )
            }
        }
    }

    fun clearProviderLookup() {
        _state.value = _state.value.copy(providerMatches = emptyList(), providerLookupLoading = false, providerLookupError = null)
    }

    private fun mapProviderLookupError(throwable: Throwable): String {
        if (throwable is HttpException) {
            return when (throwable.code()) {
                401, 403 -> "Provider search unauthorized. Sign out and sign in again."
                404 -> "Provider lookup endpoint is unavailable on current API. Update backend deployment or enter Provider ID manually."
                else -> "Provider lookup failed (HTTP ${throwable.code()})."
            }
        }

        return "Unable to search providers"
    }

    fun clearPaymentRedirect() {
        _state.value = _state.value.copy(paymentRedirectUrl = null)
    }
}
