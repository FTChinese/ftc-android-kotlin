# API Endpoint Inventory

整理时间: 2026-03-17

## 范围说明

这份清单基于以下代码：

- Android repo:
  - [app/src/main/java/com/ft/ftchinese/repository/Endpoint.kt](/Users/oliverzhang/sandbox/ftc-android-kotlin/app/src/main/java/com/ft/ftchinese/repository/Endpoint.kt)
  - [app/src/main/java/com/ft/ftchinese/repository/ApiConfig.kt](/Users/oliverzhang/sandbox/ftc-android-kotlin/app/src/main/java/com/ft/ftchinese/repository/ApiConfig.kt)
  - [app/src/main/java/com/ft/ftchinese/repository/AuthClient.kt](/Users/oliverzhang/sandbox/ftc-android-kotlin/app/src/main/java/com/ft/ftchinese/repository/AuthClient.kt)
  - [app/src/main/java/com/ft/ftchinese/repository/AccountRepo.kt](/Users/oliverzhang/sandbox/ftc-android-kotlin/app/src/main/java/com/ft/ftchinese/repository/AccountRepo.kt)
  - [app/src/main/java/com/ft/ftchinese/repository/LinkRepo.kt](/Users/oliverzhang/sandbox/ftc-android-kotlin/app/src/main/java/com/ft/ftchinese/repository/LinkRepo.kt)
  - [app/src/main/java/com/ft/ftchinese/repository/FtcPayClient.kt](/Users/oliverzhang/sandbox/ftc-android-kotlin/app/src/main/java/com/ft/ftchinese/repository/FtcPayClient.kt)
  - [app/src/main/java/com/ft/ftchinese/repository/StripeClient.kt](/Users/oliverzhang/sandbox/ftc-android-kotlin/app/src/main/java/com/ft/ftchinese/repository/StripeClient.kt)
  - [app/src/main/java/com/ft/ftchinese/repository/AppleClient.kt](/Users/oliverzhang/sandbox/ftc-android-kotlin/app/src/main/java/com/ft/ftchinese/repository/AppleClient.kt)
  - [app/src/main/java/com/ft/ftchinese/repository/PaywallClient.kt](/Users/oliverzhang/sandbox/ftc-android-kotlin/app/src/main/java/com/ft/ftchinese/repository/PaywallClient.kt)
  - [app/src/main/java/com/ft/ftchinese/repository/ReleaseRepo.kt](/Users/oliverzhang/sandbox/ftc-android-kotlin/app/src/main/java/com/ft/ftchinese/repository/ReleaseRepo.kt)
- Node repo:
  - [routes/index.js](/Users/oliverzhang/ft/ftcoffer/routes/index.js)
  - [routes/android_index.js](/Users/oliverzhang/ft/ftcoffer/routes/android_index.js)

## 关键结论

- `ftcoffer` 当前把 Android API 挂载在 `/api`，不是 `/api/v6`。
- 下文所有路径都按当前 Node 有效路径写成 `/api/...`。
- 清单分为三类：
  - Android 当前直接调用的接口
  - Node repo 里额外提供，但 Android 当前没有直接调用的接口
  - Android 客户端声明了，但当前 Node repo 里未找到或实现不一致的接口

## 1. Android 当前直接调用的接口

### 1.1 认证与登录

| Method | Path | 用途 |
| --- | --- | --- |
| GET | `/api/auth/email/exists` | 检查邮箱是否已存在 |
| POST | `/api/auth/email/login` | 邮箱密码登录 |
| POST | `/api/auth/email/signup` | 邮箱注册 |
| PUT | `/api/auth/mobile/verification` | 发送手机号验证码，用于手机号登录/注册 |
| POST | `/api/auth/mobile/verification` | 校验手机号验证码并登录 |
| POST | `/api/auth/mobile/link` | 把手机号登录绑定到已有邮箱账号 |
| POST | `/api/auth/mobile/signup` | 用手机号创建账号 |
| POST | `/api/auth/password-reset/letter` | 发送重置密码邮件或验证码 |
| GET | `/api/auth/password-reset/codes` | 校验重置验证码，换取重置会话 |
| POST | `/api/auth/password-reset` | 提交新密码完成重置 |
| POST | `/api/auth/token_sso` | 使用 SSO token 换取 app 账号会话 |
| POST | `/api/auth/wx/login` | 微信登录 |
| POST | `/api/auth/wx/refresh` | 刷新微信会话信息 |

### 1.2 账号与资料

