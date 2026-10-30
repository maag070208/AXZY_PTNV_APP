package com.axzydev.puertonuevoapp.core.util

import java.util.TimeZone

actual fun localOffsetMillis(atMillis: Long): Long = TimeZone.getDefault().getOffset(atMillis).toLong()
