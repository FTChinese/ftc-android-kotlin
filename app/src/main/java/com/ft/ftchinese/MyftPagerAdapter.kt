package com.ft.ftchinese

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.ft.ftchinese.models.MyftTab
import com.ft.ftchinese.models.MyftTabId

class MyftPagerAdapter(
        fm: FragmentManager
) : FragmentStatePagerAdapter(fm) {

    private val tabs = listOf(
            MyftTab(id = MyftTabId.READ, title = "阅读历史"),
            MyftTab(id = MyftTabId.STARRED, title = "收藏文章"),
            MyftTab(id = MyftTabId.FOLLOWING, title = "关注")
    )

    override fun getItem(position: Int): Fragment {

        val tab = tabs[position]

        return when (tab.id) {
            MyftTabId.READ -> ReadArticleFragment.newInstance()
            MyftTabId.STARRED -> StarredArticleFragment.newInstance()
            MyftTabId.FOLLOWING -> FollowingFragment.newInstance()
        }
    }

    override fun getCount(): Int {
        return tabs.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return tabs[position].title
    }
}