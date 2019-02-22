package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.*
import kotlinx.android.synthetic.main.activity_my_subscription.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

class MySubsActivity : AppCompatActivity(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var job: Job? = null

    private lateinit var viewAdapter: RowAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var sessionManager: SessionManager

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    private fun stopRefresh() {
        swipe_refresh.isRefreshing = false
    }

    /**
     * Refresh account.
     * Use different API endpoints depending on the login method.
     */
    override fun onRefresh() {
        if (!isNetworkConnected()) {
            stopRefresh()

            toast(R.string.prompt_no_network)
            return
        }

        val account = sessionManager.loadAccount()

        if (account == null) {
            stopRefresh()
            toast("Your account data not found!")
            return
        }

        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val refreshedAccount = withContext(Dispatchers.IO) {
                    account.refresh()
                }

                info("Refreshed account: $refreshedAccount")

                if (refreshedAccount == null) {
                    toast("Account not retrieved")
                    return@launch
                }

                toast(R.string.success_updated)

                sessionManager.saveAccount(refreshedAccount)

                val rows = buildRows(refreshedAccount)

                viewAdapter.refreshData(rows)
                viewAdapter.notifyDataSetChanged()

            } catch (e: ClientError) {
                info("$e")

                stopRefresh()

                when (e.statusCode) {
                    // Logout this user if account not found during refresh.
                    404 -> {
                        toast(R.string.api_account_not_found)
                        sessionManager.logout()
                    }
                    else -> handleApiError(e)
                }

            } catch (e: Exception) {
                stopRefresh()

                handleException(e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_subscription)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        swipe_refresh.setOnRefreshListener(this)

        sessionManager = SessionManager.getInstance(this)

        val account = sessionManager.loadAccount() ?: return

        val rows = buildRows(account)

        viewManager = LinearLayoutManager(this)
        viewAdapter = RowAdapter(rows)

        member_rv.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.frag_customer_service, CustomerServiceFragment.newInstance())
                .commit()
    }

    private fun buildRows(account: Account): Array<TableRow> {

        val tierText = if (account.isVip) {
            getString(R.string.tier_vip)
        } else {
            getTierCycleText(account.membership.key)
        }

        val endDateText = if (account.isVip) {
            getString(R.string.cycle_vip)
        } else {
            formatLocalDate(account.membership.expireDate)
        } ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        val row1 = TableRow(
                header = getString(R.string.label_member_tier),
                data = tierText,
                isBold = true
        )

        val row2 = TableRow(
                header = getString(R.string.label_member_duration),

                data = endDateText ?: "",
                color = ContextCompat.getColor(this, R.color.colorClaret)
        )

        return arrayOf(row1, row2)
    }

    companion object {
        fun start(context: Context?) {
            context?.startActivity(Intent(context, MySubsActivity::class.java))
        }
    }
}

class RowAdapter(private var rows: Array<TableRow>) :
        RecyclerView.Adapter<RowAdapter.ViewHolder>() {

    class ViewHolder (view: View): RecyclerView.ViewHolder(view) {
        val labelView: TextView = view.findViewById(R.id.label_tv)
        val valueView: TextView = view.findViewById(R.id.value_tv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_table_row, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]
        holder.labelView.text = row.header
        holder.valueView.text = row.data
        if (row.color != null) {
            holder.valueView.setTextColor(row.color)
        }

        if (row.isBold) {
            holder.valueView.setTypeface(holder.valueView.typeface, Typeface.BOLD)
        }
    }

    fun refreshData(rows: Array<TableRow>) {
        this.rows = rows
    }
}

data class TableRow(
        val header: String,
        val data: String,
        val isBold: Boolean = false,
        val color: Int? = null
)