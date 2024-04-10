package com.example.led

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.led.databinding.ActivityMainBinding
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

// TODO("create a separate class e.g. BluetoothService")

class MainActivity : ComponentActivity() {

    // constants
    private val LOG_TAG_NAME: String = "LOCAL_TEST"
    private val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // default well-known SSP UUID

    // intent request codes
    private val REQUEST_ENABLE_BT: Int = 1
    private val REQUEST_BT_CONNECT: Int = 2

    //
    private lateinit var binding: ActivityMainBinding

    // BT-related vars
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.enableBluetoothButton.setOnClickListener {
            enableBluetooth()
        }

        binding.getDevicesButton.setOnClickListener {
            getPairedBluetoothDevices()
            // TODO("get new bluetooth devices besides already paired once and initiate pair on select device")
        }

        binding.toggleLedButton.setOnCheckedChangeListener { _, isChecked ->
            toggleLed(isChecked)
        }

        // request runtime permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BT_CONNECT)
            }
        }

        // init BT adapter
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.adapter
        } else {
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // some cleanup
        if (bluetoothSocket != null) {
            bluetoothSocket?.close()
            bluetoothSocket = null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Bluetooth is required for this app", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_BT_CONNECT) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // TODO("Not yet implemented")
            } else {
                Toast.makeText(this, "Bluetooth is required for this app", Toast.LENGTH_SHORT).show()
                // TODO("direct user to settings")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            Toast.makeText(this, "Bluetooth is already enabled", Toast.LENGTH_SHORT).show()
        }

    }

    @SuppressLint("MissingPermission")
    private fun getPairedBluetoothDevices() {
        if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(this, "Enable bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        // get bounded devices
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        // transform to ArrayList so that it can be attached to an adapter
        val deviceArrayList: ArrayList<Device>? = pairedDevices
            ?.filter { pairedDevice ->
                pairedDevice.name.indexOf("HC-") > -1
            }?.map { pairedDevice ->
                Device(pairedDevice.name, pairedDevice.address)
            }?.toCollection(ArrayList())

        // populate ListView
        if (deviceArrayList != null) {
            val adapter = DeviceAdapter(this, deviceArrayList)

            binding.deviceListView.adapter = adapter
            binding.deviceListView.setOnItemClickListener { _, _, position, _ ->
                val device = adapter.getItem(position)
                pairDevice(device)
            }
        } else {
            Log.d(LOG_TAG_NAME, "No paired device found.")
        }
    }

    private fun toggleLed(isChecked: Boolean) {
        if (isChecked) {
            sendCommandData("C_ON")
        } else {
            sendCommandData("C_OFF")
        }
    }

    @SuppressLint("MissingPermission")
    private fun pairDevice(device: Device) {
        val (name, macAddress) = device

        if (bluetoothSocket != null) {
            bluetoothSocket?.close()
            bluetoothSocket = null
        }

        // since socket is blocking, run on new thread
        Thread(Runnable {
            try {
                val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(macAddress)

                Log.d(LOG_TAG_NAME, "$bluetoothDevice?.name")

                bluetoothSocket = bluetoothDevice?.createRfcommSocketToServiceRecord(APP_UUID)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream

                runOnUiThread {
                    Toast.makeText(this, "Connected to $name", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Log.d(LOG_TAG_NAME, "${e.message}")
            }
        }).start()
    }

    private fun sendCommandData(data: String) {
        if (bluetoothSocket != null) {
            val command = data + '\n'
            outputStream?.write(command.toByteArray())
        } else {
            Toast.makeText(this, "No bluetooth device connected.", Toast.LENGTH_SHORT).show()
        }
    }
}

