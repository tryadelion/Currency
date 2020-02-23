package cat.cpteric.currency.model

import java.util.*

data class Rate @JvmOverloads constructor(
    val code: String = "UNKNOWN",
    var value: Double = 0.0
) {
    private val currency = Currency.getInstance(code)

    fun getCurrencyFlagUrl(): String {
        return "https://www.countryflags.io/${code.dropLast(1)}/flat/64.png"
    }

    fun getDisplayName(): String {
        return currency.getDisplayName(Locale.getDefault())
    }
}