package co.yml.network.android.engine.cache.disklrucache

import androidx.annotation.VisibleForTesting
import co.yml.network.android.engine.cache.disklrucache.DiskLruCache.Editor
import java.io.BufferedWriter
import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets.US_ASCII
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * A cache that uses a bounded amount of space on a filesystem. Each cache
 * entry has a string key and a fixed number of values. Each key must match
 * the regex **[a-z0-9_-]{1,64}**. Values are byte sequences,
 * accessible as streams or files. Each value must be between `0` and
 * `Integer.MAX_VALUE` bytes in length.
 *
 *
 * The cache stores its data in a directory on the filesystem. This
 * directory must be exclusive to the cache; the cache may delete or overwrite
 * files from its directory. It is an error for multiple processes to use the
 * same cache directory at the same time.
 *
 *
 * This cache limits the number of bytes that it will store on the
 * filesystem. When the number of stored bytes exceeds the limit, the cache will
 * remove entries in the background until the limit is satisfied. The limit is
 * not strict: the cache may temporarily exceed it while waiting for files to be
 * deleted. The limit does not include filesystem overhead or the cache
 * journal so space-sensitive applications should set a conservative limit.
 *
 *
 * Clients call [.edit] to create or update the values of an entry. An
 * entry may have only one editor at one time; if a value is not available to be
 * edited then [.edit] will return null.
 *
 *  * When an entry is being **created** it is necessary to
 * supply a full set of values; the empty value should be used as a
 * placeholder if necessary.
 *  * When an entry is being **edited**, it is not necessary
 * to supply data for every value; values default to their previous
 * value.
 *
 * Every [edit] call must be matched by a call to [Editor.commit]
 * or [Editor.abort]. Committing is atomic: a read observes the full set
 * of values as they were before or after the commit, but never a mix of values.
 *
 *
 * Clients call [.get] to read a snapshot of an entry. The read will
 * observe the value at the time that [.get] was called. Updates and
 * removals after the call do not impact ongoing reads.
 *
 *
 * This class is tolerant of some I/O errors. If files are missing from the
 * filesystem, the corresponding entries will be dropped from the cache. If
 * an error occurs while writing a cache value, the edit will fail silently.
 * Callers should handle other problems by catching `IOException` and
 * responding appropriately.
 *
 *  inspired by https://github.com/JakeWharton/DiskLruCache
 */
