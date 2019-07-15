package com.ft.ftchinese.model.order

import org.junit.Test
import org.threeten.bp.ZonedDateTime
import java.util.*

class IdempotencyTest {
    @Test fun stale() {
        val i = Idempotency(
                key =  UUID.randomUUID().toString(),
                created = ZonedDateTime.now().plusHours(-25)
        )

        println("Stale ${i.stale()}")
    }
}
