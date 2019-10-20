package net.gotev.uploadservice.extensions

import android.content.Context
import android.os.PowerManager

fun PowerManager.WakeLock?.safeRelease() {
    this?.apply { if (isHeld) release() }
}

fun Context.acquirePartialWakeLock(
    currentWakeLock: PowerManager.WakeLock?,
    tag: String
): PowerManager.WakeLock {
    if (currentWakeLock?.isHeld == true) {
        return currentWakeLock
    }

    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

    return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag).apply {
        setReferenceCounted(false)
        if (!isHeld) acquire()
    }
}
