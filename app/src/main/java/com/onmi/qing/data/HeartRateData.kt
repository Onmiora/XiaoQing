package com.onmi.qing.data

import android.bluetooth.BluetoothDevice

// 心率数据点 - 用于HRV分析
data class HeartRateData(
    val timestamp: Long,
    val heartRate: Int,
    val hrv: Int? = null  // R-R interval in ms (if available)
)

// BLE设备信息
data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val device: BluetoothDevice? = null
)

