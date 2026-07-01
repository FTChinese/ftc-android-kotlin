# Android corp and subscribe link migration notes

## Scope

This note documents how iOS keeps FTChinese campaign/corp/subscription journeys inside the native app, what the Android app already has, where Android currently leaks these links, and a migration plan for only:

- FTChinese `/m/corp/...` pages
- `subscribe://...` links

No app code has been changed for this investigation.

## iOS behavior

The iOS entry point is `NewFTCApp-iOS/Page/Web.swift`.

`UIViewController.webView(_:decidePolicyFor:decisionHandler:)` intercepts `WKNavigationType.linkActivated`, calls `openLink(_:)`, and cancels the normal WebView navigation. That makes `openLink` the single router for links tapped inside app WebViews.

Inside `openLink(_:)`, iOS does three important things before any fallback browser behavior:

1. It records campaign attribution with `updateCCode(url.absoluteString)` when a `ccode` parameter exists.
2. It recognizes FTChinese URL patterns with `LinkPattern`.
3. It routes recognized links into native view controllers instead of Safari.

The relevant patterns are in `NewFTCApp-iOS/Page/APIs.swift`:

- `LinkPattern.pagemaker`: `.../m/corp/preview.html?pageid=...`
- `LinkPattern.corp`: `.../m/corp/<slug>.html`
- `LinkPattern.subscription`: `ftacademy.cn/subscription...`
- `subscribe://...`: handled as a URL scheme in the `openLink` switch

For `/m/corp/<slug>.html`, this branch sets `type = "corp"`:

```swift
} else if let contentId = urlString.matchingStrings(regexes: LinkPattern.corp) {
    id = contentId
    type = "corp"
}
```

`corp`, `pagemaker`, `campaign`, `tag`, `channel`, and similar list/page types then call `openDataView(id, of: type, in: urlString)`.

`openDataView` creates a `ContentCollection` and builds:

- a partial/list URL, usually with `bodyonly=yes`
- a full page URL
- `screenName = "\(type)/\(id)"`
- `type = type`

For `corp`, `APIs.getUrl` builds:

```swift
"\(webPageDomain)m/corp/\(id.addUrlEncoding())\(partialParameter)&webview=ftcapp"
```

For `pagemaker`, it builds:

```swift
"\(webPageDomain)m/corp/preview.html\(partialParameter)&pageid=\(id)&webview=ftcapp\(hideAdParameter)"
```

`openDataView` also preserves selected query parameters from the original URL via `combineParameters`, including `ccode`, `p`, `view`, `to`, `membership`, and `action`.

For `subscribe://...`, iOS calls `subscribe(_:)`. The URL shape is:

```text
subscribe://<membership-key>/<price>?ccode=<campaign>&offer=<offer-id>
```

The iOS handler:

- reads the membership key from `url.host`
- finds the product in `IAPProducts.memberships`
- reads `ccode` and `offer`
- requires native login before buying
- saves conversion attribution
- chooses WeChat or Apple IAP via `IAP.getPaymentMethod`
- launches native payment, not Safari

For `https://www.ftacademy.cn/subscription...`, iOS uses `handleSubscriptionLink(_:)`. If the link has `tap=<membership-key>`, it can buy directly. Otherwise it either opens the native membership screen or opens the campaign page inside an app WebView, depending on payment method and offer/campaign context.

The seamless behavior is not only the URL router. `ContentCollection.initWebView` injects native context at document start:

- connection state
- data/image preferences
- privilege information
- user info via `WebviewHelper.getUserInfoScript()`
- follow state

`WebviewHelper.swift` also contains explicit auth-cookie sync/recovery logic for `accessToken`, `refreshToken`, and legacy identity cookies. So iOS keeps the user in an authenticated app WebView, and payment links are intercepted by native code before they can leave the app.

## Android behavior found

The main Android router is already close to the iOS design.

The central pieces are:

