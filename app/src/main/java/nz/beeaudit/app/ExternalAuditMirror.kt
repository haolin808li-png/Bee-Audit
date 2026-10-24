package nz.beeaudit.app

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

class ExternalAuditMirror(private val context: Context) {
    fun mirrorFile(
        rootUri: Uri,
        source: File,
        pin: String,
        visitDate: String,
        group: String,
        sequenceNumber: Int,
    ): Boolean {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return false
        val recordDir = root
            .directory("audit")
            .directory(AuditFileNames.clean(pin))
            .directory(AuditFileNames.clean(visitDate))
            .directory(AuditFileNames.recordPrefix(pin, group, sequenceNumber))
        val target = recordDir.recreateFile(source.name) ?: return false
        context.contentResolver.openOutputStream(target.uri)?.use { output ->
            source.inputStream().use { input -> input.copyTo(output) }
        } ?: return false
        return true
    }
}

private fun DocumentFile.directory(name: String): DocumentFile {
    return findFile(name)?.takeIf { it.isDirectory } ?: createDirectory(name)
        ?: error("Could not create directory $name")
}

private fun DocumentFile.recreateFile(name: String): DocumentFile? {
    findFile(name)?.delete()
    return createFile(mimeTypeFor(name), name)
}

private fun mimeTypeFor(name: String): String {
    return when {
        name.endsWith(".jpg", ignoreCase = true) -> "image/jpeg"
        name.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
        name.endsWith(".json", ignoreCase = true) -> "application/json"
        name.endsWith(".csv", ignoreCase = true) -> "text/csv"
        else -> "application/octet-stream"
    }
}