| Method | Path | 用途 |
| --- | --- | --- |
| GET | `/api/account` | 刷新当前账号资料 |
| GET | `/api/account/wx` | 按微信 `unionId` 取账号资料 |
| PATCH | `/api/account/email` | 修改邮箱 |
| POST | `/api/account/email/request-verification` | 重新发送邮箱验证邮件 |
| PATCH | `/api/account/name` | 修改用户名 |
| PATCH | `/api/account/password` | 修改密码 |
| PUT | `/api/account/mobile/verification` | 发送“修改手机号”验证码 |
| PATCH | `/api/account/mobile` | 提交新手机号和验证码，完成换绑 |
| GET | `/api/account/address` | 读取地址 |
| PATCH | `/api/account/address` | 更新地址 |
| DELETE | `/api/account` | 删除账号 |

### 1.3 微信账号绑定

| Method | Path | 用途 |
| --- | --- | --- |
| POST | `/api/account/wx/signup` | 微信用户补邮箱注册 |
| POST | `/api/account/wx/link` | 绑定微信和现有 FT 账号 |
| POST | `/api/account/wx/unlink` | 解绑微信 |

### 1.4 App 版本与支付墙

| Method | Path | 用途 |
| --- | --- | --- |
| GET | `/api/apps/android/latest` | 检查 Android 最新版本 |
| GET | `/api/paywall` | 拉取 paywall 数据 |

### 1.5 订单、支付与会员

| Method | Path | 用途 |
| --- | --- | --- |
| POST | `/api/wxpay/app` | 创建微信 app 支付订单 |
| POST | `/api/alipay/app` | 创建支付宝 app 支付订单 |
| POST | `/api/membership/addons` | 使用会员 addon |
| POST | `/api/orders/:id/verify-payment` | 校验订单支付结果 |

### 1.6 Stripe 与 Apple IAP

| Method | Path | 用途 |
| --- | --- | --- |
| POST | `/api/stripe/customers` | 创建 Stripe customer |
| GET | `/api/stripe/customers/:id` | 获取 Stripe customer |
| GET | `/api/stripe/payment-methods/:id` | 客户端期望按 payment method id 读取详情 |
| POST | `/api/stripe/customers/:id/default-payment-method` | 设置 customer 默认支付方式 |
| POST | `/api/stripe/subs` | 创建 Stripe 订阅 |
| GET | `/api/stripe/subs/:id` | 获取 Stripe 订阅 |
| POST | `/api/stripe/subs/:id/cancel` | 取消 Stripe 订阅 |
| POST | `/api/stripe/subs/:id` | 客户端期望更新 Stripe 订阅 |
| POST | `/api/stripe/subs/:id/refresh` | 客户端期望刷新 Stripe 订阅状态 |
| POST | `/api/stripe/subs/:id/reactivate` | 客户端期望恢复 Stripe 订阅 |
| POST | `/api/stripe/subs/:id/default-payment-method` | 客户端期望设置 subscription 默认支付方式 |
| GET | `/api/stripe/subs/:id/default-payment-method` | 客户端期望读取 subscription 默认支付方式 |
| GET | `/api/stripe/customers/:id/default-payment-method` | 客户端期望读取 customer 默认支付方式 |
| GET | `/api/stripe/subs/:id/latest-invoice/any-coupon` | 读取最近发票是否用了 coupon |
| POST | `/api/stripe/payment-sheet/setup` | 获取 Stripe PaymentSheet 参数 |
| PATCH | `/api/apple/subs/:originalTxId` | 刷新 Apple IAP 订阅状态 |

## 2. Node repo 里额外提供，但 Android 当前没有直接调用的接口

### 2.1 邮箱验证与会话管理

| Method | Path | 用途 |
| --- | --- | --- |
| POST | `/api/auth/email/verification/:token` | 点击邮件验证链接 |
| POST | `/api/auth/token/refresh` | 刷新 session token |
| POST | `/api/auth/logout` | 登出当前 session |
| GET | `/api/auth/sessions` | 列出当前用户活跃 session |
| DELETE | `/api/auth/sessions/:sessionId` | 注销指定 session |
| POST | `/api/auth/sessions/revoke-others` | 注销除当前外的其他 session |

### 2.2 账号扩展接口

| Method | Path | 用途 |
| --- | --- | --- |
| POST | `/api/account/mobile` | 删除手机号绑定 |
| GET | `/api/account/profile` | 读取 profile |
| PATCH | `/api/account/profile` | 更新 profile |

### 2.3 会员、订单与发票

| Method | Path | 用途 |
| --- | --- | --- |
| GET | `/api/membership` | 读取会员信息 |
| GET | `/api/orders` | 订单列表 |
| GET | `/api/orders/:id` | 单个订单详情 |
| GET | `/api/ftc-pay/invoices` | 发票列表 |
| GET | `/api/ftc-pay/invoices/:id` | 单张发票详情 |
| GET | `/api/ftc-pay/discounts/:id` | 折扣使用情况 |

### 2.4 App 版本管理

