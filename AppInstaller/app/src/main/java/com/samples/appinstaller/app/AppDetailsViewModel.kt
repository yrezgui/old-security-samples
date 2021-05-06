package com.samples.appinstaller.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.samples.appinstaller.data.SampleStore

class AppDetailsViewModel : ViewModel() {
    val selectedApp = SampleStore[2]

    private val _text = MutableLiveData<String>().apply {
        value = "This is app Fragment"
    }
    val text: LiveData<String> = _text
}