- `app/src/main/java/com/ft/ftchinese/ui/web/FtcWebView.kt`
- `app/src/main/java/com/ft/ftchinese/ui/web/FtcWebViewClient.kt`
- `app/src/main/java/com/ft/ftchinese/ui/web/WebViewCallback.kt`
- `app/src/main/java/com/ft/ftchinese/ui/web/channelFromUri.kt`
- `app/src/main/java/com/ft/ftchinese/ui/article/chl/ChannelActivityScreen.kt`
- `app/src/main/java/com/ft/ftchinese/ui/webpage/WebpageScreen.kt`

`FtcWebViewClient.shouldOverrideUrlLoading` already intercepts WebView clicks, syncs the access-token cookie for the target URL, converts the URL to a `WvUrlEvent`, dispatches it through `WebViewCallback`, and returns `true`.

That means links handled by `FtcWebView` should stay native.

`WvUrlEvent.fromUri` currently handles:

- `mailto:` as email
- `ftcregister:` / `weixinlogin:` as native login
- `ftchinese:` as article/native FT routing
- FT article/content URLs as `Article`
- `ftacademy.cn/subscription(.html)` as `FtaSubs`
- internal/trusted FT hosts as `ofInSiteLink`
- everything else as `External`, which opens Custom Tabs

For internal FT links, `ofInSiteLink` routes path segment `m` to:

```kotlin
Channel(marketingChannelFromUri(uri))
```

`marketingChannelFromUri` is already aware of:

```text
/m/marketing/intelligence.html
/m/corp/preview.html?pageid=huawei2018
```

and turns them into `ChannelSource`.

`ChannelActivityScreen` then loads that `ChannelSource` through `FtcWebView`, so follow-up links on those pages can also pass through the native router.

For authentication, Android already mirrors the iOS idea:

- `WebViewAccessTokenCookieManager` writes the native `accessToken` cookie to trusted FT origins.
- `FtcWebView` calls `syncAccessToken` and `syncAccessTokenForUrl`.
- `TemplateBuilder.withUserInfo(account)` injects `androidUserInfo`, `window.userId`, `window.username`, `window.user_name`, and `window.gUserType` into native-rendered HTML.
- `routeWebViewBridgeLink` routes JavaScript bridge links back through `WebViewCallback`.

The native subscription entry point is also present:

- `WvUrlEvent.FtaSubs`
- `WebViewCallback` dispatches it to `SubsActivity.start(context)`
- `PaywallTracker.from` stores campaign attribution
- `SubscriptionCatalogState.loadCatalog` passes `PaywallTracker.campaignCcode()` to `SubscriptionCatalogRepo.fromServer`
- `PaywallActivityScreen` prompts native login if needed and then uses native FTC/Stripe checkout flows

## Android gaps

### 1. `subscribe://` is not a native event

`WvUrlEvent.fromUri` has no `subscribe` scheme branch.

Today `subscribe://premium/1498?ccode=...` falls into:

```kotlin
else -> External(uri)
```

`WebViewCallback.External` calls `launchCustomTabs(context, uri)`. That is the wrong behavior for a custom payment scheme. It either opens the browser/default handler or fails outside the native subscription flow.

### 2. Some campaign entry points bypass the central router

Most content screens use `FtcWebView`, but splash ad clicks use:

- `app/src/main/java/com/ft/ftchinese/ui/main/splash/SplashActivityScreen.kt`
- `app/src/main/java/com/ft/ftchinese/ui/webpage/WebTabScreen.kt`

`WebTabScreen` creates its own `AccompanistWebViewClient`. Its `shouldOverrideUrlLoading` only syncs cookies and then calls `super.shouldOverrideUrlLoading(...)`.

That path does not dispatch to `WvUrlEvent.fromUri`.

So a splash/campaign page opened in `WebTabScreen` can display inside the app initially, but links clicked inside it, including `subscribe://`, are not guaranteed to use the native FT router.

### 3. `/m/corp/...` behavior is broad but under-tested

The main router already maps path segment `m` to `Channel(marketingChannelFromUri(uri))`, which should cover `/m/corp/preview.html?pageid=...` and `/m/corp/<slug>.html`.

However:

