package com.revishingstudio.invoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.graphics.Color
import androidx.activity.SystemBarStyle
import com.revishingstudio.invoice.ui.AppTheme
import com.revishingstudio.invoice.ui.InvoiceAppUi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always light, so keep dark status and navigation bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val repository = (application as InvoiceApp).repository
        setContent {
            AppTheme {
                InvoiceAppUi(repository)
            }
        }
    }
}
