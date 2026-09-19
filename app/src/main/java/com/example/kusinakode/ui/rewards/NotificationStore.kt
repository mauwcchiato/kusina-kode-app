package com.example.kusinakode.ui.rewards

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Read / archived / deleted state for the notifications inbox.
 *
 * The feed itself is the server's reward history, which has no notion of an
 * inbox — so the per-entry state lives here, keyed by the reward's own
 * reference. Deleting hides a row locally; it never touches the ledger, which
 * is the point: an on-chain record should not be erasable from a phone.
 */
object NotificationStore {

    private const val PREFS = "kusinakode_notifications"
    private const val K_READ = "read_ids"
    private const val K_ARCHIVED = "archived_ids"
    private const val K_DELETED = "deleted_ids"

    private val _read = MutableStateFlow<Set<String>>(emptySet())
    val read: StateFlow<Set<String>> = _read.asStateFlow()

    private val _archived = MutableStateFlow<Set<String>>(emptySet())
    val archived: StateFlow<Set<String>> = _archived.asStateFlow()

    private val _deleted = MutableStateFlow<Set<String>>(emptySet())
    val deleted: StateFlow<Set<String>> = _deleted.asStateFlow()

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _read.value = p.getStringSet(K_READ, emptySet()).orEmpty()
        _archived.value = p.getStringSet(K_ARCHIVED, emptySet()).orEmpty()
        _deleted.value = p.getStringSet(K_DELETED, emptySet()).orEmpty()
    }

    private fun save(context: Context, key: String, value: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(key, value)
            .apply()
    }

    fun markRead(context: Context, id: String, isRead: Boolean = true) {
        _read.value = if (isRead) _read.value + id else _read.value - id
        save(context, K_READ, _read.value)
    }

    fun markAllRead(context: Context, ids: Collection<String>) {
        _read.value = _read.value + ids
        save(context, K_READ, _read.value)
    }

    fun archive(context: Context, id: String, isArchived: Boolean = true) {
        _archived.value = if (isArchived) _archived.value + id else _archived.value - id
        save(context, K_ARCHIVED, _archived.value)
        // Archiving is a "dealt with" gesture, so it reads too.
        if (isArchived) markRead(context, id)
    }

    /**
     * Archives or restores a whole selection at once.
     *
     * One write rather than one per row: looping [archive] over a hundred
     * selected entries would rewrite the same preference a hundred times.
     */
    fun archiveAll(context: Context, ids: Collection<String>, isArchived: Boolean = true) {
        if (ids.isEmpty()) return
        _archived.value =
            if (isArchived) _archived.value + ids else _archived.value - ids.toSet()
        save(context, K_ARCHIVED, _archived.value)
        // Archiving is a "dealt with" gesture, so it reads too.
        if (isArchived) markAllRead(context, ids)
    }

    fun delete(context: Context, id: String) {
        _deleted.value = _deleted.value + id
        save(context, K_DELETED, _deleted.value)
    }

    fun deleteAll(context: Context, ids: Collection<String>) {
        if (ids.isEmpty()) return
        _deleted.value = _deleted.value + ids
        save(context, K_DELETED, _deleted.value)
    }

    fun markUnreadAll(context: Context, ids: Collection<String>) {
        if (ids.isEmpty()) return
        _read.value = _read.value - ids.toSet()
        save(context, K_READ, _read.value)
    }

    fun restoreAll(context: Context) {
        _deleted.value = emptySet()
        _archived.value = emptySet()
        save(context, K_DELETED, emptySet())
        save(context, K_ARCHIVED, emptySet())
    }
}
