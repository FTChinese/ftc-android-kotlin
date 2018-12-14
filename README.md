## Logo on ActionBar

Reference: https://developer.android.com/training/multiscreen/screendensities

* xhdpi：2.0
* hdpi：1.5
* mdpi：1.0（基准）

Reference: https://stackoverflow.com/questions/15248207/actionbar-logo-size

* drawable-mdpi/ic_logo_wide.png (75 x 32 px)
* drawable-hdpi/ic_logo_wide.png (112 x 48 px)
* drawable-xhdpi/ic_logo_wide.png (149 x 64 px)

## Localization

```
<resource type>-b+<language code>[+<country code>]
```

The locale is a combination of the language and the country. The language is defined by the [ISO 639-1](https://en.wikipedia.org/wiki/ISO_639-1) standard while the country is defined by the [ISO 3166-1](https://en.wikipedia.org/wiki/ISO_3166-1) standard.


Locale change is not easy to implement at runtime. Just prepare localized resources and leave it to the system to handle.

### Localize Chinese Language

```
values-b+zh+CN
values-b+zh+TW
values-b+zh+HK
values-b+zh+MO
```

## Files

Add those two files in the root of the project:

`config.properties`

```
wechat.appId="......."
access_token="......."
```

`keystore.properties`

```
storePassword="......"
keyPassword="......"
keyAlias="......."
storeFile="......."
```

Add `google-service.json` to the `app` directory.

## Wechat Login Process

### Step 1: Launch Wechat
In the `SignInOrUpFragment`, declare a property `private var wxApi: IWXAPI? = null`. Initialize wechat api in `onCreate`:

```kotlin
wxApi = WXAPIFactory.createWXAPI(context, your_app_id)
wxApi?.registerApp(your_app_id)
```

Respond to click on wechat login button:
```kotlin
wechat_sign_in_button.setOnClickListener {
    val nonce = generateNonce(5)
    val req = SendAuth.Req()
    req.scope = "snsapi_userinfo"
    req.state = nonce
    wxApi?.sendReq(req)
}
```

Then user will be redirected to wechat app.

### Step 2: Get Code in WxEntryActivity

Wechat will redirect user back to the `WxEntryActivity` in your app after user clicked `确认登陆` button.

You should handle the response in `onResp()` function.
```kotlin
override fun onResp(resp: BaseResp?) {
    when (resp?.type) {
        ConstantsAPI.COMMAND_SENDAUTH -> {
            info("Wx auth")
            // process login here
        }
        ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX -> {
            finish()
        }
        else -> {
            finish()
        }
    }
}
```

In such case, the `resp` is type `SendAuth.Resp` -- the actual meaning of `SDK通过SendAuth的Resp返回数据给调用方` in Wechat's official documentation.

Then you need to distinguish the response's `ErrCode` first:

* `ERR_OK = 0`(用户同意) 
* `ERR_AUTH_DENIED = -4`（用户拒绝授权） 
* `ERR_USER_CANCEL = -2`（用户取消）

In case `ERR_OK = 0`, cast the `BaseResp` into `SendAuth.Resp`. Only with `SendAuth.Resp` could you get the `code`, `state`, `lang` and `country` fields.

### Step 3: Ask API for Access Token

Post the `code` to API, and API uses the code to ask for Wechat API for access token.

Access token, refresh token should not be saved by client.

### Step 4: Use `unionid` to Get User Data
 
## Problems

Alipay has a Maven repository `implementation 'com.alipay.sdk:alipay-sdk-java:3.3.49.ALL'`. It's the server-side SDK. DO NOT USE IT for Android!