- there are no tests proving `/m/corp/<slug>.html` and `/m/corp/preview.html?pageid=...` stay native
- arbitrary corp pages can get blank titles because `pathToTitle` only knows selected page ids
- known tab pages sometimes define `/m/corp/preview.html` as `HTML_TYPE_FRAGMENT`, but `marketingChannelFromUri` uses `HTML_TYPE_COMPLETE` for dynamic links
- splash ad pages may use `WebTabScreen`, which bypasses this router anyway

The practical bug is therefore likely a combination of missing `subscribe://` routing and at least one entry point not using `FtcWebViewClient`.

## Secure migration plan

### Naming note: `subscribe://` vs `subscriber://`

The codebase and web templates currently use `subscribe://`, not `subscriber://`.

Search results:

- iOS handles `subscribe://` in `Web.swift`.
- NEXT and ftcoffer emit `subscribe://premium/...` and `subscribe://standard/...`.
- No shipped `subscriber://` link was found.

Recommendation:

- Treat `subscribe://` as the canonical scheme.
- Optionally accept `subscriber://` as an alias only if marketing content has already shipped with that typo.
- Log alias usage so it can be removed later.

### Security principles

The Android app should copy iOS's native user experience, not iOS's trust model.

Rules:

1. Never trust the price embedded in `subscribe://premium/1498?...`.
2. Never trust `offer` as proof that a user is eligible for a discount.
3. Treat `ccode` as attribution and campaign lookup input, not as an authorization token.
4. Keep all final product, price, discount, and payment eligibility decisions on the server.
5. Native Android should only route, attribute, request a server-verified catalog/quote, and start native checkout from verified data.
6. All `/m/corp/...` and subscription flows must stay inside FT-owned hosts or native screens. Unknown external hosts still go to Custom Tabs.

### Target Android user experience

The desired flow should be:

1. User taps a campaign/splash/article link to `/m/corp/...`.
2. Android opens it inside the app with `FtcWebView`, synced auth cookies, and native account context.
3. User taps a `subscribe://...` link.
4. Android intercepts it before WebView or browser handles it.
5. Android records campaign attribution from `ccode`.
6. Android opens the native subscription flow.
7. The native subscription flow fetches a server-verified catalog/checkout intent using the current native account and attribution.
8. Logged-in users continue directly to native checkout. Logged-out users see the native login prompt and return to the native subscription flow after login.

### Phase 1: unify WebView routing

Make every in-app WebView that can display FT campaign content dispatch clicked links through the same router.

Current strong path:

- `FtcWebView`
- `FtcWebViewClient.shouldOverrideUrlLoading`
- `WvUrlEvent.fromUri`
- `WebViewCallback.onOverrideUrlLoading`

Known weaker path:

- `SplashActivityScreen`
- `WebTabScreen`
- custom `AccompanistWebViewClient`
- `shouldOverrideUrlLoading` syncs cookies and calls `super`

Change `WebTabScreen.shouldOverrideUrlLoading` to match `FtcWebViewClient`:

```kotlin
override fun shouldOverrideUrlLoading(
    view: AndroidWebView?,
    request: WebResourceRequest?
): Boolean {
    val uri = request?.url ?: return super.shouldOverrideUrlLoading(view, request)
    WebViewAccessTokenCookieManager.syncAccessTokenForUrl(view, uri.toString())
    webViewCallback.onOverrideUrlLoading(WvUrlEvent.fromUri(uri))
    return true
}
```

Keep the toolbar "open in browser" action as an explicit user action. It should not determine what happens when the user taps links inside the campaign page.

### Phase 2: route `/m/corp/...` deliberately

Android already routes trusted host path segment `m` to `Channel(marketingChannelFromUri(uri))`. That broadly covers `/m/corp/preview.html?pageid=...` and `/m/corp/<slug>.html`, but it is too implicit and under-tested.

Add explicit classification helpers:

```kotlin
private fun isCorpPreview(uri: Uri): Boolean =
    uri.path == "/m/corp/preview.html" && !uri.getQueryParameter("pageid").isNullOrBlank()

private fun isCorpPage(uri: Uri): Boolean =
    uri.pathSegments.size == 3 &&
    uri.pathSegments[0] == "m" &&
    uri.pathSegments[1] == "corp" &&
    uri.pathSegments[2].endsWith(".html")
```

