package net.ogatomo.tomoyansblog

import android.Manifest
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.SHARE_STATE_OFF
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import net.ogatomo.tomoyansblog.data.Article
import net.ogatomo.tomoyansblog.data.BlogRepository
import net.ogatomo.tomoyansblog.data.NotificationPreferences
import net.ogatomo.tomoyansblog.data.ThemeMode
import net.ogatomo.tomoyansblog.notifications.BlogPushManager
import net.ogatomo.tomoyansblog.ui.theme.TomoyansBlogTheme

class MainActivity : ComponentActivity() {
    private val pendingInitialUrl = mutableStateOf<String?>(null)
    private val openInitialUrlExternally = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        enableEdgeToEdge()
        pendingInitialUrl.value = intent.getStringExtra(EXTRA_INITIAL_URL)
        openInitialUrlExternally.value = intent.getBooleanExtra(EXTRA_OPEN_INITIAL_URL_EXTERNALLY, false)

        setContent {
            val preferences = remember { NotificationPreferences(applicationContext) }
            var themeMode by remember { mutableStateOf(preferences.getThemeMode()) }
            val useDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            TomoyansBlogTheme(darkTheme = useDarkTheme) {
                ConfigureSystemBars()
                Surface(modifier = Modifier.fillMaxSize()) {
                    BlogApp(
                        initialUrl = pendingInitialUrl.value,
                        openInitialUrlExternally = openInitialUrlExternally.value,
                        onInitialUrlConsumed = {
                            pendingInitialUrl.value = null
                            openInitialUrlExternally.value = false
                        },
                        themeMode = themeMode,
                        onThemeModeChange = {
                            themeMode = it
                            preferences.setThemeMode(it)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingInitialUrl.value = intent.getStringExtra(EXTRA_INITIAL_URL)
        openInitialUrlExternally.value = intent.getBooleanExtra(EXTRA_OPEN_INITIAL_URL_EXTERNALLY, false)
    }

    companion object {
        const val EXTRA_INITIAL_URL = "initial_url"
        const val EXTRA_OPEN_INITIAL_URL_EXTERNALLY = "open_initial_url_externally"
        const val ROUTE_HOME = "home"
        const val ROUTE_ABOUT = "about"
        const val ROUTE_SETTINGS = "settings"
    }
}

@Composable
private fun ConfigureSystemBars() {
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalActivity.current as ComponentActivity
    val statusBarLight = android.graphics.Color.parseColor("#7086BD")
    val statusBarDark = android.graphics.Color.parseColor("#435071")
    val navigationBarLight = android.graphics.Color.TRANSPARENT
    val navigationBarDark = android.graphics.Color.TRANSPARENT

    DisposableEffect(isDarkMode, context) {
        context.enableEdgeToEdge(
            statusBarStyle = if (!isDarkMode) {
                SystemBarStyle.light(
                    statusBarLight,
                    statusBarDark
                )
            } else {
                SystemBarStyle.dark(statusBarDark)
            },
            navigationBarStyle = if (!isDarkMode) {
                SystemBarStyle.light(
                    navigationBarLight,
                    navigationBarDark
                )
            } else {
                SystemBarStyle.dark(navigationBarDark)
            }
        )

        onDispose { }
    }
}

private sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Success(val articles: List<Article>) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

private class BlogViewModel(
    private val repository: BlogRepository,
    private val preferences: NotificationPreferences
) : ViewModel() {
    var feedState by mutableStateOf<FeedUiState>(FeedUiState.Loading)
        private set

    var alwaysOpenInExternalBrowser by mutableStateOf(preferences.isAlwaysOpenInExternalBrowser())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            feedState = FeedUiState.Loading
            feedState = try {
                val articles = repository.fetchLatestArticles()
                articles.firstOrNull()?.guid?.let { latestGuid ->
                    if (preferences.getLastSeenGuid().isNullOrBlank()) {
                        preferences.setLastSeenGuid(latestGuid)
                    }
                }
                FeedUiState.Success(articles)
            } catch (error: Exception) {
                FeedUiState.Error("記事情報の取得に失敗しました")
            }
        }
    }

    fun updateAlwaysOpenInExternalBrowser(enabled: Boolean) {
        alwaysOpenInExternalBrowser = enabled
        preferences.setAlwaysOpenInExternalBrowser(enabled)
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearCache(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                context.imageLoader.memoryCache?.clear()
                context.imageLoader.diskCache?.clear()
                context.cacheDir.deleteRecursively()
                context.cacheDir.mkdirs()
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BlogViewModel(
                    repository = BlogRepository(),
                    preferences = NotificationPreferences(context.applicationContext)
                ) as T
            }
        }
    }
}

@Composable
private fun BlogApp(
    initialUrl: String?,
    openInitialUrlExternally: Boolean,
    onInitialUrlConsumed: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { NotificationPreferences(context.applicationContext) }
    val activity = context as? ComponentActivity
    val viewModel: BlogViewModel = viewModel(factory = BlogViewModel.factory(context))
    val navController = rememberNavController()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        BlogPushManager.syncSubscription(context)
    }

