package com.ft.ftchinese.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun Drawer() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalDrawer(
        drawerState = drawerState,
        drawerContent = {
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp),
                onClick = { scope.launch { drawerState.close() } },
                content = { Text("Close Drawer") }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .weight(1.0f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = if (drawerState.isClosed) ">>> Swipe >>>" else "<<< Swipe <<<")
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { scope.launch { drawerState.open() } }) {
                        Text("Click to open")
                    }
                }

                BottomNavView(
                    onSelect = {},
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDrawer() {
    Drawer()
}
