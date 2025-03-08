package com.john.ai.myapplication

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat


// 权限处理组件
@Composable
fun PermissionHandler(viewModel: BluetoothViewModel) {
    val context = LocalContext.current
    val requiredPermissions = remember { getRequiredPermissions() }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        viewModel.handlePermissionResult(perms.all { it.value })
    }

    LaunchedEffect(Unit) {
        if (requiredPermissions.all { ContextCompat.checkSelfPermission(context, it) == PERMISSION_GRANTED }) {
            viewModel.checkBluetoothState()
        } else {
            launcher.launch(requiredPermissions)
        }
    }
}

private fun getRequiredPermissions(): Array<String> {
    val permissions = mutableListOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    return permissions.toTypedArray()
}