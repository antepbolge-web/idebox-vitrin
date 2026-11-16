package tr.kilitekrani

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        private const val HOST_ID = 1024
        private const val REQ_BAGLA = 1
        private const val REQ_AYAR = 2
        private const val REQ_DUVAR = 3
        private const val TERCIH = "kilit_ekrani"
        private const val ANAHTAR_WIDGETLAR = "widgetlar"
        private const val ANAHTAR_DUVAR = "duvar_kagidi"
    }

    private lateinit var pil: TextView
    private lateinit var widgetAlani: LinearLayout
    private lateinit var arkaplan: ImageView
    private lateinit var perde: View
    private lateinit var widgetYoneticisi: AppWidgetManager
    private lateinit var widgetSunucusu: AppWidgetHost

    /** Ekleme sürecindeki widget numarası (iptal olursa silinir). */
    private var bekleyenWidget = -1

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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        pil = findViewById(R.id.pil)
        widgetAlani = findViewById(R.id.widgetAlani)
        arkaplan = findViewById(R.id.arkaplan)
        perde = findViewById(R.id.perde)

        widgetYoneticisi = AppWidgetManager.getInstance(this)
        widgetSunucusu = AppWidgetHost(applicationContext, HOST_ID)

        findViewById<Button>(R.id.widgetEkle).setOnClickListener { widgetSec() }
        findViewById<Button>(R.id.widgetKaldir).setOnClickListener { widgetKaldirmaListesi() }
        findViewById<Button>(R.id.duvarKagidi).setOnClickListener { duvarKagidiSec() }

        bekleyenWidget = savedInstanceState?.getInt("bekleyen", -1) ?: -1
        duvarKagidiniYukle()
        widgetlariGoster()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("bekleyen", bekleyenWidget)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(pilAlici, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        widgetSunucusu.startListening()
    }

    override fun onStop() {
        unregisterReceiver(pilAlici)
        widgetSunucusu.stopListening()
        super.onStop()
    }

    // ---------- Widget kayıtları ----------

    private fun kayitliWidgetlar(): MutableList<Int> {
        val metin = getSharedPreferences(TERCIH, MODE_PRIVATE)
            .getString(ANAHTAR_WIDGETLAR, "") ?: ""
        return metin.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableList()
    }

    private fun widgetlariKaydet(liste: List<Int>) {
        getSharedPreferences(TERCIH, MODE_PRIVATE).edit()
            .putString(ANAHTAR_WIDGETLAR, liste.joinToString(","))
            .apply()
    }

    private fun widgetAdi(bilgi: AppWidgetProviderInfo): String {
        val ad = bilgi.loadLabel(packageManager)
        val uygulama = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(bilgi.provider.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            bilgi.provider.packageName
        }
        return "$uygulama – $ad"
    }

    // ---------- Widget gösterme ----------

    /**
     * İki sütunlu yerleşim: geniş widget'lar tam satır, küçükler yan yana ikişer.
     */
    private fun widgetlariGoster() {
        widgetAlani.removeAllViews()
        val yogunluk = resources.displayMetrics.density
        val ekranGenisligi = resources.displayMetrics.widthPixels
        val bosluk = (12 * yogunluk).toInt()
        val gecerli = mutableListOf<Int>()
        var satir: LinearLayout? = null

        fun satiriKapat() {
            // Tek widget kaldıysa sola yaslanmasın diye boş yer tutucu ekle
            satir?.let {
                if (it.childCount == 1) {
                    it.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
                }
            }
            satir = null
        }

        for (id in kayitliWidgetlar()) {
            val bilgi = widgetYoneticisi.getAppWidgetInfo(id) ?: continue
            gecerli.add(id)
            val gorunum = widgetSunucusu.createView(applicationContext, id, bilgi)
            gorunum.setAppWidget(id, bilgi)
            val yukseklik = maxOf(bilgi.minHeight, (120 * yogunluk).toInt())
            val genis = bilgi.minWidth > ekranGenisligi * 0.55

            if (genis) {
                satiriKapat()
                val parametre = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, yukseklik
                )
                parametre.topMargin = bosluk
                widgetAlani.addView(gorunum, parametre)
            } else {
                if (satir == null || satir!!.childCount >= 2) {
                    satiriKapat()
                    val yeni = LinearLayout(this)
                    yeni.orientation = LinearLayout.HORIZONTAL
                    val satirParametre = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    satirParametre.topMargin = bosluk
                    widgetAlani.addView(yeni, satirParametre)
                    satir = yeni
                }
                val parametre = LinearLayout.LayoutParams(0, yukseklik, 1f)
                if (satir!!.childCount == 1) parametre.marginStart = bosluk
                satir!!.addView(gorunum, parametre)
            }
        }
        satiriKapat()
        widgetlariKaydet(gecerli)
    }

    // ---------- Widget ekleme ----------

    private fun widgetSec() {
        val saglayicilar = widgetYoneticisi.installedProviders
            .sortedBy { widgetAdi(it).lowercase() }
        if (saglayicilar.isEmpty()) {
            Toast.makeText(this, "Telefonda widget bulunamadı", Toast.LENGTH_LONG).show()
            return
        }
        val adlar = saglayicilar.map { widgetAdi(it) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Widget seçin")
            .setItems(adlar) { _, sira -> widgetBagla(saglayicilar[sira]) }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun widgetBagla(bilgi: AppWidgetProviderInfo) {
        val id = widgetSunucusu.allocateAppWidgetId()
        bekleyenWidget = id
        if (widgetYoneticisi.bindAppWidgetIdIfAllowed(id, bilgi.provider)) {
            widgetAyarla(id)
        } else {
            // Telefon, bu uygulamanın widget kullanmasına izin isteyecek
            val istek = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, bilgi.provider)
            startActivityForResult(istek, REQ_BAGLA)
        }
    }

    private fun widgetAyarla(id: Int) {
        val bilgi = widgetYoneticisi.getAppWidgetInfo(id)
        if (bilgi?.configure != null) {
            try {
                widgetSunucusu.startAppWidgetConfigureActivityForResult(
                    this, id, 0, REQ_AYAR, null
                )
                return
            } catch (e: Exception) {
                Toast.makeText(this, "Widget ayar ekranı açılamadı", Toast.LENGTH_LONG).show()
            }
        }
        widgetEkle(id)
    }

    private fun widgetEkle(id: Int) {
        val liste = kayitliWidgetlar()
        if (id !in liste) liste.add(id)
        widgetlariKaydet(liste)
        bekleyenWidget = -1
        widgetlariGoster()
    }

    private fun bekleyeniIptalEt() {
        if (bekleyenWidget != -1) {
            widgetSunucusu.deleteAppWidgetId(bekleyenWidget)
            bekleyenWidget = -1
        }
    }

    // ---------- Widget kaldırma ----------

    private fun widgetKaldirmaListesi() {
        val liste = kayitliWidgetlar()
        if (liste.isEmpty()) {
            Toast.makeText(this, "Kaldırılacak widget yok", Toast.LENGTH_SHORT).show()
            return
        }
        val adlar = liste.map { id ->
            widgetYoneticisi.getAppWidgetInfo(id)?.let { widgetAdi(it) } ?: "Bilinmeyen widget"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Kaldırılacak widget")
            .setItems(adlar) { _, sira ->
                val id = liste.removeAt(sira)
                widgetSunucusu.deleteAppWidgetId(id)
                widgetlariKaydet(liste)
                widgetlariGoster()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    // ---------- Duvar kâğıdı ----------

    private fun duvarKagidiSec() {
        val istek = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("image/*")
        startActivityForResult(istek, REQ_DUVAR)
    }

    private fun duvarKagidiniYukle() {
        val metin = getSharedPreferences(TERCIH, MODE_PRIVATE)
            .getString(ANAHTAR_DUVAR, null) ?: return
        val resim = resimOku(Uri.parse(metin))
        if (resim != null) {
            arkaplan.setImageBitmap(resim)
            perde.visibility = View.VISIBLE
        }
    }

    /** Resmi ekran boyutuna yakın küçülterek okur (bellek taşmasın diye). */
    private fun resimOku(adres: Uri): Bitmap? {
        return try {
            val olcu = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(adres)?.use { BitmapFactory.decodeStream(it, null, olcu) }
            val hedefG = resources.displayMetrics.widthPixels
            val hedefY = resources.displayMetrics.heightPixels
            var oran = 1
            while (olcu.outWidth / (oran * 2) >= hedefG && olcu.outHeight / (oran * 2) >= hedefY) {
                oran *= 2
            }
            val secenek = BitmapFactory.Options().apply { inSampleSize = oran }
            contentResolver.openInputStream(adres)?.use { BitmapFactory.decodeStream(it, null, secenek) }
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Sonuçlar ----------

    @Deprecated("Activity sonuç API'si")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_BAGLA -> {
                if (resultCode == RESULT_OK && bekleyenWidget != -1) {
                    widgetAyarla(bekleyenWidget)
                } else {
                    bekleyeniIptalEt()
                }
            }
            REQ_AYAR -> {
                if (resultCode == RESULT_OK && bekleyenWidget != -1) {
                    widgetEkle(bekleyenWidget)
                } else {
                    bekleyeniIptalEt()
                }
            }
            REQ_DUVAR -> {
                val adres = data?.data
                if (resultCode == RESULT_OK && adres != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            adres, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                        // Kalıcı izin verilmediyse yine de bu oturumda gösterilir
                    }
                    getSharedPreferences(TERCIH, MODE_PRIVATE).edit()
                        .putString(ANAHTAR_DUVAR, adres.toString())
                        .apply()
                    duvarKagidiniYukle()
                }
            }
        }
    }
}
