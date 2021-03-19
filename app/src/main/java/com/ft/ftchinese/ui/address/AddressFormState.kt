package com.ft.ftchinese.ui.address

enum class AddressField {
    Country,
    Province,
    City,
    District,
    Street,
    Postcode;
}

enum class FormStatus {
    Intact,
    Invalid,
    Changed;
}

data class AddressFormState(
    val error: Int? = null,
    val field: AddressField? = null,
    val status: FormStatus = FormStatus.Intact
)
