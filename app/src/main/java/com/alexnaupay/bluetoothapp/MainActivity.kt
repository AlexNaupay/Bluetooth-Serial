package com.alexnaupay.bluetoothapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.UUID




class MainActivity : AppCompatActivity() {

    val MY_UUID = UUID.fromString("0000110A-0000-1000-8000-00805F9B34FB")

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // returns boolean representind whether the
            // permission is granted or not
            if (isGranted) {
                // permission granted continue the normal workflow of app
                //downloadPublication(urlPdf)
                Log.d(TAG, "Bluetooth permission granted")
            } else {
                // if permission denied then check whether never ask
                // again is selected or not by making use of
                // !ActivityCompat.shouldShowRequestPermissionRationale(
                // requireActivity(), Manifest.permission.CAMERA)
                Toast.makeText(applicationContext, "Conceder permisos para bluetooth", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: ${intent.action}")

            val action: String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address

                    Log.d(TAG, "deviceName: $deviceName")
                    Log.d(TAG, "deviceHardwareAddress: $deviceHardwareAddress")

                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val button = findViewById<Button>(R.id.scanButton)


        Log.d(TAG, "*********** App Started ***********")

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.w(TAG, "Device doesn't support Bluetooth")
        }

        //requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)

        if (bluetoothAdapter?.isEnabled == false) {
            Log.w(TAG, "Bluetooth disabled")
            //val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }else{
            Log.i(TAG, "Bluetooth is enabled")
        }


        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
            }

        } else {
            // todo
        }


        Log.w(TAG, "==== List of devices =====")

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            Log.i(TAG, deviceName)
            Log.i(TAG, device.address)
            Log.i(TAG, device.bondState.toString())
            Log.i(TAG, ".........")

             // val bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
             // bluetoothSocket.connect();

        }
        // 48:E7:29:9F:90:06


        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        button.setOnClickListener {
            val result = bluetoothAdapter?.startDiscovery()

            Log.w(TAG, "Result of startDiscovery: $result")

        }

    }

    fun askBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Directly ask for the permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            // downloadPublication(urlPdf)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
    }

}

