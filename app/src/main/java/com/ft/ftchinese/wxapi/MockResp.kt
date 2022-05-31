package com.ft.ftchinese.wxapi

import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseResp

class MockResp : BaseResp() {
    val errCode = BaseResp.ErrCode.ERR_OK

    override fun getType(): Int {
        return ConstantsAPI.COMMAND_PAY_BY_WX
    }

    override fun checkArgs(): Boolean {
        return true
    }
}
