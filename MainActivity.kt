package com.pahlavan.gamebooster

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Debug
import android.os.StatFs
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var networkText: TextView
    private lateinit var deviceText: TextView
    private lateinit var profilesText: TextView
    private val prefs by lazy { getSharedPreferences("pgbx", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 24, 32)
        window.navigationBarColor = Color.rgb(16, 24, 32)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 18, 20, 24); setBackgroundColor(Color.rgb(16,24,32)) }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        content.addView(label("PAHLAVAN  /  GAME BOOSTER X", 22, "#00BFA5"))
        content.addView(label("نسخه آزمایشی 0.1 • Android 8+", 13, "#AAB7C4"))
        content.addView(gap(16))
        status = cardText("وضعیت: آماده بررسی")
        content.addView(status)
        content.addView(label("پایش شبکه", 18, "#FFFFFF"))
        networkText = cardText("پینگ: —    جیتر: —    از دست‌رفتن اتصال: —")
        content.addView(networkText)
        content.addView(button("بررسی اتصال و پینگ", "#00796B") { runNetworkTest() })
        content.addView(gap(10))
        content.addView(label("وضعیت دستگاه", 18, "#FFFFFF"))
        deviceText = cardText("در حال خواندن اطلاعات دستگاه…")
        content.addView(deviceText)
        content.addView(button("به‌روزرسانی آمار دستگاه", "#37474F") { refreshDeviceStats() })
        content.addView(gap(10))
        content.addView(label("پنل شناور داخل بازی", 18, "#FFFFFF"))
        content.addView(label("برای نمایش روی بازی، اجازهٔ «نمایش روی برنامه‌های دیگر» لازم است.", 13, "#AAB7C4"))
        content.addView(button("فعال‌کردن دکمه شناور", "#00695C") { enableOverlay() })
        content.addView(button("خاموش‌کردن دکمه شناور", "#455A64") { stopService(Intent(this, FloatingWidgetService::class.java)) })
        content.addView(gap(10))
        content.addView(label("پروفایل‌های سرور (فقط ذخیره و مدیریت)", 18, "#FFFFFF"))
        content.addView(label("در این نسخه اتصال VPN واقعی هنوز فعال نیست. افزودن پروفایل به‌تنهایی تونل نمی‌سازد.", 13, "#FFCC80"))
        profilesText = cardText("")
        content.addView(profilesText)
        content.addView(button("افزودن پروفایل سرور", "#5D4037") { addProfileDialog() })
        content.addView(button("واردکردن فایل تنظیمات JSON", "#5D4037") { importJson() })
        content.addView(button("حذف همه پروفایل‌ها", "#6D3030") { prefs.edit().remove("profiles").apply(); refreshProfiles() })
        content.addView(gap(18))
        content.addView(label("نکته: این برنامه فریم‌ریت را جادویی بالا نمی‌برد. دما و آمار CPU/GPU هم بسته به محدودیت اندروید ممکن است کامل نباشد.", 12, "#AAB7C4"))
        setContentView(root)
        refreshDeviceStats(); refreshProfiles()
    }

    private fun label(text: String, size: Int, color: String) = TextView(this).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(Color.parseColor(color)); setPadding(2, 7, 2, 7)
    }
    private fun cardText(text: String) = TextView(this).apply {
        this.text = text; textSize = 15f; setTextColor(Color.WHITE); setPadding(14, 14, 14, 14)
        setBackgroundColor(Color.rgb(31, 43, 54)); gravity = Gravity.CENTER_VERTICAL
    }
    private fun gap(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, h) }
    private fun button(text: String, color: String, action: () -> Unit) = Button(this).apply {
        this.text = text; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor(color)); isAllCaps = false
        setOnClickListener { action() }
    }

    private fun runNetworkTest() {
        status.text = "وضعیت: در حال بررسی چند اتصال HTTPS…"
        networkText.text = "در حال اندازه‌گیری…"
        thread {
            val targets = listOf("1.1.1.1" to 443, "8.8.8.8" to 443, "www.google.com" to 443)
            val times = mutableListOf<Long>(); var failed = 0
            repeat(6) { i ->
                val target = targets[i % targets.size]
                val start = System.nanoTime()
                try { Socket().use { it.connect(InetSocketAddress(target.first, target.second), 1800) }; times.add((System.nanoTime()-start)/1_000_000) }
                catch (_: Exception) { failed++ }
            }
            val sorted = times.sorted()
            val avg = if (times.isEmpty()) -1 else times.average().toLong()
            val jitter = if (times.size < 2) -1 else times.zipWithNext { a,b -> abs(a-b) }.average().toLong()
            runOnUiThread {
                networkText.text = if (times.isEmpty()) "اتصال آزمایشی ناموفق بود؛ ممکن است اینترنت/فایروال مقصد را مسدود کرده باشد." else
                    "میانگین زمان اتصال: ${avg}ms\nنوسان تقریبی: ${jitter}ms\nموفق: ${times.size}/6  •  ناموفق: $failed/6\nاین عدد پینگ داخل سرور بازی نیست؛ آزمون TCP است."
                status.text = "وضعیت: بررسی تمام شد"
            }
        }
    }

    private fun refreshDeviceStats() {
        val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val mi = android.app.ActivityManager.MemoryInfo(); am.getMemoryInfo(mi)
        val runtime = Runtime.getRuntime()
        val usedApp = (runtime.totalMemory() - runtime.freeMemory()) / (1024*1024)
        val totalRam = mi.totalMem / (1024*1024); val freeRam = mi.availMem / (1024*1024)
        val battery = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tempTenths = battery?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val temp = if (tempTenths >= 0) "${tempTenths / 10.0}°C (دمای باتری)" else "نامشخص"
        deviceText.text = "RAM کل: ${totalRam}MB  •  RAM آزاد: ${freeRam}MB\nحافظه مصرفی خود برنامه: ${usedApp}MB\nدمای باتری: $temp\nGPU/دمای CPU: در این نسخه قابل‌خواندنِ مطمئن نیست."
    }

    private fun profiles(): JSONArray = try { JSONArray(prefs.getString("profiles", "[]")) } catch (_: Exception) { JSONArray() }
    private fun refreshProfiles() {
        val arr = profiles(); val lines = (0 until arr.length()).mapNotNull { i -> try { val o=arr.getJSONObject(i); "${i+1}. ${o.optString("name")} • ${o.optString("type")} • ${o.optString("address")}" } catch (_: Exception) { null } }
        profilesText.text = if (lines.isEmpty()) "هنوز پروفایلی ذخیره نشده." else lines.joinToString("\n")
    }
    private fun addProfileDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,8,24,0) }
        val name = EditText(this).apply { hint = "نام پروفایل" }
        val type = EditText(this).apply { hint = "نوع (مثلاً WireGuard / OpenVPN)" }
        val addr = EditText(this).apply { hint = "آدرس سرور یا توضیح" }
        box.addView(name); box.addView(type); box.addView(addr)
        android.app.AlertDialog.Builder(this).setTitle("پروفایل جدید").setView(box).setNegativeButton("لغو", null).setPositiveButton("ذخیره") { _, _ ->
            if (name.text.isNotBlank()) { val a=profiles(); a.put(JSONObject().put("name",name.text.toString()).put("type",type.text.toString()).put("address",addr.text.toString())); prefs.edit().putString("profiles",a.toString()).apply(); refreshProfiles() }
        }.show()
    }
    private val picker = 81
    private fun importJson() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }
        try { startActivityForResult(i, picker) } catch (_: Exception) { Toast.makeText(this,"انتخاب فایل در دسترس نیست",Toast.LENGTH_SHORT).show() }
    }
    @Deprecated("Deprecated in Android API but compatible with minSdk")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == picker && resultCode == RESULT_OK && data?.data != null) {
            try {
                val raw = contentResolver.openInputStream(data.data!!)?.bufferedReader()?.use { it.readText() } ?: ""
                val parsed = JSONArray(raw); val existing = profiles()
                for (i in 0 until parsed.length()) existing.put(parsed.getJSONObject(i))
                prefs.edit().putString("profiles", existing.toString()).apply(); refreshProfiles()
                Toast.makeText(this,"پروفایل‌های JSON وارد شدند (اتصال VPN فعال نمی‌شود).",Toast.LENGTH_LONG).show()
            } catch (_: Exception) { Toast.makeText(this,"فایل باید آرایه JSON از پروفایل‌ها باشد.",Toast.LENGTH_LONG).show() }
        }
    }
    private fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this,"اجازه را فعال کن و دوباره برگرد و دکمه را بزن.",Toast.LENGTH_LONG).show(); return
        }
        startService(Intent(this, FloatingWidgetService::class.java))
    }
}
