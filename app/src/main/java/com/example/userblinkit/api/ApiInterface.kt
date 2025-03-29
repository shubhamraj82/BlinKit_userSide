package com.example.userblinkit.api

import com.example.userblinkit.models.CheckStatus
import retrofit2.http.GET
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiInterface {

    @GET("apis/pg-sandbox/pg/v1/status/{merchantId}/{transactionId}")
    suspend fun checkStatus(
        @HeaderMap headers : Map<String, String>,
        @Path("merchantId") merchantId: String,
        @Path("transactionId") transactionId: String
    ): Response<CheckStatus>


}