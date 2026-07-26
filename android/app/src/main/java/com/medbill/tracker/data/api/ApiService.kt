package com.medbill.tracker.data.api

import com.medbill.tracker.data.model.Bill
import com.medbill.tracker.data.model.BillCreateRequest
import com.medbill.tracker.data.model.BillUpdateRequest
import com.medbill.tracker.data.model.Dashboard
import com.medbill.tracker.data.model.DocumentRecord
import com.medbill.tracker.data.model.PaymentSessionRequest
import com.medbill.tracker.data.model.PaymentSessionResponse
import com.medbill.tracker.data.model.AuthMe
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

interface ApiService {
    @GET("api/auth/me")
    suspend fun me(): AuthMe

    @GET("api/bills")
    suspend fun getBills(): List<Bill>

    @GET("api/bills/{id}")
    suspend fun getBill(@Path("id") id: String): Bill

    @POST("api/bills")
    suspend fun createBill(@Body request: BillCreateRequest): Bill

    @PUT("api/bills/{id}")
    suspend fun updateBill(@Path("id") id: String, @Body request: BillUpdateRequest): Bill

    @DELETE("api/bills/{id}")
    suspend fun deleteBill(@Path("id") id: String)

    @GET("api/dashboard")
    suspend fun getDashboard(): Dashboard

    @GET("api/bills/{billId}/documents")
    suspend fun getDocuments(@Path("billId") billId: String): List<DocumentRecord>

    @POST("api/bills/{billId}/payments/session")
    suspend fun createPaymentSession(
        @Path("billId") billId: String,
        @Body request: PaymentSessionRequest
    ): PaymentSessionResponse
}
