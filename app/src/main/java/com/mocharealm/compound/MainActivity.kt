package com.mocharealm.compound

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mocharealm.compound.ui.AppNav
import com.mocharealm.compound.ui.theme.CompoundTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompoundTheme {
                AppNav()
            }
        }
    }
}