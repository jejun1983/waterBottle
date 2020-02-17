package com.idevel.waterbottle.utils

import android.app.ActivityManager
import android.content.Context
import com.idevel.waterbottle.activity.MainActivity


fun isAppRunning(context: Context): Boolean{
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val activitys = activityManager.getRunningTasks(3)
    var isActivityFound = false
    for (i in activitys.indices) {
        if (activitys[i].topActivity.toString().contains(MainActivity::class.java.name, ignoreCase = true)) {
            isActivityFound = true
        }
    }
    DLog.d("isAppRunning : $isActivityFound")
    return isActivityFound
}
