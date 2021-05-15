package com.ft.ftchinese.model.splash

import com.ft.ftchinese.model.fetch.KDate
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.util.Pair
import org.jetbrains.anko.AnkoLogger
import org.threeten.bp.LocalDate

/**
 * [TodayAds] contains a list of [ScreenAd] sorted out from
 * [Schedule] for today.
 */
data class TodayAds(
    @KDate
    val date: LocalDate,
    val items: List<ScreenAd>
) : AnkoLogger {

    /**
     * Reference https://stackoverflow.com/questions/9330394/how-to-pick-an-item-by-its-probability
     *
     * We use Apache Math library http://commons.apache.org/proper/commons-math/userguide/distribution.html
     */
    fun pickRandom(): ScreenAd? {
        if (items.isEmpty()) {
            return null
        }

        // Create probability mass function enumerated as a list of <T, probability>
        val pmf = items.map {
            Pair(it, it.weight.toDouble())
        }.toMutableList()

        val distribution = EnumeratedDistribution(pmf)

        return distribution.sample()?.apply {
            this.date = this@TodayAds.date
        }
    }
}
