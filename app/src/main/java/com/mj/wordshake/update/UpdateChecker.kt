package com.mj.wordshake.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** A published release worth offering. */
data class Release(
    val tag: String,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val bytes: Long,
)

/**
 * Checks GitHub Releases for a newer build, downloads the APK into the cache
 * and hands it to the system installer.
 *
 * There is no version.json here: the release API already reports the tag and
 * the asset URL, so the tag is the version and the first `.apk` asset is the
 * download. That means a release cannot be published in a state where the app
 * knows about a version it cannot fetch.
 */
class UpdateChecker(private val context: Context) {

    /** Deletes an APK left behind by an install that was started but abandoned. */
    fun cleanupStaleApk() {
        runCatching { apkFile().takeIf { it.exists() }?.delete() }
    }

    fun currentVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
    }.getOrDefault("0")

    /** Null when the network is unreachable or the release has no APK attached. */
    suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val body = URL(RELEASES_URL).openHttp().use { connection ->
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.inputStream.bufferedReader().use { it.readText() }
            }
            val json = JSONObject(body)
            val assets = json.getJSONArray("assets")
            var url: String? = null
            var size = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk", ignoreCase = true)) {
                    url = asset.getString("browser_download_url")
                    size = asset.optLong("size", 0L)
                    break
                }
            }
            val tag = json.getString("tag_name")
            Release(
                tag = tag,
                title = json.optString("name", tag).ifBlank { tag },
                notes = plainText(json.optString("body", "")),
                apkUrl = url ?: return@runCatching null,
                bytes = size,
            )
        }.getOrNull()
    }

    /**
     * Downloads [release], reporting progress as a 0..1 fraction, or null on
     * failure. A partial file is deleted rather than left for the installer to
     * choke on.
     */
    suspend fun download(release: Release, onProgress: (Float) -> Unit): File? =
        withContext(Dispatchers.IO) {
            val target = apkFile()
            runCatching {
                URL(release.apkUrl).openHttp().use { connection ->
                    connection.instanceFollowRedirects = true
                    connection.readTimeout = DOWNLOAD_TIMEOUT_MS
                    val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.bytes
                    connection.inputStream.use { input ->
                        target.outputStream().use { output ->
                            val buffer = ByteArray(BUFFER)
                            var done = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                done += read
                                if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
                target
            }.getOrElse {
                target.delete()
                null
            }
        }

    /**
     * Sideloading needs the user's blessing per app on API 26+, and it cannot
     * be requested inline — only pointed at.
     */
    fun canInstall(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }

    fun install(apk: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        true
    }.getOrDefault(false)

    private fun apkFile() = File(context.cacheDir, "WordShake-update.apk")

    private fun URL.openHttp(): HttpURLConnection =
        (openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = CONNECT_TIMEOUT_MS
        }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try {
            block(this)
        } finally {
            disconnect()
        }

    companion object {
        const val RELEASES_URL =
            "https://api.github.com/repos/Oliver29Klozoff/WordShake/releases/latest"

        private const val APK_MIME = "application/vnd.android.package-archive"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val DOWNLOAD_TIMEOUT_MS = 60_000
        private const val BUFFER = 16 * 1024

        private val LINK = Regex("""\[([^]]*)]\([^)]*\)""")
        private val EMPHASIS = Regex("""(\*{1,3}|_{1,3}|`)(.+?)\1""")
        private val HEADING = Regex("""(?m)^\s{0,3}#{1,6}\s*""")
        private val BULLET = Regex("""(?m)^\s{0,3}[-*+]\s+""")
        private val BLANK_RUN = Regex("""\n{3,}""")

        /**
         * Flattens GitHub's markdown for a plain [android.widget.TextView]-style
         * Text. Without this the notes arrive full of literal asterisks, which
         * reads as a bug rather than as emphasis.
         */
        fun plainText(markdown: String): String = markdown
            .replace("\r\n", "\n")
            .replace(LINK, "$1")
            .replace(HEADING, "")
            .replace(BULLET, "• ")
            // Run twice so bold-inside-italic and similar nesting unwraps.
            .replace(EMPHASIS, "$2")
            .replace(EMPHASIS, "$2")
            .lineSequence().joinToString("\n") { it.trimEnd() }
            .replace(BLANK_RUN, "\n\n")
            .trim()

        /**
         * True when [tag] names a later version than [current]. Tags may carry
         * a leading "v" and either side may have a different number of parts,
         * so "v1.0" beats "0.9" and "0.4.1" beats "0.4".
         */
        fun isNewer(tag: String, current: String): Boolean {
            fun parts(text: String) =
                text.trim().trimStart('v', 'V').split(".").map { part ->
                    part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
                }

            val remote = parts(tag)
            val local = parts(current)
            for (i in 0 until maxOf(remote.size, local.size)) {
                val r = remote.getOrElse(i) { 0 }
                val l = local.getOrElse(i) { 0 }
                if (r != l) return r > l
            }
            return false
        }
    }
}