| Method | Path | 用途 |
| --- | --- | --- |
| GET | `/api/apps/android/releases` | 版本列表 |
| GET | `/api/apps/android/releases/:versionName` | 单个版本详情 |
| POST | `/api/apps/android/releases` | 创建版本记录 |
| PATCH | `/api/apps/android/releases/:versionName` | 更新版本记录 |
| DELETE | `/api/apps/android/releases/:versionName` | 删除版本记录 |
| GET | `/api/__version` | 服务版本或状态信息 |

### 2.5 Paywall 与支付入口扩展

| Method | Path | 用途 |
| --- | --- | --- |
| GET | `/api/paywall/active/prices` | 当前生效价格列表 |
| POST | `/api/paywall/banner` | 保存 paywall banner |
| POST | `/api/wxpay/desktop` | 微信桌面支付入口 |
| POST | `/api/wxpay/mobile` | 微信移动网页支付入口 |
| POST | `/api/wxpay/jsapi` | 微信 JSAPI 支付入口 |
| POST | `/api/alipay/desktop` | 支付宝桌面支付入口 |
| POST | `/api/alipay/mobile` | 支付宝移动支付入口 |
| POST | `/api/webhook/wxpay` | 微信支付 webhook |
| POST | `/api/webhook/alipay` | 支付宝 webhook |

### 2.6 Stripe 管理与价格接口

| Method | Path | 用途 |
| --- | --- | --- |
| POST | `/api/stripe/payment-methods/add` | 绑定 payment method 到 customer |
| GET | `/api/stripe/payment-methods/:customerId` | 列出 customer 的 payment methods |
| DELETE | `/api/stripe/payment-methods/:paymentMethodId` | 删除 payment method |
| POST | `/api/stripe/payment-methods/:customerId/default` | 为 customer 设置默认 payment method |
| GET | `/api/stripe/prices` | 读取 Stripe 价格列表 |
| GET | `/api/stripe/prices/paywall` | 读取 paywall 用价格列表 |
| GET | `/api/stripe/prices/:id` | 读取单个价格 |
| POST | `/api/stripe/prices/:id/activate` | 激活价格 |
| POST | `/api/stripe/prices/:id/deactivate` | 停用价格 |
| GET | `/api/stripe/prices/:id/coupons` | 读取价格对应优惠券 |
| GET | `/api/stripe/publishable-key` | 获取 Stripe publishable key |

## 3. Android 已声明，但当前 Node repo 未找到或实现不一致

### 3.1 Node repo 未找到的接口

| Method | Path | 说明 |
| --- | --- | --- |
| PATCH | `/api/apple/subs/:originalTxId` | Android 客户端声明了 Apple IAP 刷新接口，但当前 `ftcoffer` 未找到路由 |
| POST | `/api/stripe/subs/:id/refresh` | Android 客户端声明了，但当前 `ftcoffer` 未找到路由 |
| POST | `/api/stripe/subs/:id` | Android 客户端声明了“更新订阅”，但当前 `ftcoffer` 未找到路由 |
| POST | `/api/stripe/subs/:id/reactivate` | Android 客户端声明了，但当前 `ftcoffer` 未找到路由 |
| POST | `/api/stripe/subs/:id/default-payment-method` | Android 客户端声明了，但当前 `ftcoffer` 未找到路由 |
| GET | `/api/stripe/subs/:id/default-payment-method` | Android 客户端声明了，但当前 `ftcoffer` 未找到路由 |
| GET | `/api/stripe/customers/:id/default-payment-method` | Android 客户端声明了只读接口，但当前 `ftcoffer` 只有对应的 `POST` 更新接口 |
| GET | `/api/stripe/subs/:id/latest-invoice/any-coupon` | Android 客户端声明了，但当前 `ftcoffer` 未找到路由 |

### 3.2 路径存在，但语义或请求格式不一致

| Android 期望 | Node 当前实现 | 差异 |
| --- | --- | --- |
| `GET /api/stripe/payment-methods/:id` | `GET /api/stripe/payment-methods/:customerId` | Android 把 `:id` 当 payment method id 使用；Node 把它当 customer id，返回的是列表而不是单条 payment method |
| `POST /api/stripe/customers/:id/default-payment-method`，请求体字段 `defaultPaymentMethod` | 同一路径存在，但 Node 当前读取请求体字段 `paymentMethodId` | 请求体字段名不一致 |

## 4. 备注

- Android repo 里还有少量旧接口或外部接口，不属于当前 Node Android API 范围，因此未并入上表：
  - `https://www.chineseft.net/index.php/jsapi/...`
  - Google conversion tracking
  - `${refer}/engagement.php`
- 如果后续要做迁移排查，这份文档可直接作为：
  - Android 现有依赖清单
  - Node 已承接接口清单
  - Android 与 Node 不一致清单
