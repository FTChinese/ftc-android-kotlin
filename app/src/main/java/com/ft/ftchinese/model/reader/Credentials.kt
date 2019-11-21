package com.ft.ftchinese.model.reader

data class Credentials(
        val email: String,
        val password: String,
        val deviceToken: String
)  {

//    fun login(): String? {
//        val (_, body) = Fetch()
//                .post(NextApi.LOGIN)
//                .setClient()
//                .noCache()
//                .jsonBody(json.toJsonString(this))
//                .responseApi()
//
//        return if (body == null) {
//            null
//        } else {
//            decodeUserId(body)
//        }
//    }

//    private fun decodeUserId(json: String): String? {
//        return try {
//            JSONObject(json).getString("id")
//        } catch (e: JSONException) {
//            info(e)
//            null
//        }
//    }

    /**
     * If unionId is null, it indicates this is a regular registration;
     * If unionId is not null, it indicates a
     * wechat-logged-in user is trying to create a new
     * account and this new account should be bound to this
     * wechat account after creation.
     */
//    fun signUp(unionId: String? = null): String? {
//        val fetch = if (unionId != null) {
//            Fetch().post(NextApi.WX_SIGNUP)
//                    .setUnionId(unionId)
//        } else {
//            Fetch().post(NextApi.SIGN_UP)
//        }
//
//
//        val (_, body) = fetch
//                .setClient()
//                .noCache()
//                .jsonBody(json.toJsonString(this))
//                .responseApi()
//
//        return if (body == null) {
//            null
//        } else {
//            decodeUserId(body)
//        }
//    }
}
