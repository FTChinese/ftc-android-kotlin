package com.ft.ftchinese.ui.main.search

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.ui.theme.OTheme

class SearchableActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SearchApp(
                onExit = {
                    finish()
                }
            )
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, SearchableActivity::class.java))
        }
    }
}

@Composable
private fun SearchApp(
    onExit: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()

    OTheme {
        Scaffold(
            scaffoldState = scaffoldState
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "search",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = "search"
                ) {
                    SearchActivityScreen(
                        scaffoldState = scaffoldState,
                        onBack =  {
                            val ok = navController.popBackStack()
                            if (!ok) {
                                onExit()
                            }
                        }
                    )
                }
            }
        }
    }
}
