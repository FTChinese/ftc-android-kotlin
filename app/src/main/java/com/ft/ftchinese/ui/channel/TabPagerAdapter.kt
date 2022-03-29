package com.ft.ftchinese.ui.channel

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.content.ChannelSource

/**
 * A [FragmentStatePagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/mPages.
 */
class TabPagerAdapter(
        private var pages: List<ChannelSource>,
        fm: FragmentManager
) : FragmentStatePagerAdapter(
        fm,
        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) {

    override fun getItem(position: Int): Fragment {

        return ChannelFragment.newInstance(pages[position])
    }

    override fun getCount(): Int {
        return pages.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return pages[position].title
    }
}
