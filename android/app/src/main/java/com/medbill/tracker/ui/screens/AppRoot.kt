package com.medbill.tracker.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medbill.tracker.auth.AuthManager
import com.medbill.tracker.data.model.Bill
import com.medbill.tracker.data.model.BillCreateRequest
import com.medbill.tracker.ui.components.MedBillBottomNav
import com.medbill.tracker.ui.navigation.Route
import com.medbill.tracker.vm.AuthViewModel
import com.medbill.tracker.vm.BillsViewModel
import java.math.BigDecimal

@Composable
fun AppRoot(billsViewModel: BillsViewModel) {
    val context = LocalContext.current
    val authViewModel = remember { AuthViewModel(AuthManager(context)) }
    val authState by authViewModel.state.collectAsState()
    val billsState by billsViewModel.state.collectAsState()
    val navController = rememberNavController()
    val backstack = navController.currentBackStackEntryAsState().value
    val currentRoute = backstack?.destination?.route

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            billsViewModel.refresh()
            navController.navigate(Route.Dashboard.path) {
                popUpTo(Route.SignIn.path) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (authState.isAuthenticated && currentRoute != Route.SignIn.path) {
                MedBillBottomNav(navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (authState.isAuthenticated) Route.Dashboard.path else Route.SignIn.path,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.SignIn.path) {
                SignInScreen(
                    loading = authState.loading,
                    error = authState.error,
                    onSignIn = { authViewModel.signIn(context as Activity) }
                )
            }

            composable(Route.Dashboard.path) {
                DashboardScreen(
                    loading = billsState.loading,
                    totalOutstanding = billsState.dashboard?.totalOutstanding?.toPlainString() ?: "0.00",
                    paidThisYear = billsState.dashboard?.paidThisYear?.toPlainString() ?: "0.00",
                    overdueCount = billsState.dashboard?.overdueCount ?: 0,
                    dueSoon = billsState.dashboard?.dueInNext14Days ?: 0
                )
            }

            composable(Route.Bills.path) {
                BillsScreen(
                    bills = billsState.bills,
                    onSelect = {
                        billsViewModel.selectBill(it.billId)
                        navController.navigate(Route.BillDetail.withBillId(it.billId))
                    }
                )
            }

            composable(Route.AddBill.path) {
                AddBillScreen { request ->
                    billsViewModel.addBill(request)
                    navController.navigate(Route.Bills.path)
                }
            }

            composable(
                route = Route.BillDetail.path,
                arguments = listOf(navArgument("billId") { type = NavType.StringType })
            ) {
                BillDetailScreen(
                    bill = billsState.selectedBill,
                    onPay = { navController.navigate(Route.Payment.path) },
                    onViewDocs = { navController.navigate(Route.Documents.path) }
                )
            }

            composable(Route.Payment.path) {
                PaymentScreen(
                    bill = billsState.selectedBill,
                    redirectUrl = billsState.paymentRedirectUrl,
                    onPay = { amount -> billsViewModel.createPaymentSession(amount) },
                    onOpenPayment = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        billsViewModel.clearPaymentRedirect()
                    }
                )
            }

            composable(Route.Documents.path) {
                DocumentsScreen(billsState.documents)
            }

            composable(Route.Settings.path) {
                SettingsScreen(onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Route.SignIn.path) {
                        popUpTo(Route.Dashboard.path) { inclusive = true }
                    }
                })
            }
        }
    }
}

