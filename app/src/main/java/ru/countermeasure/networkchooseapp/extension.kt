package ru.countermeasure.networkchooseapp

import java.text.SimpleDateFormat
import java.util.*

fun Date.format(
    pattern: String,
    timezone: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault()
): String {
    val sdf = SimpleDateFormat(pattern, locale)
    sdf.timeZone = timezone
    return sdf.format(this)
}
