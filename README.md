## Problems with Android Studio

* As of Android Studio Arctic Fox 2020.3.1 Canary 14, clicking app inspection will crash your app! You need to unplug your device and close kill Android Studio.

## Logo on ActionBar

Reference: https://developer.android.com/training/multiscreen/screendensities

* xhdpi：2.0
* hdpi：1.5
* mdpi：1.0（基准）

Reference: https://stackoverflow.com/questions/15248207/actionbar-logo-size

* drawable-mdpi/ic_logo_wide.png (75 x 32 px)
* drawable-hdpi/ic_logo_wide.png (112 x 48 px)
* drawable-xhdpi/ic_logo_wide.png (149 x 64 px)

## Data Binding

### Nested Layout

If you include a layout inside another layout:

```
<include
    android:id="@+id/toolbar"
    layout="@layout/simple_toolbar" />
```

And the toolbar:

```
<androidx.appcompat.widget.Toolbar xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/toolbar"
    android:layout_width="0dp"
    android:layout_height="?attr/actionBarSize"
    android:elevation="4dp"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toTopOf="parent" />
```

The IDs are chained like this: `binding.toolbar.toolbar`

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

## WebView

User:

```ts
var androidUserInfo = {
    id: string;
    unionId: string | null;
    stripeId: string | null;
    userName: string | null;
    email: string;
    isVerified: boolean;
    avatarUrl: string | null;
    loginMethod: 'email' | 'wechat';
    wechat: {
        nickname: string | null;
        avatarUrl: string | null;
    };
    membership: {
        id: string | null;
        tier: 'standard' | 'premium' | null;
        cycle: 'month' | 'year' | null;
        expireDate: string | null;
        payMethod: 'alipay' | 'wechat' | 'stripe' | 'apple' | 'b2b';
        autoRenew: boolean;
        status: 'incomplete' | 'incomplete_expired' | 'trialing' | 'active' | 'past_due' | 'canceled' | 'unpaid' | null;
        vip: boolean;
    };
}
```

See `model/content/StoryBuilder#withUserInfo` method.

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
 
## Knowledge base

Alipay has a Maven repository `implementation 'com.alipay.sdk:alipay-sdk-java:3.3.49.ALL'`. It's the server-side SDK. DO NOT USE IT for Android!

### Webview

User Agent

```
Mozilla/5.0 (Linux; Android 9; TA-1041 Build/PPR1.180610.011; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/73.0.3683.75 Mobile Safari/537.36

Mozilla/5.0 (Linux; Android 9; TA-1041) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Mobile Safari/537.36
```

### Data binding with `app:srcCompat`

It seems when you are using data binding layout, you cannot use `app:srcCompat` to set images. It won't compile. Use `android:src` instead.

### Data binding with `include` tag

See:

https://medium.com/androiddevelopers/android-data-binding-that-include-thing-1c8791dd6038

https://medium.com/@elia.maracani/android-data-binding-passing-a-variable-to-an-include-d-layout-3567099b58f

## Versions

v5.0.0 -> API v3
