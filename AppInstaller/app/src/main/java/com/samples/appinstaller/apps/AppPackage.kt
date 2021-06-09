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
import androidx.room.Index
import androidx.room.PrimaryKey

data class AppPackage(
    val id: String,
    val name: String,
    val company: String,
    @DrawableRes val icon: Int,
    val status: AppStatus = AppStatus.UNINSTALLED,
    val updatedAt: Long = -1
)

enum class AppStatus {
    UNINSTALLED, INSTALLED, INSTALLING, UPGRADING
}

@Entity(
    tableName = "install_sessions",
    indices = [Index(name = "index_package_name", value = ["package_name"])]
)
data class InstallSession(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "active") val active: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long = -1
)

enum class SessionStatus {
    INACTIVE, ACTIVE, COMMITTED
}