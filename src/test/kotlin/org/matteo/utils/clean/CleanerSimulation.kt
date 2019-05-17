package org.matteo.utils.clean

import java.io.File
import java.util.*

object CleanerSimulation {

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 2) {
            System.err.println(String.format("Usage: %s <yyyyMMdd> <file>", CleanerSimulation::class.java.simpleName))
        } else {
            val reference = args[0]
            val checkers = listOf(
                Checkers.YEARLY.with(1),
                Checkers.MONTHLY.with(1),
                Checkers.WEEKLY.with(4),
                Checkers.DAILY.with(3)
            )
            val deleted = Cleaner.clean(SimulatedEraser(args[1]), stringToDate(reference), checkers, true)
            for (string in deleted) {
                println(string)
            }
            println("Total: ${deleted.size}")
        }
    }

    internal class SimulatedEraser(private val file: String) : Eraser<String> {

        override val deletables: Collection<Deletable<String>>
            get() {
                val deletables = mutableListOf<Deletable<String>>()
                File(file).useLines {
                    it.forEach { line ->
                        val date = stringToDate(line)
                        deletables.add(object : Deletable<String> {
                            override val date: Date
                                get() = date

                            override val element: String
                                get() = line
                        })
                    }
                }
                return deletables
            }

        override fun erase(deletable: String) {
            println("Erasing $deletable")
        }
    }

}
