package com.axzydev.puertonuevoapp.core.util

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone

actual fun localOffsetMillis(atMillis: Long): Long {
    val date = NSDate.dateWithTimeIntervalSince1970(atMillis / 1000.0)
    return NSTimeZone.localTimeZone.secondsFromGMTForDate(date) * 1000L
}
