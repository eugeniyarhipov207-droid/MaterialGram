package com.kyecg.materialgram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.github.tdlibandroid.ktx.TdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.util.Locale
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur

private val Purple = Color(0xFF7655D9)
private val LightPurple = Color(0xFFEAE2FF)
private val DeepPurple = Color(0xFF4F359F)
private val TelegramBlue = Color(0xFF2AABEE)

private val LightColors = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = LightPurple,
    onPrimaryContainer = DeepPurple,
    secondary = Color(0xFF65558F),
    background = Color(0xFFFAF8FF),
    surface = Color(0xFFFAF8FF),
    surfaceVariant = Color(0xFFF0ECF7)
)

data class Country(
    val region: String,
    val name: String,
    val flag: String,
    val callingCode: Int
) {
    val dialCode: String
        get() = "+$callingCode"
}

private fun flagEmoji(region: String): String {
    return region.uppercase(Locale.ROOT)
        .map { char ->
            String(Character.toChars(0x1F1E6 + char.code - 'A'.code))
        }
        .joinToString("")
}

private fun loadCountries(): List<Country> {
    val phoneUtil = PhoneNumberUtil.getInstance()
    val russianLocale = Locale("ru")

    return phoneUtil.supportedRegions
        .mapNotNull { region ->
            val callingCode =
                phoneUtil.getCountryCodeForRegion(region)

            if (callingCode <= 0 || region.length != 2) {
                return@mapNotNull null
            }

            val name = Locale("", region)
                .getDisplayCountry(russianLocale)
                .ifBlank { region }

            Country(
                region = region,
                name = name,
                flag = flagEmoji(region),
                callingCode = callingCode
            )
        }
        .distinctBy { it.region }
        .sortedBy { it.name.lowercase(russianLocale) }
}

class MainActivity : ComponentActivity() {

    private lateinit var tdClient: TdClient

    private val authState = mutableStateOf("Подключение к Telegram…")
    private val authError = mutableStateOf("")
    private val authStep = mutableStateOf("loading")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor =
            android.graphics.Color.rgb(250, 248, 255)

        window.navigationBarColor =
            android.graphics.Color.rgb(250, 248, 255)

        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        System.loadLibrary("tdjni")

        tdClient = TdClient(
            filesDir = filesDir.absolutePath + "/tdlib",
            verbosityLevel = 1,
            apiId = BuildConfig.TELEGRAM_API_ID,
            apiHash = BuildConfig.TELEGRAM_API_HASH
        )

        lifecycleScope.launch(Dispatchers.IO) {
            tdClient.updates.collect { update ->
                if (update is TdApi.UpdateAuthorizationState) {
                    handleAuthorizationState(update.authorizationState)
                }
            }
        }

        tdClient.init()

        setContent {
    MaterialGramTheme {
        if (authStep.value == "ready") {
            val chatListViewModel = remember(tdClient) {
                ChatListViewModel(tdClient)
            }

            DisposableEffect(chatListViewModel) {
                onDispose {
                    chatListViewModel.close()
                }
            }

            MainAppScreen(
    viewModel = chatListViewModel,
    onChatClick = { chatId ->
        android.widget.Toast.makeText(
            this@MainActivity,
            "Чат ID: $chatId",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
)
        } else {
            LoginScreen(
                authStep = authStep.value,
                authMessage = authState.value,
                authError = authError.value,
                onSendPhone = ::sendPhone,
                onSendCode = ::sendCode,
                onSendPassword = ::sendPassword
            )
        }
    }
}
}

    private fun handleAuthorizationState(
        state: TdApi.AuthorizationState
    ) {
        when (state) {
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                authStep.value = "phone"
                authState.value = "Введите номер телефона"
            }

            is TdApi.AuthorizationStateWaitCode -> {
                authStep.value = "code"
                authState.value = "Введите код из Telegram"
            }

            is TdApi.AuthorizationStateWaitPassword -> {
                authStep.value = "password"
                authState.value = "Введите пароль двухэтапной проверки"
            }

            is TdApi.AuthorizationStateReady -> {
                authStep.value = "ready"
                authState.value = "Вы вошли в аккаунт!"
                authError.value = ""
            }

            is TdApi.AuthorizationStateLoggingOut -> {
                authStep.value = "loading"
                authState.value = "Выход из аккаунта…"
            }

            is TdApi.AuthorizationStateClosing -> {
                authStep.value = "loading"
                authState.value = "Закрытие соединения…"
            }

            is TdApi.AuthorizationStateClosed -> {
                authStep.value = "loading"
                authState.value = "Соединение закрыто"
            }

            else -> {
                authStep.value = "loading"
                authState.value = "Подготовка соединения…"
            }
        }
    }

