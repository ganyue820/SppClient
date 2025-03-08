package com.john.ai.myapplication

import android.annotation.SuppressLint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@SuppressLint("MissingPermission")
@Composable
fun BluetoothSPPClient() {
    val context = LocalContext.current
    val viewModel = remember { BluetoothViewModel(context) }
    val state by viewModel.state.collectAsState()

    PermissionHandler(viewModel)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 控制按钮
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = { viewModel.startA2DPProfile() }) {
                Text("Bonded Devices")
            }
            Button(onClick = { viewModel.disconnect() }) {
                Text("Disconnect")
            }
        }

        // 设备列表
        Text("Bonded Devices:", style = MaterialTheme.typography.titleSmall)
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(state.discoveredDevices) { device ->
                Button(
                    onClick = { viewModel.connectToDevice(device) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${device.name ?: "Unknown"} - ${device.address}")
                }
            }
        }

        // 连接状态
        Text(
            text = "Status: ${state.connectionStatus}",
            style = MaterialTheme.typography.titleSmall
        )

        // 数据发送
        TextField(
            value = state.messageToSend,
            onValueChange = { viewModel.updateMessageToSend(it) },
            label = { Text("Message to send") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.sendMessage() },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Send")
        }

        // 接收数据
        Text("Received Data:", style = MaterialTheme.typography.titleSmall)
        Text(state.receivedData)
    }
}