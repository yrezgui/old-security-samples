package com.samples.appinstaller

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.samples.appinstaller.AppSettings.AutoUpdateSchedule
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod
import com.samples.appinstaller.appManager.AppManager
import com.samples.appinstaller.appManager.AppPackage
import com.samples.appinstaller.appManager.AppStatus
import com.samples.appinstaller.appManager.SampleStoreDB
import com.samples.appinstaller.settings.appSettings
import com.samples.appinstaller.settings.toTemporalAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

/**
 * Timer to check sync app list happening every 30 seconds
 */
const val SYNC_TIMER = 3000L

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()

    private val appManager: AppManager by lazy { AppManager(context) }

    val isPermissionGranted: Boolean
        get() = appManager.canRequestPackageInstalls()

    private val _appSettings = MutableLiveData(AppSettings.getDefaultInstance())
    private val appSettings: LiveData<AppSettings> = _appSettings

    private val _library = MutableLiveData<Map<String, AppPackage>>(emptyMap())
    val library: LiveData<Map<String, AppPackage>> = _library

    val store: LiveData<Map<String, AppPackage>> = Transformations.map(library) {
        // We're merging our initial list of apps with entries from libraries where the installed
        // state is valid compared to the static data from SampleStoreDB
        SampleStoreDB + it
    }

    private val _sessions = MutableLiveData<Map<Int, String>>(emptyMap())
    val sessions: LiveData<Map<Int, String>> = _sessions

    private var syncLibraryJob: Job = Job()

    private val _selectedAppId: MutableLiveData<String?> = MutableLiveData()
    val selectedApp = MediatorLiveData<AppPackage?>()

    init {
        viewModelScope.launch {
            _appSettings.postValue(context.appSettings.data.first())
            syncLibrary()
            startSyncAppListJob()
            initSelectedApp()

            withContext(Dispatchers.IO) {
                context.appSettings.data.collect {
                    _appSettings.postValue(it)
                    startSyncAppListJob()
                }
            }
        }
    }

    private fun initSelectedApp() {
        selectedApp.addSource(store) {
            selectedApp.value = it[_selectedAppId.value]
        }

        selectedApp.addSource(_selectedAppId) {
            selectedApp.value = store.value!![it]
        }
    }

    fun selectApp(packageId: String) {
        _selectedAppId.value = packageId
    }

    fun openApp() {
        selectedApp.value?.let { appManager.openApp(it.id) }
    }

    fun uninstallApp() {
        viewModelScope.launch {
            selectedApp.value?.let { appManager.uninstallApp(it.id) }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun installApp() {
        val app = selectedApp.value ?: return

        viewModelScope.launch {
            val sessionInfo = appManager.getCurrentInstallSession(app.id)
                ?: appManager.getSessionInfo(appManager.createInstallSession(app.name, app.id))
                ?: return@launch

            delay(5000L)

            withContext(Dispatchers.IO) {
                appManager.writeAndCommitSession(
                    sessionId = sessionInfo.sessionId,
                    apkInputStream = context.assets.open("${app.id}.apk")
                )
            }
        }
    }

    /**
     * Start sync library job after cancelling existing one first
     */
    private fun startSyncAppListJob() {
        viewModelScope.launch {
            syncLibraryJob.cancel()
            syncLibraryJob = viewModelScope.launch {
                while (isActive) {
                    syncLibrary()
                    delay(SYNC_TIMER)
                }
            }
        }
    }

    /**
     * Sync app library and check if installed apps have updates
     */
    private suspend fun syncLibrary() {
        withContext(Dispatchers.Default) {
            val settings = appSettings.value ?: AppSettings.getDefaultInstance()
            val installedPackages = appManager.getInstalledPackageMap()
            val sessions = _sessions.value ?: appManager.getActiveInstallSessionMap()

            val installedApps = installedPackages
                .filterKeys { SampleStoreDB.containsKey(it) }
                .mapValues {
                    val storePackage = SampleStoreDB[it.key]!!.copy(status = AppStatus.INSTALLED)

                    if (settings.updateAvailabilityPeriod == UpdateAvailabilityPeriod.NONE) {
                        // If the user has disabled updates or the app is currently upgrading,
                        // we don't update the store
                        storePackage
                    } else {
                        // We check if the installed time is older than the UpdateAvailabilityPeriod
                        storePackage.copy(
                            isUpdateAvailable = checkIfUpdateIsAvailable(
                                app = storePackage,
                                packageInfo = it.value
                            )
                        )
                    }
                }

            val appsBeingInstalled = sessions
                .filterValues { SampleStoreDB.containsKey(it) }
                .mapKeys { (_, packageName) -> packageName }
                .mapValues { (packageName, _) ->
                    val app = installedApps[packageName] ?: SampleStoreDB[packageName]!!

                    when (app.status) {
                        AppStatus.INSTALLED -> app.copy(status = AppStatus.UPGRADING)
                        AppStatus.UNINSTALLED -> app.copy(status = AppStatus.INSTALLING)
                        else -> app
                    }
                }

            val updatedLibrary = installedApps + appsBeingInstalled
            _library.postValue(updatedLibrary)
        }
    }

    /**
     * Check if the installed time of a package is older than the UpdateAvailabilityPeriod setting
     */
    private fun checkIfUpdateIsAvailable(app: AppPackage, packageInfo: PackageInfo): Boolean {
        val now = Instant.now()

        val settings = appSettings.value ?: return false
        val updateAvailabilityPeriod = settings.updateAvailabilityPeriod.toTemporalAmount()

        if (app.status != AppStatus.INSTALLED) {
            return false
        }

        val between = Duration.between(Instant.ofEpochMilli(packageInfo.lastUpdateTime), now)

        val after = now.isAfter(
            Instant.ofEpochMilli(packageInfo.lastUpdateTime).plus(updateAvailabilityPeriod)
        )

        Log.d("syncAppList", "${app.id}: ${between.toMillis() / 1000}s  After: $after")

        return now.isAfter(
            Instant.ofEpochMilli(packageInfo.lastUpdateTime).plus(updateAvailabilityPeriod)
        )
    }

    /**
     * Setter for the [AutoUpdateSchedule] setting
     */
    fun setAutoUpdateSchedule(value: Int) {
        viewModelScope.launch {
            context.appSettings.updateData { currentSettings ->
                currentSettings.toBuilder()
                    .setAutoUpdateScheduleValue(value)
                    .build()
            }
        }
    }

    /**
     * Setter for the [UpdateAvailabilityPeriod] setting
     */
    fun setUpdateAvailabilityPeriod(value: Int) {
        viewModelScope.launch {
            context.appSettings.updateData { currentSettings ->
                currentSettings.toBuilder()
                    .setUpdateAvailabilityPeriodValue(value)
                    .build()
            }
        }
    }

    suspend fun getAppBySessionId(sessionId: Int): AppPackage? {
        val sessionInfo = appManager.getSessionInfo(sessionId) ?: return null
        val packageName = sessionInfo.appPackageName ?: return null
        if (!appManager.isSessionOwned(sessionInfo)) return null
        return _library.value?.get(packageName) ?: SampleStoreDB[packageName]
    }

    /**
     * Monitor install sessions progress
     */
    val sessionCallback = object : PackageInstaller.SessionCallback() {
        override fun onCreated(sessionId: Int) {
            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    val app = getAppBySessionId(sessionId) ?: return@withContext

                    // We update the list of current sessions
                    val sessions = _sessions.value?.toMutableMap() ?: mutableMapOf()
                    sessions[sessionId] = app.id
                    _sessions.postValue(_sessions.value)

                    val status = if (app.status == AppStatus.INSTALLED) {
                        AppStatus.UPGRADING
                    } else {
                        AppStatus.INSTALLING
                    }

                    val library = _library.value?.toMutableMap() ?: mutableMapOf()
                    library[app.id] = app.copy(status = status)
                    _library.postValue(library.toMap())
                }
            }
        }

        override fun onBadgingChanged(sessionId: Int) {}

        override fun onActiveChanged(sessionId: Int, active: Boolean) {
            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    var app = getAppBySessionId(sessionId) ?: return@withContext
                    val sessions = _sessions.value?.toMutableMap() ?: mutableMapOf()

                    if (active) {
                        // As the session is now active, it may have been removed previously, so we add it
                        // back the list of monitored sessions
                        sessions[sessionId] = app.id
                        app = app.copy(
                            status = if (app.status == AppStatus.INSTALLED) {
                                AppStatus.UPGRADING
                            } else {
                                AppStatus.INSTALLING
                            }
                        )
                    } else {
                        // As the session isn't active anymore, we remove it from the list of monitored
                        // sessions
                        sessions.remove(sessionId)
                        val isAppInstalled = appManager.getPackageInfo(app.id) != null
                        app = app.copy(
                            status = if (isAppInstalled) {
                                AppStatus.INSTALLED
                            } else {
                                AppStatus.UNINSTALLED
                            }
                        )
                    }

                    val library = _library.value?.toMutableMap() ?: mutableMapOf()
                    library[app.id] = app
                    _library.postValue(library.toMap())
                    _sessions.postValue(sessions.toMap())
                }
            }
        }

        override fun onProgressChanged(sessionId: Int, progress: Float) {}

        override fun onFinished(sessionId: Int, success: Boolean) {
            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    var app = getAppBySessionId(sessionId) ?: return@withContext
                    val sessions = _sessions.value?.toMutableMap() ?: mutableMapOf()

                    val isAppInstalled = appManager.getPackageInfo(app.id) != null
                    app = app.copy(
                        isUpdateAvailable = false,
                        status = if (isAppInstalled) {
                            AppStatus.INSTALLED
                        } else {
                            AppStatus.UNINSTALLED
                        }
                    )

                    val library = _library.value?.toMutableMap() ?: mutableMapOf()
                    library[app.id] = app
                    _library.postValue(library.toMap())
                    sessions.remove(sessionId)
                    _sessions.postValue(sessions.toMap())
                }
            }
        }
    }
}