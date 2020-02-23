package cat.cpteric.currency

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cat.cpteric.currency.adapter.CurrencyAdapter
import cat.cpteric.currency.model.Rate
import cat.cpteric.currency.service.CurrencyService
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.currency_rate.view.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), CurrencyAdapter.currencySelectedListener {
    var currencyAdapter = CurrencyAdapter(this)
    var selectedCurrency = Rate("EUR", 1.0)
    var disposable: Disposable? = null
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        CurrencyService.configure()
        disposable = Observable.interval(1, TimeUnit.SECONDS)
            .flatMap { CurrencyService.getCurrencies(selectedCurrency).toObservable() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d("PING", it.currency().toString())
                currencyAdapter.update(it.rates(), selectedCurrency)
            }, {
                Log.e("ERROR", it.message + "")
            })

        ratesView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = currencyAdapter
        }
        vSelectedCurrency.apply {
            vCurrencyValue.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable?) {
                    if (s == null) return
                    if (s.isNotEmpty()) selectedCurrency.value = s.toString().toDouble()
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
        updateSelectedCurrency()
    }

    override fun onStop() {
        super.onStop()
        disposable?.dispose()
    }

    private fun updateSelectedCurrency() {
        vSelectedCurrency.apply {
            vCurrencyCode.text = selectedCurrency.code
            vCurrencyName.text = selectedCurrency.getDisplayName()
            vCurrencyValue.setText("1")
            vCurrencyValue.requestFocus(vCurrencyValue.text.length)
        }
    }

    override fun onCurrencySelected(currency: Rate) {
        currency.value = 1.0
        selectedCurrency = currency
        updateSelectedCurrency()
    }
}
