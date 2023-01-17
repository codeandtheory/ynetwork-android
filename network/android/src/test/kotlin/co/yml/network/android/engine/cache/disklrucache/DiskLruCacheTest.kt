package co.yml.network.android.engine.cache.disklrucache

import co.yml.network.android.engine.cache.disklrucache.DiskLruCache
import co.yml.network.android.engine.cache.disklrucache.DiskLruCacheException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.StringWriter
import java.io.Writer
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

private const val ENTRY_1 = "Abcdef"
private const val ENTRY_2 = "Xyz"
private const val ENTRY_3 = "Lmnop"
private const val ENTRY_4 = "Qrst"

private const val APP_VERSION = 100
private const val VALUE_COUNT = 2

private const val CACHE_KEY_1 = "k1"
private const val CACHE_KEY_2 = "k2"
private const val CACHE_KEY_3 = "k3"
private const val CACHE_KEY_4 = "k4"
private const val CACHE_KEY_5 = "k5"
private const val CACHE_KEY_6 = "k6"
private const val CACHE_KEY_7 = "k7"

class DiskLruCacheTest {
    private lateinit var cacheDir: File
    private lateinit var journalFile: File
    private lateinit var journalBkpFile: File
    private lateinit var cache: DiskLruCache

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    @Throws(Exception::class)
    fun setUp() {
        cacheDir = File(tempDir, "DiskLruCacheTest")
        journalFile = File(cacheDir, DiskLruCache.JOURNAL_FILE)
        journalBkpFile = File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP)
        cacheDir.listFiles()?.forEach { it.delete() }
        cache = openCache()
    }

    @AfterEach
    @Throws(Exception::class)
    fun tearDown() {
        cache.close()
    }

    @Test
    fun emptyCache() {
        cache.close()
        assertJournalEquals()
    }

    @Test
    fun validateKey() {
        assertInvalidKey("my key")
        assertInvalidKey("has_space ")

        assertInvalidKey("my\nkey")
        assertInvalidKey("has_LF\n")

        assertInvalidKey("my\rkey")
        assertInvalidKey("has_CR\r")

        assertInvalidKey("has_invalid/")
        assertInvalidKey("has_invalid\u2603")
        assertInvalidKey("this_is_way_too_long_this_is_way_too_long_this_is_way_too_long_this_is_way_too_long_this_is_way_too_long_this_is_way_too_long")

        // Exactly 120 chars.
        assertValidKey("012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789")

        // Contains all valid characters.
        assertValidKey("abcdefghijklmnopqrstuvwxyz_0123456789")

        // Contains dash.
        assertValidKey("-20384573948576")
    }

    @Test
    fun writeAndReadEntry() {

        val creator = cache.edit(CACHE_KEY_1)

        assertThat(creator, `is`(notNullValue()))

        // Update the values, but don't commit them
        creator!![0] = ENTRY_1
        creator[1] = ENTRY_2

        // Verify we only get the earlier committed values
        assertThat(creator.getString(0), `is`(nullValue()))
        assertThat(creator.newInputStream(0), `is`(nullValue()))
        assertThat(creator.getString(1), `is`(nullValue()))
        assertThat(creator.newInputStream(1), `is`(nullValue()))

        creator.commit()
        // After committing, fetch the latest value
        val snapshot = cache[CACHE_KEY_1]
        assertThat(snapshot, `is`(notNullValue()))

        assertThat(snapshot!!.getString(0), `is`(ENTRY_1))
        assertThat(snapshot.getLength(0), `is`(ENTRY_1.length.toLong()))
        assertThat(snapshot.getString(1), `is`(ENTRY_2))
        assertThat(snapshot.getLength(1), `is`(ENTRY_2.length.toLong()))
    }

    @Test
    fun readAndWriteEntryAcrossCacheOpenAndClose() {
        val creator = cache.edit(CACHE_KEY_1)
        creator!![0] = ENTRY_1
        creator[1] = ENTRY_2
        creator.commit()
        cache.close()

        // Re-open the cache
        cache = openCache()
        val snapshot = cache[CACHE_KEY_1]
        assertThat(snapshot, `is`(notNullValue()))
        assertThat(snapshot!!.getString(0), `is`(ENTRY_1))
        assertThat(snapshot.getLength(0), `is`(ENTRY_1.length.toLong()))
        assertThat(snapshot.getString(1), `is`(ENTRY_2))
        assertThat(snapshot.getLength(1), `is`(ENTRY_2.length.toLong()))
        snapshot.close()
    }

    @Test
    fun readAndWriteEntryWithoutProperClose() {
        val creator = cache.edit(CACHE_KEY_1)
        creator!![0] = ENTRY_1
        creator[1] = ENTRY_2
        creator.commit()

        // Simulate a dirty close of 'cache' by opening the cache directory again.
        val cache2 = openCache()
        val snapshot = cache2[CACHE_KEY_1]
        assertThat(snapshot, `is`(notNullValue()))
        assertThat(snapshot!!.getString(0), `is`(ENTRY_1))
        assertThat(snapshot.getLength(0), `is`(ENTRY_1.length.toLong()))
        assertThat(snapshot.getString(1), `is`(ENTRY_2))
        assertThat(snapshot.getLength(1), `is`(ENTRY_2.length.toLong()))
        snapshot.close()
        cache2.close()
    }

    @Test
    fun journalWithEditAndPublish() {
        val creator = cache.edit(CACHE_KEY_1)
        assertJournalEquals("${DiskLruCache.DIRTY} $CACHE_KEY_1") // DIRTY must always be flushed.
        creator!![0] = ENTRY_1
        creator[1] = ENTRY_2
        creator.commit()
        cache.close()
        assertJournalEquals(
            "${DiskLruCache.DIRTY} $CACHE_KEY_1",
            "${DiskLruCache.CLEAN} $CACHE_KEY_1 ${ENTRY_1.length} ${ENTRY_2.length}"
        )
    }

    @Test
    fun revertedNewFileIsRemoveInJournal() {
        val creator = cache.edit(CACHE_KEY_1)
        assertJournalEquals("${DiskLruCache.DIRTY} $CACHE_KEY_1") // DIRTY must always be flushed.
        creator!![0] = ENTRY_1
        creator[1] = ENTRY_2
        creator.abort()
        cache.close()
        assertJournalEquals("${DiskLruCache.DIRTY} $CACHE_KEY_1", "REMOVE $CACHE_KEY_1")
    }

    @Test
    fun unterminatedEditIsRevertedOnClose() {
        cache.edit(CACHE_KEY_1)
        cache.close()
        assertJournalEquals(
            "${DiskLruCache.DIRTY} $CACHE_KEY_1",
            "${DiskLruCache.REMOVE} $CACHE_KEY_1"
        )
    }

    @Test
    fun journalDoesNotIncludeReadOfYetUnpublishedValue() {
        val creator = cache.edit(CACHE_KEY_1)
        assertThat(cache[CACHE_KEY_1], `is`(nullValue()))
        creator!![0] = ENTRY_2
        creator[1] = ENTRY_1
        creator.commit()
        cache.close()
        assertJournalEquals(
            "${DiskLruCache.DIRTY} $CACHE_KEY_1",
            "${DiskLruCache.CLEAN} $CACHE_KEY_1 ${ENTRY_2.length} ${ENTRY_1.length}"
        )
    }

    @Test
    fun journalWithEditAndPublishAndRead() {
        val k1Creator = cache.edit(CACHE_KEY_1)
        k1Creator!![0] = ENTRY_1
        k1Creator[1] = ENTRY_2
        k1Creator.commit()

        val k2Creator = cache.edit(CACHE_KEY_2)
        k2Creator!![0] = ENTRY_3
        k2Creator[1] = ENTRY_4
        k2Creator.commit()

        val k1Snapshot = cache[CACHE_KEY_1]
        k1Snapshot!!.close()
        cache.close()

        assertJournalEquals(
            "${DiskLruCache.DIRTY} $CACHE_KEY_1",
            "${DiskLruCache.CLEAN} $CACHE_KEY_1 ${ENTRY_1.length} ${ENTRY_2.length}",
            "${DiskLruCache.DIRTY} $CACHE_KEY_2",
            "${DiskLruCache.CLEAN} $CACHE_KEY_2 ${ENTRY_3.length} ${ENTRY_4.length}",
            "${DiskLruCache.READ} $CACHE_KEY_1"
        )
    }

    @Test
    fun assertNewOutputStreamErrors() {
        val editor = cache.edit(CACHE_KEY_1)

        assertThrows<IllegalArgumentException> { editor?.newOutputStream(-1) }
        assertThrows<IllegalArgumentException> { editor?.newOutputStream(VALUE_COUNT + 1) }
    }

    @Test
    fun cannotOperateOnEditAfterPublish() {
        val editor = cache.edit(CACHE_KEY_1)
        editor!![0] = ENTRY_1
        editor[1] = ENTRY_2
        editor.commit()
        assertInoperable(editor)
    }

    @Test
    fun cannotOperateOnEditAfterRevert() {
        val editor = cache.edit(CACHE_KEY_1)
        editor!![0] = ENTRY_1
        editor[1] = ENTRY_2
        editor.abort()
        assertInoperable(editor)
    }

    @Test
    fun explicitRemoveAppliedToDiskImmediately() {
        val editor = cache.edit(CACHE_KEY_1)
        editor!![0] = ENTRY_1
        editor[1] = ENTRY_2
        editor.commit()

        val k1 = getCleanFile(CACHE_KEY_1, 0)
        assertThat(readFile(k1), `is`(ENTRY_1))

        cache.remove(CACHE_KEY_1)
        assertThat(k1.exists(), `is`(false))
    }

    @Test
    fun openWithDirtyKeyDeletesAllFilesForThatKey() {
        cache.close()
        val cleanFile0 = getCleanFile(CACHE_KEY_1, 0)
        val cleanFile1 = getCleanFile(CACHE_KEY_1, 1)
        val dirtyFile0 = getDirtyFile(CACHE_KEY_1, 0)
        val dirtyFile1 = getDirtyFile(CACHE_KEY_1, 1)

        writeFile(cleanFile0, ENTRY_1)
        writeFile(cleanFile1, ENTRY_2)
        writeFile(dirtyFile0, ENTRY_3)
        writeFile(dirtyFile1, ENTRY_4)
        createJournal(
            "${DiskLruCache.CLEAN} $CACHE_KEY_1 1 1",
            "${DiskLruCache.DIRTY} $CACHE_KEY_1"
        )
        cache = openCache()
        assertThat(cleanFile0.exists(), `is`(false))
        assertThat(cleanFile1.exists(), `is`(false))
        assertThat(dirtyFile0.exists(), `is`(false))
        assertThat(dirtyFile1.exists(), `is`(false))
        assertThat(cache[CACHE_KEY_1], `is`(nullValue()))
    }

    @Test
    fun openWithTruncatedLineDiscardsThatLine() {
        cache.close()
        writeFile(getCleanFile(CACHE_KEY_1, 0), ENTRY_1)
        writeFile(getCleanFile(CACHE_KEY_1, 1), ENTRY_2)

        writeFile(
            journalFile,
            "${DiskLruCache.MAGIC}\n${DiskLruCache.VERSION_1}\n$APP_VERSION\n$VALUE_COUNT\n\n${DiskLruCache.CLEAN} $CACHE_KEY_1 ${ENTRY_1.length} ${ENTRY_2.length}"
        )

        cache = openCache()
        assertThat(cache[CACHE_KEY_1], `is`(nullValue()))

        // The journal is not corrupt when editing after a truncated line.
        set(CACHE_KEY_1, ENTRY_3, ENTRY_4)
        cache.close()
        cache = openCache()
        assertValue(CACHE_KEY_1, ENTRY_3, ENTRY_4)
    }

    @Test
    fun openWithCorruptJournalWithIncorrectValueCount() {
        cache.close()
        writeFile(getCleanFile(CACHE_KEY_1, 0), ENTRY_1)
        writeFile(getCleanFile(CACHE_KEY_1, 1), ENTRY_2)

        val lines = """
            ${DiskLruCache.MAGIC}
            ${DiskLruCache.VERSION_1}
            $APP_VERSION
            $VALUE_COUNT

            ${DiskLruCache.CLEAN} $CACHE_KEY_1 ${ENTRY_1.length} ${ENTRY_2.length} ${ENTRY_3.length}

        """.trimIndent()

        writeFile(journalFile, lines)

        // Assert opening cache with corrupted Journal doesn't throw an exception,
        // instead it is handled internally.
        cache = openCache()
    }

    @Test
    fun openWithCorruptJournalWithIncorrectSizes() {
        cache.close()
        writeFile(getCleanFile(CACHE_KEY_1, 0), ENTRY_1)
        writeFile(getCleanFile(CACHE_KEY_1, 1), ENTRY_2)

        val lines = """
            ${DiskLruCache.MAGIC}
            ${DiskLruCache.VERSION_1}
            $APP_VERSION
            $VALUE_COUNT

            ${DiskLruCache.CLEAN} $CACHE_KEY_1 ${ENTRY_1.length} 00000x001

        """.trimIndent()

        writeFile(journalFile, lines)

        // Assert opening cache with corrupted Journal doesn't throw an exception,
        // instead it is handled internally.
        cache = openCache()
    }

    @Test
    fun createNewEntryWithTooFewValuesFails() {
        val creator = cache.edit(CACHE_KEY_1)
        creator!![1] = ENTRY_2
        assertThrows<DiskLruCacheException> { creator.commit() }

        // After committing the invalid data, assert that all the related files are removed.
        assertThat(getCleanFile(CACHE_KEY_1, 0).exists(), `is`(false))
        assertThat(getCleanFile(CACHE_KEY_1, 1).exists(), `is`(false))
        assertThat(getDirtyFile(CACHE_KEY_1, 0).exists(), `is`(false))
        assertThat(getDirtyFile(CACHE_KEY_1, 1).exists(), `is`(false))
        assertThat(cache[CACHE_KEY_1], `is`(nullValue()))

        // Assert that a failed/invalid commit doesn't affect next commit.
        val creator2 = cache.edit(CACHE_KEY_1)
        creator2!![0] = ENTRY_3
        creator2[1] = ENTRY_4
        creator2.commit()
    }

    @Test
    fun revertWithTooFewValues() {
        val creator = cache.edit(CACHE_KEY_1)
        creator!![1] = ENTRY_2
        creator.abort()

        // After aborting the invalid data commit, assert that all the related files are removed.
        assertThat(getCleanFile(CACHE_KEY_1, 0).exists(), `is`(false))
        assertThat(getCleanFile(CACHE_KEY_1, 1).exists(), `is`(false))
        assertThat(getDirtyFile(CACHE_KEY_1, 0).exists(), `is`(false))
        assertThat(getDirtyFile(CACHE_KEY_1, 1).exists(), `is`(false))
        assertThat(cache[CACHE_KEY_1], `is`(nullValue()))
    }

    @Test
    fun growMaxSize() {
        cache.close()
        cache = openCache(cacheSize = 10)

        set(CACHE_KEY_1, "a", "aaa") // size 4 = lengthOf("a" + "aaa")
        set(CACHE_KEY_2, "bb", "bbbb") // size 6 = lengthOf("bb" + "bbbb")
        assertThat(cache.size(), `is`(10))

        cache.maxSize = 20
        set(CACHE_KEY_3, "c", "c") // size 2 = lengthOf("c" + "c")

        // Assert that the size has been updated properly
        assertThat(cache.size(), `is`(12))
    }

    @Test
    fun shrinkMaxSizeEvicts() {
        cache.close()
        cache = openCache(cacheSize = 20)

        val mockFuture = mockk<Future<Void>>()
        val mockExecutorService = mockk<ThreadPoolExecutor>()
        every { mockExecutorService.submit(cache.cleanupCallable) } returns mockFuture
        cache.executorService = mockExecutorService

        set(CACHE_KEY_1, "a", "aaa") // size 4
        set(CACHE_KEY_2, "bb", "bbbb") // size 6
        set(CACHE_KEY_3, "c", "c") // size 12

        verify(exactly = 0) { mockExecutorService.submit(cache.cleanupCallable) }
        cache.maxSize = 10
        // Assert that after shrinking the size, a background thread is launched to purge the cache.
        verify(exactly = 1) { mockExecutorService.submit(cache.cleanupCallable) }
    }

    @Test
    fun evictOnInsert() {
        cache.close()
        cache = openCache(cacheSize = 10)
        set(CACHE_KEY_1, "a", "aaa") // size 4
        set(CACHE_KEY_2, "bb", "bbbb") // size 6
        assertThat(cache.size(), `is`(10))

        // Cause the size to grow to 12 should evict 'CACHE_KEY_1'.
        set(CACHE_KEY_3, "c", "c")
        cache.flush()   // Write all the changes to the cache.

        assertThat(cache.size(), `is`(8))
        assertAbsent(CACHE_KEY_1)
        assertValue(CACHE_KEY_2, "bb", "bbbb")
        assertValue(CACHE_KEY_3, "c", "c")

        // Causing the size to grow to 10 should evict nothing.
        set("k4", "d", "d")
        cache.flush()   // Write all the changes to the cache.

        assertThat(cache.size(), `is`(10))
        assertAbsent(CACHE_KEY_1)
        assertValue(CACHE_KEY_2, "bb", "bbbb")
        assertValue(CACHE_KEY_3, "c", "c")
        assertValue(CACHE_KEY_4, "d", "d")

        // Causing the size to grow to 18 should evict 'CACHE_KEY_2' and 'CACHE_KEY_3'.
        set(CACHE_KEY_5, "eeee", "eeee")
        cache.flush()   // Write all the changes to the cache.

        assertThat(cache.size(), `is`(10))
        assertAbsent(CACHE_KEY_1)
        assertAbsent(CACHE_KEY_2)
        assertAbsent(CACHE_KEY_3)
        assertValue(CACHE_KEY_4, "d", "d")
        assertValue(CACHE_KEY_5, "eeee", "eeee")
    }

    @Test
    fun evictionHonorsLruFromCurrentSession() {
        cache.close()
        cache = openCache(cacheSize = 10)
        set(CACHE_KEY_1, "a", "a")
        set(CACHE_KEY_2, "b", "b")
        set(CACHE_KEY_3, "c", "c")
        set(CACHE_KEY_4, "d", "d")
        set(CACHE_KEY_5, "e", "e")
        cache[CACHE_KEY_2]!!.close() // 'CACHE_KEY_2' is now least recently used.

        // Causing the size to grow to 12 should evict 'CACHE_KEY_1'.
        set(CACHE_KEY_6, "f", "f")
        // Causing the size to grow to 12 should evict 'CACHE_KEY_3'.
        set(CACHE_KEY_7, "g", "g")
        cache.flush()

        assertThat(cache.size(), `is`(10))
        assertAbsent(CACHE_KEY_1)
        assertValue(CACHE_KEY_2, "b", "b")
        assertAbsent(CACHE_KEY_3)
        assertValue(CACHE_KEY_4, "d", "d")
        assertValue(CACHE_KEY_5, "e", "e")
        assertValue(CACHE_KEY_6, "f", "f")
        assertValue(CACHE_KEY_7, "g", "g")
    }

    @Test
    fun evictionHonorsLruFromPreviousSession() {
        set(CACHE_KEY_1, "a", "a")
        set(CACHE_KEY_2, "b", "b")
        set(CACHE_KEY_3, "c", "c")
        set(CACHE_KEY_4, "d", "d")
        set(CACHE_KEY_5, "e", "e")
        set(CACHE_KEY_6, "f", "f")
        cache[CACHE_KEY_2]!!.close() // 'CACHE_KEY_2' is now least recently used.

        assertThat(cache.size(), `is`(12))

        cache.close()
        cache = openCache(cacheSize = 10)

        set(CACHE_KEY_7, "g", "g")
        cache.flush()

        assertThat(cache.size(), `is`(10))
        assertAbsent(CACHE_KEY_1)
        assertValue(CACHE_KEY_2, "b", "b")
        assertAbsent(CACHE_KEY_3)
        assertValue(CACHE_KEY_4, "d", "d")
        assertValue(CACHE_KEY_5, "e", "e")
        assertValue(CACHE_KEY_6, "f", "f")
        assertValue(CACHE_KEY_7, "g", "g")
    }

    @Test
    fun cacheSingleEntryOfSizeGreaterThanMaxSize() {
        cache.close()
        cache = openCache(cacheSize = 10)
        set(CACHE_KEY_1, "aaaaa", "aaaaaa") // size=11
        cache.flush()

        assertAbsent(CACHE_KEY_1)
    }

    @Test
    fun constructorDoesNotAllowZeroCacheSize() {
        val exception = assertThrows<IllegalArgumentException> { openCache(cacheSize = 0) }
        assertThat(exception.message, `is`("maxSize <= 0"))
    }

    @Test
    fun constructorDoesNotAllowZeroValuesPerEntry() {
        val exception =
            assertThrows<IllegalArgumentException> { openCache(valueCount = 0, cacheSize = 10) }
        assertThat(exception.message, `is`("valueCount <= 0"))
    }

    @Test
    fun removeAbsentElement() {
        // Assert that removing absent key doesn't throw any error.
        cache.remove(CACHE_KEY_1)
    }

    @Test
    fun readingTheSameStreamMultipleTimes() {
        set(CACHE_KEY_1, "a", "b")
        val snapshot = cache[CACHE_KEY_1]
        val inputStream1 = snapshot!!.getInputStream(0)
        val inputStream2 = snapshot.getInputStream(0)

        // Assert that even though we open the stream multiple times, the streams are same/equal.
        assertThat(inputStream1, `is`(inputStream2))
        snapshot.close()
    }

    @Test
    fun rebuildJournalOnRepeatedReads() {
        set(CACHE_KEY_1, "a", "a")
        set(CACHE_KEY_2, "b", "b")
        var lastJournalLength = 0L
        while (true) {
            val journalLength = journalFile.length()
            assertValue(CACHE_KEY_1, "a", "a")
            assertValue(CACHE_KEY_2, "b", "b")
            if (journalLength < lastJournalLength) {
                println("Journal compacted from $lastJournalLength bytes to $journalLength bytes")
                break // Test passed!
            }
            lastJournalLength = journalLength
        }
    }

    @Test
    fun rebuildJournalOnRepeatedEdits() {
        var lastJournalLength: Long = 0
        while (true) {
            val journalLength: Long = journalFile.length()
            set("a", "a", "a")
            set("b", "b", "b")
            if (journalLength < lastJournalLength) {
                println("Journal compacted from $lastJournalLength bytes to $journalLength bytes")
                break
            }
            lastJournalLength = journalLength
        }

        // Sanity check that a rebuilt journal behaves normally.
        assertValue("a", "a", "a")
        assertValue("b", "b", "b")
    }

    @Test
    fun restoreBackupFile() {
        val creator = cache.edit(CACHE_KEY_1)
        creator!![0] = ENTRY_1
        creator[1] = ENTRY_2
        creator.commit()
        cache.close()

        // Rename journal file to backup file.
        assertThat(journalFile.renameTo(journalBkpFile), `is`(true))
        assertThat(journalFile.exists(), `is`(false))

        cache = openCache()
        val snapshot = cache[CACHE_KEY_1]
        assertThat(snapshot!!.getString(0), `is`(ENTRY_1))
        assertThat(snapshot.getLength(0), `is`(ENTRY_1.length.toLong()))
        assertThat(snapshot.getString(1), `is`(ENTRY_2))
        assertThat(snapshot.getLength(1), `is`(ENTRY_2.length.toLong()))
        assertThat(journalBkpFile.exists(), `is`(false))

        assertThat(journalFile.exists(), `is`(true))
    }

    @Test
    fun journalFileIsPreferredOverBackupFile() {
        var creator = cache.edit(CACHE_KEY_1)
        creator!![0] = ENTRY_1
        creator[1] = ENTRY_2
        creator.commit()
        cache.flush()

        journalFile.copyTo(journalBkpFile, true)

        creator = cache.edit(CACHE_KEY_2)
        creator!![0] = ENTRY_3
        creator[1] = ENTRY_4
        creator.commit()
        cache.close()

        assertThat(journalFile.exists(), `is`(true))
        assertThat(journalBkpFile.exists(), `is`(true))

        cache = openCache()
        val snapshotA = cache[CACHE_KEY_1]
        assertThat(snapshotA!!.getString(0), `is`(ENTRY_1))
        assertThat(snapshotA.getLength(0), `is`(ENTRY_1.length.toLong()))
        assertThat(snapshotA.getString(1), `is`(ENTRY_2))
        assertThat(snapshotA.getLength(1), `is`(ENTRY_2.length.toLong()))

        val snapshotB = cache[CACHE_KEY_2]
        assertThat(snapshotB!!.getString(0), `is`(ENTRY_3))
        assertThat(snapshotB.getLength(0), `is`(ENTRY_3.length.toLong()))
        assertThat(snapshotB.getString(1), `is`(ENTRY_4))
        assertThat(snapshotB.getLength(1), `is`(ENTRY_4.length.toLong()))
        assertThat(
            journalBkpFile.exists(), `is`(false)
        )
        assertThat(journalFile.exists(), `is`(true))
    }

    @Test
    fun openCreatesDirectoryIfNecessary() {
        cache.close()
        val dir = File(tempDir, "testOpenCreatesDirectoryIfNecessary")
        cache = openCache(directory = dir)
        set(CACHE_KEY_1, "a", "a")

        assertThat(File(dir, "$CACHE_KEY_1.0").exists(), `is`(true))
        assertThat(File(dir, "$CACHE_KEY_1.1").exists(), `is`(true))
        assertThat(File(dir, "journal").exists(), `is`(true))
    }

    @Test
    fun fileDeletedExternally() {
        set(CACHE_KEY_1, "a", "a")
        getCleanFile(CACHE_KEY_1, 1).delete()
        assertThat(cache[CACHE_KEY_1], `is`(nullValue()))
    }

    @Test
    fun editSnapshotAfterChangeCommitted() {
        set(CACHE_KEY_1, "a", "a")
        val snapshot = cache[CACHE_KEY_1]
        val editor = snapshot!!.edit()
        editor!![0] = "b"
        editor.commit()

        assertThat(snapshot.edit(), `is`(nullValue()))
    }

    @Test
    fun editSinceEvictedAndRecreated() {
        cache.close()
        cache = openCache(cacheSize = 10)
        set(CACHE_KEY_1, "aa", "aaa") // size 5
        val snapshot = cache[CACHE_KEY_1]
        set(CACHE_KEY_2, "bb", "bbb") // size 5
        set(CACHE_KEY_3, "cc", "ccc") // size 5; will evict 'CACHE_KEY_1'
        set(CACHE_KEY_1, "a", "aaaa") // size 5; will evict 'CACHE_KEY_2'
        cache.flush()

        assertThat(snapshot!!.edit(), `is`(nullValue()))
    }

    /** @see <a href="https://github.com/JakeWharton/DiskLruCache/issues/2">https://github.com/JakeWharton/DiskLruCache/issues/2</a>
     */
    @Test
    fun aggressiveClearingHandlesWrite() {
        cacheDir.deleteRecursively()

        set(CACHE_KEY_1, "a", "a")
        // Assert that after deleting the existing file, when user set some value, a new file would be created and used.
        assertValue(CACHE_KEY_1, "a", "a")
    }

    /** @see <a href="https://github.com/JakeWharton/DiskLruCache/issues/2">https://github.com/JakeWharton/DiskLruCache/issues/2</a>
     */
    @Test
    fun aggressiveClearingHandlesEdit() {
        set(CACHE_KEY_1, "a", "a")
        val editor = cache[CACHE_KEY_1]!!.edit()

        cacheDir.deleteRecursively()
        editor!![1] = "a2"
        editor.commit()
    }

    @Test
    fun removeHandlesMissingFile() {
        set(CACHE_KEY_1, "a", "a")
        getCleanFile(CACHE_KEY_1, 0).delete()
        cache.remove(CACHE_KEY_1)
    }

    /** @see <a href="https://github.com/JakeWharton/DiskLruCache/issues/2">https://github.com/JakeWharton/DiskLruCache/issues/2</a>
     */
    @Test
    fun aggressiveClearingHandlesRead() {
        cacheDir.deleteRecursively()
        assertThat(cache[CACHE_KEY_1], `is`(nullValue()))
    }

    private fun assertJournalEquals(vararg expectedBodyLines: String) {
        val expectedLines = ArrayList<String>().apply {
            add(DiskLruCache.MAGIC)
            add(DiskLruCache.VERSION_1)
            add(APP_VERSION.toString())
            add(VALUE_COUNT.toString())
            add("")
            addAll(expectedBodyLines.asList())
        }
        assertThat(readJournalLines(), `is`(expectedLines))
    }

    @Throws(Exception::class)
    private fun createJournal(
        vararg bodyLines: String,
        magic: String = DiskLruCache.MAGIC,
        version: String = DiskLruCache.VERSION_1,
        appVersion: String = APP_VERSION.toString(),
        valueCount: String = VALUE_COUNT.toString(),
        blank: String = ""
    ) {
        val writer: Writer = FileWriter(journalFile)
        writer.write("$magic\n")
        writer.write("$version\n")
        writer.write("$appVersion\n")
        writer.write("$valueCount\n")
        writer.write("$blank\n")
        for (line in bodyLines) {
            writer.write("$line\n")
        }
        writer.close()
    }

    private fun readJournalLines() = ArrayList<String>().apply {
        BufferedReader(FileReader(journalFile)).useLines(::addAll)
    }

    private fun getCleanFile(key: String, index: Int) = File(cacheDir, "$key.$index")

    private fun getDirtyFile(key: String, index: Int) = File(cacheDir, "$key.$index.tmp")

    private fun generateSomeGarbageFiles() {
        val dir1 = File(cacheDir, "dir1")
        val dir2 = File(dir1, "dir2")
        writeFile(getCleanFile("g1", 0), "A")
        writeFile(getCleanFile("g1", 1), "B")
        writeFile(getCleanFile("g2", 0), "C")
        writeFile(getCleanFile("g2", 1), "D")
        writeFile(getCleanFile("g2", 1), "D")
        writeFile(File(cacheDir, "otherFile0"), "E")
        dir1.mkdir()
        dir2.mkdir()
        writeFile(File(dir2, "otherFile1"), "F")
    }

    private fun assertGarbageFilesAllDeleted() {
        assertThat(getCleanFile("g1", 0).exists(), `is`(true))
        assertThat(getCleanFile("g1", 1).exists(), `is`(false))
        assertThat(getCleanFile("g2", 0).exists(), `is`(false))
        assertThat(getCleanFile("g2", 1).exists(), `is`(false))
        assertThat(File(cacheDir, "otherFile0").exists(), `is`(false))
        assertThat(File(cacheDir, "dir1").exists(), `is`(false))
    }

    private fun set(key: String, value0: String, value1: String) {
        val editor = cache.edit(key)
        editor!![0] = value0
        editor[1] = value1
        editor.commit()
    }

    private fun assertAbsent(key: String) {
        val snapshot = cache[key]
        try {
            assertThat(snapshot, `is`(nullValue()))
        } finally {
            snapshot?.close()
        }
        assertThat(getCleanFile(key, 0).exists(), `is`(false))
        assertThat(getCleanFile(key, 1).exists(), `is`(false))
        assertThat(getDirtyFile(key, 0).exists(), `is`(false))
        assertThat(getDirtyFile(key, 1).exists(), `is`(false))
    }

    private fun assertValue(key: String, value0: String, value1: String) {
        val snapshot = cache[key]
        assertThat(snapshot!!.getString(0), `is`(value0))
        assertThat(snapshot.getLength(0), `is`(value0.length.toLong()))
        assertThat(snapshot.getString(1), `is`(value1))
        assertThat(snapshot.getLength(1), `is`(value1.length.toLong()))
        assertThat(getCleanFile(key, 0).exists(), `is`(true))
        assertThat(getCleanFile(key, 1).exists(), `is`(true))
        snapshot.close()
    }

    private fun readFile(file: File): String {
        val reader = FileReader(file)
        val writer = StringWriter()
        val buffer = CharArray(1024)
        var count: Int
        while (reader.read(buffer).also { count = it } != -1) {
            writer.write(buffer, 0, count)
        }
        reader.close()
        return writer.toString()
    }

    private fun writeFile(file: File?, content: String?) {
        FileWriter(file).use { it.write(content) }
    }

    /**
     * Assert that the passed [editor] is not further operable i.e. any operation performed on passed
     * [editor] would throw an error
     */
    private fun assertInoperable(editor: DiskLruCache.Editor) {
        assertThrows<IllegalStateException> { editor.getString(0) }
        assertThrows<IllegalStateException> { editor[0] = ENTRY_1 }
        assertThrows<IllegalStateException> { editor.newInputStream(0) }
        assertThrows<IllegalStateException> { editor.newOutputStream(0) }
        assertThrows<IllegalStateException> { editor.commit() }
        assertThrows<IllegalStateException> { editor.abort() }
    }

    private fun openCache(
        directory: File = cacheDir,
        valueCount: Int = VALUE_COUNT,
        cacheSize: Long = Long.MAX_VALUE
    ) = DiskLruCache.open(directory, APP_VERSION, valueCount, cacheSize)

    private fun assertInvalidKey(key: String) {
        val exception = assertThrows<IllegalArgumentException> { cache.edit(key) }
        assertThat(
            exception.message,
            `is`("keys must match regex ${DiskLruCache.STRING_KEY_PATTERN}: \"$key\"")
        )
    }

    private fun assertValidKey(key: String) {
        val editor = cache.edit(key)
        editor?.abortUnlessCommitted()
    }
}