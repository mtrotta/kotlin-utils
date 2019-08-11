package org.matteo.utils.clean

import java.util.*

interface WorkingCalendar {

    fun isWorkingDay(date: Date): Boolean

}

object TargetCalendar : WorkingCalendar {

    override fun isWorkingDay(date: Date): Boolean {
        return isWorkingDay(getCalendar(date))
    }

    private val holidays = arrayOf(
        intArrayOf(1, 1), // New Year Day
        intArrayOf(5, 1), // Labor Day
        intArrayOf(12, 25), // Christmas
        intArrayOf(12, 26) // Family Day
    )

    private fun isWorkingDay(calendar: Calendar): Boolean {
        val holidays = getHolidays(calendar.get(Calendar.YEAR))
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek != Calendar.SUNDAY && dayOfWeek != Calendar.SATURDAY && !holidays.contains(calendar)
    }

    private fun getHolidays(vararg years: Int): Set<Calendar> {
        val targetCalendar = TreeSet<Calendar>()

        for (year in years) {

            for (holiday in holidays) {
                val calendar = GregorianCalendar()
                calendar.set(year, holiday[0], holiday[1])
                targetCalendar.add(calendar)
            }

            val easter = findEaster(year)

            val goodFriday = GregorianCalendar()
            goodFriday.time = easter
            goodFriday.add(Calendar.DAY_OF_YEAR, -2)

            val easterMonday = GregorianCalendar()
            easterMonday.time = easter
            easterMonday.add(Calendar.DAY_OF_YEAR, 1)

            targetCalendar.add(goodFriday)
            targetCalendar.add(easterMonday)
        }

        if (targetCalendar.isEmpty()) {
            throw CalendarException("Unable to find a suitable calendar")
        }

        return targetCalendar
    }

    private fun findEaster(year: Int): Date {
        if (year < 1573 || year > 2499) {
            throw IllegalArgumentException("invalid year for easter: $year")
        }

        val a = year % 19
        val b = year % 4
        val c = year % 7

        var m = 0
        var n = 0

        when {
            year in 1900..2099 -> {
                m = 24
                n = 5
            }
            year in 2100..2199 -> {
                m = 24
                n = 6
            }
            year in 1583..1699 -> {
                m = 22
                n = 2
            }
            year in 1700..1799 -> {
                m = 23
                n = 3
            }
            year in 1800..1899 -> {
                m = 23
                n = 4
            }
            year in 2200..2299 -> {
                m = 25
                n = 0
            }
            year in 2300..2399 -> {
                m = 26
                n = 1
            }
            year >= 2400 -> {
                m = 25
                n = 1
            }
        }

        val d = (19 * a + m) % 30
        val e = (2 * b + 4 * c + 6 * d + n) % 7

        val calendar = GregorianCalendar()
        calendar.set(Calendar.YEAR, year)

        if (d + e < 10) {
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, Calendar.MARCH)
            calendar.set(Calendar.DAY_OF_MONTH, d + e + 22)
        } else {
            calendar.set(Calendar.MONTH, Calendar.APRIL)
            var day = d + e - 9
            if (26 == day) {
                day = 19
            } else if (25 == day && 28 == d && e == 6 && a > 10) {
                day = 18
            }
            calendar.set(Calendar.DAY_OF_MONTH, day)
        }

        return calendar.time
    }

    private fun getCalendar(date: Date): Calendar {
        val calendar = GregorianCalendar()
        calendar.time = date
        return calendar
    }

}
