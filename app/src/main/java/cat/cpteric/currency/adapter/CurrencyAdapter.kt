package cat.cpteric.currency.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cat.cpteric.currency.R
import cat.cpteric.currency.model.Rate
import cat.cpteric.currency.util.CircleTransform
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.currency_rate.view.*

class CurrencyAdapter(private val listener: CurrencySelectedListener) :
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
        Picasso.get().load(item.getCurrencyFlagUrl()).transform(CircleTransform())
            .into(holder.image)
        holder.value.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                onClick(item, position)
            }
        }
        holder.itemView.setOnClickListener {
            onClick(item, position)
        }
    }

    fun onClick(item: Rate, position: Int) {
        listener.onCurrencySelected(item)
        rates.removeAt(position)
        notifyDataSetChanged()
    }

    fun update(
        newRates: List<Rate>,
        selectedCurrency: Rate
    ) {
        newRates
            .map {
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
        rates.sortBy { it.code }
        notifyDataSetChanged()
    }

    class CurrencyRateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.vCurrencyIcon
        val name: TextView = itemView.vCurrencyName
        val code: TextView = itemView.vCurrencyCode
        val value: EditText = itemView.vCurrencyValue
    }

    interface CurrencySelectedListener {
        fun onCurrencySelected(currency: Rate)
    }
}