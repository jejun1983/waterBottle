package api

import android.content.Context
import kr.co.medialog.PlayInfoData
import kr.co.medialog.SettingInfoData
import kr.co.medialog.mlapi.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

/**
 * Umobi Api 호출
 */
interface OwlApi {

    companion object {
        // 광고 url 정보
        val REAL_URL = "https://pps.uplussave.com"

        fun create(context: Context): OwlApi {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val okHttpClient = OkHttpClient().newBuilder()
            if (BuildConfig.DEBUG) {
                okHttpClient.connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .addInterceptor { chain ->
                            val original = chain.request()

                            val request = original.newBuilder()
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Accept", "application/json")
                                    .method(original.method(), original.body())
                                    .build()

                            chain.proceed(request)
                        }
                        .addInterceptor(logging)
                        .build()
            } else {
                okHttpClient.connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .addInterceptor { chain ->
                            val original = chain.request()

                            val request = original.newBuilder()
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Accept", "application/json")
                                    .method(original.method(), original.body())
                                    .build()

                            chain.proceed(request)
                        }
                        .build()
            }

            val retrofit = Retrofit.Builder()
                    .baseUrl(REAL_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient.build())
                    .build()

            return retrofit.create(OwlApi::class.java)
        }
    }

    @Headers("Accept: application/json")
    @GET
    fun getSettingInfo(@Url url: String): Call<SettingInfoData>

    @Headers("Accept: application/json")
    @GET
    fun getPlayInfo(@Url url: String): Call<PlayInfoData>
}
