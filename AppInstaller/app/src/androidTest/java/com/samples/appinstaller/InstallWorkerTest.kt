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
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.samples.appinstaller.apps.SampleStoreDB
import com.samples.appinstaller.workers.InstallWorker
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallWorkerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testEmptyParams() {
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
    fun testInexistentApp() {
        val asyncWorker = TestListenableWorkerBuilder<InstallWorker>(
            context = context,
            inputData = workDataOf(
                InstallWorker.PACKAGE_ID to 0,
                InstallWorker.PACKAGE_NAME to "com.app.impossible",
                InstallWorker.PACKAGE_LABEL to "Impossible App"
            )
        ).build()

        runBlocking {
            val asyncResult = asyncWorker.doWork()
            assertThat(asyncResult, `is`(ListenableWorker.Result.Failure()))
        }
    }

    @Test
    fun testExistingApp() {
        val app = SampleStoreDB.entries.first().value

        val asyncWorker = TestListenableWorkerBuilder<InstallWorker>(
            context = context,
            inputData = workDataOf(
                InstallWorker.PACKAGE_ID to app.id,
                InstallWorker.PACKAGE_NAME to app.name,
                InstallWorker.PACKAGE_LABEL to app.label
            )
        ).build()

        runBlocking {
            val asyncResult = asyncWorker.doWork()
            assertThat(asyncResult, `is`(ListenableWorker.Result.Success()))
        }
    }
}
