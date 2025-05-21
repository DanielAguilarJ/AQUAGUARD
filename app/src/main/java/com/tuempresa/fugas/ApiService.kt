package com.tuempresa.fugas.network

import com.tuempresa.fugas.model.SensorData
import com.tuempresa.fugas.model.AlertData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded

interface ApiService {
    @GET("datos/ultimos")
    suspend fun getUltimosDatos(): List<SensorData>

    @POST("alertas")
    suspend fun sendAlert(@Body alert: AlertData): Response<Unit>

    @GET("alertas/historial")
    suspend fun getAlertHistory(): List<AlertData>

    @FormUrlEncoded
    @PATCH("alertas/{timestamp}/status")
    suspend fun updateAlertStatus(
        @Path("timestamp") timestamp: String,
        @Field("revisada") revisada: Boolean? = null,
        @Field("eliminada") eliminada: Boolean? = null
    ): Response<Unit>

    // Vinculación de dispositivo al usuario/app
    @POST("device/link")
    suspend fun linkDevice(
        @Body request: com.tuempresa.fugas.model.LinkDeviceRequest
    ): Response<Unit>
}
