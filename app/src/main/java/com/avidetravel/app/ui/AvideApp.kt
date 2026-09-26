package com.avidetravel.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.avidetravel.app.data.Agent
import com.avidetravel.app.data.AvideApi
import com.avidetravel.app.data.ChatMessage
import com.avidetravel.app.data.Service
import com.avidetravel.app.data.ServiceVideoReview
import com.avidetravel.app.data.SessionStore
import com.avidetravel.app.data.TravelTip
import com.avidetravel.app.data.UserProfile
import com.avidetravel.app.data.VideoReview
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class MainTab(val label: String, val icon: String) {
    HOME("Home", "⌂"),
    DEALS("Deals", "✦"),
    EXPLORE("Explore", "◎"),
    CHAT("Chat", "💬"),
    PROFILE("Profile", "●")
}

private enum class ContentPage {
    TIPS,
    VIDEOS,
    FAVORITES,
    NOTIFICATIONS
}

private val destinations = listOf(
    "All",
    "Dominican Republic",
    "Mexico",
    "Jamaica",
    "Aruba",
    "Bahamas",
    "Hawaii",
    "United States",
    "Cruise"
)

@Composable
fun AvideApp(
    googleAuthCode: String? = null,
    onGoogleAuthConsumed: () -> Unit = {},
    viewModel: AvideViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

    var tab by remember { mutableStateOf(MainTab.HOME) }
    var contentPage by remember { mutableStateOf<ContentPage?>(null) }
    var dealDestination by remember { mutableStateOf("All") }
    var selectedService by remember { mutableStateOf<Service?>(null) }
    var chatService by remember { mutableStateOf<Service?>(null) }
    var currentUser by remember { mutableStateOf(SessionStore.currentUser(context)) }
    var favorites by remember { mutableStateOf(SessionStore.favoriteIds(context)) }
    var unreadCount by remember { mutableStateOf(SessionStore.unreadInboxCount(context)) }

    LaunchedEffect(googleAuthCode) {
        val code = googleAuthCode?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        runCatching { AvideApi.exchangeGoogleCode(code) }
            .onSuccess { login ->
                SessionStore.saveAuth(context, login)
                currentUser = login.user
                contentPage = null
                tab = MainTab.PROFILE
            }
        onGoogleAuthConsumed()
    }

    fun openChat(service: Service? = null) {
        chatService = service
        contentPage = null
        tab = MainTab.CHAT
    }

    fun toggleFavorite(id: Long) {
        favorites = SessionStore.toggleFavorite(context, id)
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            unreadCount = SessionStore.unreadInboxCount(context)
            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            AvideHeader(
                unreadCount = unreadCount,
                onLogoClick = {
                    contentPage = null
                    tab = MainTab.HOME
                },
                onNotifications = {
                    contentPage = ContentPage.NOTIFICATIONS
                    unreadCount = 0
                    SessionStore.markInboxRead(context)
                }
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = contentPage == null && tab == item,
                        onClick = {
                            contentPage = null
                            if (item == MainTab.DEALS) dealDestination = "All"
                            tab = item
                        },
                        icon = { Text(item.icon, fontSize = 18.sp) },
                        label = { Text(item.label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)

        when (contentPage) {
            ContentPage.TIPS -> TravelTipsScreen(
                modifier = modifier,
                onBack = { contentPage = null }
            )
            ContentPage.VIDEOS -> VideoReviewsScreen(
                modifier = modifier,
                onBack = { contentPage = null }
            )
            ContentPage.FAVORITES -> FavoritesScreen(
                services = state.services,
                favoriteIds = favorites,
                modifier = modifier,
                onBack = { contentPage = null },
                onOpenDeal = { selectedService = it },
                onToggleFavorite = ::toggleFavorite
            )
            ContentPage.NOTIFICATIONS -> NotificationInboxScreen(
                modifier = modifier,
                onBack = { contentPage = null },
                onOpenDeal = { id ->
                    state.services.firstOrNull { it.id == id }?.let { selectedService = it }
                },
                onOpenTips = { contentPage = ContentPage.TIPS },
                onOpenChat = {
                    contentPage = null
                    tab = MainTab.CHAT
                }
            )
            null -> when (tab) {
                MainTab.HOME -> HomeScreen(
                    state = state,
                    modifier = modifier,
                    onRefresh = { viewModel.refresh() },
                    onOpenDeal = { selectedService = it },
                    onDeals = {
                        dealDestination = "All"
                        tab = MainTab.DEALS
                    },
                    onDestination = {
                        dealDestination = it
                        tab = MainTab.DEALS
                    },
                    onTips = { contentPage = ContentPage.TIPS },
                    onVideos = { contentPage = ContentPage.VIDEOS },
                    onChat = { openChat() },
                    favorites = favorites,
                    onToggleFavorite = ::toggleFavorite
                )
                MainTab.DEALS -> DealsScreen(
                    state = state,
                    modifier = modifier,
                    initialDestination = dealDestination,
                    onRefresh = { viewModel.refresh() },
                    onOpenDeal = { selectedService = it },
                    favoriteIds = favorites,
                    onToggleFavorite = ::toggleFavorite
                )
                MainTab.EXPLORE -> ExploreScreen(
                    modifier = modifier,
                    favoriteCount = favorites.size,
                    onTips = { contentPage = ContentPage.TIPS },
                    onVideos = { contentPage = ContentPage.VIDEOS },
                    onFavorites = { contentPage = ContentPage.FAVORITES },
                    onDeals = {
                        dealDestination = "All"
                        tab = MainTab.DEALS
                    },
                    onChat = { openChat() }
                )
                MainTab.CHAT -> ChatScreen(
                    modifier = modifier,
                    currentUser = currentUser,
                    aboutService = chatService,
                    onClearDealContext = { chatService = null }
                )
                MainTab.PROFILE -> ProfileScreen(
                    modifier = modifier,
                    currentUser = currentUser,
                    favoriteCount = favorites.size,
                    onUserChanged = {
                        currentUser = SessionStore.currentUser(context)
                    },
                    onFavorites = { contentPage = ContentPage.FAVORITES }
                )
            }
        }
    }

    selectedService?.let { service ->
        DealDialog(
            service = service,
            agent = state.agents.firstOrNull { it.id == service.agentId },
            isFavorite = service.id in favorites,
            onToggleFavorite = { toggleFavorite(service.id) },
            onAsk = {
                selectedService = null
                openChat(service)
            },
            onDismiss = { selectedService = null }
        )
    }
}

@Composable
private fun AvideHeader(
    unreadCount: Int,
    onLogoClick: () -> Unit,
    onNotifications: () -> Unit
) {
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onLogoClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("AvideTravel", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                    Text(
                        "Travel deals & real agents",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f),
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onNotifications),
                contentAlignment = Alignment.Center
            ) {
                Text("🔔", fontSize = 23.sp)
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            unreadCount.coerceAtMost(9).toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: AvideUiState,
    modifier: Modifier,
    onRefresh: () -> Unit,
    onOpenDeal: (Service) -> Unit,
    onDeals: () -> Unit,
    onDestination: (String) -> Unit,
    onTips: () -> Unit,
    onVideos: () -> Unit,
    onChat: () -> Unit,
    favorites: Set<Long>,
    onToggleFavorite: (Long) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
            ) {
                AsyncImage(
                    model = "https://avide.travel/front.jpg",
                    contentDescription = "Travel destination",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = .42f))
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(22.dp)
                ) {
                    Text(
                        "TRAVEL SMARTER",
                        color = Color(0xFFFFD58A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        "Dream trips, real agents, better deals.",
                        color = Color.White,
                        fontSize = 29.sp,
                        lineHeight = 33.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hand-picked vacation packages, resorts and cruises from real travel agents.",
                        color = Color.White.copy(alpha = .92f),
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(15.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onDeals) { Text("View Deals") }
                        OutlinedButton(onClick = onChat) { Text("Chat", color = Color.White) }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Explore destinations",
                subtitle = "Find live AvideTravel deals by destination",
                modifier = Modifier.padding(18.dp, 22.dp, 18.dp, 10.dp)
            )
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("🇩🇴", "Dominican", Modifier.weight(1f)) {
                        onDestination("Dominican Republic")
                    }
                    QuickAction("🇲🇽", "Mexico", Modifier.weight(1f)) {
                        onDestination("Mexico")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("🇯🇲", "Jamaica", Modifier.weight(1f)) {
                        onDestination("Jamaica")
                    }
                    QuickAction("💡", "Travel Tips", Modifier.weight(1f), onTips)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp, 18.dp, 18.dp, 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction("▶️", "Resort Videos", Modifier.weight(1f), onVideos)
                QuickAction("💬", "Ask an Agent", Modifier.weight(1f), onChat)
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp, 10.dp, 18.dp, 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeader("Featured deals", "Live offers from AvideTravel agents")
                TextButton(onClick = onDeals) { Text("See all") }
            }
        }

        when {
            state.loading && state.services.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.services.isEmpty() -> item {
                ErrorCard(state.error, onRefresh)
            }
            else -> item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.services.take(8), key = { it.id }) { service ->
                        DealCard(
                            service = service,
                            modifier = Modifier.width(280.dp),
                            isFavorite = service.id in favorites,
                            onFavorite = { onToggleFavorite(service.id) },
                            onClick = { onOpenDeal(service) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DealsScreen(
    state: AvideUiState,
    modifier: Modifier,
    initialDestination: String,
    onRefresh: () -> Unit,
    onOpenDeal: (Service) -> Unit,
    favoriteIds: Set<Long>,
    onToggleFavorite: (Long) -> Unit
) {
    var selectedDestination by remember(initialDestination) { mutableStateOf(initialDestination) }

    val filtered = remember(state.services, selectedDestination) {
        if (selectedDestination == "All") state.services
        else state.services.filter {
            it.location.contains(selectedDestination, ignoreCase = true) ||
                it.category.contains(selectedDestination, ignoreCase = true) ||
                it.title.contains(selectedDestination, ignoreCase = true)
        }
    }

    Column(modifier.fillMaxSize()) {
        SectionHeader(
            "Travel deals",
            "Live offers published by AvideTravel agents",
            Modifier.padding(18.dp, 18.dp, 18.dp, 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(destinations) { destination ->
                FilterChip(
                    selected = selectedDestination == destination,
                    onClick = { selectedDestination = destination },
                    label = { Text(destination) }
                )
            }
        }

        when {
            state.loading && state.services.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.services.isEmpty() -> {
                ErrorCard(state.error, onRefresh)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${filtered.size} offers", fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = onRefresh, enabled = !state.refreshing) {
                                Text(if (state.refreshing) "Refreshing…" else "Refresh")
                            }
                        }
                    }
                    items(filtered, key = { it.id }) { service ->
                        DealCard(
                            service = service,
                            modifier = Modifier.fillMaxWidth(),
                            isFavorite = service.id in favoriteIds,
                            onFavorite = { onToggleFavorite(service.id) },
                            onClick = { onOpenDeal(service) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreScreen(
    modifier: Modifier,
    favoriteCount: Int,
    onTips: () -> Unit,
    onVideos: () -> Unit,
    onFavorites: () -> Unit,
    onDeals: () -> Unit,
    onChat: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader("Explore", "Everything here stays inside the AvideTravel app") }
        item { ExploreCard("🌴", "Travel Deals", "Browse current AvideTravel offers", onDeals) }
        item { ExploreCard("💡", "Travel Tips", "Native guides and new publications", onTips) }
        item { ExploreCard("▶️", "Resort Video Reviews", "See the resort before you book", onVideos) }
        item {
            ExploreCard(
                "♥",
                "Saved Deals",
                if (favoriteCount == 1) "1 saved deal" else "$favoriteCount saved deals",
                onFavorites
            )
        }
        item { ExploreCard("💬", "Chat with an Agent", "Ask questions without leaving the app", onChat) }
    }
}

@Composable
private fun TravelTipsScreen(
    modifier: Modifier,
    onBack: () -> Unit
) {
    var tips by remember { mutableStateOf<List<TravelTip>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<TravelTip?>(null) }

    LaunchedEffect(Unit) {
        runCatching { AvideApi.getTravelTips() }
            .onSuccess { tips = it }
            .onFailure { error = it.message }
        loading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackHeader("Travel Tips", "Latest AvideTravel publications", onBack) }
        if (loading) item { CenterLoader() }
        error?.let { message -> item { ErrorCard(message) {} } }
        items(tips, key = { it.id }) { tip ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selected = tip }
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    tip.coverImage?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = tip.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(tip.topicTitle, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        Text(tip.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        if (tip.excerpt.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                tip.excerpt,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    selected?.let { tip ->
        Dialog(onDismissRequest = { selected = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(.94f)
                    .fillMaxHeight(.9f)
            ) {
                LazyColumn(contentPadding = PaddingValues(18.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Travel Guide", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { selected = null }) { Text("Close") }
                        }
                    }
                    tip.coverImage?.let { image ->
                        item {
                            AsyncImage(
                                model = image,
                                contentDescription = tip.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    item {
                        Text(tip.title, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            tip.content.ifBlank { tip.excerpt },
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoReviewsScreen(
    modifier: Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var reviews by remember { mutableStateOf<List<VideoReview>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { AvideApi.getVideoReviews() }
            .onSuccess { reviews = it }
            .onFailure { error = it.message }
        loading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BackHeader("Resort Video Reviews", "Watch before you book", onBack) }
        if (loading) item { CenterLoader() }
        error?.let { item { ErrorCard(it) {} } }
        items(reviews, key = { it.id }) { review ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openYoutube(context, review.youtubeUrl) }
            ) {
                AsyncImage(
                    model = review.thumbnailUrl,
                    contentDescription = review.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                )
                Column(Modifier.padding(14.dp)) {
                    Text(review.hotelName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(review.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    if (review.channelName.isNotBlank()) {
                        Text("Video by ${review.channelName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { openYoutube(context, review.youtubeUrl) }) {
                        Text("Watch on YouTube")
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesScreen(
    services: List<Service>,
    favoriteIds: Set<Long>,
    modifier: Modifier,
    onBack: () -> Unit,
    onOpenDeal: (Service) -> Unit,
    onToggleFavorite: (Long) -> Unit
) {
    val saved = remember(services, favoriteIds) { services.filter { it.id in favoriteIds } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackHeader("Saved Deals", "${saved.size} saved", onBack) }
        if (saved.isEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(20.dp)) {
                        Text("No saved deals yet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Tap ♥ on any deal to keep it here.")
                    }
                }
            }
        }
        items(saved, key = { it.id }) { service ->
            DealCard(
                service = service,
                modifier = Modifier.fillMaxWidth(),
                isFavorite = true,
                onFavorite = { onToggleFavorite(service.id) },
                onClick = { onOpenDeal(service) }
            )
        }
    }
}

@Composable
private fun NotificationInboxScreen(
    modifier: Modifier,
    onBack: () -> Unit,
    onOpenDeal: (Long) -> Unit,
    onOpenTips: () -> Unit,
    onOpenChat: () -> Unit
) {
    val context = LocalContext.current
    val inbox = remember { SessionStore.inboxItems(context) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { BackHeader("Notifications", "New deals and publications", onBack) }
        if (inbox.isEmpty()) {
            item {
                Card {
                    Text(
                        "New AvideTravel deals and Travel Tips will appear here.",
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
        items(inbox, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (item.type == "deal") item.refId?.toLongOrNull()?.let(onOpenDeal)
                        if (item.type == "tip") onOpenTips()
                        if (item.type == "chat") onOpenChat()
                    }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(item.title, fontWeight = FontWeight.Bold)
                    Text(item.body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(
    modifier: Modifier,
    currentUser: UserProfile?,
    aboutService: Service?,
    onClearDealContext: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(currentUser?.id) {
        mutableStateOf(currentUser?.name?.ifBlank { null } ?: SessionStore.guestName(context))
    }
    var email by remember(currentUser?.id) {
        mutableStateOf(currentUser?.email ?: SessionStore.guestEmail(context))
    }
    var conversationId by remember { mutableStateOf(SessionStore.conversationId(context)) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(conversationId) {
        while (isActive) {
            val id = conversationId
            if (!id.isNullOrBlank()) {
                runCatching { AvideApi.getChatMessages(id) }
                    .onSuccess {
                        messages = it
                        it.lastOrNull { message -> message.sender == "admin" }
                            ?.let { message -> SessionStore.setLastChatMessageId(context, message.id) }
                    }
            }
            delay(5000)
        }
    }

    Column(modifier.fillMaxSize()) {
        SectionHeader(
            "Chat with AvideTravel",
            "Messages go directly to our travel team",
            Modifier.padding(18.dp, 18.dp, 18.dp, 8.dp)
        )

        if (aboutService != null) {
            Card(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Asking about", fontSize = 11.sp)
                        Text(aboutService.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = onClearDealContext) { Text("Clear") }
                }
            }
        }

        if (currentUser == null) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        "Ask about a resort, dates, price, room type or anything else about your trip.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)
                    )
                }
            }
            items(messages, key = { it.id }) { message ->
                val isUser = message.sender != "admin"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            message.text,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Message your travel agent…") },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            Button(
                enabled = !sending && draft.isNotBlank(),
                onClick = {
                    if (name.isBlank() || email.isBlank()) {
                        error = "Please enter your name and email first."
                        return@Button
                    }
                    val text = draft.trim()
                    sending = true
                    error = null
                    scope.launch {
                        runCatching {
                            SessionStore.saveGuestIdentity(context, name, email)
                            AvideApi.sendChat(
                                conversationId = conversationId,
                                userName = name,
                                userEmail = email,
                                message = text,
                                service = aboutService
                            )
                        }.onSuccess { result ->
                            conversationId = result.conversationId
                            SessionStore.saveConversationId(context, result.conversationId)
                            result.message?.let { messages = messages + it }
                            draft = ""
                        }.onFailure {
                            error = it.message ?: "Message could not be sent."
                        }
                        sending = false
                    }
                }
            ) {
                Text(if (sending) "…" else "Send")
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    modifier: Modifier,
    currentUser: UserProfile?,
    favoriteCount: Int,
    onUserChanged: () -> Unit,
    onFavorites: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notificationsEnabled by remember {
        mutableStateOf(SessionStore.notificationsEnabled(context))
    }

    if (currentUser != null) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Profile", "Your AvideTravel account") }
            item {
                Card {
                    Column(Modifier.padding(18.dp)) {
                        Text(currentUser.name.ifBlank { currentUser.email }, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text(currentUser.email, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                        if (currentUser.phone.isNotBlank()) Text(currentUser.phone)
                    }
                }
            }
            item {
                Card(modifier = Modifier.clickable(onClick = onFavorites)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("♥ Saved Deals", fontWeight = FontWeight.Bold)
                        Text(favoriteCount.toString())
                    }
                }
            }
            item {
                Card {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Deal & article notifications", fontWeight = FontWeight.Bold)
                            Text("Background checks approximately every 30 minutes", fontSize = 12.sp)
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = {
                                notificationsEnabled = it
                                SessionStore.setNotificationsEnabled(context, it)
                            }
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        SessionStore.logout(context)
                        onUserChanged()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign out")
                }
            }
        }
        return
    }

    var createMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var consentEmail by remember { mutableStateOf(true) }
    var consentSms by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionHeader("Profile", "Account is optional — browsing and chat also work as a guest") }

        item {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://avide.travel/mobile/google-auth")
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue with Google")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(Modifier.weight(1f))
                Text(
                    "  or use email  ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f)
                )
                HorizontalDivider(Modifier.weight(1f))
            }
        }

        if (createMode) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(firstName, { firstName = it }, label = { Text("First name") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(lastName, { lastName = it }, label = { Text("Last name") }, modifier = Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(
                    phone,
                    { phone = it },
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            OutlinedTextField(
                email,
                { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                password,
                { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (createMode) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consentEmail, onCheckedChange = { consentEmail = it })
                    Text("I agree to receive account and travel emails")
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consentSms, onCheckedChange = { consentSms = it })
                    Text("I want optional SMS updates")
                }
            }
        }

        message?.let {
            item {
                Text(
                    it,
                    color = if (it.startsWith("Welcome")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Button(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    busy = true
                    message = null
                    scope.launch {
                        runCatching {
                            if (createMode) {
                                AvideApi.signup(
                                    firstName = firstName,
                                    lastName = lastName,
                                    email = email,
                                    phone = phone,
                                    password = password,
                                    consentEmail = consentEmail,
                                    consentSms = consentSms
                                )
                            }
                            AvideApi.login(email, password)
                        }.onSuccess { login ->
                            SessionStore.saveAuth(context, login)
                            message = "Welcome to AvideTravel."
                            onUserChanged()
                        }.onFailure {
                            message = it.message ?: "Unable to continue."
                        }
                        busy = false
                    }
                }
            ) {
                Text(
                    when {
                        busy -> "Please wait…"
                        createMode -> "Create account"
                        else -> "Sign in"
                    }
                )
            }
        }

        item {
            TextButton(onClick = {
                createMode = !createMode
                message = null
            }) {
                Text(if (createMode) "Already have an account? Sign in" else "New here? Create account")
            }
        }

        item {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Card(modifier = Modifier.clickable(onClick = onFavorites)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("♥ Local Saved Deals")
                    Text(favoriteCount.toString())
                }
            }
        }
    }
}

@Composable
private fun DealCard(
    service: Service,
    modifier: Modifier,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                if (!service.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = service.imageUrl,
                        contentDescription = service.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌴", fontSize = 42.sp)
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clickable(onClick = onFavorite),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = .9f)
                ) {
                    Text(
                        if (isFavorite) "♥" else "♡",
                        color = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 20.sp
                    )
                }
            }

            Column(Modifier.padding(14.dp)) {
                Text(
                    service.location.ifBlank { service.category.ifBlank { "AvideTravel Deal" } },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    service.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("From", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                        Text(service.displayPrice(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    if (service.duration.isNotBlank()) Text(service.duration, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DealDialog(
    service: Service,
    agent: Agent?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAsk: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val images = service.imageUrls.ifEmpty { listOfNotNull(service.imageUrl) }
    val pagerState = rememberPagerState(pageCount = { maxOf(1, images.size) })
    var video by remember(service.id) { mutableStateOf<ServiceVideoReview?>(null) }
    var videoLoading by remember(service.id) { mutableStateOf(true) }

    LaunchedEffect(service.id) {
        video = runCatching { AvideApi.getReviewForService(service.id) }.getOrNull()
        videoLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(.95f)
                .fillMaxHeight(.92f)
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = 18.dp)) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) { Text("Close") }
                        TextButton(onClick = onToggleFavorite) {
                            Text(if (isFavorite) "♥ Saved" else "♡ Save")
                        }
                    }
                }

                item {
                    if (images.isNotEmpty()) {
                        Box {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            ) { page ->
                                AsyncImage(
                                    model = images[page],
                                    contentDescription = service.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (images.size > 1) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = .6f)
                                ) {
                                    Text(
                                        "${pagerState.currentPage + 1}/${images.size}",
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            service.location.ifBlank { "AvideTravel Deal" },
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(service.title, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text(service.displayPrice(), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        if (service.duration.isNotBlank()) Text(service.duration)
                        if (service.startDate != null || service.endDate != null) {
                            Text(
                                listOfNotNull(service.startDate, service.endDate).joinToString(" → "),
                                fontSize = 13.sp
                            )
                        }

                        val tags = buildList {
                            if (service.allInclusive) add("All inclusive")
                            if (service.flightIncluded) add("Flights included")
                            if (service.transferIncluded) add("Transfers")
                            if (service.familyFriendly) add("Family friendly")
                            if (service.adultsOnly) add("Adults only")
                            if (service.beachAccess) add("Beach")
                            if (service.pool) add("Pool")
                            if (service.wifi) add("Wi-Fi")
                        }
                        if (tags.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(tags.joinToString("  •  "), fontSize = 13.sp)
                        }

                        val description = service.description.ifBlank { service.shortDescription }
                        if (description.isNotBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Text(description, fontSize = 15.sp, lineHeight = 22.sp)
                        }

                        agent?.let {
                            Spacer(Modifier.height(14.dp))
                            Text("Your travel agent: ${it.name}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (videoLoading) {
                    item {
                        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Checking for resort video…", fontSize = 13.sp)
                        }
                    }
                }

                video?.let { match ->
                    item {
                        Card(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            AsyncImage(
                                model = match.review.thumbnailUrl,
                                contentDescription = match.review.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clickable { openYoutube(context, match.review.youtubeUrl) }
                            )
                            Column(Modifier.padding(14.dp)) {
                                Text("See the resort before you book", fontWeight = FontWeight.Bold)
                                Text(match.review.title)
                                if (match.review.channelName.isNotBlank()) {
                                    Text(
                                        "Video by ${match.review.channelName}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = { openYoutube(context, match.review.youtubeUrl) }) {
                                    Text("Watch video")
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onAsk,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text("Ask an Agent About This Deal")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: String,
    title: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 25.sp)
            Spacer(Modifier.width(9.dp))
            Text(title, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExploreCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
            }
            Text("›", fontSize = 28.sp)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun BackHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Column {
        TextButton(onClick = onBack) { Text("‹ Back") }
        SectionHeader(title, subtitle)
    }
}

@Composable
private fun CenterLoader() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card {
        Column(Modifier.padding(18.dp)) {
            Text("Could not load data", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Button(onClick = onRetry) { Text("Try again") }
        }
    }
}

private fun openYoutube(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
