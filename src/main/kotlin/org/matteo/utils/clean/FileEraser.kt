package org.matteo.utils.clean

import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class FileEraser @JvmOverloads constructor(private val paths: Collection<String> = listOf()) : Eraser<File> {

    private val dateFormat: SimpleDateFormat
    private val datePattern: Pattern

    override val deletables: Collection<Deletable<File>>
        get() {
            return paths.map { path -> File(path) }
                .filter { folder -> folder.isDirectory }
                .flatMap { folder ->
                    LOGGER.info("Analysing folder {}", folder.path)
                    folder.listFiles()?.toList() ?: emptyList()
                }.mapNotNull { file -> getDeletable(file) }
        }

    init {
        this.dateFormat = SimpleDateFormat(DATE_FORMAT)
        this.dateFormat.isLenient = false
        this.datePattern = Pattern.compile(DATE_PATTERN)
    }

    override fun erase(deletable: File) {
        LOGGER.info("Deleting {}", deletable.path)
        delete(deletable)
    }

    fun getDeletable(file: File): Deletable<File>? {
        val matcher = datePattern.matcher(file.name)
        if (matcher.find()) {
            val group = matcher.group()
            try {
                val date = getDate(group)
                return object : Deletable<File> {
                    override val date: Date
                        get() = date

                    override val element: File
                        get() = file
                }
            } catch (e: ParseException) {
                LOGGER.warn("Ignoring file '{}' since it has an invalid date: {}", file, e.message)
            }

        }
        return null
    }

    @Throws(ParseException::class)
    fun getDate(group: String): Date {
        return dateFormat.parse(group)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(FileEraser::class.java)

        private const val DATE_FORMAT = "yyyyMMdd"
        private const val DATE_PATTERN = "(\\\\B|\\\\b)*20[0-9]{2}[01][0-9][0-3][0-9](\\\\B|\\\\b)*"

        @Throws(IOException::class)
        private fun delete(path: File) {
            if (path.exists()) {
                if (path.isDirectory) {
                    path.listFiles()?.forEach { file ->
                        if (file.isDirectory) {
                            delete(file)
                        } else {
                            Files.delete(file.toPath())
                        }
                    }
                }
                Files.delete(path.toPath())
            }
        }
    }

}
