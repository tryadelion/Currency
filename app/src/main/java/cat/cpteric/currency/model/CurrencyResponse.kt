package cat.cpteric.currency.model

import com.google.gson.annotations.SerializedName

data class CurrencyResponse(
    @SerializedName("baseCurrency") private val baseCurrency: String,
    @SerializedName("rates") private val rateMap: HashMap<String, Double>
) {
    fun currency() = Rate(baseCurrency, 1.0)
    fun rates() = rateMap.map {
        Rate(it.key, it.value)
    }
}
