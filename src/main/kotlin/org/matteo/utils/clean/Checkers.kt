package org.matteo.utils.clean

import java.util.*

enum class Checkers constructor(private val checker: DateChecker) {

    YEARLY(object : DateChecker {
        override fun isDate(date: Date): Boolean {
            return DateUtility.isLastWorkingDayOfYear(date)
        }

        override fun getMinimum(date: Date, maxElaborations: Int): Date {
            return DateUtility.getLastWorkingDayOfYear(date, maxElaborations)
        }
    }),

    QUARTERLY(object : DateChecker {
        override fun isDate(date: Date): Boolean {
            return DateUtility.isLastWorkingDayOfQuarter(date)
        }

        override fun getMinimum(date: Date, maxElaborations: Int): Date {
            return DateUtility.getLastWorkingDayOfQuarter(date, maxElaborations)
        }
    }),

    MONTHLY(object : DateChecker {
        override fun isDate(date: Date): Boolean {
            return DateUtility.isLastWorkingDayOfMonth(date)
        }

        override fun getMinimum(date: Date, maxElaborations: Int): Date {
            return DateUtility.getLastWorkingDayOfMonth(date, maxElaborations)
        }
    }),

    WEEKLY(object : DateChecker {
        override fun isDate(date: Date): Boolean {
            return DateUtility.isLastWorkingDayOfWeek(date)
        }

        override fun getMinimum(date: Date, maxElaborations: Int): Date {
            return DateUtility.getLastWorkingDayOfWeek(date, maxElaborations)
        }
    }),

    THURSDAYS(object : DateChecker {
        override fun isDate(date: Date): Boolean {
            return DateUtility.isDayOfWeek(date, Calendar.THURSDAY)
        }

        override fun getMinimum(date: Date, maxElaborations: Int): Date {
            return DateUtility.getLastDay(date, Calendar.THURSDAY, maxElaborations)
        }
    }),

    DAILY(object : DateChecker {
        override fun isDate(date: Date): Boolean {
            return true
        }

        override fun getMinimum(date: Date, maxElaborations: Int): Date {
            return DateUtility.getLastWorkingDay(date, maxElaborations)
        }
    });

    fun with(maxElaborations: Int): CheckerConfiguration {
        return CheckerConfiguration(checker, this.ordinal, maxElaborations)
    }
}
