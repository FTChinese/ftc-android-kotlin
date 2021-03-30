package com.ft.ftchinese.ui.paywall

const val paywallGuide = """
## 说明

### 订阅方式

FTC中文网订阅方式分为一次性购买与自动续订。通过支付宝/微信购买的为一次性购买，通过Stripe或苹果内购为订阅模式。

### 关于Stripe订阅

* Stripe为自动续订，到期扣款；
* FT中文网的Stripe支付需使用国际信用卡，如果您的信用卡上只有银联标记，可能无法使用；
* 微信登录的用户需要绑定FT中文网账号后才可以使用Stripe订阅。

### 关于苹果应用内订阅

* 苹果应用内购买属于自动续订，即到期扣款；
* 苹果内购由苹果公司管理，FT中文网只有您的订阅信息。如果需要关闭自动续订，请在您的苹果设备上操作：打开AppStore，点击您的账号，选择“订阅”项；
* 标准版升级到高端版也请在苹果设备上的FT中文网App内操作。

### 关于微信/支付宝购买

* 微信和支付宝购买属于一次性购买，订阅到期自动停止，您可以多次购买，购买的订阅期限累加。

### 自动续订期间与一次性购买之间的切换

* 通过支付宝/微信购买且会员尚未到期，如果选择Stripe订阅，我们会把您的当前的剩余天数保留至Stripe订阅失效后重新启用；
* 通过Stripe/苹果内购订阅，可以使用微信/支付宝提供的一次性购买，购买的订阅期限将在自动续订失效后启用；
* 采用Stripe/苹果内购订阅的标准会员，如需升级高端会员请使用原始订阅渠道升级。

### 关于企业版订阅

* 此处的所有支付方式均属于个人订阅，企业版请联系您的机构管理人员；
* 企业版订阅的个人可以在此采取一次性购买，购买的订阅期限将在企业版到期后启用。
"""
