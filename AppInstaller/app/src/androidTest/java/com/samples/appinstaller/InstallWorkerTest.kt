/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samples.appinstaller

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.samples.appinstaller.apps.InstallSession
import com.samples.appinstaller.apps.SampleStoreDB
import com.samples.appinstaller.workers.InstallWorker
import com.samples.appinstaller.workers.PackageInstallerUtils
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class InstallWorkerTest {
    private lateinit var context: Context
    private lateinit var appDB: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        appDB = Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()
    }

    @After
    fun tearDown() {
        runBlocking {
            appDB.sessionDao().deleteAllSessions()
        }
    }

    @Test
    fun testInstallWithEmptyParams() {
        val asyncWorker = TestListenableWorkerBuilder<InstallWorker>(
            context = context,
            inputData = workDataOf()
        ).build()

        runBlocking {
            val asyncResult = asyncWorker.doWork()
            assertThat(asyncResult, `is`(ListenableWorker.Result.Failure()))
        }
    }

    @Test
    fun testInstallInexistentApp() {
        val asyncWorker = TestListenableWorkerBuilder<InstallWorker>(
            context = context,
            inputData = workDataOf(
                InstallWorker.PACKAGE_ID_KEY to 0,
                InstallWorker.PACKAGE_NAME_KEY to "com.app.impossible",
                InstallWorker.PACKAGE_LABEL_KEY to "Impossible App"
            )
        ).build()

        runBlocking {
            val asyncResult = asyncWorker.doWork()
            assertThat(asyncResult, `is`(ListenableWorker.Result.Failure()))
        }
    }

    @Test
    fun testInstallApp() {
        val app = SampleStoreDB.entries.random().value

        val asyncWorker = TestListenableWorkerBuilder<InstallWorker>(
            context = context,
            inputData = workDataOf(
                InstallWorker.PACKAGE_ID_KEY to app.id,
                InstallWorker.PACKAGE_NAME_KEY to app.name,
                InstallWorker.PACKAGE_LABEL_KEY to app.label
            )
        ).build()

        runBlocking {
            val asyncResult = asyncWorker.doWork()
            assertThat(asyncResult, `is`(instanceOf(ListenableWorker.Result.Success()::class.java)))
            assertThat(
                asyncResult.outputData.getString(InstallWorker.PACKAGE_NAME_KEY),
                `is`(app.name)
            )
        }
    }

    @Test
    fun testInstallUsingExistingSession() {
        val app = SampleStoreDB.entries.random().value

        runBlocking {
            val sessionId = PackageInstallerUtils.createInstallSession(context, app.name, app.label)

            @Suppress("BlockingMethodInNonBlockingContext")
            PackageInstallerUtils.writeSession(
                context = context,
                sessionId = sessionId,
                apkInputStream = context.assets.open("${app.name}.apk")
            )
            PackageInstallerUtils.commitSession(context, sessionId, Intent())
            val installSession = InstallSession(app.name, sessionId)
            appDB.sessionDao().insert(installSession)

            val asyncWorker = TestListenableWorkerBuilder<InstallWorker>(
                context = context,
                inputData = workDataOf(
                    InstallWorker.PACKAGE_ID_KEY to app.id,
                    InstallWorker.PACKAGE_NAME_KEY to app.name,
                    InstallWorker.PACKAGE_LABEL_KEY to app.label
                )
            ).build()

            val asyncResult = asyncWorker.doWork()
            assertThat(asyncResult, `is`(instanceOf(ListenableWorker.Result.Success()::class.java)))
            assertThat(
                asyncResult.outputData.getString(InstallWorker.PACKAGE_NAME_KEY),
                `is`(app.name)
            )
            assertThat(
                asyncResult.outputData.getInt(InstallWorker.SESSION_ID_KEY, -1),
                `is`(sessionId)
            )
        }
    }

    @Test
    fun testInstallUsingInexistingSession() {
        val app = SampleStoreDB.entries.random().value

        runBlocking {
            val installSession = InstallSession(app.name, Random.nextInt())
            appDB.sessionDao().insert(installSession)

            val asyncWorker = TestListenableWorkerBuilder<InstallWorker>(
                context = context,
                inputData = workDataOf(
                    InstallWorker.PACKAGE_ID_KEY to app.id,
                    InstallWorker.PACKAGE_NAME_KEY to app.name,
                    InstallWorker.PACKAGE_LABEL_KEY to app.label
                )
            ).build()

            val asyncResult = asyncWorker.doWork()
            assertThat(asyncResult, `is`(ListenableWorker.Result.Failure()))

            val savedInstallSession = appDB.sessionDao().findBySessionId(installSession.sessionId)
            assertThat(savedInstallSession, nullValue())
        }
    }
}
