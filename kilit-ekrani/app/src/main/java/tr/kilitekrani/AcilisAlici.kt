package tr.kilitekrani

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Telefon yeniden başlayınca kilit ekranı servisini başlatır. */
class AcilisAlici : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                KilitServisi.baslat(context)
            } catch (e: Exception) {
                // Sistem izin vermezse uygulama ilk açıldığında başlar
            }
        }
    }
}
