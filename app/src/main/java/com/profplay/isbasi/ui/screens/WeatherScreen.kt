package com.profplay.isbasi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.profplay.isbasi.viewmodel.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    navController: NavController,
    viewModel: WeatherViewModel
) {
    val weather by viewModel.weatherState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var cityQuery by remember { mutableStateOf("Salihli") }

    // Ekran ilk açıldığında yükle
    LaunchedEffect(Unit) {
        viewModel.loadWeather()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hava Durumu") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arama Kutusu
            OutlinedTextField(
                value = cityQuery,
                onValueChange = { cityQuery = it },
                label = { Text("Şehir / İlçe Ara") },
                trailingIcon = {
                    IconButton(onClick = { viewModel.loadWeather(cityQuery) }) {
                        Icon(Icons.Default.Search, contentDescription = "Ara")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text("Hata: $error", color = Color.Red)
            } else if (weather != null) {
                WeatherCard(weather!!)
            }
        }
    }
}

@Composable
fun WeatherCard(data: com.profplay.isbasi.data.model.WeatherResponse) {
    // Basit ikon eşleştirmesi
    val icon = when (data.weather.firstOrNull()?.icon?.substring(0, 2)) {
        "01" -> Icons.Default.WbSunny // Güneşli (Veya Icons.Default.Info)
        "02", "03", "04" -> Icons.Default.Cloud // Bulutlu (Extended varsa)
        "09", "10" -> Icons.Default.WaterDrop // Yağmurlu (Extended varsa, yoksa Info)
        "13" -> Icons.Default.AcUnit // Karlı
        else -> Icons.Default.Info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF42A5F5), Color(0xFF1E88E5))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.Yellow
                )

                Text(
                    text = "${data.main.temp.toInt()}°C",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )

                Text(
                    text = data.weather.firstOrNull()?.description?.uppercase() ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoItem("Nem", "%${data.main.humidity}")
                    InfoItem("Rüzgar", "${data.wind.speed} m/s")
                }
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.7f))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}