package tr.kilitekrani

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var pil: TextView

    private val pilAlici = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val seviye = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val olcek = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val durum = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val sarjda = durum == BatteryManager.BATTERY_STATUS_CHARGING ||
                durum == BatteryManager.BATTERY_STATUS_FULL
            if (seviye >= 0 && olcek > 0) {
                val yuzde = seviye * 100 / olcek
                pil.text = "Pil: %$yuzde" + if (sarjda) " (şarjda)" else ""
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pil = findViewById(R.id.pil)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(pilAlici, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStop() {
        unregisterReceiver(pilAlici)
        super.onStop()
    }
}
