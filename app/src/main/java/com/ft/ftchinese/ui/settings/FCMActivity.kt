package com.ft.ftchinese.ui.settings

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityFcmBinding
import com.ft.ftchinese.ui.lists.SingleLineItem
import com.ft.ftchinese.ui.lists.SingleLineItemViewHolder
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class FCMActivity : AppCompatActivity(), AnkoLogger {

    private var errorDialog: Dialog? = null
    private var listAdapter: ListAdapter? = null
    private lateinit var binding: ActivityFcmBinding

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_fcm)
        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        listAdapter = ListAdapter(listOf())

        binding.rvFcmProgress.apply {
            layoutManager = LinearLayoutManager(this@FCMActivity)
            adapter = listAdapter
        }

        // See https://developer.android.com/training/notify-user/channels#UpdateChannel
        // Open settings for a specific channel
        binding.tvNotificationSetting.setOnClickListener {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
                putExtra(Settings.EXTRA_CHANNEL_ID, getString(R.string.news_notification_channel_id))
            }

            startActivity(intent)
        }

        binding.btnCheckFcm.setOnClickListener {
            binding.inProgress = true

            // Clean data every time user clicked the check button.
            listAdapter?.clear()

            // Check if Google Play Service is avaiable on device.
            val item = buildPlayServiceText(checkPlayServices())

            // Show the checking result.
            listAdapter?.add(item)

            retrieveRegistrationToken()
        }
    }

    private fun buildPlayServiceText(available: Boolean): SingleLineItem {
        return if (available) {
            SingleLineItem(
                    text = getString(R.string.play_service_available),
                    icon = R.drawable.ic_done_claret_24dp
            )
        } else {
            SingleLineItem(
                    text = getString(R.string.play_service_not_available),
                    icon = R.drawable.ic_error_outline_claret_24dp
            )
        }
    }

    private fun buildTokenText(retrieved: Boolean): SingleLineItem {
        return if (retrieved) {
            SingleLineItem(
                    text = getString(R.string.fcm_accessible),
                    icon = R.drawable.ic_done_claret_24dp
            )
        } else {
            SingleLineItem(
                    text = getString(R.string.fcm_inaccessible),
                    icon = R.drawable.ic_error_outline_claret_24dp
            )
        }
    }

    private fun checkPlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                if (errorDialog == null) {
                    errorDialog = googleApiAvailability.getErrorDialog(this, resultCode, 2404)
                    errorDialog?.setCancelable(false)
                }

                if (errorDialog?.isShowing == false) {
                    errorDialog?.show()
                }
            }
        }

        return resultCode == ConnectionResult.SUCCESS
    }

    private fun retrieveRegistrationToken() {
        FirebaseInstanceId
                .getInstance()
                .instanceId
                .addOnCompleteListener(OnCompleteListener {
                    if (!it.isSuccessful) {
                        info("getInstanceId failed", it.exception)

                        listAdapter?.add(buildTokenText(false))

                        binding.inProgress = false
                        return@OnCompleteListener
                    }

                    val token = it.result?.token

                    info("Token $token")

                    listAdapter?.add(buildTokenText(true))
                    binding.inProgress = false
                })
    }

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, FCMActivity::class.java))
        }
    }

    inner class ListAdapter(rows: List<SingleLineItem>) : RecyclerView.Adapter<SingleLineItemViewHolder>() {

        private var items: MutableList<SingleLineItem> = rows.toMutableList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleLineItemViewHolder {
            return SingleLineItemViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: SingleLineItemViewHolder, position: Int) {
            val item = items[position]
            holder.disclosure.visibility = View.GONE
            if (item.icon != null) {
                holder.icon.setImageResource(item.icon)
            }
            holder.text.text = item.text
        }

        override fun getItemCount() = items.size

        fun add(item: SingleLineItem) {
            items.add(item)
            notifyDataSetChanged()
        }

        fun clear() {
            items.clear()
        }
    }
}