    private fun sendPhone(phone: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    tdClient.send(
                        TdApi.SetAuthenticationPhoneNumber(
                            phone,
                            null
                        )
                    )
                }
                authError.value = ""
            } catch (e: Exception) {
                authError.value = e.message ?: "Не удалось отправить номер"
            }
        }
    }

    private fun sendCode(code: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    tdClient.send(
                        TdApi.CheckAuthenticationCode(code)
                    )
                }
                authError.value = ""
            } catch (e: Exception) {
                authError.value = e.message ?: "Неверный код или ошибка входа"
            }
        }
    }

    private fun sendPassword(password: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    tdClient.send(
                        TdApi.CheckAuthenticationPassword(password)
                    )
                }
                authError.value = ""
            } catch (e: Exception) {
                authError.value = e.message ?: "Ошибка проверки пароля"
            }
        }
    }

    override fun onDestroy() {
        if (::tdClient.isInitialized) {
            tdClient.close()
        }
        super.onDestroy()
    }
}

@Composable
fun MaterialGramTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}

@Composable
private fun TelegramLogo(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(116.dp)
            .clip(CircleShape)
            .background(TelegramBlue)
            .semantics {
                contentDescription = "Логотип Telegram"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(66.dp)) {
            val s = size.width / 24f

            val plane = Path().apply {
                moveTo(23.5f * s, 4.2f * s)
                lineTo(20.1f * s, 21.0f * s)
                cubicTo(
                    19.8f * s, 22.2f * s,
                    19.1f * s, 22.5f * s,
                    18.1f * s, 21.9f * s
                )
                lineTo(12.6f * s, 17.8f * s)
                lineTo(9.9f * s, 20.4f * s)
                cubicTo(
                    9.6f * s, 20.7f * s,
                    9.3f * s, 21.0f * s,
                    8.7f * s, 21.0f * s
                )
                lineTo(9.1f * s, 15.3f * s)
                lineTo(19.6f * s, 6.4f * s)
                cubicTo(
                    20.1f * s, 6.0f * s,
                    19.5f * s, 5.8f * s,
                    18.9f * s, 6.2f * s
                )
                lineTo(5.9f * s, 14.3f * s)
                lineTo(0.8f * s, 12.7f * s)
                cubicTo(
                    -0.3f * s, 12.3f * s,
                    -0.3f * s, 11.5f * s,
                    1.0f * s, 11.0f * s
                )
                lineTo(22.0f * s, 3.0f * s)
                cubicTo(
                    23.0f * s, 2.6f * s,
                    23.8f * s, 3.2f * s,
                    23.5f * s, 4.2f * s
                )
                close()
            }

            drawPath(plane, Color.White)
        }
    }
}

