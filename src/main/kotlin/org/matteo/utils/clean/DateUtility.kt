package org.matteo.utils.clean

import java.util.*

object DateUtility {

    private val workingCalendar = TargetCalendar()

    private fun getCalendar(date: Date): Calendar {
        val calendar = GregorianCalendar()
        calendar.time = date
        return calendar
    }

    @Throws(CalendarException::class)
    fun isLastWorkingDayOfYear(date: Date): Boolean {
        return date == getLastWorkingDayOfYear(date)
    }

    @Throws(CalendarException::class)
    fun getLastWorkingDayOfYear(date: Date): Date {
        val calendar = getCalendar(date)
        return getLastWorkingDay(calendar, Calendar.DAY_OF_YEAR)
    }

    @Throws(CalendarException::class)
    fun getLastWorkingDayOfYear(date: Date, shift: Int): Date {
        val calendar = getCalendar(date)
        calendar.add(Calendar.YEAR, -shift)
        return getLastWorkingDay(calendar, Calendar.DAY_OF_YEAR)
    }

    @Throws(CalendarException::class)
    fun isLastWorkingDayOfQuarter(date: Date): Boolean {
        val quarters = getQuarters(date)
        return quarters.contains(date)
    }

    @Throws(CalendarException::class)
    private fun getQuarters(date: Date): Set<Date> {
        return getMonthFractions(date, Calendar.MARCH, Calendar.JUNE, Calendar.SEPTEMBER, Calendar.DECEMBER)
    }

    @Throws(CalendarException::class)
    fun getLastWorkingDayOfQuarter(date: Date, shift: Int): Date {
        val calendar = getCalendar(date)
        var ctr = 0
        var quarter: Date
        var iterator: Iterator<Date>? = null
        do {
            if (iterator == null || !iterator.hasNext()) {
                iterator = getQuarters(calendar.time).iterator()
                calendar.add(Calendar.YEAR, -1)
            }
            quarter = iterator.next()
            if (date.after(quarter)) {
                ctr++
            }
        } while (ctr < shift)
        return quarter
    }

    @Throws(CalendarException::class)
    private fun getMonthFractions(date: Date, vararg months: Int): Set<Date> {
        val calendar = getCalendar(date)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val quarters = TreeSet(Collections.reverseOrder<Date>())
        for (month in months) {
            calendar.set(Calendar.MONTH, month)
            quarters.add(getLastWorkingDayOfMonth(calendar))
        }
        return quarters
    }

    @Throws(CalendarException::class)
    private fun getLastWorkingDayOfMonth(calendar: Calendar): Date {
        return getLastWorkingDay(calendar, Calendar.DAY_OF_MONTH)
    }

    @Throws(CalendarException::class)
    fun getLastWorkingDayOfMonth(date: Date, shift: Int): Date {
        val calendar = getCalendar(date)
        calendar.add(Calendar.MONTH, -shift)
        return getLastWorkingDay(calendar, Calendar.DAY_OF_MONTH)
    }

    @Throws(CalendarException::class)
    fun isLastWorkingDayOfMonth(date: Date): Boolean {
        val calendar = getCalendar(date)
        return date == getLastWorkingDayOfMonth(calendar)
    }

    @Throws(CalendarException::class)
    fun getLastWorkingDayOfWeek(calendar: Calendar): Date {
        return getLastWorkingDay(calendar, Calendar.DAY_OF_WEEK)
    }

    @Throws(CalendarException::class)
    fun getLastWorkingDayOfWeek(date: Date, max: Int): Date {
        val shift = if (isLastWorkingDayOfWeek(date)) max - 1 else max
        val calendar = getCalendar(date)
        calendar.add(Calendar.WEEK_OF_MONTH, -shift)
        return getLastWorkingDay(calendar, Calendar.DAY_OF_WEEK)
    }

    @Throws(CalendarException::class)
    fun isLastWorkingDayOfWeek(date: Date): Boolean {
        val calendar = getCalendar(date)
        return date == getLastWorkingDayOfWeek(calendar)
    }

    @Throws(CalendarException::class)
    private fun getLastWorkingDay(calendar: Calendar, maximum: Int): Date {
        val last = getCalendar(calendar.time)
        last.set(maximum, calendar.getActualMaximum(maximum))
        while (!workingCalendar.isWorkingDay(last.time)) {
            last.add(Calendar.DAY_OF_YEAR, -1)
        }
        return last.time
    }

    @Throws(CalendarException::class)
    fun getLastWorkingDay(date: Date, shift: Int): Date {
        val calendar = getCalendar(date)
        var ctr = 0
        do {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            if (workingCalendar.isWorkingDay(calendar.time)) {
                ctr++
            }
        } while (ctr < shift)
        return calendar.time
    }

    @Throws(CalendarException::class)
    fun addWorkingDay(date: Date, rollOffDays: Int): Date {
        val calendar = getCalendar(date)
        var ctr = 0
        val shift = if (rollOffDays > 0) 1 else -1
        while (Math.abs(ctr) < Math.abs(rollOffDays)) {
            calendar.add(Calendar.DAY_OF_YEAR, shift)
            if (workingCalendar.isWorkingDay(calendar.time)) {
                ctr += shift
            }
        }
        return calendar.time
    }

    fun getLastDay(date: Date, day: Int, shift: Int): Date {
        val calendar = getCalendar(date)
        var diff = day - calendar.get(Calendar.DAY_OF_WEEK)
        if (diff > 0) {
            diff -= 7
        }
        calendar.add(Calendar.DAY_OF_MONTH, diff)
        calendar.add(Calendar.WEEK_OF_YEAR, -shift)
        return calendar.time
    }

    private fun isDayOfWeek(calendar: Calendar, day: Int): Boolean {
        return calendar.get(Calendar.DAY_OF_WEEK) == day
    }

    fun isDayOfWeek(date: Date, day: Int): Boolean {
        return isDayOfWeek(getCalendar(date), day)
    }
}
