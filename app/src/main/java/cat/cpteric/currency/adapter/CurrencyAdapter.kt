package cat.cpteric.currency.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cat.cpteric.currency.R
import cat.cpteric.currency.model.Rate
import kotlinx.android.synthetic.main.currency_rate.view.*

class CurrencyAdapter(val listener: currencySelectedListener) :
    RecyclerView.Adapter<CurrencyAdapter.CurrencyRateViewHolder>() {
    var rates = arrayListOf<Rate>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyRateViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return CurrencyRateViewHolder(inflater.inflate(R.layout.currency_rate, parent, false))
    }

    override fun getItemCount(): Int {
        return rates.size
    }

    override fun onBindViewHolder(holder: CurrencyRateViewHolder, position: Int) {
        val item = rates[position]
        holder.code.text = item.code
        holder.name.text = item.getDisplayName()
        holder.value.setText(item.value.toString())
        holder.itemView.setOnClickListener {
            listener.onCurrencySelected(item)
            rates.removeAt(position)
            notifyDataSetChanged()
        }
    }

    fun update(
        newRates: List<Rate>,
        selectedCurrency: Rate
    ) {
        newRates.map {
            if (rates.find { oldRate -> it.code == oldRate.code } == null) {
                it.value.times(selectedCurrency.value)
                rates.add(it)
            }
        }
        rates.map { rate ->
            rate.value =
                newRates.find { rate.code == it.code }?.value?.times(selectedCurrency.value)
                    ?: rate.value
        }
        notifyDataSetChanged()
    }

    class CurrencyRateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image = itemView.vCurrencyIcon
        val name = itemView.vCurrencyName
        val code = itemView.vCurrencyCode
        val value = itemView.vCurrencyValue
    }

    interface currencySelectedListener {
        fun onCurrencySelected(currency: Rate)
    }
}