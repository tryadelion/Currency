package cat.cpteric.currency

import android.util.Log
import cat.cpteric.currency.adapter.CurrencyAdapter
import cat.cpteric.currency.model.Rate
import cat.cpteric.currency.service.CurrencyService
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class MainPresenter(
    private val listener: PresenterInteraction,
    initialCurrency: Rate
) :
    CurrencyAdapter.CurrencySelectedListener {
    private var selectedCurrency: Rate = initialCurrency
    private var disposable: Disposable? = null

    init {
        CurrencyService.configure()
    }

    fun onResume() {
        if (disposable?.isDisposed == true || disposable == null) {
            requestRates()
        }
    }

    private fun requestRates() {
        disposable = Observable.interval(1, TimeUnit.SECONDS)
            .flatMap { CurrencyService.getCurrencies(selectedCurrency).toObservable() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                listener.onDatasetUpdated(it.rates(), selectedCurrency)
            }, {
                Log.e("ERROR", it.message + "")
            })
    }

    fun onStop() {
        disposable?.dispose()
    }

    fun updateSelected(newValue: Double) {
        selectedCurrency.value = newValue
    }

    fun updateSelectedCurrency() {
        listener.onSelectedCurrencyChanged(selectedCurrency)
    }

    override fun onCurrencySelected(currency: Rate) {
        currency.value = 1.0
        selectedCurrency = currency
        updateSelectedCurrency()
    }

    interface PresenterInteraction {
        fun onSelectedCurrencyChanged(currency: Rate)
        fun onDatasetUpdated(items: List<Rate>, currency: Rate)
    }
}