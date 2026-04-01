package com.codag.jetbrains.watch

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

/**
 * Listens for VFS file changes and triggers re-analysis for supported files.
 */
class CodagFileWatcher : BulkFileListener {

    private val log = Logger.getInstance(CodagFileWatcher::class.java)
    private val filter = FileChangeFilter()

    override fun after(events: MutableList<out VFileEvent>) {
        val changedPaths = events
            .filterIsInstance<VFileContentChangeEvent>()
            .mapNotNull { it.file?.path }

        val eligible = FileChangeFilter.filterBatchForReanalysis(changedPaths)
            .filter { path ->
                val now = System.currentTimeMillis()
                !filter.isDebouncedAt(path, now)
            }

        if (eligible.isNotEmpty()) {
            log.info("Codag: ${eligible.size} file(s) changed, eligible for re-analysis")
            // TODO S5+: trigger incremental re-analysis via CodagAnalysisAction
        }
    }
}
