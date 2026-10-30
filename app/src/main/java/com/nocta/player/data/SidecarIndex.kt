package com.nocta.player.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Indexes the folder the user granted through the Storage Access Framework so that
 * `.lrc` lyrics and `.mp4` canvas clips that live next to the audio files can be found.
 */
class SidecarIndex(
    private val context: Context,
    private val settings: SettingsStore
) {

    private val mutex = Mutex()

    @Volatile
    private var indexedTree: String? = null

    @Volatile
    private var index: Map<String, Uri> = emptyMap()

    fun invalidate() {
        indexedTree = null
        index = emptyMap()
    }

    suspend fun find(name: String): Uri? {
        ensureIndexed()
        return index[name.lowercase()]
    }

    suspend fun findFirst(vararg names: String): Uri? {
        ensureIndexed()
        return names.firstNotNullOfOrNull { index[it.lowercase()] }
    }

    suspend fun readText(name: String): String? = withContext(Dispatchers.IO) {
        val uri = find(name) ?: return@withContext null
        runCatching {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
        }.getOrNull()
    }

    suspend fun ensureIndexed() {
        val tree = settings.treeUri ?: return
        if (indexedTree == tree.toString()) return

        mutex.withLock {
            if (indexedTree == tree.toString()) return
            val root = DocumentFile.fromTreeUri(context, tree) ?: return

            val map = HashMap<String, Uri>()
            runCatching {
                collect(root, map, depth = 0, counter = intArrayOf(0))
            }

            index = map
            indexedTree = tree.toString()
        }
    }

    private fun collect(
        dir: DocumentFile,
        map: MutableMap<String, Uri>,
        depth: Int,
        counter: IntArray
    ) {
        val children = runCatching { dir.listFiles() }.getOrNull() ?: return
        for (child in children) {
            if (counter[0] > 4000) return
            counter[0]++
            val name = child.name ?: continue
            if (child.isFile) {
                map.putIfAbsent(name.lowercase(), child.uri)
            } else if (child.isDirectory && depth < 2) {
                collect(child, map, depth + 1, counter)
            }
        }
    }
}
