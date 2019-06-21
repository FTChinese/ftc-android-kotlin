package com.ft.ftchinese.ui.channel

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.ChannelSource
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * A [FragmentStatePagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/mPages.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class TabPagerAdapter(
        private var pages: Array<ChannelSource>,
        fm: FragmentManager
) : FragmentStatePagerAdapter(
        fm,
        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
), AnkoLogger {

    override fun getItem(position: Int): Fragment {
        if (BuildConfig.DEBUG) {
            info("TabPagerAdapter getItem $position. Data passed to ChannelFragment: ${pages[position]}")
        }

        return ChannelFragment.newInstance(pages[position])
    }

    override fun getCount(): Int {
        return pages.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return pages[position].title
    }
}
