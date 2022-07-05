package com.ft.ftchinese.ui.article.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.ContentAlpha
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.model.reader.Access
import com.ft.ftchinese.model.reader.MemberStatus
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColors

sealed class BarrierEvent {
    object Login : BarrierEvent()
    data class Paywall(val upgrade: Boolean) : BarrierEvent()
    object MySubs : BarrierEvent()
    object Quit : BarrierEvent()
}

@Composable
fun PermissionBarrier(
    modifier: Modifier = Modifier,
    access: Access,
    onClick: (BarrierEvent) -> Unit,
) {

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .clickable(
                onClick = {
                    onClick(BarrierEvent.Quit)
                }
            )
            .background(Color.Black.copy(ContentAlpha.disabled))
            .fillMaxSize()
            .then(modifier)
    ) {


        Column(
            modifier = Modifier
                .background(OColors.whiteBackground)
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            IconButton(
                onClick = { onClick(BarrierEvent.Quit) },
                modifier = Modifier.align(Alignment.End)
            ) {
                IconClose()
            }

            Column(
                modifier = Modifier
                    .padding(
                        horizontal = Dimens.dp16
                    )
                    .fillMaxWidth()
            ) {

                Heading3(
                    text = stringResource(id = R.string.barrier_title)
                )

                Spacer(modifier = Modifier.height(Dimens.dp16))

                when (access.status) {
                    MemberStatus.NotLoggedIn -> {
                        DenialInfo(
                            buttonText = stringResource(id = R.string.btn_login),
                            prompt = stringResource(id = R.string.prompt_login_to_read),
                            onClick = {
                                onClick(BarrierEvent.Login)
                            }
                        )
                    }
                    MemberStatus.Empty -> {
                        val prefix = if (access.content == Permission.PREMIUM) {
                            context.getString(R.string.restricted_to_premium)
                        } else {
                            context.getString(R.string.restricted_to_member)
                        }
                        DenialInfo(
                            buttonText = stringResource(id = R.string.btn_subscribe_now),
                            prompt = "$prefix，${context.getString(R.string.not_subscribed_yet)}",
                            onClick = {
                                onClick(BarrierEvent.Paywall(false))
                            }
                        )
                    }
                    MemberStatus.Expired -> {
                        val prefix = if (access.content == Permission.PREMIUM) {
                            context.getString(R.string.restricted_to_premium)
                        } else {
                            context.getString(R.string.restricted_to_member)
                        }

                        DenialInfo(
                            buttonText = stringResource(R.string.btn_subscribe_now),
                            prompt = "$prefix，${context.getString(R.string.subscription_expired)}",
                            onClick = {
                                onClick(BarrierEvent.Paywall(false))
                            }
                        )
                    }
                    MemberStatus.ActiveStandard -> {
                        if (access.content == Permission.PREMIUM) {
                            DenialInfo(
                                buttonText = stringResource(id = R.string.btn_upgrade_now),
                                prompt = "${context.getString(R.string.restricted_to_premium)}，${context.getString(R.string.current_is_standard)}",
                                onClick = {
                                    onClick(BarrierEvent.Paywall(true))
                                },
                            )
                        }
                    }
                    else -> {

                    }
                }

                if (access.loggedIn) {
                    GotoMySubs(
                        onClick = {
                            onClick(BarrierEvent.MySubs)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Dimens.dp32))
        }

    }
}

@Composable
fun DenialInfo(
    buttonText: String,
    prompt: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        PrimaryButton(
            onClick = onClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = buttonText)
            IconArrowForward()
        }

        Spacer(modifier = Modifier.height(Dimens.dp16))

        BodyText2(
            text = prompt,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Dimens.dp32))
    }
}

@Composable
fun GotoMySubs(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        BodyText2(
            text = stringResource(id = R.string.barrier_sync_membership),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        PlainTextButton(
            onClick = onClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = stringResource(id = R.string.title_my_subs)
            )
            IconArrowForward()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPermissionBarrier_NotLoggedIn() {
    PermissionBarrier(
        access = Access(
            status = MemberStatus.NotLoggedIn,
            rights = Permission.FREE.id,
            content = Permission.STANDARD,
            lang = Language.CHINESE,
        ),
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPermissionBarrier_NotMember() {
    PermissionBarrier(
        access = Access(
            status = MemberStatus.Empty,
            rights = Permission.FREE.id,
            content = Permission.STANDARD,
            lang = Language.CHINESE,
        ),
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPermissionBarrier_Expired() {
    PermissionBarrier(
        access = Access(
            status = MemberStatus.Expired,
            rights = Permission.FREE.id,
            content = Permission.STANDARD,
            lang = Language.CHINESE,
        ),
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPermissionBarrier_Premium() {
    PermissionBarrier(
        access = Access(
            status = MemberStatus.ActiveStandard,
            rights = Permission.STANDARD.id,
            content = Permission.PREMIUM,
            lang = Language.CHINESE,
        ),
        onClick = {}
    )
}
