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

import com.samples.appinstaller.R

val SampleStoreDB = mapOf(
    "com.acme.spaceshooter" to AppPackage(
        id = 1001,
        name = "com.acme.spaceshooter",
        label = "Space Shooter",
        company = "ACME Inc.",
        icon = R.mipmap.ic_app_spaceshooter_round
    ),
    "com.champollion.pockettranslator" to AppPackage(
        id = 1002,
        name = "com.champollion.pockettranslator",
        label = "Pocket Translator",
        company = "Champollion SA",
        icon = R.mipmap.ic_app_pockettranslator_round
    ),
    "com.echolabs.citymaker" to AppPackage(
        id = 1003,
        name = "com.echolabs.citymaker",
        label = "City Maker",
        company = "Echo Labs Ltd",
        icon = R.mipmap.ic_app_citymaker_round
    ),
    "com.paca.nicekart" to AppPackage(
        id = 1004,
        name = "com.paca.nicekart",
        label = "Nice Kart",
        company = "PACA SARL",
        icon = R.mipmap.ic_app_nicekart_round
    ),
)
