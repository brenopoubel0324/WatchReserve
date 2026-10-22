package dev.brenopoubel.watchreserve.advancedlab.core

import android.content.Context
import java.io.File
import java.util.UUID

class ReportStore(context: Context) {
    private val directory = File(context.noBackupFilesDir, "diagnostics").apply {
        check(isDirectory || mkdirs()) { "Armazenamento privado indisponível." }
    }
    fun pending(id: String = UUID.randomUUID().toString()): File {
        require(UUID.fromString(id).toString() == id)
        return File(directory, "report-$id.pending")
    }
    fun complete(pending: File): File {
        require(pending.parentFile == directory && pending.extension == "pending")
        val destination = File(directory, pending.nameWithoutExtension + ".zip")
        check(pending.renameTo(destination)) { "Falha ao concluir o relatório privado." }
        return destination
    }
    fun latest(): File? = directory.listFiles()?.filter {
        it.isFile && it.name.matches(Regex("report-[0-9a-f-]{36}\\.zip"))
    }?.maxByOrNull { it.lastModified() }
}
