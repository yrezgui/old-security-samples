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

import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_REMOVED
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.samples.appinstaller.databinding.ActivityMainBinding
import com.samples.appinstaller.workers.INSTALL_INTENT_NAME
import com.samples.appinstaller.workers.SEND_INSTALL_UPDATES_PERMISSION
import com.samples.appinstaller.workers.UNINSTALL_INTENT_NAME
import com.samples.appinstaller.workers.UPGRADE_INTENT_NAME

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_library,
                R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp(appBarConfiguration) ||
                super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        val handler = Handler(Looper.getMainLooper())

        registerReceiver(
            appViewModel.packageInstallCallback,
            IntentFilter().apply {
                addAction(INSTALL_INTENT_NAME)
                addAction(UNINSTALL_INTENT_NAME)
                addAction(UPGRADE_INTENT_NAME)
            },
            SEND_INSTALL_UPDATES_PERMISSION,
            handler
        )

        // We have to register the observer twice as parameters are different between these two
        // groups of intents
        registerReceiver(
            appViewModel.packageInstallCallback,
            IntentFilter().apply {
                addAction(ACTION_PACKAGE_ADDED)
                addAction(ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            },
            null,
            handler
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(appViewModel.packageInstallCallback)
    }
}