Recommended routing:

- `/m/corp/preview.html?pageid=...`: `WvUrlEvent.Channel(marketingChannelFromUri(uri))` when the page is a list/channel-like page.
- `/m/corp/<slug>.html`: `WvUrlEvent.CorpPage(uri)` dispatched to `WebpageActivity` with `FtcWebView` when it is a full campaign/terms/landing page.
- Existing known tab pages can stay as `ChannelSource` if they rely on list parsing, cached channel HTML, or native channel navigation.

When building the in-app URL, preserve only an allowlist of query parameters:

```text
ccode
utm_campaign
utm_source
utm_medium
p
page
view
to
membership
action
ad
webview
```

Always append or preserve:

```text
webview=ftcapp
```

Only append:

```text
bodyonly=yes
```

when using the `ChannelSource`/fragment path. Do not force `bodyonly=yes` on full landing pages unless that page is known to support it.

### Phase 3: add a native subscription event

Add a new router event:

```kotlin
data class Subscribe(
    val originalUri: Uri,
    val tier: Tier?,
    val ccode: String?,
    val from: String?,
    val offerHint: String?,
    val priceHint: String?,
    val sourceScheme: String,
) : WvUrlEvent()
```

In `WvUrlEvent.fromUri`, handle:

```kotlin
"subscribe" -> ofSubscribe(uri)
"subscriber" -> ofSubscribe(uri) // optional alias, log usage
```

Parsing:

- `tier`: parse `uri.host` with `Tier.fromString`.
- `priceHint`: read `uri.lastPathSegment`, but never use it for checkout amount.
- `ccode`: read query parameter `ccode`.
- `from`: read query parameter `from`.
- `offerHint`: read query parameter `offer`.
- `attributionId`: use `ccode ?: from`, after validation.

Validate attribution strings before storing:

- max length such as 128 characters
- allow `[A-Za-z0-9._:-]`
- drop or truncate anything else

This protects analytics and downstream API logs from unbounded or weird input.

### Phase 4: preserve attribution securely

Current Android already has:

- `PaywallTracker.from`
- `PaywallTracker.campaignCcode()`
- `SubscriptionCatalogState.loadCatalog(..., PaywallTracker.campaignCcode())`

Extend this instead of adding a second attribution store.

For `subscribe://premium/1498?ccode=foo&offer=bar`, dispatch should set something like:

```kotlin
PaywallTracker.from = PaywallSource(
    id = sanitizedCcodeOrFrom,
    type = "promotion",
    title = "subscribe://$tier",
    category = GACategory.SUBSCRIPTION,
    action = GAAction.DISPLAY,
    label = "subscribe/$tier"
)
```

If `offerHint` is needed by the server catalog, do not overload `PaywallSource.id`. Add a small subscription-entry state object, for example:

```kotlin
data class SubscriptionEntryIntent(
    val tier: Tier?,
    val ccode: String?,
    val offerHint: String?,
    val sourceUri: String,
)
```

Pass it as `SubsActivity` extras or store it in a short-lived in-memory singleton. Prefer intent extras because the process may be killed.

### Phase 5: server-verified catalog and quote

This is the security-critical part.

Android should not convert `subscribe://premium/1498?...` directly to a payable item.

Instead, `SubsActivity` should load the subscription catalog with:

- current user id/access token
- sanitized `ccode`
- requested `tier`
- optional `offerHint`
- app platform/version

The server should return only options the current user may actually buy:

- product tier
- billing cycle
- valid payment methods
- canonical FTC price id / Stripe price id
- display price
- discount amount
- campaign id/ccode actually applied
- eligibility status
- optional short-lived `quoteId`

Best secure contract:

```json
{
  "quoteId": "subq_...",
  "expiresAt": "2026-07-01T12:30:00Z",
  "product": {
    "tier": "premium",
    "cycle": "year"
  },
  "checkout": {
    "ftcPriceId": "plan_...",
    "stripePriceId": "price_...",
    "payableAmount": 1498,
    "currency": "cny",
    "campaignCcode": "foo",
    "offerId": "premium_retention_offer_2025"
  }
}
```

