package com.ft.ftchinese.ui.search

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.ui.theme.OColor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchBar(
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    TopAppBar(
        backgroundColor = OColor.wheat
    ) {
       IconButton(
           onClick = {
               focusManager.clearFocus()
               keyboardController?.hide()
               onBack()
           }
       ) {
           Icon(
               imageVector = Icons.Filled.ArrowBack,
               contentDescription = "Back",
               tint = OColor.black90
           )
       }


    }
}

@Composable
fun SearchTextField(
    focused: Boolean,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember {
        FocusRequester()
    }

    Surface(
        modifier = modifier
            .then(
                Modifier
                    .height(56.dp)
                    .padding(
                        top = 8.dp,
                        bottom = 8.dp,
                        start = if (!focused) 16.dp else 0.dp,
                        end = 16.dp
                    )
            ),
        color = Color(0xffF5F5F5),
        shape = RoundedCornerShape(percent = 50),
    ) {

    }
}
