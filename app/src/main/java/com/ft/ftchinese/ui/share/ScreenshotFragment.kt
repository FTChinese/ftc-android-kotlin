package com.ft.ftchinese.ui.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentScreenshotBinding
import com.ft.ftchinese.ui.dialog.ScopedBottomSheetDialogFragment
import org.jetbrains.anko.sdk27.coroutines.onClick

/**
 * A simple [Fragment] subclass.
 * Use the [ScreenshotFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ScreenshotFragment : ScopedBottomSheetDialogFragment() {

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
            ViewModelProvider(this)[ScreenshotViewModel::class.java]
        } ?: throw Exception("Invalid activity")

        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {

        // Retrieve the image URI and display it.
        viewModel.imageRowCreated.observe(viewLifecycleOwner) { screenshot ->
            // See https://www.cnblogs.com/yongfengnice/p/13576466.html
            Glide.with(this)
                .load(screenshot.imageUri)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) // This is important to show the original image; otherwise it loads a thumbnail-like one.
                .format(DecodeFormat.PREFER_RGB_565) // Reduce memory usage.
                .into(binding.screenshotImage)
        }
    }

    private fun initUI() {
        binding.toolbar.bottomSheetToolbar.onClick {
            dismiss()
        }

        // Show share icons.
        binding.shareScreenshotRv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.HORIZONTAL
            }
            adapter = Adapter()
        }
    }

    inner class Adapter : RecyclerView.Adapter<SocialShareViewHolder>() {
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
