package com.app.exploringmqtt

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.exploringmqtt.databinding.ActivityMainBinding
import com.app.exploringmqtt.mqtt.MqttClientHelper
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = "MainActivity"
        private val BATTERY_STATUS_TOPIC = "shantnu_kmr/feeds/battery-percentage"
    }

    private val mqttClient by lazy {
        MqttClientHelper(this)
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textViewMsgPayload.movementMethod = ScrollingMovementMethod()

        setMqttCallBack()

        binding.textViewNumMsgs.text = "0"

        binding.btnPub.setOnClickListener {
            Log.d(TAG, "onCreate: Published the message")
            var toastMsg: String
            val topic = "shantnu_kmr/feeds/" + binding.editTextPubTopic.text.toString().trim()
            toastMsg = "Cannot publish to empty topic!"
            if (!mqttClient.isConnected()) {
                toastMsg = "Please connect to the broker"
            } else if (topic.isNotEmpty()) {
                toastMsg = try {
                    mqttClient.publish(topic, binding.editTextMsgPayload.text.toString())
                    "Published to topic '$topic'"
                } catch (ex: MqttException) {
                    "Error publishing to topic: $topic"
                }
            }
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
        }

        binding.btnSub.setOnClickListener {
            Log.d(TAG, "onCreate: subscribed to the message")
            var toastMsg: String
            val topic = "shantnu_kmr/feeds/" + binding.editTextSubTopic.text.toString().trim()
            toastMsg = "Cannot subscribe to empty topic"
            if (!mqttClient.mqttAndroidClient.isConnected) {
                toastMsg = "Please connect to the broker"
            } else if (topic.isNotEmpty()) {
                toastMsg = try {
                    mqttClient.subscribe(topic)
                    "Subscribed to topic: ${topic}"
                } catch (ex: MqttException) {
                    "Error subscribing to topic: $topic"
                }
            }
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
        }

        binding.btnConnectionStatus.setOnClickListener {
            Log.d(TAG, "onCreate: MQTT Client is $mqttClient")
            if (mqttClient.isConnected()) {
                mqttClient.disconnectClient()
                binding.btnConnectionStatus.text = "Connect"
                binding.connectionStatus.text = "Disconnected from Broker"
                binding.connectionStatusImg.setImageResource(R.drawable.state_disconnected)
                Log.d(TAG, "onCreate: MQTT Client is disconnected now")
            } else if (!mqttClient.isConnected()) {
                mqttClient.connectClient()
                binding.btnConnectionStatus.text = "Disconnect"
                binding.connectionStatus.text = "Connected to Broker"
                binding.connectionStatusImg.setImageResource(R.drawable.state_connected)
                Log.d(TAG, "onCreate: MQTT Client is connected now")
            }
        }
        binding.btnAutomatePublishing.setOnClickListener {
            subscribeAndPushBatteryStatus(BATTERY_STATUS_TOPIC)
        }

        binding.textViewMsgPayload.setOnClickListener {
            hideKeyboard()
        }

        binding.checkConnection.setOnClickListener {
            if (checkConnection()) {
                Toast.makeText(this, "Connection is live", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No Connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkConnection(): Boolean {
        return mqttClient.isConnected()
    }

    /**
     * subscribe to BATTERY_STATUS_TOPIC and push dummy data to the broker
     */
    private fun subscribeAndPushBatteryStatus(topic: String?) {

        try {
            if (mqttClient.isConnected())
                mqttClient.subscribe(BATTERY_STATUS_TOPIC)
        } catch (ex: MqttException) {
            "Error subscribing to topic: $topic"
        }

        if (mqttClient == null) {
            Log.d(TAG, "subscribeAndPushBatteryStatus: Android client is null")
        } else {
            Thread(Runnable {
                for (i in 100 downTo 70) {
                    try {
                        mqttClient.publish(BATTERY_STATUS_TOPIC, i.toString())
                    } catch (ex: MqttException) {
                        "Error publishing to topic: $BATTERY_STATUS_TOPIC"
                    }
                    Thread.sleep(2000)
                }
            }).start()
        }
    }

    private fun setMqttCallBack() {
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                val toastMsg = "Connected to host:\n'$ADAFRUIT_MQTT_HOST'."
                Log.w("Debug", toastMsg)
                Toast.makeText(this@MainActivity, toastMsg, Toast.LENGTH_SHORT).show()
            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.w("Debug", "Message received from host '$ADAFRUIT_MQTT_HOST': $message")
                binding.textViewNumMsgs.text =
                    ("${binding.textViewNumMsgs.text.toString().toInt() + 1}")
                val str = "\n$message\n${binding.textViewMsgPayload.text}"
                binding.textViewMsgPayload.text = str
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.w("Debug", "Message published to host '$ADAFRUIT_MQTT_HOST'")
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                val toastMsg = "Connected to host:\n '$ADAFRUIT_MQTT_HOST'."
                Log.w("Debug", toastMsg)
                Toast.makeText(this@MainActivity, toastMsg, Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val manager = getSystemService(
                INPUT_METHOD_SERVICE
            ) as InputMethodManager
            manager
                .hideSoftInputFromWindow(
                    view.windowToken, 0
                )
        }
    }


    override fun onDestroy() {
        mqttClient.destroy()
        super.onDestroy()
    }

}