class DiskLruCache private constructor(
    /** Returns the directory where this cache stores its data.  */
    /**
     * This cache uses a journal file named "journal". A typical journal file
     * looks like this:
     *     libcore.io.DiskLruCache
     *     1
     *     100
     *     2
     *
     *     CLEAN 3400330d1dfc7f3f7f4b8d4d803dfcf6 832 21054
     *     DIRTY 335c4c6028171cfddfbaae1a9c313c52
     *     CLEAN 335c4c6028171cfddfbaae1a9c313c52 3934 2342
     *     REMOVE 335c4c6028171cfddfbaae1a9c313c52
     *     DIRTY 1ab96a171faeeee38496d8b330771a7a
     *     CLEAN 1ab96a171faeeee38496d8b330771a7a 1600 234
     *     READ 335c4c6028171cfddfbaae1a9c313c52
     *     READ 3400330d1dfc7f3f7f4b8d4d803dfcf6
     *
     * The first five lines of the journal form its header. They are the
     * constant string "libcore.io.DiskLruCache", the disk cache's version,
     * the application's version, the value count, and a blank line.
     *
     * Each of the subsequent lines in the file is a record of the state of a
     * cache entry. Each line contains space-separated values: a state, a key,
     * and optional state-specific values.
     *   o DIRTY lines track that an entry is actively being created or updated.
     *     Every successful DIRTY action should be followed by a CLEAN or REMOVE
     *     action. DIRTY lines without a matching CLEAN or REMOVE indicate that
     *     temporary files may need to be deleted.
     *   o CLEAN lines track a cache entry that has been successfully published
     *     and may be read. A publish line is followed by the lengths of each of
     *     its values.
     *   o READ lines track accesses for LRU.
     *   o REMOVE lines track entries that have been deleted.
     *
     * The journal file is appended to as cache operations occur. The journal may
     * occasionally be compacted by dropping redundant lines. A temporary file named
     * "journal.tmp" will be used during compaction; that file should be deleted if
     * it exists when the cache is opened.
     *
     * inspired by https://github.com/JakeWharton/DiskLruCache
     */
    val directory: File,
    private val appVersion: Int,
    private val valueCount: Int,
    maxSize: Long
) : Closeable {
    private val journalFile = File(directory, JOURNAL_FILE)
    private val journalFileTmp = File(directory, JOURNAL_FILE_TEMP)
    private val journalFileBackup = File(directory, JOURNAL_FILE_BACKUP)
    private var journalWriter: Writer? = null
    private val lruEntries = LinkedHashMap<String, Entry>(0, 0.75f, true)
    private var redundantOpCount = 0

    /** Returns true if this cache has been closed.  */
    val isClosed: Boolean
        @Synchronized get() = journalWriter == null

    /**
     * Store the number of bytes currently being used to store the values in
     * this cache. This may be greater than the max size if a background
     * deletion is pending.
     */
    var size: Long = 0
        @Synchronized get
        private set

    /**
     * Store the maximum number of bytes that this cache should use to store its data.
     */
    var maxSize = maxSize
        @Synchronized get
        @Synchronized set(value) {
            field = value
            executorService.submit(cleanupCallable)
        }

    /**
     * To differentiate between old and current snapshots, each entry is given
     * a sequence number each time an edit is committed. A snapshot is stale if
     * its sequence number is not equal to its entry's sequence number.
     */
    private var nextSequenceNumber: Long = 0

    /** This cache uses a single background thread to evict entries.  */
    internal var executorService =
        ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, LinkedBlockingQueue())
        @VisibleForTesting internal set

    internal val cleanupCallable: Callable<Void> = Callable {
        synchronized(this@DiskLruCache) {
            if (journalWriter == null) {
                return@Callable null // Closed.
            }
            trimToSize()
            if (journalRebuildRequired()) {
                rebuildJournal()
                redundantOpCount = 0
            }
        }
        return@Callable null
    }
        @VisibleForTesting get

    @Throws(DiskLruCacheException::class)
    private fun readJournal() {
        val reader = StrictLineReader(FileInputStream(journalFile), US_ASCII)
        try {
            val magic = reader.readLine()
            val version = reader.readLine()
            val appVersionString = reader.readLine()
            val valueCountString = reader.readLine()
            val blank = reader.readLine()
            if (MAGIC != magic
                || VERSION_1 != version
                || appVersion.toString() != appVersionString
                || valueCount.toString() != valueCountString
                || "" != blank
            ) {
                throw DiskLruCacheException(
                    ErrorCode.UNEXPECTED_JOURNAL_HEADER,
                    "unexpected journal header: expected [\"$MAGIC\", \"$VERSION_1\", \"$appVersion\", \"$valueCount\", \"$blank\"], actual [\"$magic\", \"$version\", \"$appVersionString\", \"$valueCountString\", \"$blank\"]"
                )
            }
            var lineCount = 0
            while (true) {
                try {
                    readJournalLine(reader.readLine())
                    lineCount++
                } catch (endOfJournal: EOFException) {
                    break
                }
            }
            redundantOpCount = lineCount - lruEntries.size

            // If we ended on a truncated line, rebuild the journal before appending to it.
            if (reader.hasUnterminatedLine()) {
                rebuildJournal()
            } else {
                journalWriter = BufferedWriter(
                    OutputStreamWriter(
                        FileOutputStream(journalFile, true),
                        US_ASCII
                    )
                )
            }
        } finally {
            closeQuietly(reader)
        }
    }

    @Throws(DiskLruCacheException::class)
    private fun readJournalLine(line: String) {
        val firstSpace = line.indexOf(' ')
        if (firstSpace == -1) {
            throw DiskLruCacheException(
                ErrorCode.INVALID_JOURNAL_LINE,
                "Invalid journal line: $line"
            )
        }
        val keyBegin = firstSpace + 1
        val secondSpace = line.indexOf(' ', keyBegin)
        val key: String
        if (secondSpace == -1) {
            key = line.substring(keyBegin)
            if (firstSpace == REMOVE.length && line.startsWith(REMOVE)) {
                lruEntries.remove(key)
                return
            }
        } else {
            key = line.substring(keyBegin, secondSpace)
        }
        val entry = lruEntries.getOrPut(key) { Entry(key) }
        if (secondSpace != -1 && firstSpace == CLEAN.length && line.startsWith(CLEAN)) {
            val parts = line.substring(secondSpace + 1).split(" ").toTypedArray()
            entry.apply {
                readable = true
                currentEditor = null
                setLengths(parts)
            }
        } else if (secondSpace == -1 && firstSpace == DIRTY.length && line.startsWith(DIRTY)) {
            entry.currentEditor = Editor(entry)
        } else if (secondSpace == -1 && firstSpace == READ.length && line.startsWith(READ)) {
            // This work was already done by calling lruEntries.get().
        } else {
            throw DiskLruCacheException(
                ErrorCode.INVALID_JOURNAL_LINE,
                "Invalid journal line: $line"
            )
        }
    }

    /**
     * Computes the initial size and collects garbage as a part of opening the
     * cache. Dirty entries are assumed to be inconsistent and will be deleted.
     */
    @Throws(DiskLruCacheException::class)
    private fun processJournal() {
        deleteIfExists(journalFileTmp)
        val iterator = lruEntries.values.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.currentEditor == null) {
                for (index in 0 until valueCount) {
                    size += entry.lengths[index]
                }
            } else {
                entry.currentEditor = null
                for (index in 0 until valueCount) {
                    deleteIfExists(entry.getCleanFile(index))
                    deleteIfExists(entry.getDirtyFile(index))
                }
                iterator.remove()
            }
        }
    }

    /**
     * Creates a new journal that omits redundant information. This replaces the
     * current journal if it exists.
     */
    @Synchronized
    @Throws(IOException::class)
    private fun rebuildJournal() {
        journalWriter?.close()
        val bufferedWriter = BufferedWriter(
            OutputStreamWriter(FileOutputStream(journalFileTmp), US_ASCII)
        )
        bufferedWriter.use { writer ->
            writer.write("$MAGIC\n$VERSION_1\n$appVersion\n$valueCount\n\n")
            lruEntries.values.forEach {
                if (it.currentEditor != null) {
                    writer.write("$DIRTY ${it.key}\n")
                } else {
                    writer.write("$CLEAN ${it.key}${it.getLengths()}\n")
                }
            }
        }
        if (journalFile.exists()) {
            renameTo(journalFile, journalFileBackup, true)
        }
        renameTo(journalFileTmp, journalFile, false)
        journalFileBackup.delete()
        journalWriter =
            BufferedWriter(OutputStreamWriter(FileOutputStream(journalFile, true), US_ASCII))
    }

    /**
     * Returns a snapshot of the entry named [key], or null if it doesn't
     * exist is not currently readable. If a value is returned, it is moved to
     * the head of the LRU queue.
     */
    @Synchronized
    @Throws(IOException::class)
    operator fun get(key: String): Snapshot? {
        checkNotClosed()
        validateKey(key)
        val entry = lruEntries[key] ?: return null
        if (!entry.readable) {
            return null
        }

        // Open all streams eagerly to guarantee that we see a single published
        // snapshot. If we opened streams lazily then the streams could come
        // from different edits.
        val inputStream = arrayOfNulls<InputStream>(valueCount)
        try {
            for (index in 0 until valueCount) {
                inputStream[index] = FileInputStream(entry.getCleanFile(index))
            }
        } catch (e: FileNotFoundException) {
            // A file must have been deleted manually!
            inputStream.forEach { closeQuietly(it) }
            return null
        }
        redundantOpCount++
        journalWriter?.append("$READ $key\n")
        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
        return Snapshot(key, entry.sequenceNumber, inputStream, entry.lengths)
    }

    /**
     * Returns an editor for the entry named [key], or null if another
     * edit is in progress.
     */

    @Synchronized
    @Throws(IOException::class)
    internal fun edit(key: String, expectedSequenceNumber: Long = ANY_SEQUENCE_NUMBER): Editor? {
        checkNotClosed()
        validateKey(key)
        var entry = lruEntries[key]
        if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER && entry?.sequenceNumber != expectedSequenceNumber) {
            return null // Snapshot is stale.
        }
        if (entry == null) {
            entry = Entry(key)
            lruEntries[key] = entry
        } else if (entry.currentEditor != null) {
            return null // Another edit is in progress.
        }
        val editor = Editor(entry)
        entry.currentEditor = editor

        // Flush the journal before creating files to prevent file leaks.
        journalWriter?.apply {
            write("$DIRTY $key\n")
            flush()
        }
        return editor
    }

    /**
     * Returns the number of bytes currently being used to store the values in
     * this cache. This may be greater than the max size if a background
     * deletion is pending.
     */
    @Synchronized
    fun size(): Long {
        return size
    }

    @Synchronized
    @Throws(IOException::class)
    private fun completeEdit(editor: Editor, success: Boolean) {
        val entry = editor.entry
        check(entry.currentEditor == editor)

        // If this edit is creating the entry for the first time, every index must have a value.
        if (success && !entry.readable) {
            for (i in 0 until valueCount) {
                editor.written?.get(i)?.let { written ->
                    if (!written) {
                        editor.abort()
                        throw DiskLruCacheException(
                            ErrorCode.NO_VALUE_AT_INDEX,
                            "Newly created entry didn't create value for index $i"
                        )
                    }
                }
                if (!entry.getDirtyFile(i).exists()) {
                    editor.abort()
                    return
                }
            }
        }
        for (i in 0 until valueCount) {
            val dirty = entry.getDirtyFile(i)
            if (success) {
                if (dirty.exists()) {
                    val clean = entry.getCleanFile(i)
                    dirty.renameTo(clean)
                    val oldLength = entry.lengths[i]
                    val newLength = clean.length()
                    entry.lengths[i] = newLength
                    size = size - oldLength + newLength
                }
            } else {
                deleteIfExists(dirty)
            }
        }
        redundantOpCount++
        entry.currentEditor = null
        if (entry.readable || success) {
            entry.readable = true
            journalWriter?.write("$CLEAN ${entry.key}${entry.getLengths()}\n")
            if (success) {
                entry.sequenceNumber = nextSequenceNumber++
            }
        } else {
            lruEntries.remove(entry.key)
            journalWriter?.write("$REMOVE ${entry.key}\n")
        }
        journalWriter?.flush()
        if (size > maxSize || journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
    }

    /**
     * We only rebuild the journal when it will halve the size of the journal
     * and eliminate at least 2000 ops.
     */
    private fun journalRebuildRequired(): Boolean {
        val redundantOpCompactThreshold = 2000
        return (redundantOpCount >= redundantOpCompactThreshold && redundantOpCount >= lruEntries.size)
    }

    /**
     * Drops the entry for [key] if it exists and can be removed. Entries
     * actively being edited cannot be removed.
     *
     * @return true if an entry was removed.
     */
    @Synchronized
    @Throws(IOException::class)
    fun remove(key: String): Boolean {
        checkNotClosed()
        validateKey(key)
        val entry = lruEntries[key]
        if (entry == null || entry.currentEditor != null) {
            return false
        }
        for (i in 0 until valueCount) {
            val file = entry.getCleanFile(i)
            if (file.exists() && !file.delete()) {
                throw DiskLruCacheException(ErrorCode.DELETE_FILE_FAILED, "failed to delete $file")
            }
            size -= entry.lengths[i]
            entry.lengths[i] = 0
        }
        redundantOpCount++
        journalWriter?.append("$REMOVE $key\n")
        lruEntries.remove(key)
        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
        return true
    }

    private fun checkNotClosed() {
        checkNotNull(journalWriter) { "cache is closed" }
    }

    /** Force buffered operations to the filesystem.  */
    @Synchronized
    @Throws(IOException::class)
    fun flush() {
        checkNotClosed()
        trimToSize()
        journalWriter?.flush()
    }

    /** Closes this cache. Stored values will remain on the filesystem.  */
    @Synchronized
    @Throws(IOException::class)
    override fun close() {
        if (journalWriter == null) {
            return  // Already closed.
        }
        lruEntries.values.forEach { it.currentEditor?.abort() }
        trimToSize()
        journalWriter?.close()
        journalWriter = null
    }

    @Throws(IOException::class)
    private fun trimToSize() {
        while (size > maxSize) {
            remove(lruEntries.entries.first().key)
        }
    }

    /**
     * Closes the cache and deletes all of its stored values. This will delete
     * all files in the cache directory including files that weren't created by
     * the cache.
     */
    @Throws(IOException::class)
    fun delete() {
        close()
        deleteContents(directory)
    }

    private fun validateKey(key: String) {
        require(STRING_KEY_PATTERN.matcher(key).matches())
        { "keys must match regex $STRING_KEY_PATTERN: \"$key\"" }
    }

    /** A snapshot of the values for an entry.  */
    inner class Snapshot(
        private val key: String,
        private val sequenceNumber: Long,
        private val inputStreams: Array<InputStream?>,
        private val lengths: LongArray
    ) : Closeable {
        /**
         * Returns an editor for this snapshot's entry, or null if either the
         * entry has changed since this snapshot was created or if another edit
         * is in progress.
         */
        @Throws(IOException::class)
        fun edit(): Editor? = this@DiskLruCache.edit(key, sequenceNumber)

        /** Returns the unbuffered stream with the value for `index`.  */
        fun getInputStream(index: Int) = inputStreams[index]

        /** Returns the string value for `index`.  */
        @Throws(IOException::class)
        fun getString(index: Int) =
            getInputStream(index)?.let { inputStreamToString(it) }

        /** Returns the byte length of the value for `index`.  */
        fun getLength(index: Int) = lengths[index]

        override fun close(): Unit = inputStreams.forEach { closeQuietly(it) }
    }

    /** Edits the values for an entry.  */
    inner class Editor internal constructor(val entry: Entry) {
        val written: BooleanArray? = if (entry.readable) null else BooleanArray(valueCount)
        private var hasErrors = false
        private var committed = false

        /**
         * Returns an unbuffered input stream to read the last committed value,
         * or null if no value has been committed.
         */
        @Throws(IOException::class)
        fun newInputStream(index: Int): InputStream? {
            synchronized(this@DiskLruCache) {
                check(entry.currentEditor == this)
                return if (!entry.readable) {
                    null
                } else try {
                    FileInputStream(entry.getCleanFile(index))
                } catch (e: FileNotFoundException) {
                    null
                }
            }
        }

        /**
         * Returns the last committed value as a string, or null if no value
         * has been committed.
         */
        @Throws(IOException::class)
        fun getString(index: Int): String? = newInputStream(index)?.let { inputStreamToString(it) }

        /**
         * @return a new unbuffered output stream to write the value at
         * [index]. If the underlying output stream encounters errors
         * when writing to the filesystem, this edit will be aborted when
         * [commit] is called. The returned output stream does not throw
         * IOExceptions.
         */
        @Throws(IOException::class)
        fun newOutputStream(index: Int): OutputStream {
            require(!(index < 0 || index >= valueCount)) {
                "Expected index $index to be greater than 0 and less than the maximum value count of $valueCount"
            }
            synchronized(this@DiskLruCache) {
                check(entry.currentEditor == this)
                written?.set(index, true)
                val dirtyFile = entry.getDirtyFile(index)
                val outputStream = try {
                    FileOutputStream(dirtyFile)
                } catch (e: FileNotFoundException) {
                    // Attempt to recreate the cache directory.
                    directory.mkdirs()
                    try {
                        FileOutputStream(dirtyFile)
                    } catch (e2: FileNotFoundException) {
                        // We are unable to recover. Silently eat the writes.
                        return NULL_OUTPUT_STREAM
                    }
                }
                return FaultHidingOutputStream(outputStream)
            }
        }

        /** Sets the value at [index] to [value].  */
        @Throws(IOException::class)
        operator fun set(index: Int, value: String?) {
            require(value != null) { "Value cannot be null" }
            var writer: Writer? = null
            try {
                writer = OutputStreamWriter(newOutputStream(index), UTF_8)
                writer.write(value)
            } finally {
                closeQuietly(writer)
            }
        }

        /**
         * Commits this edit so it is visible to readers. This releases the
         * edit lock so another edit may be started on the same key.
         */
        @Throws(IOException::class)
        fun commit() {
            if (hasErrors) {
                completeEdit(this, false)
                remove(entry.key) // The previous entry is stale.
            } else {
                completeEdit(this, true)
            }
            committed = true
        }

        /**
         * Aborts this edit. This releases the edit lock so another edit may be
         * started on the same key.
         */
        @Throws(IOException::class)
        fun abort() {
            completeEdit(this, false)
        }

        fun abortUnlessCommitted() {
            if (!committed) {
                try {
                    abort()
                } catch (ignored: IOException) {
                }
            }
        }

        private inner class FaultHidingOutputStream(out: OutputStream) : FilterOutputStream(out) {
            override fun write(oneByte: Int) {
                try {
                    out.write(oneByte)
                } catch (e: IOException) {
                    hasErrors = true
                }
            }

            override fun write(buffer: ByteArray, offset: Int, length: Int) {
                try {
                    out.write(buffer, offset, length)
                } catch (e: IOException) {
                    hasErrors = true
                }
            }

            override fun close() {
                try {
                    out.close()
                } catch (e: IOException) {
                    hasErrors = true
                }
            }

            override fun flush() {
                try {
                    out.flush()
                } catch (e: IOException) {
                    hasErrors = true
                }
            }
        }

    }

    inner class Entry constructor(val key: String) {
        /** Lengths of this entry's files.  */
        val lengths: LongArray = LongArray(valueCount)

        /** True if this entry has ever been published.  */
        var readable = false

        /** The ongoing edit or null if this entry is not being edited.  */
        var currentEditor: Editor? = null

        /** The sequence number of the most recently committed edit to this entry.  */
        var sequenceNumber: Long = 0

        @Throws(IOException::class)
        fun getLengths(): String {
            val result = StringBuilder()
            for (size in lengths) {
                result.append(' ').append(size)
            }
            return result.toString()
        }

        /** Set lengths using decimal numbers like "10123".  */
        @Throws(IOException::class)
        fun setLengths(strings: Array<String>) {
            if (strings.size != valueCount) {
                throw DiskLruCacheException(
                    ErrorCode.UNEXPECTED_JOURNAL_LINE,
                    "unexpected journal line: " + strings.contentToString()
                )
            }
            try {
                for (i in strings.indices) {
                    lengths[i] = strings[i].toLong()
                }
            } catch (e: NumberFormatException) {
                throw DiskLruCacheException(
                    ErrorCode.UNEXPECTED_JOURNAL_LINE,
                    "unexpected journal line: " + strings.contentToString()
                )

            }
        }

        fun getCleanFile(i: Int): File {
            return File(directory, "$key.$i")
        }

        fun getDirtyFile(i: Int): File {
            return File(directory, "$key.$i.tmp")
        }

    }

    companion object {
        const val JOURNAL_FILE = "journal"
        const val JOURNAL_FILE_TEMP = "journal.tmp"
        const val JOURNAL_FILE_BACKUP = "journal.bkp"
        const val MAGIC = "libcore.io.DiskLruCache"
        const val VERSION_1 = "1"
        const val ANY_SEQUENCE_NUMBER: Long = -1
        val STRING_KEY_PATTERN: Pattern = Pattern.compile("[a-z0-9_-]{1,120}")

        @VisibleForTesting
        internal const val CLEAN = "CLEAN"

        @VisibleForTesting
        internal const val DIRTY = "DIRTY"

        @VisibleForTesting
        internal const val REMOVE = "REMOVE"

        @VisibleForTesting
        internal const val READ = "READ"

        /**
         * Opens the cache in `directory`, creating a cache if none exists
         * there.
         *
         * @param directory a writable directory
         * @param valueCount the number of values per cache entry. Must be positive.
         * @param maxSize the maximum number of bytes this cache should use to store
         * @throws IOException if reading or writing the cache directory fails
         */
        @Throws(IOException::class)
        fun open(directory: File, appVersion: Int, valueCount: Int, maxSize: Long): DiskLruCache {
            require(maxSize > 0) { "maxSize <= 0" }
            require(valueCount > 0) { "valueCount <= 0" }

            // If a bkp file exists, use it instead.
            val backupFile = File(directory, JOURNAL_FILE_BACKUP)
            if (backupFile.exists()) {
                val journalFile = File(directory, JOURNAL_FILE)
                // If journal file also exists just delete backup file.
                if (journalFile.exists()) {
                    backupFile.delete()
                } else {
                    renameTo(backupFile, journalFile, false)
                }
            }

            // Prefer to pick up where we left off.
            var cache = DiskLruCache(
                directory,
                appVersion,
                valueCount,
                maxSize
            )
            if (cache.journalFile.exists()) {
                try {
                    cache.readJournal()
                    cache.processJournal()
                    cache.journalWriter =
                        OutputStreamWriter(FileOutputStream(cache.journalFile, true), US_ASCII)
                    return cache
                } catch (journalIsCorrupt: DiskLruCacheException) {
                    println("DiskLruCache $directory is corrupt: $journalIsCorrupt, removing")
                    cache.delete()
                } catch (journalIsCorrupt: IOException) {
                    println("DiskLruCache $directory is corrupt: $journalIsCorrupt, removing")
                    cache.delete()
                }
            }

            // Create a new empty cache.
            directory.mkdirs()
            cache = DiskLruCache(
                directory,
                appVersion,
                valueCount,
                maxSize
            )
            cache.rebuildJournal()
            return cache
        }

        @Throws(IOException::class)
        private fun deleteIfExists(file: File) {
            if (file.exists() && !file.delete()) {
                throw DiskLruCacheException(
                    ErrorCode.DELETE_FILE_FAILED,
                    "existing file could not be deleted"
                )
            }
        }

        @Throws(IOException::class)
        private fun renameTo(from: File, to: File, deleteDestination: Boolean) {
            if (deleteDestination) {
                deleteIfExists(to)
            }
            if (!from.renameTo(to)) {
                throw DiskLruCacheException(
                    ErrorCode.RENAME_FILE_FAILED,
                    "existing file could not be deleted"
                )
            }
        }

        @Throws(IOException::class)
        private fun inputStreamToString(inputStream: InputStream): String =
            readFully(InputStreamReader(inputStream, UTF_8))

        private val NULL_OUTPUT_STREAM: OutputStream = object : OutputStream() {
            @Throws(IOException::class)
            override fun write(b: Int) {
                // Eat all writes silently. Nom nom.
            }
        }
    }
}