The final order/payment endpoint should accept `quoteId` or a server-known price id and then re-check:

- user identity
- campaign validity window
- lifecycle eligibility, such as new, renew, win-back, retention
- whether the user already used this offer
- product availability on Android
- payment method availability
- final payable amount from server data

If adding `quoteId` is too large for the first iteration, the final order endpoint must still recompute the payable amount from server-side catalog/price/campaign rules and ignore any URL/client price.

### Phase 6: native paywall targeting without unsafe direct checkout

Initial Android behavior should open the native catalog, with a helpful target:

- `tier == premium`: show premium first or scroll/highlight premium.
- `tier == standard`: show standard first or scroll/highlight standard.
- `offerHint`: let the server decide whether this creates a discount option.

Do not auto-start payment from the URL in the first migration.

Later, direct checkout can be added only if:

- the user is logged in
- the catalog returns exactly one eligible option for the entry intent
- the user confirms the native checkout screen
- the final payment endpoint verifies the quote again

The URL path price should remain a display/legacy compatibility hint only.

### Phase 7: login and account continuity

Keep Android behavior consistent with iOS:

- If native account exists, sync `accessToken` cookies before loading corp/subscription WebViews.
- Inject `androidUserInfo` into native-rendered HTML where applicable.
- If `subscribe://` is tapped while logged out, open `AuthActivity`.
- After successful login, return to `SubsActivity` with the original `SubscriptionEntryIntent` intact.
- Reload account and catalog after login.

This avoids a web login prompt inside campaign pages and keeps payment tied to the native account.

### Phase 8: tests

Router tests:

- `subscribe://premium/1498?ccode=android_test` returns `WvUrlEvent.Subscribe`.
- `subscribe://standard/258?from=ft_discount` returns `WvUrlEvent.Subscribe`.
- Optional: `subscriber://premium/1498?ccode=x` returns `WvUrlEvent.Subscribe` only if alias support is enabled.
- Malformed attribution is dropped or sanitized.
- Price path is parsed as `priceHint` only.
- `https://www.ftchinese.com/m/corp/preview.html?pageid=events&ccode=x` returns `Channel`.
- `https://www.ftchinese.com/m/corp/service.html?ad=no` returns `CorpPage` or another in-app event, not `External`.
- `https://evil.example/m/corp/service.html` returns `External`.
- `https://www.ftacademy.cn/subscription.html?ccode=x` still returns `FtaSubs`.

Subscription state tests:

- `PaywallTracker.campaignCcode()` gets sanitized `ccode`.
- `from` is used only when `ccode` is absent.
- `offerHint` does not become a payable discount without server catalog confirmation.
- `SubsActivity` receives target tier and attribution extras.

Manual tests:

1. Splash ad opens `/m/corp/...` inside app.
2. The page can load as a logged-in native user without web login.
3. Tapping `subscribe://premium/1498?ccode=x&offer=y` opens native subscription.
4. The native catalog request contains `ccode=x`, requested tier `premium`, and optional `offerHint=y`.
5. The displayed price comes from server catalog, not from `1498`.
6. Native checkout uses server-provided price/quote.
7. Logged-out user logs in natively and returns to the same subscription intent.
8. Unknown external links still open Custom Tabs.

## Recommended implementation sequence

1. Add router tests for current behavior to expose the gaps.
2. Route `WebTabScreen` through `WvUrlEvent`.
3. Add explicit `/m/corp/...` route helpers and tests.
4. Add `WvUrlEvent.Subscribe` for `subscribe://`, with optional `subscriber://` alias.
5. Add `SubscriptionEntryIntent` extras to `SubsActivity`.
6. Use the entry intent to set `PaywallTracker.from` and target tier.
7. Pass sanitized attribution and offer hints to the catalog request.
8. Ensure checkout/payment endpoints ignore URL price and verify final price server-side.
9. Add manual QA against real campaign pages.

This gives Android the same seamless native journey as iOS while removing the unsafe part of the iOS model: client-controlled price.
