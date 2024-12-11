package com.alexnaupay.bluetoothapp.serial

import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alexnaupay.bluetoothapp.R
import com.alexnaupay.bluetoothapp.serial.SerialService.SerialBinder
import com.alexnaupay.bluetoothapp.serial.TextUtil.HexWatcher
import java.lang.Exception
import java.lang.StringBuilder
import java.util.ArrayDeque

class SerialActivityKt : AppCompatActivity(), ServiceConnection, SerialListener {
    private enum class Connected {
        False, Pending, True
    }

    private var deviceAddress: String? = null
    private var service: SerialService? = null

    private var connected = Connected.False
    private val hexEnabled = false
    private var initialStart = true

    private var receiveText: TextView? = null
    private var sendText: TextView? = null
    private var sendButton: Button? = null

    private val hexWatcher: HexWatcher? = null

    private var pendingNewline = false
    private val newline = TextUtil.newline_crlf

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_serial)

        receiveText = findViewById<TextView>(R.id.text)
        sendButton = findViewById<Button>(R.id.sendButton)
        sendText = findViewById<TextView>(R.id.sendText)

        deviceAddress = "48:E7:29:9F:90:06"
        bindService(Intent(this, SerialService::class.java), this, BIND_AUTO_CREATE)

        sendButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            send(
                sendText!!.getText().toString()
            )
        })
    }

    public override fun onStart() {
        super.onStart()
        if (service != null) {
            service!!.attach(this) // *** Listener ***
        } else {
            // prevents service destroy on unbind from recreated activity caused by orientation change
            startService(Intent(this, SerialService::class.java))
        }
    }

    public override fun onResume() {
        super.onResume()
        if (initialStart && service != null) {
            initialStart = false
            runOnUiThread(Runnable { this.connect() }) // Connect
        }
    }

    public override fun onStop() {
        if (service != null && !isChangingConfigurations()) {
            service!!.detach()
        }
        super.onStop()
    }

    public override fun onDestroy() {
        if (connected != Connected.False) {
            disconnect()
        }
        stopService(Intent(this, SerialService::class.java))
        super.onDestroy()
    }

    private fun status(str: String?) {
        val spn = SpannableStringBuilder(str + '\n')
        spn.setSpan(
            ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)),
            0,
            spn.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        receiveText!!.append(spn)
    }

    private fun connect() {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            status("connecting...")
            status(device.getUuids()[0].getUuid().toString())

            connected = Connected.Pending
            val socket = SerialSocket(getApplicationContext(), device)
            service!!.connect(socket)
        } catch (e: Exception) {
            onSerialConnectError(e)
        }
    }

    private fun disconnect() {
        connected = Connected.False
        service!!.disconnect()
    }

    private fun send(str: String) {
        if (connected != Connected.True) {
            Toast.makeText(this, "not connected", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            var msg: String?
            var data: ByteArray?
            if (hexEnabled) {
                val sb = StringBuilder()
                TextUtil.toHexString(sb, TextUtil.fromHexString(str))
                TextUtil.toHexString(sb, newline.toByteArray())
                msg = sb.toString()
                data = TextUtil.fromHexString(msg)
            } else {
                msg = str
                data = (str + newline).toByteArray()
            }
            val spn = SpannableStringBuilder(msg + '\n')
            spn.setSpan(
                ForegroundColorSpan(getResources().getColor(R.color.black)),
                0,
                spn.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            receiveText!!.append(spn)
            service!!.write(data)
        } catch (e: Exception) {
            onSerialIoError(e)
        }
    }

    private fun receive(datas: ArrayDeque<ByteArray?>) {
        val spn = SpannableStringBuilder()
        for (data in datas) {
            if (hexEnabled) {
                spn.append(TextUtil.toHexString(data)).append('\n')
            } else {
                var msg: String = data.toString()
                if (newline == TextUtil.newline_crlf && msg.length > 0) {
                    // don't show CR as ^M if directly before LF
                    msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf)
                    // special handling if CR and LF come in separate fragments
                    if (pendingNewline && msg.get(0) == '\n') {
                        if (spn.length >= 2) {
                            spn.delete(spn.length - 2, spn.length)
                        } else {
                            val edt = receiveText!!.getEditableText()
                            if (edt != null && edt.length >= 2) edt.delete(
                                edt.length - 2,
                                edt.length
                            )
                        }
                    }
                    pendingNewline = msg.get(msg.length - 1) == '\r'
                }
                spn.append(TextUtil.toCaretString(msg, newline.length != 0))
            }
        }
        receiveText!!.append(spn)
    }


    // ServiceConnection methods
    override fun onServiceConnected(name: ComponentName?, binder: IBinder) {
        service = (binder as SerialBinder).getService()
        service!!.attach(this)
        if (initialStart /*&& isResumed()*/) {
            initialStart = false
            runOnUiThread(Runnable { this.connect() })
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        service = null
    }


    // End ServiceConnection methods
    // SerialListener methods
    override fun onSerialConnect() {
        status("connected")
        connected = Connected.True
    }

    override fun onSerialConnectError(e: Exception) {
        status("connection failed: " + e.message)
        disconnect()
    }

    override fun onSerialRead(data: ByteArray?) {
        val datas = ArrayDeque<ByteArray?>()
        datas.add(data)
        receive(datas)
    }

    override fun onSerialRead(datas: ArrayDeque<ByteArray?>) {
        receive(datas)
    }

    override fun onSerialIoError(e: Exception) {
        status("connection lost: " + e.message)
        disconnect()
    } // End SerialListener methods
}