@Composable
private fun CountryPicker(
    countries: List<Country>,
    selectedCountry: Country,
    onSelect: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }

    val filteredCountries = remember(search, countries) {
        countries.filter { country ->
            country.name.contains(search, ignoreCase = true) ||
                country.dialCode.contains(search) ||
                country.region.contains(search, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Выбор страны",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Поиск страны или кода")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(
                        items = filteredCountries,
                        key = { it.region }
                    ) { country ->
                        val isSelected =
                            country.region == selectedCountry.region

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable { onSelect(country) }
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 13.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(country.flag, fontSize = 25.sp)

                            Spacer(modifier = Modifier.width(13.dp))

                            Text(
                                text = country.name,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (isSelected) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )

                            Text(
                                text = country.dialCode,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (filteredCountries.isEmpty()) {
                        item {
                            Text(
                                text = "Страна не найдена",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    authStep: String,
    authMessage: String,
    authError: String,
    onSendPhone: (String) -> Unit,
    onSendCode: (String) -> Unit,
    onSendPassword: (String) -> Unit
) {
    val countries = remember { loadCountries() }

    var selectedCountry by remember {
        mutableStateOf(
            countries.firstOrNull { it.region == "RU" }
                ?: countries.first()
        )
    }

    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var showCountryPicker by remember {
        mutableStateOf(false)
    }

    when (authStep) {
        "phone" -> {
            PhoneScreen(
                selectedCountry = selectedCountry,
                phone = phone,
                authError = authError,
                onPhoneChange = {
                    phone = it.filter(Char::isDigit).take(15)
                },
                onCountryClick = {
                    showCountryPicker = true
                },
                onContinue = {
                    val fullPhone =
                        selectedCountry.dialCode +
                            phone.filter(Char::isDigit)

                    onSendPhone(fullPhone)
                }
            )
        }

        "code" -> {
            CodeScreen(
                phone = "${selectedCountry.dialCode} $phone",
                code = code,
                authError = authError,
                onCodeChange = {
                    code = it.filter(Char::isDigit).take(8)
                },
                onContinue = {
                    onSendCode(code)
                },
                onChangePhone = {
                    code = ""
                }
            )
        }

        "password" -> {
            PasswordScreen(
                password = password,
                authError = authError,
                onPasswordChange = {
                    password = it
                },
                onContinue = {
                    onSendPassword(password)
                }
            )
        }

        "ready" -> {
            StatusScreen(
                title = "Добро пожаловать!",
                message = "MaterialGram подключён к Telegram.",
                loading = false
            )
        }

        else -> {
            StatusScreen(
                title = "MaterialGram",
                message = authMessage,
                loading = true
            )
        }
    }

    if (showCountryPicker && authStep == "phone") {
        CountryPicker(
            countries = countries,
            selectedCountry = selectedCountry,
            onSelect = {
                selectedCountry = it
                showCountryPicker = false
            },
            onDismiss = {
                showCountryPicker = false
            }
        )
    }
}

@Composable
private fun PhoneScreen(
    selectedCountry: Country,
    phone: String,
    authError: String,
    onPhoneChange: (String) -> Unit,
    onCountryClick: () -> Unit,
    onContinue: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "MaterialGram",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "BETA",
                color = colors.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        TelegramLogo()

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Всё начинается\nс общения.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 42.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            "Твои разговоры, каналы и друзья —\nв одном красивом месте.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 25.sp
        )

        Spacer(modifier = Modifier.height(42.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = colors.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Вход в аккаунт",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Укажи номер телефона, чтобы продолжить.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onCountryClick),
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 18.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                selectedCountry.flag,
                                fontSize = 22.sp
                            )

                            Spacer(modifier = Modifier.width(7.dp))

                            Text(
                                selectedCountry.dialCode,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                "⌄",
                                color = colors.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = onPhoneChange,
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Номер телефона")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        )
                    )
                }

                if (authError.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        authError,
                        color = colors.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onContinue,
                    enabled = phone.length in 4..15,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Продолжить",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    Text("→", fontSize = 21.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Мы отправим код подтверждения\nна указанный номер.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CodeScreen(
    phone: String,
    code: String,
    authError: String,
    onCodeChange: (String) -> Unit,
    onContinue: () -> Unit,
    onChangePhone: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "MaterialGram",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "BETA",
                color = colors.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(64.dp))

        TelegramLogo()

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Проверь сообщения",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Введите код подтверждения\nдля номера $phone",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Код из Telegram")
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        if (authError.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                authError,
                color = colors.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onContinue,
            enabled = code.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                "Продолжить",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        TextButton(onClick = onChangePhone) {
            Text("Изменить номер телефона")
        }
    }
}

@Composable
private fun PasswordScreen(
    password: String,
    authError: String,
    onPasswordChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        TelegramLogo()

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Облачный пароль",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Для этого аккаунта включена двухэтапная проверка.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Пароль")
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            visualTransformation =
                androidx.compose.ui.text.input.PasswordVisualTransformation()
        )

        if (authError.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                authError,
                color = colors.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onContinue,
            enabled = password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                "Войти",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusScreen(
    title: String,
    message: String,
    loading: Boolean
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TelegramLogo()

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (loading) {
            Spacer(modifier = Modifier.height(28.dp))

            CircularProgressIndicator(
                color = colors.primary
            )
        }
    }
}
