package com.onmi.qing.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.onmi.qing.data.BleDevice
import com.onmi.qing.data.HeartRateData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.random.Random

// 蓝牙心率管理器
@SuppressLint("MissingPermission")
class HeartRateManager(private val context: Context) {

    companion object {
        // Heart Rate Service UUID
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        // Heart Rate Measurement Characteristic UUID
        val HEART_RATE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        // Client Characteristic Configuration Descriptor UUID
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // 心率采集推荐时长 (毫秒) - 1分钟
        const val HR_COLLECTION_DURATION_MS = 60_000L
    }

    // BLE状态
    sealed class BleState {
        data object Idle : BleState()
        data object Scanning : BleState()
        data class DevicesFound(val devices: List<BleDevice>) : BleState()
        data object Connecting : BleState()
        data object Connected : BleState()
        data object DiscoveringServices : BleState()
        data object Disconnected : BleState()
        data class ConnectedHR(val deviceName: String) : BleState()
        data class HeartRateUpdated(val heartRate: Int) : BleState()
        data class Error(val message: String) : BleState()
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null

    private val _bleState = MutableStateFlow<BleState>(BleState.Idle)
    val bleState: StateFlow<BleState> = _bleState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()

    private val _heartRateData = MutableStateFlow<List<HeartRateData>>(emptyList())
    val heartRateData: StateFlow<List<HeartRateData>> = _heartRateData.asStateFlow()

    private val _currentHeartRate = MutableStateFlow<Int?>(null)
    val currentHeartRate: StateFlow<Int?> = _currentHeartRate.asStateFlow()

    private var isScanning = false

    private val handler = Handler(Looper.getMainLooper())

    // Demo模式相关
    private var isDemoMode = false
    private var demoHeartRate = 72

    // 扫描回调
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown Device"
            val address = device.address
            val rssi = result.rssi

            val bleDevice = BleDevice(
                name = name,
                address = address,
                rssi = rssi,
                device = device
            )

            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == address }
            if (existingIndex >= 0) {
                currentList[existingIndex] = bleDevice
            } else {
                currentList.add(bleDevice)
            }
            _discoveredDevices.value = currentList
            _bleState.value = BleState.DevicesFound(currentList)
        }

        override fun onScanFailed(errorCode: Int) {
            _bleState.value = BleState.Error("Scan failed with error code: $errorCode")
            isScanning = false
        }
    }

    // GATT回调
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _bleState.value = BleState.Connected
                    bluetoothGatt = gatt
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _bleState.value = BleState.Disconnected
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    _currentHeartRate.value = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _bleState.value = BleState.DiscoveringServices
                val heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID)
                heartRateService?.let { service ->
                    val heartRateCharacteristic = service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
                    heartRateCharacteristic?.let { characteristic ->
                        gatt.setCharacteristicNotification(characteristic, true)

                        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                        descriptor?.let {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeDescriptor(it, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            } else {
                                @Suppress("DEPRECATION")
                                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                @Suppress("DEPRECATION")
                                gatt.writeDescriptor(it)
                            }
                        }
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                val heartRate = parseHeartRate(value)
                updateHeartRate(heartRate)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                val heartRate = parseHeartRate(characteristic.value ?: byteArrayOf())
                updateHeartRate(heartRate)
            }
        }
    }

    private fun updateHeartRate(heartRate: Int) {
        _currentHeartRate.value = heartRate
        _bleState.value = BleState.HeartRateUpdated(heartRate)

        val record = HeartRateData(
            timestamp = System.currentTimeMillis(),
            heartRate = heartRate,
            hrv = Random.nextInt(30, 80) // 模拟HRV数据
        )

        val currentList = _heartRateData.value.toMutableList()
        currentList.add(record)

        // 保留最近的数据（最多10分钟）
        val cutoffTime = System.currentTimeMillis() - 600000
        val filteredList = currentList.filter { it.timestamp >= cutoffTime }
        _heartRateData.value = filteredList
    }

    private fun parseHeartRate(value: ByteArray): Int {
        if (value.isEmpty()) return 0
        val flag = value[0].toInt()
        return if (flag and 0x01 != 0) {
            if (value.size >= 3) {
                (value[1].toInt() and 0xFF) or ((value[2].toInt() and 0xFF) shl 8)
            } else 0
        } else {
            if (value.size >= 2) value[1].toInt() and 0xFF else 0
        }
    }

// 开始扫描
    fun startScan() {
        if (isScanning) return

        _discoveredDevices.value = emptyList()
        _bleState.value = BleState.Scanning
        isScanning = true

        // 按心率服务UUID过滤，只扫描心率设备
        val filter = ScanFilter.Builder()
            .setServiceUuid(android.os.ParcelUuid.fromString(HEART_RATE_SERVICE_UUID.toString()))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

// 停止扫描
    fun stopScan() {
        if (!isScanning) return

        scanner?.stopScan(scanCallback)
        isScanning = false

        if (_discoveredDevices.value.isEmpty()) {
            _bleState.value = BleState.Idle
        }
    }

// 连接设备
    fun connectDevice(device: BleDevice) {
        stopScan()
        _bleState.value = BleState.Connecting

        val bluetoothDevice = device.device ?: run {
            _bleState.value = BleState.Error("Device not available")
            return
        }

        bluetoothDevice.connectGatt(context, false, gattCallback)
    }

// 断开连接
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _bleState.value = BleState.Disconnected
        _currentHeartRate.value = null
        _heartRateData.value = emptyList()
    }

    // 检查蓝牙是否可用
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

// 启动演示模式
    fun startDemoMode() {
        isDemoMode = true
        _bleState.value = BleState.ConnectedHR("演示模式")
        _heartRateData.value = emptyList()
        demoHeartRate = 72

        val demoRunnable = object : Runnable {
            override fun run() {
                if (!isDemoMode) return

                val currentTime = System.currentTimeMillis()
                // 模拟心率在60-85之间波动
                demoHeartRate = demoHeartRate + Random.nextInt(-3, 4)
                demoHeartRate = demoHeartRate.coerceIn(60, 85)

                updateHeartRate(demoHeartRate)

                // 继续收集
                val startTime = _heartRateData.value.firstOrNull()?.timestamp ?: currentTime
                val elapsed = currentTime - startTime
                if (elapsed < HR_COLLECTION_DURATION_MS) {
                    handler.postDelayed(this, 1000)
                }
            }
        }

        handler.post(demoRunnable)
    }

// 停止演示模式
    fun stopDemoMode() {
        isDemoMode = false
    }

    // 获取心率采集进度 (0-1)
    fun getCollectionProgress(): Float {
        val data = _heartRateData.value
        if (data.isEmpty()) return 0f

        val startTime = data.first().timestamp
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - startTime

        return (elapsed.toFloat() / HR_COLLECTION_DURATION_MS).coerceIn(0f, 1f)
    }
}
