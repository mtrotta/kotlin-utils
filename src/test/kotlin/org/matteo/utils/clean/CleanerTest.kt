package org.matteo.utils.clean

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.*

fun stringToDate(date: String): Date = SimpleDateFormat("yyyyMMdd").parse(date)

internal class CleanerTest {

    @Test
    @Throws(Exception::class)
    fun testDelete() {

        val checkers = listOf(
            Checkers.YEARLY.with(1),
            Checkers.QUARTERLY.with(2),
            Checkers.MONTHLY.with(3),
            Checkers.DAILY.with(4)
        )

        val daily = Fake("20130125")
        val daily2 = Fake("20130124")
        val daily3 = Fake("20130123")
        val daily4 = Fake("20130122")
        val daily5 = Fake("20130121")
        val monthly = Fake("20121130")
        val monthly2 = Fake("20121031")
        val monthly3 = Fake("20120831")
        val quarterly = Fake("20121231")
        val quarterly1 = Fake("20120928")
        val quarterly2 = Fake("20120629")
        val quarterly3 = Fake("20110930")
        val yearly = Fake("20121231")
        val yearly2 = Fake("20111230")

        val deletables = listOf(
            daily,
            daily2,
            daily3,
            daily4,
            daily5,
            monthly,
            monthly2,
            monthly3,
            quarterly,
            quarterly1,
            quarterly2,
            quarterly3,
            yearly,
            yearly2
        )

        val eraser = object : Eraser<Fake> {

            override val deletables: Collection<Deletable<Fake>>
                get() {
                    val list = ArrayList<Deletable<Fake>>()
                    for (fake in deletables) {
                        list.add(object : Deletable<Fake> {
                            override val date: Date
                                get() = fake.date

                            override val element: Fake
                                get() = fake
                        })
                    }
                    return list
                }

            override fun erase(deletable: Fake) {
                deletable.setDeleted()
            }
        }

        val cleaned = Cleaner.clean(eraser, stringToDate("20130126"), checkers, false)

        Assertions.assertEquals(5, cleaned.size)

        Assertions.assertFalse(daily.deleted)
        Assertions.assertFalse(daily2.deleted)
        Assertions.assertFalse(daily3.deleted)
        Assertions.assertFalse(daily4.deleted)
        Assertions.assertTrue(daily5.deleted)
        Assertions.assertFalse(monthly.deleted)
        Assertions.assertFalse(monthly2.deleted)
        Assertions.assertTrue(monthly3.deleted)
        Assertions.assertFalse(quarterly.deleted)
        Assertions.assertFalse(quarterly1.deleted)
        Assertions.assertTrue(quarterly2.deleted)
        Assertions.assertTrue(quarterly3.deleted)
        Assertions.assertFalse(yearly.deleted)
        Assertions.assertTrue(yearly2.deleted)
    }

    class Fake internal constructor(string: String) {

        val date: Date = stringToDate(string)
        var deleted: Boolean = false

        internal fun setDeleted() {
            this.deleted = true
        }

    }

}
