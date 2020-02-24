package cat.cpteric.currency

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cat.cpteric.currency.adapter.CurrencyAdapter
import cat.cpteric.currency.model.Rate
import cat.cpteric.currency.util.CircleTransform
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.currency_rate.view.*

class MainActivity : AppCompatActivity(), MainPresenter.PresenterInteraction {

    private val presenter = MainPresenter(this, Rate("EUR", 1.0))
    private val currencyAdapter = CurrencyAdapter(presenter)


    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.elevation = 0f
        initViews()
        presenter.updateSelectedCurrency()
    }

    private fun initViews() {
        ratesView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = currencyAdapter
        }
        vSelectedCurrency.apply {
            vCurrencyValue.isEnabled = true
            vCurrencyValue.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable?) {
                    if (s.toString() == ".") s?.clear()
                    if (s == null) return
                    if (s.isNotEmpty()) presenter.updateSelected(s.toString().toDouble())
                    vCurrencyValue.requestFocus(vCurrencyValue.text.length)
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.onResume()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onSelectedCurrencyChanged(currency: Rate) {
        vSelectedCurrency.apply {
            vCurrencyCode.text = currency.code
            vCurrencyName.text = currency.getDisplayName()
            vCurrencyValue.setText("")
            Picasso.get().load(currency.getCurrencyFlagUrl()).transform(CircleTransform())
                .into(vCurrencyIcon)
        }
    }

    override fun onDatasetUpdated(items: List<Rate>, currency: Rate) {
        currencyAdapter.update(items, currency)
    }
}
