package org.matteo.utils.delta

enum class DeltaType {
    ADDITIONAL,
    MATCH,
    MISSING
}

interface Delta<T> : Comparable<T> {
    fun apply(type: DeltaType, other: T?)
}

class DeltaCalculator<T : Delta<T>> {

    fun delta(list1: List<T>, list2: List<T>): List<T> {

        val list = mutableListOf<T>()

        val iterator1 = list1.sorted().iterator()
        val iterator2 = list2.sorted().iterator()
        var read1 = iterator1.hasNext()
        var read2 = iterator2.hasNext()
        var t1: T? = null
        var t2: T? = null

        while (read1 || read2 || t1 != null || t2 != null) {
            if (read1) {
                t1 = iterator1.next()
            }
            if (read2) {
                t2 = iterator2.next()
            }

            val result = delta(t1, t2)

            val type: DeltaType = result.deltaType
            val main = result.main
            main.apply(type, result.other)

            when (type) {
                DeltaType.ADDITIONAL -> {
                    read1 = iterator1.hasNext()
                    read2 = false
                    t1 = null
                }
                DeltaType.MISSING -> {
                    read1 = false
                    read2 = iterator2.hasNext()
                    t2 = null
                }
                DeltaType.MATCH -> {
                    read1 = iterator1.hasNext()
                    read2 = iterator2.hasNext()
                    t1 = null
                    t2 = null
                }
            }

            list.add(main)
        }

        return list
    }

    private fun delta(t1: T?, t2: T?): Result<T> {
        if (t1 != null && t2 != null) {
            val check = t1.compareTo(t2)
            return when {
                check < 0 -> Result(DeltaType.ADDITIONAL, t1)
                check > 0 -> Result(DeltaType.MISSING, t2)
                else -> Result(DeltaType.MATCH, t1, t2)
            }
        } else if (t1 != null) {
            return Result(DeltaType.ADDITIONAL, t1)
        } else if (t2 != null) {
            return Result(DeltaType.MISSING, t2)
        }
        throw IllegalStateException("Can't be here, this is a bug!")
    }

    data class Result<T> constructor(val deltaType: DeltaType, val main: T, val other: T? = null)

}


