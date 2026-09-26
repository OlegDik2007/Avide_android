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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.avidetravel.app.data.Agent
import com.avidetravel.app.data.Service

private enum class MainTab(val label: String, val icon: String) {
    HOME("Home", "⌂"),
    DEALS("Deals", "✦"),
    EXPLORE("Explore", "◎"),
    CONTACT("Contact", "☎")
}

private data class ExploreItem(
    val icon: String,
    val title: String,
    val subtitle: String,
    val url: String
)

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

private val exploreItems = listOf(
    ExploreItem("🏨", "Stays", "Hotels & resorts", "https://avide.travel/hotels"),
    ExploreItem("✈️", "Flights", "Search airfare", "https://avide.travel/flights"),
    ExploreItem("🌴", "Vacation Deals", "Live agent offers", "https://avide.travel/services"),
    ExploreItem("🚗", "Car Rentals", "Cars at your destination", "https://avide.travel/carrentals"),
    ExploreItem("🛳️", "Cruises", "Ocean & island escapes", "https://avide.travel/cruises"),
    ExploreItem("🎟️", "Concerts & Sports", "Event tickets", "https://avide.travel/concert_sport"),
    ExploreItem("🗺️", "Tours & Attractions", "Things to do", "https://avide.travel/tours"),
    ExploreItem("💡", "Travel Tips", "Useful planning guides", "https://avide.travel/blog/travel-tips")
)

@Composable
fun AvideApp(viewModel: AvideViewModel = viewModel()) {
    val state = viewModel.state
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var dealDestination by remember { mutableStateOf("All") }
    var selectedService by remember { mutableStateOf<Service?>(null) }

    Scaffold(
        topBar = { AvideHeader(onLogoClick = { tab = MainTab.HOME }) },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = {
                            if (item == MainTab.DEALS) dealDestination = "All"
                            tab = item
                        },
                        icon = { Text(item.icon, fontSize = 20.sp) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            MainTab.HOME -> HomeScreen(
                state = state,
                modifier = Modifier.padding(padding),
                onRefresh = { viewModel.refresh() },
                onOpenDeal = { selectedService = it },
                onDeals = {
                    dealDestination = "All"
                    tab = MainTab.DEALS
                },
                onDestination = { destination ->
                    dealDestination = destination
                    tab = MainTab.DEALS
                },
                onExplore = { tab = MainTab.EXPLORE }
            )
            MainTab.DEALS -> DealsScreen(
                state = state,
                modifier = Modifier.padding(padding),
                initialDestination = dealDestination,
                onRefresh = { viewModel.refresh() },
                onOpenDeal = { selectedService = it }
            )
            MainTab.EXPLORE -> ExploreScreen(Modifier.padding(padding))
            MainTab.CONTACT -> ContactScreen(state.agents, Modifier.padding(padding))
        }
    }

    selectedService?.let { service ->
        DealDialog(
            service = service,
            agent = state.agents.firstOrNull { it.id == service.agentId },
            onDismiss = { selectedService = null }
        )
    }
}

@Composable
private fun AvideHeader(onLogoClick: () -> Unit) {
    Surface(shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onLogoClick)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("AvideTravel", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text(
                    "Your journey starts here",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f),
                    fontSize = 12.sp
                )
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
    onExplore: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
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
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hand-picked vacation packages, resorts and cruises from real travel agents.",
                        color = Color.White.copy(alpha = .92f),
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onDeals) { Text("View Deals") }
                        OutlinedButton(onClick = onExplore) { Text("Explore", color = Color.White) }
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
                    QuickAction("🇩🇴", "Dominican", Modifier.weight(1f)) { onDestination("Dominican Republic") }
                    QuickAction("🇲🇽", "Mexico", Modifier.weight(1f)) { onDestination("Mexico") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("🇯🇲", "Jamaica", Modifier.weight(1f)) { onDestination("Jamaica") }
                    QuickAction("💡", "Travel Tips", Modifier.weight(1f)) {
                        openUrl(context, "https://avide.travel/blog/travel-tips")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.padding(18.dp, 18.dp, 18.dp, 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨", fontSize = 30.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Avide AI Travel Agent", fontWeight = FontWeight.Bold)
                        Text("Get destination ideas and planning help in seconds.", fontSize = 13.sp)
                    }
                    TextButton(onClick = { openUrl(context, "https://avide.travel") }) {
                        Text("Ask")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp, 16.dp, 18.dp, 8.dp),
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
                            onClick = { onOpenDeal(service) }
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.padding(18.dp, 22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12253A))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("PERSONAL SERVICE", color = Color(0xFFFFD58A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Prefer a real travel agent?", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tell us what you want and an AvideTravel agent can help you plan it.",
                        color = Color.White.copy(alpha = .85f)
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { openUrl(context, "https://avide.travel/contact") }) {
                        Text("Contact an Agent")
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
    onOpenDeal: (Service) -> Unit
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
                    if (state.error != null) {
                        item {
                            Text(
                                state.error,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
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
                        DealCard(service, Modifier.fillMaxWidth()) { onOpenDeal(service) }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "No current deals match this destination.",
                                modifier = Modifier.padding(vertical = 24.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreScreen(modifier: Modifier) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader("Explore", "Plan each part of your trip") }
        items(exploreItems) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openUrl(context, item.url) }
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.icon, fontSize = 30.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            item.subtitle,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f)
                        )
                    }
                    Text("›", fontSize = 28.sp)
                }
            }
        }
    }
}

