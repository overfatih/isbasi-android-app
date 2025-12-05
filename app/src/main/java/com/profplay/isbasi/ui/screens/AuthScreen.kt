package com.profplay.isbasi.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.profplay.isbasi.viewmodel.AuthUiState
import com.profplay.isbasi.viewmodel.AuthViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.profplay.isbasi.R
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: (userRole: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("employee") } //{admin, employer, employee, supervisor} {admin, işveren, işçi, dayıbaşı}

    // EKRAN BOYUTUNU ALMA
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Logo boyutunu ekran genişliğinin %40'ı olarak ayarla (Bayağı büyük olur)
    val logoSize = screenWidth * 0.50f
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_i2_sbasi2), // Birleşik logo
            contentDescription = "İşbaşı Logosu",
            contentScale = ContentScale.Fit, // Orantıyı bozmadan sığdır
            modifier = Modifier
                .width(logoSize) // Genişliği ayarla (yükseklik orantılı gelir)
                // Eğer kare değilse .size(logoSize) yerine .width() daha güvenlidir
                .padding(bottom = 48.dp)
        )

        // 🔘 Switch ile giriş / kayıt geçişi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Giriş  ", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = isSignUp,
                onCheckedChange = { isSignUp = it }
            )
            Text("  Kayıt Ol", style = MaterialTheme.typography.bodyMedium)
        }

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Şifre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else
                    Icons.Filled.VisibilityOff

                // İkona tıklanınca görünürlüğü tersine çevir
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = if (passwordVisible) "Gizle" else "Göster")
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 🔽 Role dropdown sadece kayıt modunda gösterilir
        AnimatedVisibility(visible = isSignUp) {
            RoleDropdown { selected ->
                role = selected
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isSignUp) {
                    println("Kayıt: $email, $role")
                    viewModel.signUp(email, password, role)
                } else {
                    println("Giriş: $email")
                    viewModel.login(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) "Kayıt Ol" else "Giriş Yap")
        }

        when (uiState) {
            is AuthUiState.Idle -> {}
            is AuthUiState.Loading -> Text("Loading...")
            is AuthUiState.Success -> {
                Text("Success!")
                LaunchedEffect(Unit) {
                    onAuthSuccess((uiState as AuthUiState.Success).userRole)
                }
            }
            is AuthUiState.Error -> Text("Error: ${(uiState as AuthUiState.Error).message}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleDropdown(
    onRoleSelected: (String) -> Unit
) {
    val roles = listOf(
        "işçi" to "employee",
        "işveren" to "employer"
        /*"dayıbaşı" to "supervisor"*/
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Role") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            roles.forEach { (display, value) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = {
                        selectedText = display
                        expanded = false
                        onRoleSelected(value) // burada employee / employer / supervisor döner
                    }
                )
            }
        }
    }
}

