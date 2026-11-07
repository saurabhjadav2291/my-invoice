package com.revishingstudio.invoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.revishingstudio.invoice.ui.AppTheme
import com.revishingstudio.invoice.ui.InvoiceAppUi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status bar icon colours are set per screen in InvoiceAppUi.
        enableEdgeToEdge()
        val repository = (application as InvoiceApp).repository
        setContent {
            // The app is light only (design.md section 2.4).
            AppTheme {
                InvoiceAppUi(repository)
            }
        }
    }
}
