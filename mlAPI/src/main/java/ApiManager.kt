
import android.annotation.SuppressLint
import android.content.Context
import api.Constants
import api.OnResultListener
import api.OwlApi
import kr.co.medialog.PlayInfoData
import kr.co.medialog.SettingInfoData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ApiManager(val context: Context) {

    var mUmobiApi: OwlApi = OwlApi.create(context)

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: ApiManager? = null

        @JvmStatic
        fun getInstance(context: Context): ApiManager =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: ApiManager(context).also { INSTANCE = it }
                }
    }

    fun getSettingInfo(url: String, listener: OnResultListener<Any>) {
        if (url.isNullOrEmpty()) {
            return
        }

        val call = mUmobiApi.getSettingInfo(url)

        call.enqueue(object : Callback<SettingInfoData> {
            override fun onResponse(call: Call<SettingInfoData>, response: Response<SettingInfoData>) {
                if (response.code() == 200) {
                    if (response.body() != null) {
                        listener.onResult(response.body() as SettingInfoData, Constants.OWL_APP_SETTING_DATA)
                    }
                }
            }

            override fun onFailure(error: Call<SettingInfoData>, t: Throwable) {
                listener.onFail(t, Constants.OWL_APP_SETTING_DATA)
            }
        })
    }

    fun getPlayInfo(url: String, contentId: String, listener: OnResultListener<Any>) {
        if (url.isNullOrEmpty()) {
            return
        }

        var sb = StringBuilder()
        sb.append(url)
        sb.append(contentId)

        val call = mUmobiApi.getPlayInfo(sb.toString())

        call.enqueue(object : Callback<PlayInfoData> {
            override fun onResponse(call: Call<PlayInfoData>, response: Response<PlayInfoData>) {
                if (response.code() == 200) {
                    if (response.body() != null) {
                        listener.onResult(response.body() as PlayInfoData, Constants.OWL_APP_PLAY_INFO_DATA)
                    }
                }
            }

            override fun onFailure(error: Call<PlayInfoData>, t: Throwable) {
                listener.onFail(t, Constants.OWL_APP_PLAY_INFO_DATA)
            }
        })
    }
}