    LaunchedEffect(Unit) {
        BlogPushManager.syncSubscription(context)
        if (
            activity != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !preferences.hasRequestedNotificationPermission() &&
            !BlogPushManager.hasNotificationPermission(context)
        ) {
            preferences.setHasRequestedNotificationPermission(true)
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                BlogPushManager.syncSubscription(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(initialUrl, openInitialUrlExternally) {
        if (!initialUrl.isNullOrBlank()) {
            if (openInitialUrlExternally) {
                openExternal(context, initialUrl)
            } else {
                openArticle(
                    context = context,
                    url = initialUrl,
                    alwaysOpenInExternalBrowser = viewModel.alwaysOpenInExternalBrowser
                )
            }
            onInitialUrlConsumed()
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainActivity.ROUTE_HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainActivity.ROUTE_HOME) {
                HomeScreen(
                    state = viewModel.feedState,
                    onRefresh = viewModel::refresh,
                    onArticleClick = { article ->
                        openArticle(
                            context = context,
                            url = article.link,
                            title = article.title,
                            alwaysOpenInExternalBrowser = viewModel.alwaysOpenInExternalBrowser
                        )
                    },
                    onInfoClick = { navController.navigate(MainActivity.ROUTE_ABOUT) },
                    onMoreClick = { openExternal(context, BlogRepository.MORE_URL) }
                )
            }
            composable(MainActivity.ROUTE_ABOUT) {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(MainActivity.ROUTE_SETTINGS) },
                    onOpenLicenses = { openOssLicenses(context) },
                    onOpenGithub = { openExternal(context, GITHUB_URL) }
                )
            }
            composable(MainActivity.ROUTE_SETTINGS) {
                SettingsScreen(
                    notificationsEnabled = notificationsEnabled,
                    alwaysOpenInExternalBrowser = viewModel.alwaysOpenInExternalBrowser,
                    themeMode = themeMode,
                    onBack = { navController.popBackStack() },
                    onNotificationSettingsClick = { openAppNotificationSettings(context) },
                    onAlwaysOpenInExternalBrowserToggle = viewModel::updateAlwaysOpenInExternalBrowser,
                    onThemeModeChange = onThemeModeChange,
                    onClearCache = {
                        viewModel.clearCache(context)
                        Toast.makeText(context, "キャッシュを削除しました", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: FeedUiState,
    onRefresh: () -> Unit,
    onArticleClick: (Article) -> Unit,
    onInfoClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val density = LocalDensity.current
    val bottomNavigationPadding = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    when (state) {
        FeedUiState.Loading -> LoadingScreen(onInfoClick = onInfoClick)
        is FeedUiState.Error -> ErrorScreen(
            message = state.message,
            onRetry = onRefresh,
            onInfoClick = onInfoClick
        )
        is FeedUiState.Success -> {
            val isTabletLayout = LocalConfiguration.current.smallestScreenWidthDp >= 600
            val featured = state.articles.firstOrNull()
            val others = state.articles.drop(1)
            val listState = rememberLazyListState()
            val visibleArticleIndexes by remember {
                derivedStateOf {
                    listState.layoutInfo.visibleItemsInfo
                        .mapNotNull { item -> item.key as? String }
                        .mapNotNull { key -> key.removePrefix(ARTICLE_ITEM_KEY_PREFIX).toIntOrNull() }
                        .toSet()
                }
            }

            if (isTabletLayout && featured != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    HomeHeader(onInfoClick = onInfoClick)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FeaturedArticleCard(
                            article = featured,
                            onClick = { onArticleClick(featured) },
                            modifier = Modifier.weight(1f)
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            state = listState,
                            contentPadding = PaddingValues(bottom = bottomNavigationPadding + 48.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "その他の記事",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            itemsIndexed(
                                items = others,
                                key = { index, _ -> "$ARTICLE_ITEM_KEY_PREFIX$index" }
                            ) { index, article ->
                                ArticleListItem(
                                    article = article,
                                    onClick = { onArticleClick(article) },
                                    shouldLoadImage = index in visibleArticleIndexes ||
                                        (index - 1) in visibleArticleIndexes ||
                                        (index + 1) in visibleArticleIndexes
                                )
                            }
                            item {
                                Button(
                                    onClick = onMoreClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = androidx.compose.ui.graphics.Color(0xFF7086BD),
                                        contentColor = androidx.compose.ui.graphics.Color.White
                                    )
                                ) {
                                    Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text("もっと見る")
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    state = listState,
                    contentPadding = PaddingValues(bottom = bottomNavigationPadding + 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HomeHeader(onInfoClick = onInfoClick)
                    }
                    if (featured != null) {
                        item {
                            FeaturedArticleCard(
                                article = featured,
                                onClick = { onArticleClick(featured) },
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                    item {
                        Text(
                            text = "その他の記事",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                    itemsIndexed(
                        items = others,
                        key = { index, _ -> "$ARTICLE_ITEM_KEY_PREFIX$index" }
                    ) { index, article ->
                        ArticleListItem(
                            article = article,
                            onClick = { onArticleClick(article) },
                            modifier = Modifier.padding(horizontal = 14.dp),
                            shouldLoadImage = index in visibleArticleIndexes ||
                                (index - 1) in visibleArticleIndexes ||
                                (index + 1) in visibleArticleIndexes
                        )
                    }
                    item {
                        Button(
                            onClick = onMoreClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFF7086BD),
                                contentColor = androidx.compose.ui.graphics.Color.White
                            )
                        ) {
                            Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("もっと見る")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onInfoClick: () -> Unit) {
    val headerColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val topInset = with(density) {
        WindowInsets.statusBars
            .union(WindowInsets.displayCutout)
            .getTop(this)
            .toDp()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerColor)
            .padding(start = 16.dp, end = 22.dp, top = topInset + 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.tomoyansblog_logo2026_white),
            contentDescription = "Tomoyan's Blog",
            modifier = Modifier
                .fillMaxWidth(0.64f)
                .height(60.dp),
            contentScale = ContentScale.Fit
        )
        Surface(
            modifier = Modifier.align(Alignment.CenterEnd),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.18f)
        ) {
            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "このアプリについて",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun AboutScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenGithub: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current
    val bottomNavigationPadding = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val versionName = remember(context) {
        runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "不明"
        }.getOrDefault("不明")
    }
    var insiderTapCount by remember { mutableStateOf(0) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var fcmToken by remember { mutableStateOf<String?>(null) }
    var isLoadingToken by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = bottomNavigationPadding + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "戻る"
                    )
                }
                Text(
                    text = "このアプリについて",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.app_about_icon),
                        contentDescription = "アプリアイコン",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable(enabled = BuildConfig.IS_DEBUG_MODE) {
                                insiderTapCount += 1
                                if (insiderTapCount >= 5) {
                                    insiderTapCount = 0
                                    showTokenDialog = true
                                    isLoadingToken = true
                                    fcmToken = null
                                    FirebaseMessaging.getInstance().token
                                        .addOnCompleteListener { task ->
                                            isLoadingToken = false
                                            fcmToken = task.result
                                        }
                                }
                            }
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Column {
                        Text(
                            text = "Tomoyan's Blog",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "by Tomoya Ogawa",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "バージョン: $versionName",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = context.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (BuildConfig.IS_DEBUG_MODE) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "DEBUG VERSION",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Spacer(modifier = Modifier.size(10.dp))
                Text("設定")
            }
        }
        item {
            Button(
                onClick = onOpenLicenses,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Outlined.Description, contentDescription = null)
                Spacer(modifier = Modifier.size(10.dp))
                Text("ライセンス")
            }
        }
        item {
            Button(
                onClick = onOpenGithub,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                Spacer(modifier = Modifier.size(10.dp))
                Text("GitHub")
            }
        }
    }

    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = {
                Text("FCM登録トークン")
            },
            text = {
                Text(
                    text = when {
                        isLoadingToken -> "取得中..."
                        !fcmToken.isNullOrBlank() -> fcmToken.orEmpty()
                        else -> "トークンを取得できませんでした。"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        fcmToken?.takeIf { it.isNotBlank() }?.let { token ->
                            clipboardManager.setText(AnnotatedString(token))
                            Toast.makeText(context, "FCM登録トークンをコピーしました", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !fcmToken.isNullOrBlank()
                ) {
                    Text("コピー")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) {
                    Text("閉じる")
                }
            }
        )
    }
}

@Composable
private fun FeaturedArticleCard(article: Article, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            ArticleThumbnail(
                imageUrl = article.imageUrl,
                contentDescription = article.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                aspectRatio = 40f / 21f,
                shouldLoadImage = true
            )
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "最新記事",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                ArticleMetaText(article = article)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ArticleListItem(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shouldLoadImage: Boolean
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ArticleThumbnail(
                imageUrl = article.imageUrl,
                contentDescription = article.title,
                modifier = Modifier.width(88.dp),
                shouldLoadImage = shouldLoadImage
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = article.publishedAt,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                article.categories.firstOrNull()?.let { category ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ArticleThumbnail(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1f,
    shouldLoadImage: Boolean = true
) {
    Box(
        modifier = modifier.aspectRatio(aspectRatio)
    ) {
        if (shouldLoadImage && !imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(9.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO IMAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ArticleMetaText(article: Article) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = article.publishedAt,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        article.categories.firstOrNull()?.let { category ->
            Text(
                text = category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    notificationsEnabled: Boolean,
    alwaysOpenInExternalBrowser: Boolean,
    themeMode: ThemeMode,
    onBack: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onAlwaysOpenInExternalBrowserToggle: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onClearCache: () -> Unit
) {
    val settingsAccentColor = MaterialTheme.colorScheme.tertiary
    val deleteButtonContentColor = Color.White
    val density = LocalDensity.current
    val bottomNavigationPadding = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = bottomNavigationPadding + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "戻る"
                    )
                }
                Text(
                    text = "設定",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item {
            Card(
                modifier = Modifier.clickable(onClick = onNotificationSettingsClick),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.76f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = settingsAccentColor
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Column {
                            Text(
                                text = "新着記事の通知",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (notificationsEnabled) {
                                    "Androidの通知設定を開きます"
                                } else {
                                    "現在Android側で通知がオフです"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.76f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInBrowser,
                            contentDescription = null,
                            tint = settingsAccentColor
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Column {
                            Text(
                                text = "常に外部ブラウザで開く",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "オンにすると記事はChrome Custom Tabではなく外部ブラウザで開きます。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = alwaysOpenInExternalBrowser,
                        onCheckedChange = onAlwaysOpenInExternalBrowserToggle
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "ダークモード",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "システム・ライト・ダークから表示モードを選べます。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> "システム"
                                            ThemeMode.LIGHT -> "ライト"
                                            ThemeMode.DARK -> "ダーク"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.68f)) {
                        Text(
                            text = "キャッシュ削除",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "画像などの一時キャッシュを削除します。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onClearCache,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = deleteButtonContentColor
                        )
                    ) {
                        Text(text = "削除", color = deleteButtonContentColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(onInfoClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeader(onInfoClick = onInfoClick)
        Box(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit, onInfoClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeader(onInfoClick = onInfoClick)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = message, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text("再読み込み")
                }
            }
        }
    }
}

private fun String.isAllowedInternalUrl(): Boolean {
    val host = Uri.parse(this).host ?: return false
    return host == "ogatomo.net" || host.endsWith(".ogatomo.net")
}

private fun openArticle(
    context: Context,
    url: String,
    title: String? = null,
    alwaysOpenInExternalBrowser: Boolean
) {
    if (alwaysOpenInExternalBrowser) {
        openExternal(context, url)
        return
    }

    openArticleInCustomTab(context, url, title)
}

private fun openArticleInCustomTab(context: Context, url: String, title: String? = null) {
    if (!url.isAllowedInternalUrl()) {
        openExternal(context, url)
        return
    }

    val colorScheme = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(android.graphics.Color.parseColor("#7086BD"))
        .build()

    val sharePendingIntent = PendingIntent.getActivity(
        context,
        url.hashCode(),
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    buildString {
                        if (!title.isNullOrBlank()) {
                            append(title)
                            append("\n")
                        }
                        append(url)
                    }
                )
            },
            "共有"
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val intent = CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(colorScheme)
        .setShowTitle(true)
        .setShareState(SHARE_STATE_OFF)
        .addMenuItem("共有", sharePendingIntent)
        .build()

    try {
        intent.launchUrl(context, Uri.parse(url))
    } catch (_: ActivityNotFoundException) {
        openExternal(context, url)
    }
}

private fun openExternal(context: Context, url: String) {
    if (url.isBlank()) {
        return
    }

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "ブラウザを開けませんでした", Toast.LENGTH_SHORT).show()
    }
}

private const val GITHUB_URL = "https://github.com/ogatomo21/tomoyansblog-android"

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent().apply {
        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        val fallbackIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(fallbackIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "通知設定を開けませんでした", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openOssLicenses(context: Context) {
    try {
        val intent = Intent(context, OpenSourceLicensesActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "ライセンス画面を開けませんでした", Toast.LENGTH_SHORT).show()
    }
}

private const val ARTICLE_ITEM_KEY_PREFIX = "article_"
