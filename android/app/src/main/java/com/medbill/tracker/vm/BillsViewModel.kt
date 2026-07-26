package com.medbill.tracker.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medbill.tracker.data.model.Bill
import com.medbill.tracker.data.model.BillCreateRequest
import com.medbill.tracker.data.model.Dashboard
import com.medbill.tracker.data.model.DocumentRecord
import com.medbill.tracker.data.repository.BillsRepository
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BillsState(
    val loading: Boolean = false,
    val dashboard: Dashboard? = null,
    val bills: List<Bill> = emptyList(),
    val selectedBill: Bill? = null,
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

    fun addBill(request: BillCreateRequest) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching {
                repository.createBill(request)
                refresh()
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = "Unable to add bill")
            }
        }
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

    fun clearPaymentRedirect() {
        _state.value = _state.value.copy(paymentRedirectUrl = null)
    }
}
