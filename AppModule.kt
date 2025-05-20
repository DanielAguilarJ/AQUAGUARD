package com.tuempresa.fugas.di

import com.tuempresa.fugas.datastore.OnboardingDataStore
import com.tuempresa.fugas.datastore.SettingsDataStore
import com.tuempresa.fugas.network.RetrofitInstance
import com.tuempresa.fugas.repository.SensorRepository
import com.tuempresa.fugas.viewmodel.SensorViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { RetrofitInstance.api }
    single { SensorRepository(get()) }
    single { SettingsDataStore(androidContext()) }
    single { OnboardingDataStore(androidContext()) }
    viewModel { SensorViewModel(get(), get()) }
}
