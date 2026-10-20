package com.denge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.denge.app.ui.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { DengeTheme { DengeApp() } } }
}
class PrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { DengeTheme {
        Surface { Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Verilerin senin kontrolünde.", style = MaterialTheme.typography.headlineMedium)
            Text("Wellfit yalnızca izin verdiğin günlük adım toplamını Health Connect'ten okur. Verilerin telefonundaki uygulama alanında saklanır. Otomatik bulut yedeklemesi kapalıdır.")
            Text("AI paylaşımını etkinleştirip günü kapattığında, profil notların ve kayıt geçmişin DeepSeek'e özet üretmek için gönderilir. Paylaşımı Ayarlar'dan kapatabilirsin. Hevy yalnızca eşitleme istediğinde API anahtarınla okunur.")
            Text("Öğün eklerken AI ile hazırla seçeneğini kullanırsan yalnızca yazdığın yemek açıklaması ve öğün/saat seçimin DeepSeek'e gönderilir. Bu işlem günlük değerlendirme ayarından bağımsızdır. AI besin değerleri tahmini olabilir; kaydetmeden önce düzenleyebilirsin.")
            Text("API anahtarların Android Keystore ile şifrelenir, yedeklere eklenmez. JSON ve Markdown dışa aktarımları kişisel kayıtlarını içerir; saklayacağın yeri sen seçersin.")
            Text("Uygulamayı kaldırmak telefondaki kayıtları siler. Kalıcı koruma için düzenli olarak JSON yedeğini başka bir konuma kaydet.")
            Button(onClick = { finish() }) { Text("Anladım") }
        } }
    } } }
}
