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
package com.samples.appinstaller.apps

import androidx.annotation.DrawableRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration

data class AppPackage(
    val id: Int,
    val name: String,
    val label: String,
    val company: String,
    @DrawableRes val icon: Int,
    val status: AppStatus = AppStatus.UNINSTALLED,
    val updatedAt: Long = -1
)

enum class AppStatus {
    UNINSTALLED, INSTALLED, INSTALLING, UPGRADING
}

@Entity(tableName = "install_sessions")
data class InstallSession(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "session_id") val sessionId: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val EXPIRE_TIME = Duration.ofDays(1).toMillis()
    }

    val isExpired: Boolean
        get() = (createdAt + EXPIRE_TIME) < System.currentTimeMillis()
}
