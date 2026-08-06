package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ActivationRecord
import com.example.data.db.AppDatabase
import com.example.data.db.UserSession
import com.example.data.model.ConnectedProduct
import com.example.data.model.DjezzyOffer
import com.example.data.model.MigrationOption
import com.example.data.repository.DjezzyRepository
import com.example.data.repository.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LoginUiState(
    val phoneInput: String = "",
    val otpInput: String = "",
    val isOtpSent: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class HomeUiState(
    val mainBalanceDzd: String = "0.00",
    val subscriptionType: String? = null,
    val connectedProducts: List<ConnectedProduct> = emptyList(),
    val isLoading: Boolean = false,
    val isActivatingWalk: Boolean = false,
    val bannerMessage: String? = null
)

data class MgmUiState(
    val friendPhoneInput: String = "",
    val isInviteSent: Boolean = false,
    val isLoading: Boolean = false,
    val friendOtpInput: String = "",
    val message: String? = null
)

data class MigrationUiState(
    val options: List<MigrationOption> = emptyList(),
    val isLoading: Boolean = false,
    val isExecuting: Boolean = false,
    val message: String? = null
)

class DjezzyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = DjezzyRepository(application, db)

    val currentSession: StateFlow<UserSession?> = repository.currentSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allActivations: StateFlow<List<ActivationRecord>> = repository.allActivations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val successCount: StateFlow<Int> = repository.successCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private val _offersCategory = MutableStateFlow("الكل")
    val offersCategory: StateFlow<String> = _offersCategory.asStateFlow()

    private val _offersSearchQuery = MutableStateFlow("")
    val offersSearchQuery: StateFlow<String> = _offersSearchQuery.asStateFlow()

    private val _isActivatingOffer = MutableStateFlow(false)
    val isActivatingOffer: StateFlow<Boolean> = _isActivatingOffer.asStateFlow()

    private val _activatingOfferName = MutableStateFlow<String?>(null)
    val activatingOfferName: StateFlow<String?> = _activatingOfferName.asStateFlow()

    private val _offerResultMessage = MutableStateFlow<String?>(null)
    val offerResultMessage: StateFlow<String?> = _offerResultMessage.asStateFlow()

    private val _mgmUiState = MutableStateFlow(MgmUiState())
    val mgmUiState: StateFlow<MgmUiState> = _mgmUiState.asStateFlow()

    private val _migrationUiState = MutableStateFlow(MigrationUiState())
    val migrationUiState: StateFlow<MigrationUiState> = _migrationUiState.asStateFlow()

    init {
        viewModelScope.launch {
            currentSession.collect { session ->
                if (session != null) {
                    refreshHomeData(session)
                }
            }
        }
    }

    // --- LOGIN FLOW ---

    fun onPhoneInputChanged(input: String) {
        _loginUiState.update { it.copy(phoneInput = input, errorMessage = null) }
    }

    fun onOtpInputChanged(input: String) {
        _loginUiState.update { it.copy(otpInput = input, errorMessage = null) }
    }

    fun requestOtp() {
        val phone = repository.cleanPhoneNumber(_loginUiState.value.phoneInput)
        if (phone == null) {
            _loginUiState.update { it.copy(errorMessage = "يرجى إدخال رقم جيزي صحيح يبدأ بـ 07") }
            return
        }

        viewModelScope.launch {
            _loginUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val res = repository.sendOtp(phone)
            when (res) {
                is Resource.Success -> {
                    _loginUiState.update {
                        it.copy(
                            isLoading = false,
                            isOtpSent = true,
                            successMessage = res.data ?: "تم إرسال رمز التحقق"
                        )
                    }
                }
                is Resource.Error -> {
                    _loginUiState.update {
                        it.copy(isLoading = false, errorMessage = res.message ?: "فشل إرسال الرمز")
                    }
                }
                else -> {}
            }
        }
    }

    fun verifyOtp() {
        val phone = repository.cleanPhoneNumber(_loginUiState.value.phoneInput) ?: return
        val otp = _loginUiState.value.otpInput.trim()
        if (otp.length != 6) {
            _loginUiState.update { it.copy(errorMessage = "يرجى إدخال رمز التحقق من 6 أرقام") }
            return
        }

        viewModelScope.launch {
            _loginUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val res = repository.verifyOtp(phone, otp)
            when (res) {
                is Resource.Success -> {
                    _loginUiState.update { LoginUiState() }
                }
                is Resource.Error -> {
                    _loginUiState.update {
                        it.copy(isLoading = false, errorMessage = res.message ?: "رمز غير صحيح")
                    }
                }
                else -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _loginUiState.update { LoginUiState() }
            _homeUiState.update { HomeUiState() }
        }
    }

    // --- HOME DASHBOARD ---

    fun refreshHomeData(session: UserSession? = currentSession.value) {
        val s = session ?: return
        viewModelScope.launch {
            _homeUiState.update { it.copy(isLoading = true) }

            val balRes = repository.getMainBalance(s)
            val prodRes = repository.getConnectedProducts(s)

            if (balRes.isTokenExpired || prodRes.isTokenExpired) {
                repository.logout()
                _loginUiState.update { it.copy(errorMessage = "انتهت صلاحية الجلسة، يرجى إعادة تسجيل الدخول") }
                return@launch
            }

            var balanceText = "0.00"
            var subType: String? = null
            if (balRes is Resource.Success && balRes.data != null) {
                balanceText = balRes.data.mainBalance ?: "0.00"
                subType = balRes.data.customerInformations?.subscriptionType?.name?.ar
                    ?: balRes.data.customerInformations?.subscriptionType?.name?.fr
            }

            var productsList = emptyList<ConnectedProduct>()
            if (prodRes is Resource.Success && prodRes.data != null) {
                productsList = prodRes.data
            }

            _homeUiState.update {
                it.copy(
                    mainBalanceDzd = balanceText,
                    subscriptionType = subType,
                    connectedProducts = productsList,
                    isLoading = false
                )
            }
        }
    }

    fun activateWalk2Go() {
        val session = currentSession.value ?: return
        viewModelScope.launch {
            _homeUiState.update { it.copy(isActivatingWalk = true, bannerMessage = null) }
            val res = repository.activateWalk2Go(session)
            if (res.isTokenExpired) {
                repository.logout()
                return@launch
            }
            val msg = res.data ?: res.message ?: "تم معالجة الطلب"
            _homeUiState.update { it.copy(isActivatingWalk = false, bannerMessage = msg) }
            refreshHomeData(session)
        }
    }

    fun dismissHomeBanner() {
        _homeUiState.update { it.copy(bannerMessage = null) }
    }

    // --- OFFERS ---

    fun setOffersCategory(category: String) {
        _offersCategory.value = category
    }

    fun setOffersSearchQuery(query: String) {
        _offersSearchQuery.value = query
    }

    fun activatePaidOffer(offer: DjezzyOffer) {
        val session = currentSession.value ?: return
        viewModelScope.launch {
            _isActivatingOffer.value = true
            _activatingOfferName.value = offer.name
            _offerResultMessage.value = null

            val res = repository.activatePaidOffer(session, offer)
            if (res.isTokenExpired) {
                repository.logout()
                _isActivatingOffer.value = false
                _activatingOfferName.value = null
                return@launch
            }

            _isActivatingOffer.value = false
            _activatingOfferName.value = null
            _offerResultMessage.value = res.data ?: res.message
            refreshHomeData(session)
        }
    }

    fun dismissOfferResultDialog() {
        _offerResultMessage.value = null
    }

    // --- MGM INVITATION ---

    fun onFriendPhoneChanged(phone: String) {
        _mgmUiState.update { it.copy(friendPhoneInput = phone, message = null) }
    }

    fun onFriendOtpChanged(otp: String) {
        _mgmUiState.update { it.copy(friendOtpInput = otp, message = null) }
    }

    fun sendMgmInvitation() {
        val session = currentSession.value ?: return
        val phone = repository.cleanPhoneNumber(_mgmUiState.value.friendPhoneInput)
        if (phone == null) {
            _mgmUiState.update { it.copy(message = "يرجى إدخال رقم جيزي صحيح يبدأ بـ 07") }
            return
        }

        viewModelScope.launch {
            _mgmUiState.update { it.copy(isLoading = true, message = null) }
            val res = repository.sendMgmInvite(session, phone)
            if (res.isTokenExpired) {
                repository.logout()
                return@launch
            }
            if (res is Resource.Success) {
                _mgmUiState.update { it.copy(isLoading = false, isInviteSent = true, message = res.data) }
            } else {
                _mgmUiState.update { it.copy(isLoading = false, message = res.message) }
            }
        }
    }

    fun verifyFriendOtpAndClaimReward() {
        val session = currentSession.value ?: return
        val friendPhone = repository.cleanPhoneNumber(_mgmUiState.value.friendPhoneInput) ?: return
        val otp = _mgmUiState.value.friendOtpInput.trim()
        if (otp.length != 6) {
            _mgmUiState.update { it.copy(message = "يرجى إدخال رمز التحقق من 6 أرقام") }
            return
        }

        viewModelScope.launch {
            _mgmUiState.update { it.copy(isLoading = true, message = "جاري التحقق وتفعيل المكافأة...") }
            // Verify Friend OTP
            val verifyRes = repository.verifyOtp(friendPhone, otp)
            if (verifyRes is Resource.Success) {
                // Now claim reward for sender
                val rewardRes = repository.activateMgmReward(session)
                _mgmUiState.update {
                    it.copy(
                        isLoading = false,
                        message = rewardRes.data ?: rewardRes.message
                    )
                }
                refreshHomeData(session)
            } else {
                _mgmUiState.update {
                    it.copy(isLoading = false, message = verifyRes.message ?: "رمز غير صحيح")
                }
            }
        }
    }

    fun resetMgmState() {
        _mgmUiState.value = MgmUiState()
    }

    // --- MIGRATION ---

    fun loadMigrationOptions() {
        val session = currentSession.value ?: return
        viewModelScope.launch {
            _migrationUiState.update { it.copy(isLoading = true, message = null) }
            val res = repository.getMigrationOptions(session)
            if (res.isTokenExpired) {
                repository.logout()
                return@launch
            }
            if (res is Resource.Success) {
                _migrationUiState.update { it.copy(isLoading = false, options = res.data ?: emptyList()) }
            } else {
                _migrationUiState.update { it.copy(isLoading = false, message = res.message ?: "لا توجد خيارات تحويل متاحة") }
            }
        }
    }

    fun executeMigration(option: MigrationOption) {
        val session = currentSession.value ?: return
        val toName = option.subscriptionTypeTo?.name?.ar ?: option.subscriptionTypeTo?.name?.fr ?: "العرض الجديد"
        viewModelScope.launch {
            _migrationUiState.update { it.copy(isExecuting = true, message = null) }
            val res = repository.executeMigration(session, option.id, toName)
            if (res.isTokenExpired) {
                repository.logout()
                return@launch
            }
            _migrationUiState.update {
                it.copy(
                    isExecuting = false,
                    message = res.data ?: res.message
                )
            }
            refreshHomeData(session)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
