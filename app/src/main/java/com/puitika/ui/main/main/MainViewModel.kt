package com.puitika.ui.main.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.puitika.data.pref.UserModel
import com.puitika.repository.PuitikaRepository

class MainViewModel(private val repository:PuitikaRepository): ViewModel() {
    fun getSession(): LiveData<UserModel> {
        return repository.getSession().asLiveData()
    }
}