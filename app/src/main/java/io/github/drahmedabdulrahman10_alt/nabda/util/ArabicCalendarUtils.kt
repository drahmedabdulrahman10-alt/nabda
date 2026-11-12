package io.github.drahmedabdulrahman10_alt.nabda.util

import android.icu.util.IslamicCalendar
import android.icu.util.ULocale
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Dual Gregorian & Hijri (Umm Al-Qura) calendar utility for "نَبضة" (Nabda).
 * Weeks strictly start on Saturday (السبت) and end on Friday (الجمعة).
 */
object ArabicCalendarUtils {

    private val arabicLocale = Locale("ar")

    private val hijriMonths = arrayOf(
        "محرّم",
        "صفر",
        "ربيع الأول",
        "ربيع الآخر",
        "جمادى الأولى",
        "جمادى الآخرة",
        "رجب",
        "شعبان",
        "رمضان",
        "شوّال",
        "ذو القعدة",
        "ذو الحجة"
    )

    private val arabicDayNames = mapOf(
        Calendar.SATURDAY to "السبت",
        Calendar.SUNDAY to "الأحد",
        Calendar.MONDAY to "الاثنين",
        Calendar.TUESDAY to "الثلاثاء",
        Calendar.WEDNESDAY to "الأربعاء",
        Calendar.THURSDAY to "الخميس",
        Calendar.FRIDAY to "الجمعة"
    )

    private val gregorianMonths = arrayOf(
        "يناير",
        "فبراير",
        "مارس",
        "أبريل",
        "مايو",
        "يونيو",
        "يوليو",
        "أغسطس",
        "سبتمبر",
        "أكتوبر",
        "نوفمبر",
        "ديسمبر"
    )

    /**
     * Normalizes a Date to the start of its day (00:00:00.000) in local timezone.
     */
    fun startOfDay(date: Date = Date()): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    /**
     * Returns the Saturday (00:00:00.000) that starts the week containing [referenceDate].
     */
    fun getSaturdayOfWeek(referenceDate: Date = Date()): Date {
        val cal = Calendar.getInstance()
        cal.time = startOfDay(referenceDate)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // In java.util.Calendar: SATURDAY = 7, SUNDAY = 1, ..., FRIDAY = 6
        val daysSinceSaturday = when (dayOfWeek) {
            Calendar.SATURDAY -> 0
            else -> dayOfWeek // Sunday(1) -> 1 day after Saturday, Friday(6) -> 6 days after Saturday
        }
        cal.add(Calendar.DAY_OF_YEAR, -daysSinceSaturday)
        return cal.time
    }

    /**
     * Returns the Friday (23:59:59) that ends the week starting on [saturdayStart].
     */
    fun getFridayOfWeek(saturdayStart: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = startOfDay(saturdayStart)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    /**
     * Shifts a date by [days] days.
     */
    fun addDays(date: Date, days: Int): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.time
    }

    /**
     * Deterministic key `yyyyMMdd` for a given Date.
     */
    fun formatDateKey(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = date
        return dateKey(cal)
    }

    /**
     * `yyyyMMdd` key for the day [cal] points at, without the cost of a date formatter.
     */
    fun dateKey(cal: Calendar): String {
        val value = cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
        return value.toString().padStart(8, '0')
    }

