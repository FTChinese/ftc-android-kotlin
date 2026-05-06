# Android API Endpoint Inventory

整理时间: 2026-05-06

## 范围

这份清单基于 Android 端 `app/src/main/java/com/ft/ftchinese/repository/` 下的 `Endpoint.kt`、`ApiConfig.kt` 和各 `*Client.kt`，并对照 `ftcoffer/routes/android_index.js` 当前挂载在 `/api` 下的 NodeJS 路由。

Android 现在有两种登录/授权模式：

- legacy: `Authorization: Bearer <app token>` 加 `X-User-Id` 或 `X-Union-Id`
- new/shared session: `Authorization: Bearer <session token>` 加 `X-User-Id` 和 `X-Device-Id`

`ftcoffer` 的 Android API contract 测试位于 `ftcoffer/unit-test/android_native_api_contract.test.js`。

## Android 当前依赖的 NodeJS `/api` 接口

### 认证与登录

| Method | Path | Android 调用点 | 期望成功响应 |
| --- | --- | --- | --- |
| GET | `/api/auth/email/exists` | `AuthClient.emailExists` | 204 表示存在；404 JSON 表示不存在 |
| POST | `/api/auth/email/login` | `AuthClient.emailLogin` | `Account`，新模式额外带 `session`/`X-Session-Token` |
| POST | `/api/auth/email/signup` | `AuthClient.emailSignUp` | `Account` |
| PUT | `/api/auth/mobile/verification` | `AuthClient.requestSMSCode` | 204 |
| POST | `/api/auth/mobile/verification` | `AuthClient.verifySMSCode` | `Account` |
| POST | `/api/auth/mobile/link` | `AuthClient.mobileLinkExistingEmail` | `Account` |
| POST | `/api/auth/mobile/signup` | `AuthClient.mobileSignUp` | `Account` |
| POST | `/api/auth/password-reset/letter` | `AuthClient.passwordResetLetter` | 204 |
| GET | `/api/auth/password-reset/codes` | `AuthClient.verifyPwResetCode` | `PwResetBearer`，必须有 `token` alias |
| POST | `/api/auth/password-reset` | `AuthClient.resetPassword` | 204 |
| POST | `/api/auth/token_sso` | `AuthClient.ssoLogin` | `Account` |
| POST | `/api/auth/wx/login` | `AuthClient.wxLogin` | `WxSession`，可带 shared session 扩展字段 |
| POST | `/api/auth/wx/refresh` | `AccountRepo.refreshWxInfo` | 204 或 200 |

### 账号与资料

| Method | Path | Android 调用点 | 期望成功响应 |
| --- | --- | --- | --- |
| GET | `/api/account` | `AccountRepo.loadFtcAccount` | `Account` |
| GET | `/api/account/wx` | `AccountRepo.loadWxAccount` | `Account` |
| PATCH | `/api/account/email` | `AccountRepo.updateEmail` | `BaseAccount` 兼容对象 |
| POST | `/api/account/email/request-verification` | `AccountRepo.requestVerification` | 204 |
| PATCH | `/api/account/name` | `AccountRepo.updateUserName` | `BaseAccount` 兼容对象 |
| PATCH | `/api/account/password` | `AccountRepo.updatePassword` | 204 |
| PUT | `/api/account/mobile/verification` | `AccountRepo.requestSMSCode` | 204 |
| PATCH | `/api/account/mobile` | `AccountRepo.updateMobile` | `BaseAccount` 兼容对象 |
| GET | `/api/account/address` | `AccountRepo.loadAddress` | `Address` |
| PATCH | `/api/account/address` | `AccountRepo.updateAddress` | `Address` |
| DELETE | `/api/account` | `AccountRepo.deleteAccount` | 204 |

### 微信账号绑定

| Method | Path | Android 调用点 | 期望成功响应 |
| --- | --- | --- | --- |
| POST | `/api/account/wx/signup` | `LinkRepo.signUp` | `Account` |
| POST | `/api/account/wx/link` | `LinkRepo.link` | 204 |
| POST | `/api/account/wx/unlink` | `LinkRepo.unlink` | 204 |

### App、推送、订阅展示

| Method | Path | Android 调用点 | 期望成功响应 |
| --- | --- | --- | --- |
| POST | `/api/push/register` | `PushClient.registerPush` | push registration JSON |
| GET | `/api/apps/android/latest` | `ReleaseRepo.getLatest` | `AppRelease` |
| GET | `/api/paywall` | `PaywallClient.retrieve` | `Paywall` |
| GET | `/api/subscription/catalog` | `SubscriptionCatalogClient.retrieve` | `SubscriptionCatalog` |
| GET | `/api/subscription/summary` | `ApiConfig.subscriptionSummary` 声明，当前未直接调用 | `SubscriptionCatalogSummary` |

### 订单、支付与会员

