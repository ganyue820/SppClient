package com.john.ai.myapplication

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.util.UUID


private val TAG: String = MainActivity::class.java.simpleName + "AA"
class BluetoothViewModel(private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(BluetoothState())
    val state: StateFlow<BluetoothState> = _state.asStateFlow()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    fun handlePermissionResult(granted: Boolean) {
        _state.update { it.copy(hasPermissions = granted) }
        if (granted) checkBluetoothState()
    }
    fun checkBluetoothState() {
        bluetoothAdapter?.let {
            _state.update { state ->
                state.copy(
                    bluetoothEnabled = it.isEnabled,
                    showEnableBt = !it.isEnabled
                )
            }
        } ?: run {
            _state.update { it.copy(error = "设备不支持蓝牙") }
        }
    }
    private var connectedThread: ConnectedThread? = null
    private val sppUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun startA2DPProfile() {
        bluetoothAdapter?.let {
            Log.d(TAG,"startA2DPProfile")
            _state.value = _state.value.copy(isScanning = true)
            it.bondedDevices.let { connectedDevice ->
                _state.value = _state.value.copy(
                    discoveredDevices = _state.value.discoveredDevices + connectedDevice
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        Log.d(TAG,"stopA2dp")
        _state.value = _state.value.copy(isScanning = false)
        connectedThread?.cancel()
        connectedThread = null
        _state.update { it.copy(connectionStatus = "Not connected") }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG,"connectToDevice")
        ConnectThread(device).start()
    }

    fun sendMessage() {
        connectedThread?.write(_state.value.messageToSend.toByteArray())
        _state.value = _state.value.copy(messageToSend = "")
    }

    fun updateMessageToSend(message: String) {
        _state.value = _state.value.copy(messageToSend = message)
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(val device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            Log.d(TAG,"createRfcommSocketToServiceRecord")
            device.createRfcommSocketToServiceRecord(sppUUID)
        }

        override fun run() {
            mmSocket?.let { socket ->
                try {
                    socket.connect()
                    connectedThread = ConnectedThread(socket)
                    connectedThread?.start()
                    _state.value = _state.value.copy(
                        connectionStatus = "Connected to ${device.name}"
                    )
                } catch (e: IOException) {
                    _state.value = _state.value.copy(
                        connectionStatus = "Connection failed"
                    )
                    try {
                        socket.close()
                    } catch (e: IOException) { }
                }
            }
        }
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream = mmSocket.inputStream
        private val mmOutStream = mmSocket.outputStream
        private val handler = Handler(Looper.getMainLooper())

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    bytes = mmInStream.read(buffer)
                    val receivedMessage = String(buffer, 0, bytes)
                    handler.post {
                        _state.value = _state.value.copy(
                            receivedData = _state.value.receivedData + "\n" + receivedMessage
                        )
                    }
                } catch (e: IOException) {
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                Log.d(TAG,"write bytes: ${bytesToHex(bytes)}")
                mmOutStream.write(bytes)
            } catch (e: IOException) { }
        }

        private fun bytesToHex(bytes: ByteArray): String {
            return bytes.joinToString("") { "%02X".format(it) }
        }


        fun cancel() {
            try {
                mmInStream.close()
                mmOutStream.close()
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing socket: ${e.message}")
            }
        }
    }

    data class BluetoothState(
        val hasPermissions: Boolean = false,
        val bluetoothEnabled: Boolean = false,
        val showEnableBt: Boolean = false,
        val error: String = "",
        val isScanning: Boolean = false,
        val discoveredDevices: List<BluetoothDevice> = emptyList(),
        val connectionStatus: String = "Not connected",
        val messageToSend: String = "",
        val receivedData: String = ""
    )
}