package com.ft.ftchinese.ui.validator

// A predicate is a function that evaluates to true when its params matches the condition of the predicate.
// When returning true, it indicates the param is not valid.
typealias Predicate = (value: String?) -> Boolean
