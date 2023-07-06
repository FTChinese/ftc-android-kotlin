# Build

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

The files are here at our Beijing Office, /tech/android-release/, unzip it and copy to the root and app folders

# Product Design

## Splash Screen

Android 12 start providing Splash Screens. We implemented our custom one till version 5.1.

## Login

### Wechat Login Workflow

#### Step 1: Launch Wechat

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

#### Step 2: Get Code in WxEntryActivity

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

#### Step 3: Ask API for Access Token

Post the `code` to API, and API uses the code to ask for Wechat API for access token.

Access token, refresh token should not be saved by client.

#### Step 4: Use `unionid` to Get User Data

### Mobile Login Workflow

## Delete Account

* Placed on the app bar menu of AccountActivity;
* When clicked, pop up a dialog to warn user;
* After confirmed, show a new page to verify password;
* If password verified and no errors returned, the account is deleted;
* If errors returned and the errors is subscription_already_exists, it indicates a valid membership exists under this account. User should contact customer service.
* In such case a dialog is shown with option to send a pre-composed email containing user's email or phone.
* For mobile-only user, we cannot verify its email; thus contains only mobile number.

## Payment

### Test

* To test Alipay/Wechat in production, use a test account on any of the release bundle;
* To test Wechat OAuth in development, use releaseWechat.
* In development environment on debugXXXX bundle, user any account you have on your local server. Wechat SDK could not be tested in such environment. It only works on a release bundle.

### Barrier

If a non-logged-in, or non-subscribed user clicked a article behind paywall, a bottom sheet dialog is shown blocking the content. This dialog provides links to login or payment.

### Paywall

Paywall data should be retrieved for both FTC prices and Stripe Prices

### Alipay

Alipay has a Maven repository `implementation 'com.alipay.sdk:alipay-sdk-java:3.3.49.ALL'`. It's the server-side SDK. DO NOT USE IT for Android!

### Wechat Pay

TODO

### Stripe Integration

Refer to https://stripe.com/docs/payments/accept-a-payment?platform=android

Do not use the payment intent workflow. Use setup intent instead.

1. To present Stripe SDK's prebuilt UI, you should get those data from server-side:
    * [Setup intent](https://stripe.com/docs/api/setup_intents) client secret, 
    * ephemeral key secret, 
    * customer id, 
    * publishable key.

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

## Image Assets

Do not edit images used as app icon manually. Follow this guide to create them using Android Studio:

https://developer.android.com/studio/write/image-asset-studio

NOTE: when generating icons using Image Asset Studio, you are recommended to delete existing ones if you mean to override them. Android Studio cannot properly handle file overridden, even though it claims it will override files with the same name.

# Issues

## Protobuf

Usage guide on protobuf is scattered on the web. The official documentation failed to includes any working configuration.

You have to refer to:

* https://medium.com/supercharges-mobile-product-guide/new-way-of-storing-data-in-android-jetpack-datastore-a1073d09393d
* https://github.com/google/protobuf-gradle-plugin

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

### Webview

User Agent

```
Mozilla/5.0 (Linux; Android 9; TA-1041 Build/PPR1.180610.011; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/73.0.3683.75 Mobile Safari/537.36

Mozilla/5.0 (Linux; Android 9; TA-1041) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Mobile Safari/537.36
```

## Versions

* v5.0.0 -> API v3.0.0. Price could define multiple discounts.
* v5.2.0 -> API v3.3.0. Account could be deleted.

------------------------

## App内使用到的权限

```
android.permission.INTERNET
android.permission.ACCESS_NETWORK_STATE
android.permission.ACCESS_WIFI_STATE
```
支付宝要用，但是这些权限似乎是系统默认的，不需要弹窗申请，弹窗申请会崩溃。

```
android.permission.REQUEST_INSTALL_PACKAGES
```

App内更新需要。

```
android.permission.READ_EXTERNAL_STORAGE
android.permission.WRITE_EXTERNAL_STORAGE
```

全屏截图分享到微信需要。由于Google限制直接分享文件超过512KB，导致微信SDK不能分享全屏截图。绕开限制的方式是先存储截图到本机相册，微信分享时再读取。

## 3rd party SDK Usage

### ApacheHttp

工具类 https://hc.apache.org/

这应该不是我们app里的东西。Vivo的检测是错误的。我们的确使用到了Apache的一个库，但不是这个，也不需要任何权限，见下。

### BouncyCastle

安全风控类 http://www.bouncycastle.org/documentation.html

没见过，不知道vivo从哪里搞到的。

### Markwon

https://github.com/noties/Markwon

Markwon is a markdown library for Android. It parses markdown following commonmark-spec with the help of amazing commonmark-java library and renders result as Android-native Spannables. No HTML is involved as an intermediate step. No WebView is required. It's extremely fast, feature-rich and extensible.

把纯文本转换成原生控件能显示的字符格式。

### Byte Units

https://github.com/JakeWharton/byteunits

把计算机表示的数字，如1024等，转换成人类易读的格式，如1kb。

### ThreeTen

https://github.com/ThreeTen/threetenbp

计算日期时间的工具。Java 8中日期时间标准库，但是安卓使用的Java版本过于古老，没有此功能，该项目用于为安卓的Java提供同等功能。

### Commons Math

The Apache Commons Mathematics Library

https://commons.apache.org/proper/commons-math/

Commons Math is a library of lightweight, self-contained mathematics and statistics components addressing the most common problems not available in the Java programming language or Commons Lang.

数学计算工具而已。

### Okhttp3

工具类 https://github.com/square/okhttp Square公司 旧金山(美国) 境外

访问网络的工具，它应该不需要权限，这也是Google官方推荐的网络访问库。

### 微信开放平台

支付类

https://open.weixin.qq.com

腾讯科技(深圳)有限公司 深圳市

#### 权限

微信文档 https://developers.weixin.qq.com/doc/oplatform/Mobile_App/Access_Guide/Android.html "在代码中使用开发工具包"：

> 添加必要的权限支持（其中网络权限如果没有使用扫码登录功能非必要；后三个权限，如果没有使用 mta，也非必要，即使有使用 mta，去掉也不影响功能）：

```
<uses-permission android:name="android.permission.INTERNET" />

<!-- for mta statistics, not necessary-->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

根据文档说明，我们用不到这些权限，所以没有添加。

### 设备标识生成库

工具类

https://docs.open.alipay.com/204/105296/

支付宝(中国)网络技术有限公司 上海市

### Stripe

支付类

https://stripe.com/docs/mobile/android

#### 权限

1 读取联系人(2) 高敏 仅SDK 冗余权限

这个权限我们没申请过也没用过

### 支付宝

支付类

https://www.alipay.com

蚂蚁金服(杭州)网络技术有限公司 杭州市

### 支付宝安全

安全风控类 https://open.alipay.com/ 蚂蚁金服(杭州)网络技术有限公司 杭州市

#### 权限

1 读取外置存储卡(2) 高敏
2 写入外部存储卡(2) 高敏

文档：https://opendocs.alipay.com/open/204/105296/#%E8%BF%90%E8%A1%8C%E6%9D%83%E9%99%90

```
android.permission.INTERNET
android.permission.ACCESS_NETWORK_STATE
android.permission.ACCESS_WIFI_STATE
```

系统似乎默认启用这些三个权限，明确申请会崩溃。

### 支付宝客户端支付安全

安全风控类 https://open.alipay.com/ 蚂蚁金服(杭州)网络技术有限公司 杭州市
