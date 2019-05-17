package org.matteo.utils.clean

import org.junit.jupiter.api.Test

import java.io.File
import java.util.Date

import org.junit.jupiter.api.Assertions.*

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 12/02/13
 */
internal class FileEraserTest {

    private val today = "20121212"

    @Test
    @Throws(Exception::class)
    fun test() {
        val eraser = FileEraser()
        val date = eraser.getDate("20121212")
        assertTrue(checkDeletable(eraser, File("log_$today"), date))
        assertTrue(checkDeletable(eraser, File("$today.log"), date))
        assertTrue(checkDeletable(eraser, File(today), date))
        assertFalse(checkDeletable(eraser, File("log"), date))
        assertFalse(checkDeletable(eraser, File("20130239"), date))
        assertFalse(checkDeletable(eraser, File("20130229"), date))
    }

    @Test
    @Throws(Exception::class)
    fun testPath() {
        val folder1 = File("folder1")
        if (!folder1.isDirectory && !folder1.mkdir()) {
            throw Exception("Unable to create folder ${folder1.absolutePath}")
        }
        val file1 = File(folder1, "${today}_file1")
        if (!file1.isFile && !file1.createNewFile()) {
            throw Exception("Unable to create file ${file1.absolutePath}")
        }
        val folder2 = File("folder2")
        if (!folder2.isDirectory && !folder2.mkdir()) {
            throw Exception("Unable to create folder ${folder2.absolutePath}")
        }
        val file2 = File(folder2, "${today}_file2")
        if (!file2.isFile && !file2.createNewFile()) {
            throw Exception("Unable to create file ${file2.absolutePath}")
        }
        val eraser = FileEraser(listOf(folder1.absolutePath, folder2.absolutePath))
        assertEquals(2, eraser.deletables.size)
        eraser.erase(folder1)
        assertFalse(folder1.exists())
        eraser.erase(folder2)
        assertFalse(folder2.exists())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteFolder() {
        val file = File("${today}_folder")
        if (!file.isDirectory && !file.mkdir()) {
            throw Exception("Unable to create folder ${file.absolutePath}")
        }
        assertTrue(file.exists())
        val eraser = FileEraser()
        eraser.erase(file)
        assertFalse(file.exists())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteFile() {
        val file = File(today + "_file")
        if (!file.isFile && !file.createNewFile()) {
            throw Exception("Unable to create folder " + file.absolutePath)
        }
        assertTrue(file.exists())
        val eraser = FileEraser()
        eraser.erase(file)
        assertFalse(file.exists())
    }

    private fun checkDeletable(eraser: FileEraser, file: File, date: Date): Boolean {
        val deletableFile = eraser.getDeletable(file)
        if (deletableFile != null) {
            assertEquals(date, deletableFile.date)
            return true
        }
        return false
    }
}