    /**
     * One-letter Arabic label for the weekday of [date], for compact day strips.
     */
    fun getArabicDayLetter(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = date
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "س"
            Calendar.SUNDAY -> "ح"
            Calendar.MONDAY -> "ن"
            Calendar.TUESDAY -> "ث"
            Calendar.WEDNESDAY -> "ر"
            Calendar.THURSDAY -> "خ"
            else -> "ج"
        }
    }

    /**
     * Formats a Date as ISO `yyyy-MM-dd` in US/ASCII digits.
     */
    fun formatIsoDate(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(date)
    }

    /**
     * Parses a date string in `yyyy-MM-dd`, `yyyy/MM/dd`, or `yyyyMMdd` format (supporting Arabic-Indic digits too)
     * and returns the normalized startOfDay Date, or null if invalid.
     */
    fun parseFlexibleDate(rawDate: String): Date? {
        val normalizedDigits = rawDate.trim()
            .map { ch ->
                when (ch) {
                    in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
                    in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
                    else -> ch
                }
            }
            .joinToString("")

        val patterns = listOf("yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "yyyyMMdd")
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }
                val parsed = sdf.parse(normalizedDigits)
                if (parsed != null) {
                    return startOfDay(parsed)
                }
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return null
    }

    private val unsafeIdChars = Regex("[^a-zA-Z0-9_-]")

    private fun safeUserId(userId: String): String =
        if (userId == "local") userId else userId.replace(unsafeIdChars, "").take(40)

    /**
     * Deterministic Week document ID scoped to userId and Saturday start date.
     */
    fun buildWeekId(userId: String, saturdayStart: Date): String {
        val safeUid = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(40)
        return "wk_${safeUid}_${formatDateKey(saturdayStart)}"
    }

    /**
     * Deterministic Day document ID scoped to userId and day date.
     */
    fun buildDayId(userId: String, dayDate: Date): String {
        val safeUid = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(40)
        return "dy_${safeUid}_${formatDateKey(dayDate)}"
    }

    /**
     * Recovers the calendar date encoded at the end of a day ID, or null if it has none.
     */
    fun dateFromDayId(dayId: String): Date? {
        val key = dayId.substringAfterLast('_')
        if (key.length != 8 || !key.all { it in '0'..'9' }) return null
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(key.substring(0, 4).toInt(), key.substring(4, 6).toInt() - 1, key.substring(6, 8).toInt())
        return cal.time
    }

    /**
     * Formats Gregorian date in Arabic, e.g. "الخميس، 4 سبتمبر 2026".
     */
    fun formatGregorianArabic(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = date
        val dayName = arabicDayNames[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val monthName = gregorianMonths[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return "$dayName، $dayOfMonth $monthName $year"
    }

    /**
     * Returns only the Arabic weekday name, e.g. "السبت".
     */
    fun getArabicDayName(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = date
        return arabicDayNames[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
    }

    /**
     * Formats Hijri date using Umm Al-Qura calendar, e.g. "22 ربيع الأول 1448 هـ".
     */
    fun formatHijriArabic(date: Date): String {
        return try {
            val uLocale = ULocale("ar@calendar=islamic-umalqura")
            val islamicCal = IslamicCalendar(uLocale)
            islamicCal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
            islamicCal.time = date
            val day = islamicCal.get(IslamicCalendar.DAY_OF_MONTH)
            val monthIdx = islamicCal.get(IslamicCalendar.MONTH).coerceIn(0, 11)
            val year = islamicCal.get(IslamicCalendar.YEAR)
            "$day ${hijriMonths[monthIdx]} $year هـ"
        } catch (e: Exception) {
            // Fallback calculation if ICU is mocked in headless unit tests
            val cal = Calendar.getInstance()
            cal.time = date
            val approxHijriYear = ((cal.get(Calendar.YEAR) - 622) * 33) / 32
            val monthIdx = cal.get(Calendar.MONTH).coerceIn(0, 11)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            "$day ${hijriMonths[monthIdx]} $approxHijriYear هـ"
        }
    }

    /**
     * Generates a human-friendly Arabic week title based on offset from the current week.
     */
    fun getWeekLabel(weekOffsetFromCurrent: Int, saturdayStart: Date, fridayEnd: Date): String {
        return when (weekOffsetFromCurrent) {
            0 -> "الأسبوع الحالي"
            -1 -> "الأسبوع الماضي"
            1 -> "الأسبوع القادم"
            2 -> "بعد أسبوعين"
            -2 -> "قبل أسبوعين"
            -3 -> "قبل ٣ أسابيع"
            -4 -> "قبل ٤ أسابيع"
            else -> {
                val calStart = Calendar.getInstance().apply { time = saturdayStart }
                val calEnd = Calendar.getInstance().apply { time = fridayEnd }
                val startMonth = gregorianMonths[calStart.get(Calendar.MONTH)]
                val endMonth = gregorianMonths[calEnd.get(Calendar.MONTH)]
                "أسبوع ${calStart.get(Calendar.DAY_OF_MONTH)} $startMonth - ${calEnd.get(Calendar.DAY_OF_MONTH)} $endMonth"
            }
        }
    }

    /**
     * Checks if two dates fall on the same calendar day.
     */
    fun isSameDay(d1: Date, d2: Date): Boolean {
        return formatDateKey(d1) == formatDateKey(d2)
    }

    /**
     * Checks if [date] is today.
     */
    fun isToday(date: Date): Boolean {
        return isSameDay(date, Date())
    }

    /**
     * Checks if [today] falls between [start] and [end] inclusive.
     */
    fun isCurrentWeek(start: Timestamp?, end: Timestamp?, now: Date = Date()): Boolean {
        if (start == null || end == null) return false
        val t = now.time
        return t >= startOfDay(start.toDate()).time && t <= end.toDate().time
    }

    /**
     * Returns all 12 Gregorian month names in Arabic (0 = يناير .. 11 = ديسمبر).
     */
    fun getGregorianMonthNames(): List<String> = gregorianMonths.toList()

    /**
     * Returns the ordered list of 7 weekday column headers starting Saturday (السبت) to Friday (الجمعة).
     */
    fun getWeekDayColumnHeaders(): List<String> = listOf(
        "السبت",
        "الأحد",
        "الاثنين",
        "الثلاثاء",
        "الأربعاء",
        "الخميس",
        "الجمعة"
    )

    /**
     * Returns the Hijri day-of-month number for a given Gregorian date (e.g. "15").
     */
    fun getHijriDayNumber(date: Date): String {
        return try {
            val uLocale = ULocale("ar@calendar=islamic-umalqura")
            val islamicCal = IslamicCalendar(uLocale)
            islamicCal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
            islamicCal.time = date
            islamicCal.get(IslamicCalendar.DAY_OF_MONTH).toString()
        } catch (_: Exception) {
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.DAY_OF_MONTH).toString()
        }
    }

    /**
     * Returns a Hijri month/year subtitle corresponding to the given Gregorian [year] and [monthZeroBased] (0..11),
     * e.g. "ربيع الآخر - جمادى الأولى 1448 هـ".
     */
    fun getHijriMonthYearSubtitle(year: Int, monthZeroBased: Int): String {
        val firstOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased.coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
        }.time
        val lastOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased.coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }.time

        return try {
            val uLocale = ULocale("ar@calendar=islamic-umalqura")
            val calStart = IslamicCalendar(uLocale).apply {
                calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
                time = firstOfMonth
            }
            val calEnd = IslamicCalendar(uLocale).apply {
                calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
                time = lastOfMonth
            }
            val m1 = hijriMonths[calStart.get(IslamicCalendar.MONTH).coerceIn(0, 11)]
            val y1 = calStart.get(IslamicCalendar.YEAR)
            val m2 = hijriMonths[calEnd.get(IslamicCalendar.MONTH).coerceIn(0, 11)]
            val y2 = calEnd.get(IslamicCalendar.YEAR)

            if (m1 == m2 && y1 == y2) {
                "$m1 $y1 هـ"
            } else if (y1 == y2) {
                "$m1 / $m2 $y1 هـ"
            } else {
                "$m1 $y1 هـ - $m2 $y2 هـ"
            }
        } catch (_: Exception) {
            formatHijriArabic(firstOfMonth)
        }
    }

    /**
     * Builds the 7-column calendar grid cells (Saturday to Friday) for a full Gregorian [year] and [monthZeroBased].
     */
    fun buildMonthGridDates(year: Int, monthZeroBased: Int): List<CalendarGridCellDate> {
        val firstDayCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased.coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val firstOfMonth = firstDayCal.time

        val lastDayCal = Calendar.getInstance().apply {
            time = firstOfMonth
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        val lastOfMonth = lastDayCal.time

        val gridStartSaturday = getSaturdayOfWeek(firstOfMonth)
        val gridEndFriday = startOfDay(getFridayOfWeek( getSaturdayOfWeek(lastOfMonth) ))

        val cells = mutableListOf<CalendarGridCellDate>()
        var cursor = gridStartSaturday
        val todayKey = formatDateKey(Date())
        var safetyCount = 0
        while (!cursor.after(gridEndFriday) && safetyCount < 49) {
            val cal = Calendar.getInstance().apply { time = cursor }
            val dKey = formatDateKey(cursor)
            cells.add(
                CalendarGridCellDate(
                    date = cursor,
                    dateKey = dKey,
                    dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                    hijriDay = getHijriDayNumber(cursor),
                    isCurrentMonth = cal.get(Calendar.MONTH) == monthZeroBased && cal.get(Calendar.YEAR) == year,
                    isToday = dKey == todayKey
                )
            )
            cursor = addDays(cursor, 1)
            safetyCount++
        }
        return cells
    }

    /**
     * Builds the 7-column calendar grid cells (Saturday to Friday) for a single week containing [referenceDate].
     */
    fun buildWeekGridDates(referenceDate: Date): List<CalendarGridCellDate> {
        val saturdayStart = getSaturdayOfWeek(referenceDate)
        val refCal = Calendar.getInstance().apply { time = referenceDate }
        val refMonth = refCal.get(Calendar.MONTH)
        val todayKey = formatDateKey(Date())

        return (0..6).map { offset ->
            val dayDate = addDays(saturdayStart, offset)
            val cal = Calendar.getInstance().apply { time = dayDate }
            val dKey = formatDateKey(dayDate)
            CalendarGridCellDate(
                date = dayDate,
                dateKey = dKey,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                hijriDay = getHijriDayNumber(dayDate),
                isCurrentMonth = cal.get(Calendar.MONTH) == refMonth,
                isToday = dKey == todayKey
            )
        }
    }
}

data class CalendarGridCellDate(
    val date: Date,
    val dateKey: String,
    val dayOfMonth: Int,
    val hijriDay: String,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

