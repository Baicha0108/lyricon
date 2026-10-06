package io.github.proify.lyricon.app.history

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import android.util.AtomicFile
import io.github.proify.lyricon.app.bridge.history.HistoryContract
import java.io.File

/** App-owned persistence remains available even when no app screen is running. */
class ListeningHistoryProvider : ContentProvider() {
    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val ctx = requireNotNull(context)
        val caller = Binder.getCallingUid()
        if (caller != Process.myUid() && ctx.packageManager.getPackagesForUid(caller)
            ?.contains("com.android.systemui") != true) {
            throw SecurityException("History is private to Lyricon and SystemUI")
        }
        synchronized(LOCK) {
            val prefs = ctx.getSharedPreferences(HistoryContract.PREFS, 0)
            val enabled = prefs.getBoolean(HistoryContract.ENABLED, false)
            val generation = prefs.getLong(HistoryContract.GENERATION, 0)
            val result = Bundle().apply { putBoolean(HistoryContract.ENABLED, enabled); putLong(HistoryContract.GENERATION, generation) }
            if (method == HistoryContract.CONFIG) return result
            val data = extras ?: return result
            if (!enabled || data.getLong(HistoryContract.GENERATION, -1) != generation) return result
            val record = HistoryContract.decode(data) ?: return result
            val database = HistoryDatabase.get(ctx)
            if (method == HistoryContract.RECORD) database.record(record)
            if (method == HistoryContract.ARTWORK) {
                val bytes = data.getByteArray("cover")?.takeIf { it.size <= 300_000 } ?: return result
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                if (options.outWidth !in 1..1024 || options.outHeight !in 1..1024) return result
                val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return result
                try {
                    val dir = File(ctx.filesDir, "listening-covers").apply { mkdirs() }
                    val target = AtomicFile(File(dir, HistoryDatabase.coverKey(record.track) + ".jpg"))
                    val output = target.startWrite()
                    try {
                        check(image.compress(Bitmap.CompressFormat.JPEG, 90, output))
                        target.finishWrite(output)
                    } catch (e: Exception) { target.failWrite(output); throw e }
                } finally { image.recycle() }
            }
            ctx.contentResolver.notifyChange(uri(ctx.packageName), null)
            result.putBoolean("saved", true)
            return result
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        val LOCK = Any()
        fun uri(packageName: String): Uri = Uri.parse("content://${HistoryContract.authority(packageName)}")
    }
}