@Composable
private fun ContactScreen(agents: List<Agent>, modifier: Modifier) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                "Talk to a travel agent",
                "Human help for packages, resorts, cruises and custom trips"
            )
        }

        if (agents.isEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(18.dp)) {
                        Text("AvideTravel", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Our agents can help you build and book your trip.")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { openUrl(context, "https://avide.travel/contact") }) {
                            Text("Contact AvideTravel")
                        }
                    }
                }
            }
        } else {
            items(agents, key = { it.id }) { agent ->
                Card {
                    Column(Modifier.padding(18.dp)) {
                        Text(agent.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (agent.companyName.isNotBlank() && agent.companyName != agent.name) {
                            Text(agent.companyName, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                        }
                        if (agent.location.isNotBlank()) Text(agent.location, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (agent.phone.isNotBlank()) {
                                OutlinedButton(onClick = { dial(context, agent.phone) }) { Text("Call") }
                            }
                            if (agent.email.isNotBlank()) {
                                OutlinedButton(onClick = { email(context, agent.email) }) { Text("Email") }
                            }
                            if (agent.website.isNotBlank()) {
                                TextButton(onClick = { openUrl(context, agent.website) }) { Text("Website") }
                            }
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Button(
                onClick = { openUrl(context, "https://avide.travel/contact") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open AvideTravel Contact")
            }
        }
    }
}

@Composable
private fun DealCard(service: Service, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
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
                    if (service.duration.isNotBlank()) {
                        Text(service.duration, fontSize = 12.sp)
                    }
                }
                val tags = buildList {
                    if (service.allInclusive) add("All inclusive")
                    if (service.flightIncluded) add("Flights")
                    if (service.transferIncluded) add("Transfers")
                    if (service.familyFriendly) add("Family")
                    if (service.adultsOnly) add("Adults only")
                }
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tags.take(3).joinToString("  •  "),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DealDialog(service: Service, agent: Agent?, onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(service.title) },
        text = {
            Column {
                if (!service.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = service.imageUrl,
                        contentDescription = service.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(service.displayPrice(), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                if (service.location.isNotBlank()) Text(service.location, color = MaterialTheme.colorScheme.primary)
                if (service.duration.isNotBlank()) Text(service.duration)
                Spacer(Modifier.height(10.dp))
                Text(
                    service.shortDescription.ifBlank { service.description.ifBlank { "Open this AvideTravel offer for full details." } },
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
                agent?.let {
                    Spacer(Modifier.height(12.dp))
                    Text("Agent: ${it.name}", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(onClick = { openUrl(context, service.webUrl()) }) {
                Text("View / Book")
            }
        },
        dismissButton = {
            Row {
                if (agent?.phone?.isNotBlank() == true) {
                    TextButton(onClick = { dial(context, agent.phone) }) { Text("Call agent") }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
private fun QuickAction(icon: String, title: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 26.sp)
            Spacer(Modifier.width(10.dp))
            Text(title, fontWeight = FontWeight.Bold)
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
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(modifier = Modifier.padding(18.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("Could not load live travel data", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Button(onClick = onRetry) { Text("Try again") }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
    }
}

private fun dial(context: Context, phone: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")))
    }
}

private fun email(context: Context, address: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(address)}")))
    }
}
