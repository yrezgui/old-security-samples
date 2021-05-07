package com.samples.appinstaller.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.samples.appinstaller.data.AppPackage
import com.samples.appinstaller.data.SampleStore

class AppDetailsViewModel : ViewModel() {
    lateinit var selectedApp: AppPackage

    fun loadApp(packageId: String) {
       selectedApp = SampleStore.find { it.id == packageId }!!
    }

    private val _text = MutableLiveData<String>().apply {
        value = "This is app Fragment"
    }
    val text: LiveData<String> = _text
}