package tr.kilitekrani

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Arka planda çalışır; ekran kapanınca kilit ekranımızı hazırlar,
 * böylece ekran açıldığında ilk görünen bizim ekranımız olur.
 */
class KilitServisi : Service() {

    companion object {
        private const val KANAL = "kilit_ekrani_servis"
        private const val BILDIRIM_NO = 1

        fun baslat(context: Context) {
            val niyet = Intent(context, KilitServisi::class.java)
            context.startForegroundService(niyet)
        }
    }

    private var kayitli = false

    private val ekranAlici = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                val ac = Intent(context, MainActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                try {
                    context.startActivity(ac)
                } catch (e: Exception) {
                    // İzin yoksa sessizce geç; kullanıcı uygulamayı elle açabilir
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bildirim = bildirimOlustur()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                BILDIRIM_NO, bildirim,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(BILDIRIM_NO, bildirim)
        }
        if (!kayitli) {
            registerReceiver(ekranAlici, IntentFilter(Intent.ACTION_SCREEN_OFF))
            kayitli = true
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (kayitli) {
            unregisterReceiver(ekranAlici)
            kayitli = false
        }
        super.onDestroy()
    }

    private fun bildirimOlustur(): Notification {
        val yonetici = getSystemService(NotificationManager::class.java)
        val kanal = NotificationChannel(
            KANAL, "Kilit ekranı", NotificationManager.IMPORTANCE_MIN
        )
        kanal.setShowBadge(false)
        yonetici.createNotificationChannel(kanal)

        val dokun = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, KANAL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Kilit ekranı etkin")
            .setContentIntent(dokun)
            .setOngoing(true)
            .build()
    }
}
