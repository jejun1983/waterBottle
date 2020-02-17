package com.idevel.waterbottle.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.idevel.waterbottle.R
import com.idevel.waterbottle.utils.getIconBadgeIntent
import com.idevel.waterbottle.utils.setIconBadge

/**
 * Created by djsworld on 2017-08-03.
 */

object PushNotification {

    private const val UMOBI_NOTI_ID = 308072

    // 푸시 노티
    const val CHANNEL_ID_PUSH = "uplussavePPAPush"

    /**
     * 실제 디바에스에 GCM으로부터 받은 메세지를 알려주는 함수이다. 디바이스 Notification Center에 나타난다.
     *
     * @param title
     * @param message
     */
    fun sendNotification(context: Context, intent: Intent, title: String?, message: String?, image: Bitmap? = null) {
        clearNotification(context)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_PUSH)
                .setSmallIcon(R.mipmap.owl)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setNumber(1)
                .setDeleteIntent(getIconBadgeIntent(context, 0))
                .setContentText(message)
                .setSound(defaultSoundUri)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

        image?.let {
            val bigPictureStyle = NotificationCompat.BigPictureStyle(notificationBuilder)
            bigPictureStyle.bigPicture(it)
                    .setBigContentTitle(title)
                    .setSummaryText(message)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(UMOBI_NOTI_ID, notificationBuilder.build())
        setIconBadge(context, 1)
    }

    fun clearNotification(context: Context) {

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancel(UMOBI_NOTI_ID)
    }
}
