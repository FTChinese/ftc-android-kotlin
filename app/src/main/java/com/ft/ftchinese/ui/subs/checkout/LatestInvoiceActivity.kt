package com.ft.ftchinese.ui.subs.checkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.subs.invoice.LatestInvoiceActivityScreen
import com.ft.ftchinese.ui.theme.OTheme

/**
 * [LatestInvoiceActivity] shows the payment result of alipay of wxpay.
 */
class LatestInvoiceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            CheckoutResultApp {
                finish()
            }
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, LatestInvoiceActivity::class.java))
        }
    }
}

@Composable
fun CheckoutResultApp(
    onExit: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    val currentScreen = CheckoutScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    OTheme {
        Scaffold(
            topBar = {
                Toolbar(
                    heading = stringResource(id = currentScreen.titleId),
                    onBack = {
                        val ok = navController.popBackStack()
                        if (!ok) {
                            onExit()
                        }
                    }
                )
            },
            scaffoldState = scaffoldState,
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = CheckoutScreen.Invoices.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = CheckoutScreen.Invoices.name
                ) {
                    LatestInvoiceActivityScreen(
                        onNext = {
                            navigateTo(
                                navController = navController,
                                screen = CheckoutScreen.BuyerInfo,
                            )
                        }
                    )

                    BuyerInfoActivityScreen(
                        scaffoldState = scaffoldState,
                        onExit = onExit
                    )
                }
            }
        }
    }
}

private fun navigateTo(
    navController: NavHostController,
    screen: CheckoutScreen
) {
    navController.navigate(screen.name)
}


