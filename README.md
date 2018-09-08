## Test locally against API

1. Install MySQL.
2. Use `https://gitlab.com/ftchinese/sql-schema` to setup schema and populate data.
3. Install Golang
4. Clone this repository `https://gitlab.com/ftchinese/next-api` into golang's designated path, which should be `~/go/src/gitlab.com/ftchinese`.
5. `cd` into `~/go/src/gitlab.com/ftchinese/next-api`
6. Execute `make`.
7. Use [Serveo](https://serveo.net/) for forward external request to your localhost: `ssh -R 80:localhost:8000 serveo.net`
8. Change `private const val BASE = "https://lacerta.serveo.net"` in this package `util/ApiEndpoint` to the URL Serveo gives you.

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

## Server-side App Launch Ad API

* It should use HTTP header (like etag) to tell client whether the resource is outdated and client should update local data;

* Use JSON. Use meaningful JSON.

DO NOT USE strings `yes` or `no` to indicate Boolean values! Use `true` or `false` please.

DO NOT USE strings for numeric values! Use number as number. Why convert numbers to strings and force client to do the conversion back?

DO NOT USE strings for array. If you mean an array, use an array. DO NOT USE a dot/comma...-separated string as array.

JSON keys should indicated the meaning of their value.


## Keystore File

In the root of this project, add a file `keystore.properties` and add this line in it: `wechat.appId="your wechat app id"`.

## Problems

It seems Kotlin coroutine does not work as expected. Even commonpool blocks the main thread.

`checkAd` launched on commonpool blocks the main thread. A network error blocks the following execution.