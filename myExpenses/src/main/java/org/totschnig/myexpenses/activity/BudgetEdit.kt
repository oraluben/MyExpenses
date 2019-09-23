package org.totschnig.myexpenses.activity

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.one_budget.*
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.viewmodel.Account
import org.totschnig.myexpenses.viewmodel.BudgetEditViewModel
import org.totschnig.myexpenses.viewmodel.data.Budget

class BudgetEdit : EditActivity(), AdapterView.OnItemSelectedListener {
    lateinit var viewModel: BudgetEditViewModel
    override fun getDiscardNewMessage() = R.string.dialog_confirm_discard_new_budget

    override fun setupListeners() {
        Title.addTextChangedListener(this)
        Description.addTextChangedListener(this)
        Amount.addTextChangedListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.one_budget)
        setupToolbar()
        viewModel = ViewModelProviders.of(this).get(BudgetEditViewModel::class.java)
        viewModel.accounts.observe(this, Observer {
            Accounts.adapter = AccountAdapter(this, it)
        })
        viewModel.loadAccounts()
        viewModel.databaseResult.observe(this,  Observer {
           if (it) finish() else {
               Toast.makeText(this, "Error while saving budget", Toast.LENGTH_LONG).show()
           }
        })
        Type.adapter = GroupingAdapter(this)
        Type.setSelection(Grouping.MONTH.ordinal)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        showDateRange(false)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        showDateRange(position == Grouping.NONE.ordinal)
    }

    private fun showDateRange(visible: Boolean) {
        DurationFromRow.isVisible = visible
        DurationToRow.isVisible = visible
    }

    override fun onResume() {
        super.onResume()
        setupListeners()
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (command == R.id.SAVE_COMMAND) {
            val account: Account = Accounts.selectedItem as Account
            val currencyUnit = currencyContext[account.currency]
            viewModel.createBudget(Budget(0, Accounts.selectedItemId,
                    Title.text.toString(), Description.text.toString(), account.currency,
                    Money(currencyUnit, validateAmountInput(Amount, false)),
                    Type.selectedItem as Grouping))
            return true;
        }
        return super.dispatchCommand(command, tag)
    }
}

class GroupingAdapter(context: Context) : ArrayAdapter<Grouping>(context, android.R.layout.simple_spinner_item, android.R.id.text1, Grouping.values()) {

    init {
        setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getView(position, convertView, parent)
        setText(position, row)
        return row
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getDropDownView(position, convertView, parent)
        setText(position, row)
        return row
    }

    private fun setText(position: Int, row: View) {
        (row.findViewById<View>(android.R.id.text1) as TextView).setText(getBudgetLabelForSpinner(getItem(position)!!))
    }

    private fun getBudgetLabelForSpinner(type: Grouping) = when (type) {
        Grouping.DAY -> R.string.daily_plain
        Grouping.WEEK -> R.string.weekly_plain
        Grouping.MONTH -> R.string.monthly
        Grouping.YEAR -> R.string.yearly_plain
        Grouping.NONE -> R.string.budget_onetime
    }
}

class AccountAdapter(context: Context, accounts: List<Account>) : ArrayAdapter<Account>(
        context, android.R.layout.simple_spinner_item, android.R.id.text1, accounts) {
    override fun hasStableIds(): Boolean = true
    override fun getItemId(position: Int): Long = getItem(position)!!.id
}