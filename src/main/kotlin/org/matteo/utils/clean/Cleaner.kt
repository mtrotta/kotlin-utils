package org.matteo.utils.clean

import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 17/05/19
 */
object Cleaner {

    @Throws(Exception::class)
    fun <T> clean(
        eraser: Eraser<T>,
        today: Date,
        checkers: List<CheckerConfiguration>,
        simulation: Boolean
    ): List<T> {

        val deleted = mutableListOf<T>()

        var ptr: Hierarchy? = null

        for (checkerConfiguration in checkers.sorted()) {
            val hierarchy = Hierarchy(checkerConfiguration.checker, today, checkerConfiguration.maxElaborations)
            ptr?.append(hierarchy)
            ptr = hierarchy
        }

        if (ptr == null) {
            throw IllegalArgumentException("Checkers are empty!")
        }

        val root = ptr.root

        val deletables = eraser.deletables

        for (deletable in deletables) {
            root.add(deletable.date)
        }

        val deletableDates = root.deletableDates

        for (deletable in deletables) {
            if (deletableDates.contains(deletable.date)) {
                val element = deletable.element
                if (!simulation) {
                    eraser.erase(element)
                }
                deleted.add(element)
            }
        }

        return deleted
    }

    private class Hierarchy
    internal constructor(private val checker: DateChecker, today: Date, maxElaborations: Int) {
        private val min: Date = checker.getMinimum(today, maxElaborations)

        private var parent: Hierarchy? = null
        private var child: Hierarchy? = null

        private val dates = TreeSet(Collections.reverseOrder<Date>())

        internal val deletableDates: Set<Date>
            get() = check(TreeSet(Collections.reverseOrder()))

        internal val root: Hierarchy
            get() {
                return parent?.root ?: this
            }

        internal fun append(hierarchy: Hierarchy) {
            this.child = hierarchy
            hierarchy.parent = this
        }

        fun add(date: Date) {
            var hierarchy: Hierarchy? = this
            while (hierarchy != null && !hierarchy.checker.isDate(date)) {
                hierarchy = hierarchy.child
            }
            hierarchy?.dates?.add(date)
        }

        private fun check(deletableDates: MutableSet<Date>): Set<Date> {
            val tail = dates.higher(min)
            if (tail != null) {
                deletableDates.addAll(dates.tailSet(tail))
            }
            child?.check(deletableDates)
            return deletableDates
        }

        internal operator fun contains(date: Date): Boolean {
            return dates.contains(date) || parentContains(date)
        }

        private fun parentContains(date: Date): Boolean {
            return parent != null && parent!!.contains(date)
        }
    }

}

interface Deletable<T> {

    val date: Date

    val element: T

}

interface Eraser<T> {

    val deletables: Collection<Deletable<T>>

    fun erase(deletable: T)

}

interface DateChecker {
    fun isDate(date: Date): Boolean

    fun getMinimum(date: Date, maxElaborations: Int): Date
}


class CalendarException(message: String) : Exception(message)

data class CheckerConfiguration(val checker: DateChecker, private val priority: Int, val maxElaborations: Int) :
    Comparable<CheckerConfiguration> {

    override fun compareTo(other: CheckerConfiguration): Int {
        return priority.compareTo(other.priority)
    }

}
