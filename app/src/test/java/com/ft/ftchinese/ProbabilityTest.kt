package com.ft.ftchinese

import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.util.Pair
import org.junit.Test

class ProbabilityTest {
    @Test fun selectAd() {
        val prob = mutableListOf(
                Pair("a", 10.0),
                Pair("b", 20.0),
                Pair("c", 15.0)
        )
        val dist = EnumeratedDistribution(prob)

        val result = dist.sample()

        print(result)
    }
}