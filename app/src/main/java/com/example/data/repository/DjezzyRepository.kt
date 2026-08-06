package com.example.data.repository

import android.content.Context
import com.example.data.api.DjezzyApiService
import com.example.data.db.*
import com.example.data.model.*
import com.example.utils.NotificationHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
    val isTokenExpired: Boolean = false
) {
    class Success<T>(data: T, message: String? = null) : Resource<T>(data, message)
    class Error<T>(message: String, data: T? = null, isTokenExpired: Boolean = false) : Resource<T>(data, message, isTokenExpired)
    class Loading<T>(data: T? = null, val progressMessage: String? = null) : Resource<T>(data)
}

class DjezzyRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val sessionDao = database.sessionDao()
    private val activationDao = database.activationDao()
    private val notificationHelper = NotificationHelper(context)

    companion object {
        const val CLIENT_ID = "87pIExRhxBb3_wGsA5eSEfyATloa"
        const val CLIENT_SECRET = "uf82p68Bgisp8Yg1Uz8Pf6_v1XYa"
        const val BASE_URL = "https://apim.djezzy.dz/mobile-api/"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "MobileApp/3.0.7")
                .header("Accept", "application/json")
                .header("Accept-Encoding", "gzip")
                .header("Content-Type", "application/json")
                .header("accept-language", "fr")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val apiService: DjezzyApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(DjezzyApiService::class.java)

    val currentSession: Flow<UserSession?> = sessionDao.getCurrentSession()
    val allActivations: Flow<List<ActivationRecord>> = activationDao.getAllActivations()
    val successCount: Flow<Int> = activationDao.getSuccessCount()

    fun cleanPhoneNumber(input: String): String? {
        val cleaned = input.replace(Regex("[^0-9]"), "")
        val phone = when {
            cleaned.startsWith("213") -> "0" + cleaned.substring(3)
            cleaned.startsWith("0") -> cleaned
            else -> "0$cleaned"
        }
        return if (phone.matches(Regex("^0[567][0-9]{8}$"))) phone else null
    }

    fun phoneToMsisdn(phone: String): String {
        return if (phone.startsWith("0")) "213" + phone.substring(1) else phone
    }

    suspend fun saveSession(phone: String, msisdn: String, token: String) {
        val session = UserSession(
            phone = phone,
            msisdn = msisdn,
            accessToken = token,
            loginTimestamp = System.currentTimeMillis(),
            isCurrent = true
        )
        sessionDao.setCurrentSession(session)
    }

    suspend fun logout() {
        sessionDao.clearCurrentSessionFlag()
    }

    suspend fun clearHistory() {
        activationDao.clearHistory()
    }

    // --- OTP Login ---

    suspend fun sendOtp(phone: String): Resource<String> {
        val msisdn = phoneToMsisdn(phone)
        var attempts = 0
        while (attempts < 5) {
            attempts++
            try {
                val response = apiService.sendOtp(msisdn = msisdn, clientId = CLIENT_ID)
                if (response.isSuccessful || response.code() in listOf(200, 201)) {
                    return Resource.Success("تم إرسال رمز التحقق بنجاح إلى $phone")
                } else if (response.code() == 429) {
                    delay(300)
                    continue
                } else {
                    return Resource.Error("خطأ من السيرفر (${response.code()})")
                }
            } catch (e: Exception) {
                delay(300)
            }
        }
        return Resource.Error("فشل الاتصال بعد عدة محاولات، يرجى المحاولة لاحقاً")
    }

    suspend fun verifyOtp(phone: String, otp: String): Resource<UserSession> {
        val msisdn = phoneToMsisdn(phone)
        var attempts = 0
        while (attempts < 5) {
            attempts++
            try {
                val response = apiService.verifyOtp(
                    otp = otp,
                    msisdn = msisdn,
                    clientId = CLIENT_ID,
                    clientSecret = CLIENT_SECRET
                )
                if (response.isSuccessful) {
                    val token = response.body()?.accessToken
                    if (!token.isNullOrEmpty()) {
                        val session = UserSession(
                            phone = phone,
                            msisdn = msisdn,
                            accessToken = token
                        )
                        sessionDao.setCurrentSession(session)
                        return Resource.Success(session, "تم تسجيل الدخول بنجاح")
                    }
                } else if (response.code() == 400) {
                    return Resource.Error("رمز التحقق غير صحيح، أعد إدخاله")
                } else if (response.code() == 429) {
                    delay(300)
                    continue
                }
            } catch (e: Exception) {
                delay(300)
            }
        }
        return Resource.Error("فشل التحقق من الرمز، أعد المحاولة")
    }

    // --- 2Go Free Walk ---

    suspend fun activateWalk2Go(session: UserSession): Resource<String> {
        val bearerToken = "Bearer ${session.accessToken}"
        try {
            val response = apiService.activateWalk2Go(session.msisdn, bearerToken)
            val code = response.code()

            if (code in listOf(200, 201, 202)) {
                val msg = "تم تفعيل 2Go أسبوعية بنجاح 🥳💜"
                recordActivation(session, "2Go Walk مجاني", "GIFTWALKWIN2GO", "2go_walk", true, msg)
                notificationHelper.showActivationNotification("تفعيل 2Go مجاني", msg, true)
                return Resource.Success(msg)
            }

            if (code == 401) {
                return Resource.Error("انتهت صلاحية الجلسة، يرجى تسجيل الدخول مجدداً", isTokenExpired = true)
            }

            val errorText = response.errorBody()?.string() ?: ""
            val isRuleMsg = code == 403
            val detailMsg = if (isRuleMsg) {
                "غير متاح حالياً. تذكير بقانون 2Go: شحن 100 دج شهرياً وتفعيل عرض 2Go بـ 100 دج للاستفادة أسبوعياً."
            } else {
                "لم يكتمل الأسبوع بعد للاستفادة من 2Go مجاناً."
            }

            recordActivation(session, "2Go Walk مجاني", "GIFTWALKWIN2GO", "2go_walk", false, detailMsg)
            notificationHelper.showActivationNotification("2Go Walk مجاني", detailMsg, false)
            return Resource.Error(detailMsg)

        } catch (e: Exception) {
            val msg = "خطأ في الاتصال: ${e.localizedMessage}"
            recordActivation(session, "2Go Walk مجاني", "GIFTWALKWIN2GO", "2go_walk", false, msg)
            return Resource.Error(msg)
        }
    }

    // --- Paid Offers (13 Offers) ---

    suspend fun activatePaidOffer(session: UserSession, offer: DjezzyOffer): Resource<String> {
        val bearerToken = "Bearer ${session.accessToken}"
        val offerName = offer.name
        val offerCode = offer.code

        try {
            if (offer.type == "shake") {
                // Step 1: SHAKE GET
                var getSuccess = false
                var attemptsGet = 0
                while (attemptsGet < 10 && !getSuccess) {
                    attemptsGet++
                    val getResp = apiService.getShakeOffer(session.msisdn, bearerToken)
                    if (getResp.code() == 401) {
                        return Resource.Error("انتهت صلاحية الجلسة", isTokenExpired = true)
                    }
                    if (getResp.isSuccessful) {
                        getSuccess = true
                    } else {
                        delay(200)
                    }
                }

                // Step 2: SHAKE POST
                var attemptsPost = 0
                while (attemptsPost < 10) {
                    attemptsPost++
                    val postResp = apiService.activateShakeOffer(
                        session.msisdn,
                        bearerToken,
                        PackageCodeBody(offerCode)
                    )
                    val code = postResp.code()

                    if (code in listOf(200, 201, 202)) {
                        val msg = "تم تفعيل $offerName بنجاح! 🥳"
                        recordActivation(session, offerName, offerCode, "paid_offer", true, msg)
                        notificationHelper.showActivationNotification("تفعيل $offerName", msg, true)
                        return Resource.Success(msg)
                    } else if (code == 402) {
                        val msg = "رصيدك غير كافي لتفعيل $offerName (المطلوب ${offer.priceDzd} دج)"
                        recordActivation(session, offerName, offerCode, "paid_offer", false, msg)
                        notificationHelper.showActivationNotification("تفعيل $offerName", msg, false)
                        return Resource.Error(msg)
                    } else if (code == 401) {
                        return Resource.Error("انتهت صلاحية الجلسة", isTokenExpired = true)
                    } else {
                        delay(200)
                    }
                }
                val msg = "فشل تفعيل العرض بعد عدة محاولات، يرجى إعادة المحاولة"
                recordActivation(session, offerName, offerCode, "paid_offer", false, msg)
                return Resource.Error(msg)

            } else {
                // Product Activation
                var attempts = 0
                while (attempts < 10) {
                    attempts++
                    val resp = apiService.activateProductOffer(
                        session.msisdn,
                        bearerToken,
                        PackageCodeBody(offerCode)
                    )
                    val code = resp.code()

                    if (code in listOf(200, 201, 202)) {
                        val msg = "تم تفعيل $offerName بنجاح! 🥳"
                        recordActivation(session, offerName, offerCode, "paid_offer", true, msg)
                        notificationHelper.showActivationNotification("تفعيل $offerName", msg, true)
                        return Resource.Success(msg)
                    } else if (code == 402) {
                        val msg = "رصيدك غير كافي لتفعيل $offerName (المطلوب ${offer.priceDzd} دج)"
                        recordActivation(session, offerName, offerCode, "paid_offer", false, msg)
                        notificationHelper.showActivationNotification("تفعيل $offerName", msg, false)
                        return Resource.Error(msg)
                    } else if (code == 401) {
                        return Resource.Error("انتهت صلاحية الجلسة", isTokenExpired = true)
                    } else {
                        delay(200)
                    }
                }
                val msg = "فشل تفعيل العرض بعد عدة محاولات"
                recordActivation(session, offerName, offerCode, "paid_offer", false, msg)
                return Resource.Error(msg)
            }
        } catch (e: Exception) {
            val msg = "خطأ في الاتصال: ${e.localizedMessage}"
            recordActivation(session, offerName, offerCode, "paid_offer", false, msg)
            return Resource.Error(msg)
        }
    }

    // --- MGM Invitation ---

    suspend fun sendMgmInvite(session: UserSession, receiverPhone: String): Resource<String> {
        val bearerToken = "Bearer ${session.accessToken}"
        val receiverMsisdn = phoneToMsisdn(receiverPhone)
        val receiverLong = receiverMsisdn.toLongOrNull() ?: return Resource.Error("رقم غير صحيح")

        try {
            val response = apiService.sendMgmInvitation(
                session.msisdn,
                bearerToken,
                MgmInvitationBody(receiverLong)
            )
            val code = response.code()

            if (code in listOf(200, 201)) {
                val msg = "تم إرسال الدعوة بنجاح إلى $receiverPhone! اطلب منه إرسال الرمز للتحقق والتفعيل"
                recordActivation(session, "دعوة صديق MGM", "MGM_INVITE", "mgm", true, msg)
                return Resource.Success(msg)
            } else if (code == 401) {
                return Resource.Error("انتهت صلاحية الجلسة", isTokenExpired = true)
            } else if (code == 400) {
                val msg = "لقد وصلت إلى الحد الأقصى لعدد الدعوات (5 دعوات)"
                recordActivation(session, "دعوة صديق MGM", "MGM_INVITE", "mgm", false, msg)
                return Resource.Error(msg)
            } else {
                val msg = "فشل إرسال الدعوة (هذا الرقم قد تم دعوته من قبل)"
                recordActivation(session, "دعوة صديق MGM", "MGM_INVITE", "mgm", false, msg)
                return Resource.Error(msg)
            }
        } catch (e: Exception) {
            return Resource.Error("خطأ في الاتصال: ${e.localizedMessage}")
        }
    }

    suspend fun activateMgmReward(session: UserSession): Resource<String> {
        val bearerToken = "Bearer ${session.accessToken}"
        try {
            val response = apiService.activateMgmReward(session.msisdn, bearerToken)
            if (response.isSuccessful || response.code() in listOf(200, 201)) {
                val msg = "🎉🥳 تم تفعيل 1Go مجاناً على حسابك بمناسبة الدعوة!"
                recordActivation(session, "مكافأة دعوة 1Go", "MGMBONUS1Go", "mgm", true, msg)
                notificationHelper.showActivationNotification("مكافأة MGM 1Go", msg, true)
                return Resource.Success(msg)
            } else if (response.code() == 401) {
                return Resource.Error("انتهت صلاحية الجلسة", isTokenExpired = true)
            } else {
                val msg = "فشل تفعيل المكافأة (كود ${response.code()})"
                recordActivation(session, "مكافأة دعوة 1Go", "MGMBONUS1Go", "mgm", false, msg)
                return Resource.Error(msg)
            }
        } catch (e: Exception) {
            return Resource.Error("خطأ في الاتصال: ${e.localizedMessage}")
        }
    }

    // --- SIM Migration ---

    suspend fun getMigrationOptions(session: UserSession): Resource<List<MigrationOption>> {
        val bearerToken = "Bearer ${session.accessToken}"
        try {
            val response = apiService.getMigrationOptions(session.msisdn, bearerToken)
            if (response.isSuccessful) {
                val list = response.body()?.data ?: emptyList()
                return Resource.Success(list)
            } else if (response.code() == 401) {
                return Resource.Error("انتهت صلاحية الجلسة", isTokenExpired = true)
            } else {
                return Resource.Error("لا توجد خيارات تحويل متاحة")
            }
        } catch (e: Exception) {
            return Resource.Error("خطأ في الاتصال: ${e.localizedMessage}")
        }
    }

    suspend fun executeMigration(session: UserSession, migrationId: String, toName: String): Resource<String> {
        val bearerToken = "Bearer ${session.accessToken}"
        try {
            val response = apiService.executeMigration(
                session.msisdn,
                bearerToken,
                MigrationExecuteBody(migrationId)
            )
            val code = response.code()
            if (code in listOf(200, 201)) {
                val msg = "تم تحويل شريحتك إلى $toName بنجاح! 🥳"
                recordActivation(session, "تحويل الشريحة إلى $toName", migrationId, "migration", true, msg)
                notificationHelper.showActivationNotification("تحويل الشريحة", msg, true)
                return Resource.Success(msg)
            } else if (code == 401) {
                return Resource.Error("انتهت صلاحية الجلسة", isTokenExpired = true)
            } else {
                val msg = "فشل تنفيذ تحويل الشريحة (كود $code)"
                recordActivation(session, "تحويل الشريحة", migrationId, "migration", false, msg)
                return Resource.Error(msg)
            }
        } catch (e: Exception) {
            return Resource.Error("خطأ في الاتصال: ${e.localizedMessage}")
        }
    }

    // --- Account Info & Balances ---

    suspend fun getMainBalance(session: UserSession): Resource<BalanceData> {
        val bearerToken = "Bearer ${session.accessToken}"
        try {
            val response = apiService.getMainBalance(session.msisdn, bearerToken)
            if (response.isSuccessful) {
                val data = response.body()?.data ?: BalanceData("0")
                return Resource.Success(data)
            } else if (response.code() == 401) {
                return Resource.Error("انتهت صلاحية الجلسة", isTokenExpired = true)
            } else {
                return Resource.Error("فشل جلب الرصيد")
            }
        } catch (e: Exception) {
            return Resource.Error("خطأ في الاتصال: ${e.localizedMessage}")
        }
    }

    suspend fun getConnectedProducts(session: UserSession): Resource<List<ConnectedProduct>> {
        val bearerToken = "Bearer ${session.accessToken}"
        try {
            val response = apiService.getConnectedProducts(session.msisdn, bearerToken)
            if (response.isSuccessful) {
                val products = response.body()?.data?.products ?: emptyList()
                return Resource.Success(products)
            } else if (response.code() == 401) {
                return Resource.Error("انتهت صلاحية الجلسة", isTokenExpired = true)
            } else {
                return Resource.Error("لا توجد باقات نشطة")
            }
        } catch (e: Exception) {
            return Resource.Error("خطأ في الاتصال: ${e.localizedMessage}")
        }
    }

    private suspend fun recordActivation(
        session: UserSession,
        offerName: String,
        offerCode: String,
        type: String,
        isSuccess: Boolean,
        message: String
    ) {
        activationDao.insertActivation(
            ActivationRecord(
                msisdn = session.msisdn,
                phone = session.phone,
                offerName = offerName,
                offerCode = offerCode,
                offerType = type,
                isSuccess = isSuccess,
                message = message,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
