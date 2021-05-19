package com.ft.ftchinese.ui.article

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentScreenshotBinding
import com.ft.ftchinese.ui.base.ScopedBottomSheetDialogFragment
import com.ft.ftchinese.ui.share.SocialApp
import com.ft.ftchinese.ui.share.SocialAppId
import com.ft.ftchinese.ui.share.SocialShareViewHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.io.File

/**
 * A simple [Fragment] subclass.
 * Use the [ScreenshotFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@ExperimentalCoroutinesApi
class ScreenshotFragment : ScopedBottomSheetDialogFragment(), AnkoLogger {

    private lateinit var binding: FragmentScreenshotBinding
    private lateinit var viewModel: ScreenshotViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_screenshot,
            container,
            false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                .get(ScreenshotViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {
        viewModel.teaserLiveData.observe(viewLifecycleOwner) {
            val imageName = it.screenshotName()
            val file = File(requireContext().filesDir, imageName).toString()
            info("Show image $file")
            binding.screenshotImage
                .setImageDrawable(
                    Drawable.createFromPath(file.toString())
                )
        }
    }

    private fun initUI() {
        binding.toolbar.bottomSheetToolbar.onClick {
            dismiss()
        }

        binding.shareScreenshotRv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.HORIZONTAL
            }
            adapter = Adapter()
        }
    }

    inner  class Adapter : RecyclerView.Adapter<SocialShareViewHolder>() {
        private val socialApps = arrayOf(
            SocialApp(
                name = "好友",
                icon = R.drawable.wechat,
                id = SocialAppId.WECHAT_FRIEND
            ),
            SocialApp(
                name = "朋友圈",
                icon = R.drawable.moments,
                id = SocialAppId.WECHAT_MOMENTS
            ),
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SocialShareViewHolder {
            return SocialShareViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: SocialShareViewHolder, position: Int) {
            val app = socialApps[position]

            holder.icon.setImageResource(app.icon)
            holder.text.text = app.name

            holder.itemView.setOnClickListener {
                viewModel.shareTo(app)
                dismiss()
            }
        }

        override fun getItemCount() = socialApps.size
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         */
        @JvmStatic
        fun newInstance() = ScreenshotFragment()
    }
}
