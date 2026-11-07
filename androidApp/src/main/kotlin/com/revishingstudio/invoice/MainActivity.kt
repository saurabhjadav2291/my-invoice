package com.revishingstudio.invoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.revishingstudio.invoice.ui.AppTheme
import com.revishingstudio.invoice.ui.AppearanceStore
import com.revishingstudio.invoice.ui.InvoiceAppUi
import com.revishingstudio.invoice.ui.LocalAppearanceStore
import com.revishingstudio.invoice.ui.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status bar icon colours are set per screen in InvoiceAppUi.
        enableEdgeToEdge()
        val repository = (application as InvoiceApp).repository
        val appearance = AppearanceStore(this)
        setContent {
            val dark = appearance.appearance.isDark()
            CompositionLocalProvider(LocalAppearanceStore provides appearance) {
                AppTheme(dark = dark) {
                    InvoiceAppUi(repository)
                }
            }
        }
    }
}
