package com.accelerator.network.android.engine.cache.disklrucache

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileWriter
import java.io.Writer

private const val APP_VERSION = 100
private const val VALUE_COUNT = 2

class DiskLruCacheAndroidTest {
    private lateinit var cacheDir: File
    private lateinit var journalFile: File
    private lateinit var journalBkpFile: File
    private lateinit var cache: DiskLruCache

    @get:Rule
    var tempDir = TemporaryFolder()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        cacheDir = tempDir.newFolder("DiskLruCacheTest")
        journalFile = File(cacheDir, DiskLruCache.JOURNAL_FILE)
        journalBkpFile = File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP)
        cacheDir.listFiles()?.forEach { it.delete() }
        cache = openCache()
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        cache.close()
    }


    /**
     * Each read sees a snapshot of the file at the time read was called.
     * This means that two reads of the same key can see different data.
     */
    @Test
    @Throws(Exception::class)
    fun readAndWriteOverlapsMaintainConsistency() {
        val v1Creator = cache.edit("k1")
        v1Creator!![0] = "AAaa"
        v1Creator[1] = "BBbb"
        v1Creator.commit()
        val snapshot1 = cache["k1"]
        val inV1 = snapshot1!!.getInputStream(0)
        assertThat(inV1!!.read(), `is`('A'.code))
        assertThat(inV1.read(), `is`('A'.code))
        val v1Updater = cache.edit("k1")
        v1Updater!![0] = "CCcc"
        v1Updater[1] = "DDdd"
        v1Updater.commit()
        val snapshot2 = cache["k1"]
        assertThat(snapshot2!!.getString(0), `is`("CCcc"))
        assertThat(snapshot2.getLength(0), `is`(4L))
        assertThat(snapshot2.getString(1), `is`("DDdd"))
        assertThat(snapshot2.getLength(1), `is`(4L))
        snapshot2.close()
        assertThat(inV1.read(), `is`('a'.code))
        assertThat(inV1.read(), `is`('a'.code))
        assertThat(snapshot1.getString(1), `is`("BBbb"))
        assertThat(snapshot1.getLength(1), `is`(4L))
        snapshot1.close()
    }

    @Test
    @Throws(Exception::class)
    fun openWithDirtyKeyDeletesAllFilesForThatKey() {
        cache.close()
        val cleanFile0 = getCleanFile("k1", 0)
        val cleanFile1 = getCleanFile("k1", 1)
        val dirtyFile0 = getDirtyFile("k1", 0)
        val dirtyFile1 = getDirtyFile("k1", 1)
        writeFile(cleanFile0, "A")
        writeFile(cleanFile1, "B")
        writeFile(dirtyFile0, "C")
        writeFile(dirtyFile1, "D")
        createJournal("CLEAN k1 1 1", "DIRTY   k1")
        cache = openCache()
        assertThat(cleanFile0.exists(), `is`(false))
        assertThat(cleanFile1.exists(), `is`(false))
        assertThat(dirtyFile0.exists(), `is`(false))
        assertThat(dirtyFile1.exists(), `is`(false))
        assertThat(cache["k1"], `is`(nullValue()))
    }

    @Test
    @Throws(Exception::class)
    fun openWithInvalidVersionClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournal(version = "0")
        cache = openCache()
        assertGarbageFilesAllDeleted()
    }

    @Test
    @Throws(Exception::class)
    fun openWithInvalidAppVersionClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournal(appVersion = "101")
        cache = openCache()
        assertGarbageFilesAllDeleted()
    }

    @Test
    @Throws(Exception::class)
    fun openWithInvalidValueCountClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournal(valueCount = "1")
        cache = openCache()
        assertGarbageFilesAllDeleted()
    }

    @Test
    @Throws(Exception::class)
    fun openWithInvalidBlankLineClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournal(blank = "x")
        cache = openCache()
        assertGarbageFilesAllDeleted()
    }

    @Test
    @Throws(Exception::class)
    fun openWithInvalidJournalLineClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournal("CLEAN k1 1 1", "BOGUS")
        cache = openCache()
        assertGarbageFilesAllDeleted()
        assertThat(cache["k1"], `is`(nullValue()))
    }

    @Test
    @Throws(Exception::class)
    fun openWithInvalidFileSizeClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournal("CLEAN k1 0000x001 1")
        cache = openCache()
        assertGarbageFilesAllDeleted()
        assertThat(cache["k1"], `is`(nullValue()))
    }

    @Test
    @Throws(Exception::class)
    fun openWithTooManyFileSizesClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournal("CLEAN k1 1 1 1")
        cache = openCache()
        assertGarbageFilesAllDeleted()
        assertThat(cache["k1"], `is`(nullValue()))
    }

    @Test
    @Throws(Exception::class)
    fun updateExistingEntryWithTooFewValuesReusesPreviousValues() {
        val creator = cache.edit("k1")
        creator!![0] = "A"
        creator[1] = "B"
        creator.commit()
        val updater = cache.edit("k1")
        updater!![0] = "C"
        updater.commit()
        val snapshot = cache["k1"]
        assertThat(snapshot!!.getString(0), `is`("C"))
        assertThat(snapshot.getLength(0), `is`(1))
        assertThat(snapshot.getString(1), `is`("B"))
        assertThat(snapshot.getLength(1), `is`(1))
        snapshot.close()
    }

    @Test
    @Throws(Exception::class)
    fun evictOnUpdate() {
        cache.close()
        cache = openCache(cacheSize = 10)
        set("a", "a", "aa") // size 3
        set("b", "b", "bb") // size 3
        set("c", "c", "cc") // size 3
        assertThat(cache.size(), `is`(9))

        // Causing the size to grow to 11 should evict 'A'.
        set("b", "b", "bbbb")
        cache.flush()
        assertThat(cache.size(), `is`(8))
        assertAbsent("a")
        assertValue("b", "b", "bbbb")
        assertValue("c", "c", "cc")
    }

    @Test
    @Throws(Exception::class)
    fun cacheSingleValueOfSizeGreaterThanMaxSize() {
        cache.close()
        cache = openCache(cacheSize = 10)
        set("a", "aaaaaaaaaaa", "a") // size=12
        cache.flush()
        assertAbsent("a")
    }

    /** @see [Issue .28](https://github.com/JakeWharton/DiskLruCache/issues/28)
     */
    @Test
    @Throws(Exception::class)
    fun rebuildJournalOnRepeatedReadsWithOpenAndClose() {
        set("a", "a", "a")
        set("b", "b", "b")
        var lastJournalLength: Long = 0
        while (true) {
            val journalLength = journalFile.length()
            assertValue("a", "a", "a")
            assertValue("b", "b", "b")
            cache.close()
            cache = openCache()
            if (journalLength < lastJournalLength) {
                println("Journal compacted from $lastJournalLength bytes to $journalLength bytes")
                break // Test passed!
            }
            lastJournalLength = journalLength
        }
    }

    /** @see [Issue .28](https://github.com/JakeWharton/DiskLruCache/issues/28)
     */
    @Test
    @Throws(Exception::class)
    fun rebuildJournalOnRepeatedEditsWithOpenAndClose() {
        var lastJournalLength: Long = 0
        while (true) {
            val journalLength = journalFile.length()
            set("a", "a", "a")
            set("b", "b", "b")
            cache.close()
            cache = openCache()
            if (journalLength < lastJournalLength) {
                println("Journal compacted from $lastJournalLength bytes to $journalLength bytes")
                break
            }
            lastJournalLength = journalLength
        }
    }

    @Test
    @Throws(Exception::class)
    fun editSameVersion() {
        set("a", "a", "a")
        val snapshot = cache["a"]
        val editor = snapshot!!.edit()
        editor!![1] = "a2"
        editor.commit()
        assertValue("a", "a", "a2")
    }

    @Test
    @Throws(Exception::class)
    fun editSnapshotAfterChangeAborted() {
        set("a", "a", "a")
        val snapshot = cache["a"]
        val toAbort = snapshot!!.edit()
        toAbort!![0] = "b"
        toAbort.abort()
        val editor = snapshot.edit()
        editor!![1] = "a2"
        editor.commit()
        assertValue("a", "a", "a2")
    }


    @Test
    @Throws(Exception::class)
    fun editSinceEvicted() {
        cache.close()
        cache = openCache(cacheSize = 10)
        set("a", "aa", "aaa") // size 5
        val snapshot = cache["a"]
        set("b", "bb", "bbb") // size 5
        set("c", "cc", "ccc") // size 5; will evict 'A'
        cache.flush()
        assertThat(snapshot!!.edit(), `is`(nullValue()))
    }

    /** @see [Issue .2](https://github.com/JakeWharton/DiskLruCache/issues/2)
     */
    @Test
    @Throws(Exception::class)
    fun aggressiveClearingHandlesPartialEdit() {
        set("a", "a", "a")
        set("b", "b", "b")
        val a = cache["a"]!!
            .edit()
        a!![0] = "a1"

        cacheDir.deleteRecursively()
        a[1] = "a2"
        a.commit()
        assertThat(cache["a"], `is`(nullValue()))
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

    private fun getCleanFile(key: String, index: Int): File {
        return File(cacheDir, "$key.$index")
    }

    private fun getDirtyFile(key: String, index: Int): File {
        return File(cacheDir, "$key.$index.tmp")
    }

    @Throws(Exception::class)
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

    @Throws(Exception::class)
    private fun assertGarbageFilesAllDeleted() {
        assertThat(getCleanFile("g1", 0).exists(), `is`(false))
        assertThat(getCleanFile("g1", 1).exists(), `is`(false))
        assertThat(getCleanFile("g2", 0).exists(), `is`(false))
        assertThat(getCleanFile("g2", 1).exists(), `is`(false))
        assertThat(File(cacheDir, "otherFile0").exists(), `is`(false))
        assertThat(File(cacheDir, "dir1").exists(), `is`(false))
    }

    @Throws(Exception::class)
    private operator fun set(key: String, value0: String, value1: String) {
        val editor = cache.edit(key)
        editor!![0] = value0
        editor[1] = value1
        editor.commit()
    }

    @Throws(Exception::class)
    private fun assertAbsent(key: String) {
        val snapshot = cache[key]
        if (snapshot != null) {
            snapshot.close()
            Assert.fail()
        }
        assertThat(getCleanFile(key, 0).exists(), `is`(false))
        assertThat(getCleanFile(key, 1).exists(), `is`(false))
        assertThat(getDirtyFile(key, 0).exists(), `is`(false))
        assertThat(getDirtyFile(key, 1).exists(), `is`(false))
    }

    @Throws(Exception::class)
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

    private fun writeFile(file: File?, content: String?) {
        FileWriter(file).use { it.write(content) }
    }

    private fun openCache(
        directory: File = cacheDir,
        valueCount: Int = VALUE_COUNT,
        cacheSize: Long = Long.MAX_VALUE
    ) = DiskLruCache.open(directory, APP_VERSION, valueCount, cacheSize)
}