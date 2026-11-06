package com.revishingstudio.invoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.revishingstudio.invoice.ui.Accent
import com.revishingstudio.invoice.ui.InvoiceAppUi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as InvoiceApp).repository
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Accent)) {
                InvoiceAppUi(repository)
            }
        }
    }
}
