package com.example.data.api

import com.example.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface DjezzyApiService {

    @POST("oauth2/registration")
    suspend fun sendOtp(
        @Query("msisdn") msisdn: String,
        @Query("client_id") clientId: String,
        @Query("scope") scope: String = "smsotp",
        @Body body: OtpRegistrationBody = OtpRegistrationBody()
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun verifyOtp(
        @Field("otp") otp: String,
        @Field("mobileNumber") msisdn: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("scope") scope: String = "djezzyAppV2",
        @Field("grant_type") grantType: String = "mobile"
    ): Response<TokenResponse>

    @POST("api/v1/services/walk/activate-reward/{msisdn}")
    suspend fun activateWalk2Go(
        @Path("msisdn") msisdn: String,
        @Header("Authorization") token: String,
        @Body body: PackageCodeBody = PackageCodeBody("GIFTWALKWIN2GO")
    ): Response<ResponseBody>

    @POST("api/v1/subscribers/activate-product/{msisdn}")
    suspend fun activateProductOffer(
        @Path("msisdn") msisdn: String,
        @Header("Authorization") token: String,
        @Body body: PackageCodeBody
    ): Response<ResponseBody>

    @GET("api/v1/services/shake/{msisdn}")
    suspend fun getShakeOffer(
        @Path("msisdn") msisdn: String,
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @POST("api/v1/services/shake/{msisdn}")
    suspend fun activateShakeOffer(
        @Path("msisdn") msisdn: String,
        @Header("Authorization") token: String,
        @Body body: PackageCodeBody
    ): Response<ResponseBody>

    @POST("api/v1/services/mgm/send-invitation/{sender}")
    suspend fun sendMgmInvitation(
        @Path("sender") senderMsisdn: String,
        @Header("Authorization") token: String,
        @Body body: MgmInvitationBody
    ): Response<ResponseBody>

    @POST("api/v1/services/mgm/activate-reward/{sender}")
    suspend fun activateMgmReward(
        @Path("sender") senderMsisdn: String,
        @Header("Authorization") token: String,
        @Body body: PackageCodeBody = PackageCodeBody("MGMBONUS1Go")
    ): Response<ResponseBody>

    @GET("api/v1/customer-care/migrations/{msisdn}")
    suspend fun getMigrationOptions(
        @Path("msisdn") msisdn: String,
        @Header("Authorization") token: String,
        @Query("application") app: String = "MOBILEAPP"
    ): Response<MigrationResponse>

    @POST("api/v1/customer-care/migrates/{msisdn}")
    suspend fun executeMigration(
        @Path("msisdn") msisdn: String,
        @Header("Authorization") token: String,
        @Body body: MigrationExecuteBody
    ): Response<ResponseBody>

    @GET("api/v1/subscribers/main-balance/{msisdn}")
    suspend fun getMainBalance(
        @Path("msisdn") msisdn: String,
        @Header("Authorization") token: String
    ): Response<BalanceResponse>

    @GET("api/v1/subscribers/connected-products-balances/{msisdn}")
    suspend fun getConnectedProducts(
        @Path("msisdn") msisdn: String,
        @Header("Authorization") token: String
    ): Response<ConnectedProductsResponse>
}
