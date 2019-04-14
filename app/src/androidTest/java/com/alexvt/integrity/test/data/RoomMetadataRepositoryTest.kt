/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.test.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alexvt.integrity.android.data.metadata.RoomMetadataRepository
import com.alexvt.integrity.lib.core.data.metadata.Snapshot
import com.snatik.storage.Storage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class RoomMetadataRepositoryTest {

    // Test data
    val basicSnapshot = Snapshot().copy(artifactId = 0, date = "0")

    // Dependencies for repository constructor
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    // Not using in-memory DB. Using unique DB name for each test in the test run.
    // todo performance test
    val dbName = "SnapshotDbTest${this.hashCode()}"

    // Class under test
    lateinit var roomMetadataRepository: RoomMetadataRepository

    @Before
    fun setUp() {
        roomMetadataRepository = RoomMetadataRepository(context, dbName)
    }


    @Test
    fun emptyDatabaseCreated() {
        assertEquals(roomMetadataRepository.getAllArtifactMetadataBlocking().size, 0)
    }

    @Test
    fun snapshotSelection() {
        val testSnapshots = with (basicSnapshot) { listOf(
                copy(artifactId = 0, date = "0"),
                copy(artifactId = 1, date = "1"),
                copy(artifactId = 1, date = "4"),
                copy(artifactId = 0, date = "3"),
                copy(artifactId = 2, date = "2"),
                copy(artifactId = 2, date = "5")
        ) }
        val latestSnapshotsPerArtifact = with (basicSnapshot) { listOf(
                copy(artifactId = 0, date = "3"),
                copy(artifactId = 1, date = "4"),
                copy(artifactId = 2, date = "5")
        ) }
        val snapshotsIn1 = with (basicSnapshot) { listOf(
                copy(artifactId = 1, date = "1"),
                copy(artifactId = 1, date = "4")
        ) }

        with (roomMetadataRepository) {
            testSnapshots.forEach { addSnapshotMetadata(it) }

            assertEquals(getAllArtifactLatestMetadataBlocking(), latestSnapshotsPerArtifact)
            assertEquals(getArtifactMetadataBlocking(1), snapshotsIn1)
        }
    }

    @Test
    fun snapshotDeletion() {
        val testSnapshots = with (basicSnapshot) { listOf(
                copy(artifactId = 0, date = "0"),
                copy(artifactId = 1, date = "1"),
                copy(artifactId = 1, date = "4"),
                copy(artifactId = 0, date = "3"),
                copy(artifactId = 2, date = "2"),
                copy(artifactId = 2, date = "5")
        ) }
        val snapshotsAfterRemoved1 = with (basicSnapshot) { listOf(
                copy(artifactId = 0, date = "0"),
                copy(artifactId = 0, date = "3"),
                copy(artifactId = 2, date = "2"),
                copy(artifactId = 2, date = "5")
        ) }
        val snapshotsAfterRemoved1and22 = with (basicSnapshot) { listOf(
                copy(artifactId = 0, date = "0"),
                copy(artifactId = 0, date = "3"),
                copy(artifactId = 2, date = "5")
        ) }

        with (roomMetadataRepository) {
            testSnapshots.forEach { addSnapshotMetadata(it) }

            assertEquals(getAllArtifactMetadataBlocking(), testSnapshots)

            removeArtifactMetadata(1)

            assertEquals(getAllArtifactMetadataBlocking(), snapshotsAfterRemoved1)

            removeSnapshotMetadata(2, "2")

            assertEquals(getAllArtifactMetadataBlocking(), snapshotsAfterRemoved1and22)
        }
    }

    @After
    fun deleteRepository() {
        val storage = Storage(context)
        val dbFolder = context.dataDir.absolutePath + "/databases"
        storage.getFiles(dbFolder).filter {
            it.name.startsWith(dbName)
        }.forEach {
            storage.deleteFile(it.path)
        }
    }
}