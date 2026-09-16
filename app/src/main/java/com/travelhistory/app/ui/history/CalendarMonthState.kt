package com.travelhistory.app.ui.history

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CalendarMonthState(
    val year: Int,
    val month: Int // 0-indexed (Calendar.MONTH)
) {
    val monthYearLabel: String
        get() {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        }

    val daysInMonth: Int
        get() {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

    /**
     * Offset for the first day of the month where Sunday = 0, Monday = 1, etc.
     */
    val firstDayOfWeekOffset: Int
        get() {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            return (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
        }

    fun getTimeInMillis(dayOfMonth: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun isFutureDate(dayOfMonth: Int): Boolean {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return target.after(today)
    }

    fun nextMonth(): CalendarMonthState {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            add(Calendar.MONTH, 1)
        }
        return CalendarMonthState(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun previousMonth(): CalendarMonthState {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            add(Calendar.MONTH, -1)
        }
        return CalendarMonthState(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    companion object {
        fun current(): CalendarMonthState {
            val cal = Calendar.getInstance()
            return CalendarMonthState(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH)
            )
        }
    }
}
