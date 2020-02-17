package com.idevel.waterbottle.fcm

import android.content.Intent
import android.os.Bundle
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.idevel.waterbottle.activity.PushPopupActivity
import com.idevel.waterbottle.fcm.PushPreferences.PUSH_DATA
import com.idevel.waterbottle.utils.DLog
import com.idevel.waterbottle.utils.isAppRunning

/**
 * Created by djsworld on 2016-02-25.
 */
class FcmListenerService : FirebaseMessagingService() {

    @Override
    override fun onMessageReceived(message: RemoteMessage) {

        val from = message.from
        DLog.v("push from : $from")

        // intent로 넘길 push 데이터
        val data = message.data
        val bundle = Bundle()
        for (entry in data) {
            bundle.putString(entry.key, entry.value)
            DLog.v(entry.key + " : " + entry.value)
        }

        // popup
        val i = Intent(applicationContext, PushPopupActivity::class.java)
        i.putExtra(PUSH_DATA, bundle)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if(!isAppRunning(applicationContext)) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(i)
    }


}
