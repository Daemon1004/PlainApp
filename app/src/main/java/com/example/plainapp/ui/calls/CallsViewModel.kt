package com.example.plainapp.ui.calls

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CallsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is calls Fragment"
    }
    val text: LiveData<String> = _text
}