@Composable
private fun SignInScreen(loading: Boolean, error: String?, onSignIn: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("MedBill Tracker", style = MaterialTheme.typography.headlineMedium)
        Text("Sign in with Microsoft Entra ID to continue", modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        Button(onClick = onSignIn, enabled = !loading) {
            Text(if (loading) "Signing in..." else "Sign in")
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun DashboardScreen(loading: Boolean, totalOutstanding: String, paidThisYear: String, overdueCount: Int, dueSoon: Int) {
    if (loading) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        StatCard("Total Outstanding", "$totalOutstanding")
        StatCard("Paid This Year", "$paidThisYear")
        StatCard("Overdue Bills", overdueCount.toString())
        StatCard("Due in 14 Days", dueSoon.toString())
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun BillsScreen(bills: List<Bill>, onSelect: (Bill) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bills", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(bills) { bill ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onSelect(bill) }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Invoice ${bill.invoiceNumber}", style = MaterialTheme.typography.titleMedium)
                        Text("Due ${bill.dueDate}")
                        Text("Status ${bill.currentStatus}")
                        Text("Patient responsibility ${bill.patientResponsibilityAmount}")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddBillScreen(onSubmit: (BillCreateRequest) -> Unit) {
    var providerId by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var serviceDate by remember { mutableStateOf("2026-01-01") }
    var billDate by remember { mutableStateOf("2026-01-02") }
    var dueDate by remember { mutableStateOf("2026-01-30") }
    var totalBilledAmount by remember { mutableStateOf("0.00") }
    var insurancePaidAmount by remember { mutableStateOf("0.00") }
    var discountAdjustmentAmount by remember { mutableStateOf("0.00") }
    var patientResponsibilityAmount by remember { mutableStateOf("0.00") }
    var notes by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Add Bill", style = MaterialTheme.typography.headlineMedium) }
        item { OutlinedTextField(value = providerId, onValueChange = { providerId = it }, label = { Text("Provider ID") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = invoiceNumber, onValueChange = { invoiceNumber = it }, label = { Text("Invoice Number") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("Account Number") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = serviceDate, onValueChange = { serviceDate = it }, label = { Text("Service Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = billDate, onValueChange = { billDate = it }, label = { Text("Bill Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = totalBilledAmount, onValueChange = { totalBilledAmount = it }, label = { Text("Total Billed") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = insurancePaidAmount, onValueChange = { insurancePaidAmount = it }, label = { Text("Insurance Paid") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = discountAdjustmentAmount, onValueChange = { discountAdjustmentAmount = it }, label = { Text("Adjustments") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = patientResponsibilityAmount, onValueChange = { patientResponsibilityAmount = it }, label = { Text("Patient Responsibility") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Button(onClick = {
                onSubmit(
                    BillCreateRequest(
                        providerId = providerId,
                        invoiceNumber = invoiceNumber,
                        accountNumber = accountNumber.ifBlank { null },
                        serviceDate = serviceDate,
                        billDate = billDate,
                        dueDate = dueDate,
                        totalBilledAmount = totalBilledAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        insurancePaidAmount = insurancePaidAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        discountAdjustmentAmount = discountAdjustmentAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        patientResponsibilityAmount = patientResponsibilityAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        notes = notes.ifBlank { null }
                    )
                )
            }) {
                Text("Save Bill")
            }
        }
    }
}

@Composable
private fun BillDetailScreen(bill: Bill?, onPay: () -> Unit, onViewDocs: () -> Unit) {
    if (bill == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Select a bill from Bills tab.")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Bill Detail", style = MaterialTheme.typography.headlineMedium)
        Text("Invoice ${bill.invoiceNumber}")
        Text("Service date ${bill.serviceDate}")
        Text("Bill date ${bill.billDate}")
        Text("Due date ${bill.dueDate}")
        Text("Status ${bill.currentStatus}")
        Text("Patient responsibility ${bill.patientResponsibilityAmount}")
        Text("Amount paid ${bill.amountPaid}")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onPay) { Text("Pay") }
            Button(onClick = onViewDocs) { Text("Documents") }
        }
    }
}

@Composable
private fun PaymentScreen(bill: Bill?, redirectUrl: String?, onPay: (BigDecimal) -> Unit, onOpenPayment: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Payment", style = MaterialTheme.typography.headlineMedium)
        if (bill == null) {
            Text("No bill selected.")
        } else {
            val due = (bill.patientResponsibilityAmount - bill.amountPaid).coerceAtLeast(BigDecimal.ZERO)
            Text("Invoice ${bill.invoiceNumber}")
            Text("Amount Due $due")
            Button(onClick = { onPay(due) }) { Text("Start Hosted Checkout") }

            if (!redirectUrl.isNullOrBlank()) {
                Text("Payment session ready")
                Button(onClick = { onOpenPayment(redirectUrl) }) { Text("Open Secure Payment Page") }
            }
        }
    }
}

@Composable
private fun DocumentsScreen(documents: List<com.medbill.tracker.data.model.DocumentRecord>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Documents", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(documents) { doc ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(doc.fileName, style = MaterialTheme.typography.titleMedium)
                        Text(doc.contentType)
                        Text("Uploaded ${doc.uploadedAt}")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(onSignOut: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Profile")
        Text("Notifications")
        Text("Export Data")
        Text("Delete Account Request")
        Button(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}