| Method | Path | Android 调用点 | 期望成功响应 |
| --- | --- | --- | --- |
| POST | `/api/wxpay/app` | `FtcPayClient.createWxOrder` | `WxPayIntent`，`params.app` 不可为 null |
| POST | `/api/alipay/app` | `FtcPayClient.createAliOrder` | `AliPayIntent` |
| POST | `/api/membership/addons` | `FtcPayClient.useAddOn` | `Membership` |
| POST | `/api/orders/:id/verify-payment` | `FtcPayClient.verifyOrder` | `VerificationResult` |

### Stripe 与 Apple IAP

| Method | Path | Android 调用点 | 期望成功响应 |
| --- | --- | --- | --- |
| POST | `/api/stripe/customers` | `StripeClient.createCustomer` | `StripeCustomer` |
| GET | `/api/stripe/customers/:id` | `StripeClient.retrieveCustomer` | `StripeCustomer` |
| GET | `/api/stripe/payment-methods/:id` | `StripeClient.retrievePaymentMethod` | 单个 `StripePaymentMethod` |
| POST | `/api/stripe/customers/:id/default-payment-method` | `StripeClient.setCusDefaultPayment` | `StripeCustomer`；支持 Android 的 `defaultPaymentMethod` 字段 |
| GET | `/api/stripe/customers/:id/default-payment-method` | `StripeClient.cusDefaultPaymentMethod` | `StripePaymentMethod` |
| POST | `/api/stripe/subs` | `StripeClient.createSubscription` | `StripeSubsResult` |
| GET | `/api/stripe/subs/:id` | `StripeClient.loadSubscription` | `StripeSubs` |
| POST | `/api/stripe/subs/:id/cancel` | `StripeClient.cancelSub` | `StripeSubsResult` |
| POST | `/api/stripe/subs/:id` | `StripeClient.updateSubs` | `StripeSubsResult` |
| POST | `/api/stripe/subs/:id/refresh` | `StripeClient.refreshSub` | `StripeSubsResult` |
| POST | `/api/stripe/subs/:id/reactivate` | `StripeClient.reactivateSub` | `StripeSubsResult` |
| POST | `/api/stripe/subs/:id/default-payment-method` | `StripeClient.setSubsDefaultPayment` | `StripeSubs`；支持 Android 的 `defaultPaymentMethod` 字段 |
| GET | `/api/stripe/subs/:id/default-payment-method` | `StripeClient.subsDefaultPaymentMethod` | `StripePaymentMethod` |
| GET | `/api/stripe/subs/:id/latest-invoice/any-coupon` | `StripeClient.loadCouponApplied` | `CouponApplied` |
| POST | `/api/stripe/payment-sheet/setup` | `StripeClient.setupWithEphemeral` | `PaymentSheetParams` |
| PATCH | `/api/apple/subs/:originalTxId` | `AppleClient.refreshIAP` | `IAPSubsResult` |

## 外部或旧系统接口

这些仍在 Android 源码中出现，但不属于当前 NodeJS `/api` 迁移范围：

| 调用点 | URL 来源 | 用途 |
| --- | --- | --- |
| `ArticleClient.crawlFile` | 文章、频道、CDN 或 WebView 传入 URL | 拉取 HTML/内容 |
| `AdClient.fetchSchedule` | `${HostConfig}/index.php/jsapi/applaunchschedule` | 启动页广告 |
| `AdClient.sendImpression` | 广告素材返回的 impression URL | 广告曝光 |
| `ConversionClient.getConversion` | Google conversion 或 `chineseft.net` 测试接口 | 转化追踪 |
| `ConversionClient.listCampaigns` | `https://www.chineseft.net/index.php/jsapi/deeplinkcampaign` | deep link campaign |
| `AuthClient.engaged` | `${refer}/engagement.php` | 阅读时长 |
| `AccountRepo.loadWxAvatar` | 微信头像 URL | 头像下载 |

## 本次检查修复项

- 新增 NodeJS `PATCH /api/apple/subs/:originalTxId`，返回 Android 可解析的 `IAPSubsResult`。
- Android `AppleClient.refreshIAP` 改为发送 `X-User-Id` 并使用 session-aware auth，兼容 legacy fallback 和新 shared session。
- `PATCH /api/account/email` 从 `{ message }` 改为返回 Android 可解析的账号对象。
- `PATCH /api/account/address` 从 204 改为返回 Android 可解析的 `Address`。
- `POST /api/stripe/customers/:id/default-payment-method` 和 `POST /api/stripe/subs/:id/default-payment-method` 覆盖 Android 现用字段 `defaultPaymentMethod`。
- `GET /api/stripe/payment-methods/:id` 覆盖 Android 现用语义：按 payment method id 返回单个 payment method。

## 测试命令

在 `ftcoffer` 目录下运行全部 Android API contract tests：

```bash
npm run test:android-api
```

只跑某一个接口测试，用 Jest 的 `-t`：

```bash
npm run test:android-api -- -t "PATCH /api/account/address"
```

