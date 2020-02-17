package cat.cpteric.currency.model

data class Rate @JvmOverloads constructor(
    val code: String = "UNKNOWN",
    val value: Double = 0.0
)