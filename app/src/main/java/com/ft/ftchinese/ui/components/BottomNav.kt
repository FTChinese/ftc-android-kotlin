package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

private val BottomNavigationHeight = 56.dp

@Composable
fun BottomNav(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.primarySurface,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = BottomNavigationDefaults.Elevation,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(BottomNavigationHeight)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun BottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = ContentAlpha.medium),
) {
    val contentColor = if (selected) selectedContentColor else unselectedContentColor

    CompositionLocalProvider(
        LocalContentColor provides contentColor
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.caption.copy(
                textAlign = TextAlign.Center
            )
        ) {
            Column(
                modifier = modifier
                    .selectable(
                        selected = selected,
                        enabled = true,
                        onClick = onClick
                    )
                    .padding(Dimens.dp8),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                icon()
                label()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomNavItemSelected() {
    BottomNavItem(
        selected = true,
        onClick = {},
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.news_inactive),
                contentDescription = null
            )
        },
        label = {
            Text(
                text = "News",
                softWrap = false,
            )
        },
        selectedContentColor = OColor.claret
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomNavItemUnselected() {
    BottomNavItem(
        selected = false,
        onClick = {},
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.english_inactive),
                contentDescription = null
            )
        },
        label = {
            Text(
                text = "English",
                softWrap = false,
            )
        },
        selectedContentColor = OColor.claret
    )
}
