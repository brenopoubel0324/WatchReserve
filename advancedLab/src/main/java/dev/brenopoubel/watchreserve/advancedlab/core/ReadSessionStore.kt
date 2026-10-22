package dev.brenopoubel.watchreserve.advancedlab.core

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.util.UUID

class ReadSessionStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "read-session.properties"))

    fun load(): ReadSession? = synchronized(lock) {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return@synchronized null
        file.openRead().bufferedReader(Charsets.UTF_8).use { input ->
            val buffer = CharArray(16_385)
            var length = 0
            while (length < buffer.size) {
                val count = input.read(buffer, length, buffer.size - length)
                if (count < 0) break
                length += count
            }
            require(length <= 16_384) { "Registro de coleta excedeu o limite." }
            ReadSession.decode(String(buffer, 0, length))
        }
    }

    fun save(session: ReadSession) = synchronized(lock) {
        val current = load()
        require(current == null || current.id == session.id || current.phase.terminal) { "Já existe uma sessão pendente." }
        val stream = file.startWrite()
        try {
            stream.write(session.encode().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (failure: Exception) { file.failWrite(stream); throw failure }
    }

    fun update(id: String, transform: (ReadSession) -> ReadSession): ReadSession = synchronized(lock) {
        val current = checkNotNull(load()) { "Registro da coleta não encontrado." }
        check(current.id == id) { "A resposta pertence a outra coleta." }
        transform(current).also { save(it) }
    }

    fun reconcile(): ReadSession? = synchronized(lock) {
        load()?.let { current ->
            if (current.phase.active) {
                check(current.ownerToken != processToken) { "Já existe uma coleta neste processo." }
                current.transition(ReadPhase.INTERRUPTED, System.currentTimeMillis(),
                    reason = "O processo anterior terminou; a coleta não foi retomada.").also { save(it) }
            } else current
        }
    }

    /** Remove apenas receita/estado de leitura, nunca um diário de alterações. */
    fun discard() = synchronized(lock) {
        val current = runCatching { load() }.getOrNull()
        check(current?.phase?.active != true) { "Encerre a coleta antes de descartar seu registro." }
        file.delete()
        check(!file.baseFile.exists()) { "Não foi possível descartar o registro de coleta." }
    }

    companion object {
        private val lock = Any()
        val processToken: String = UUID.randomUUID().toString()
    }
}
