package com.idevel.waterbottle.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.idevel.waterbottle.interfaces.SimStateChangeListener
import com.idevel.waterbottle.utils.DLog


class SimStateChangeReceiver : BroadcastReceiver() {
    private var mListener: SimStateChangeListener? = null
    private var mContext: Context? = null

    companion object {
        private const val EXTRA_SIM_STATE = "ss"
    }

    fun setListener(listener: SimStateChangeListener) {
        mListener = listener
    }

    override fun onReceive(context: Context, intent: Intent) {
        mContext = context
        val action = intent.action

        if (action == "android.intent.action.SIM_STATE_CHANGED") {
            if (intent.hasExtra(EXTRA_SIM_STATE)) {
                val simState = intent.getStringExtra(EXTRA_SIM_STATE)
                DLog.d("SIM STATE : $simState")
                when (simState) {
                    "LOADED" -> {
                        if (mListener != null) {
                            mListener!!.onUsimMount()
                        }
                    }

                    "ABSENT" -> {
                        if (mListener != null) {
                            mListener!!.onUsimUnMount()
                        }
                    }

                    else -> {
                    }
                }
            }
        }
    }
}