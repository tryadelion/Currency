package cat.cpteric.currency.service

import cat.cpteric.currency.model.CurrencyResponse
import cat.cpteric.currency.model.Rate
import io.reactivex.Flowable
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


object CurrencyService {
    private const val baseURL = "https://hiring.revolut.codes/api/android/"

    private lateinit var okHttpClient: OkHttpClient

    private lateinit var retrofit: Retrofit

    private lateinit var service: CurrencyApi

    fun getCurrencies(rate: Rate): Flowable<CurrencyResponse> =
        service.fetchCurrencies("latest", rate.code)

    fun configure() {
        okHttpClient = OkHttpClient.Builder()
            .build()
        retrofit = Retrofit.Builder()
            .baseUrl(baseURL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(okHttpClient)
            .build()
        service = retrofit.create(CurrencyApi::class.java)
    }

    interface CurrencyApi {
        @GET("{endpoint}")
        fun fetchCurrencies(
            @Path("endpoint") endpoint: String?,
            @Query("base") base: String
        ): Flowable<CurrencyResponse>
    }
}