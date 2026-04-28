package com.onmi.qing

import android.app.Application
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager

// Qing 应用 Application 类 - 负责初始化数据存储和演示模式管理器
class QingApplication : Application() {

    lateinit var dataStore: QingDataStore
        private set

    lateinit var demoModeManager: DemoModeManager
        private set

    override fun onCreate() {
        super.onCreate()
        dataStore = QingDataStore(this)
        demoModeManager = DemoModeManager(this)
    }
}
