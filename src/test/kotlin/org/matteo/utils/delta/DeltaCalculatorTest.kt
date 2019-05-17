package org.matteo.utils.delta


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 04/04/12
 * Time: 19.12
 */
internal class DeltaCalculatorTest {

    private val calculator = DeltaCalculator<FakeDelta>()

    @Test
    fun testEmpty() {
        val list = ArrayList<FakeDelta>()
        val result = calculator.delta(list, list)
        assertEquals(0, result.size)
    }

    @Test
    fun testIdentity() {
        val list = ArrayList<FakeDelta>()

        list.add(FakeDelta("001", 1.0))
        list.add(FakeDelta("002", 2.0))
        list.add(FakeDelta("003", 3.0))

        val result = calculator.delta(list, list)

        assertEquals(3, result.size)

        for (delta in result) {
            assertEquals(0.0, delta.value, 0.0)
            assertEquals(DeltaType.MATCH, delta.outcome)
        }
    }

    @Test
    fun testSecondEmpty() {
        val list = ArrayList<FakeDelta>()

        list.add(FakeDelta("001", 0.0))
        list.add(FakeDelta("002", 0.0))
        list.add(FakeDelta("003", 0.0))

        val result1 = calculator.delta(list, mutableListOf())

        assertEquals(3, result1.size)

        for (delta in result1) {
            assertEquals(0.0, delta.value, 0.0)
            assertEquals(DeltaType.ADDITIONAL, delta.outcome)
        }

        val result2 = calculator.delta(mutableListOf(), list)

        assertEquals(3, result2.size)

        for (delta in result2) {
            assertEquals(0.0, delta.value, 0.0)
            assertEquals(DeltaType.MISSING, delta.outcome)
        }
    }

    @Test
    fun testAll() {
        val list1 = ArrayList<FakeDelta>()
        list1.add(FakeDelta("001", 2.0))
        list1.add(FakeDelta("003", 3.0))
        list1.add(FakeDelta("004", 4.0))

        val list2 = ArrayList<FakeDelta>()
        list2.add(FakeDelta("001", 1.0))
        list2.add(FakeDelta("002", 2.0))
        list2.add(FakeDelta("003", 3.0))

        val result = calculator.delta(list1, list2)

        assertEquals(4, result.size)

        var test = result[0]
        assertEquals("001", test.key)
        assertEquals(1.0, test.value, 0.0)
        assertEquals(DeltaType.MATCH, test.outcome)

        test = result[1]
        assertEquals("002", test.key)
        assertEquals(2.0, test.value, 0.0)
        assertEquals(DeltaType.MISSING, test.outcome)

        test = result[2]
        assertEquals("003", test.key)
        assertEquals(0.0, test.value, 0.0)
        assertEquals(DeltaType.MATCH, test.outcome)

        test = result[3]
        assertEquals("004", test.key)
        assertEquals(4.0, test.value, 0.0)
        assertEquals(DeltaType.ADDITIONAL, test.outcome)
    }

    @Test
    fun testDisjoint() {
        val list1 = ArrayList<FakeDelta>()
        list1.add(FakeDelta("001", 1.0))
        list1.add(FakeDelta("002", 2.0))
        list1.add(FakeDelta("003", 3.0))

        val list2 = ArrayList<FakeDelta>()
        list2.add(FakeDelta("004", 4.0))
        list2.add(FakeDelta("005", 5.0))
        list2.add(FakeDelta("006", 6.0))

        val result = calculator.delta(list1, list2)

        assertEquals(6, result.size)

        var test = result[0]
        assertEquals("001", test.key)
        assertEquals(1.0, test.value, 0.0)
        assertEquals(DeltaType.ADDITIONAL, test.outcome)

        test = result[1]
        assertEquals("002", test.key)
        assertEquals(2.0, test.value, 0.0)
        assertEquals(DeltaType.ADDITIONAL, test.outcome)

        test = result[2]
        assertEquals("003", test.key)
        assertEquals(3.0, test.value, 0.0)
        assertEquals(DeltaType.ADDITIONAL, test.outcome)

        test = result[3]
        assertEquals("004", test.key)
        assertEquals(4.0, test.value, 0.0)
        assertEquals(DeltaType.MISSING, test.outcome)

        test = result[4]
        assertEquals("005", test.key)
        assertEquals(5.0, test.value, 0.0)
        assertEquals(DeltaType.MISSING, test.outcome)

        test = result[5]
        assertEquals("006", test.key)
        assertEquals(6.0, test.value, 0.0)
        assertEquals(DeltaType.MISSING, test.outcome)

    }

    @Test
    fun testUnsorted() {

        val list1 = ArrayList<FakeDelta>()
        list1.add(FakeDelta("001", 1.0))
        list1.add(FakeDelta("002", 2.0))
        list1.add(FakeDelta("003", 3.0))

        val list2 = ArrayList<FakeDelta>()
        list2.add(FakeDelta("003", 3.0))
        list2.add(FakeDelta("002", 2.0))
        list2.add(FakeDelta("001", 1.0))

        val result = calculator.delta(list1, list2)

        assertEquals(3, result.size)

        for (delta in result) {
            assertEquals(0.0, delta.value, 0.0)
            assertEquals(DeltaType.MATCH, delta.outcome)
        }
    }

    private class FakeDelta(val key: String, var value: Double) :
        Delta<FakeDelta> {
        internal var outcome: DeltaType? = null
            private set

        override fun apply(type: DeltaType, other: FakeDelta?) {
            this.outcome = type
            if (other != null) {
                value -= other.value
            }
        }

        override fun compareTo(other: FakeDelta): Int {
            return key.compareTo(other.key)
        }
    }

}
