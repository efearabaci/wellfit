# Doğrulama · 19 Ekim 2026

Son kaynaklardan `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug` ve `assembleRelease` başarıyla tamamlandı.

- 12 JVM birim testi: 0 hata.
- Android 17 / API 37 önizleme emülatöründe 5 cihaz testi: başarılı. Son test paketi doğrudan AndroidJUnitRunner ile çalıştırıldı; çıktı yerel `device-test-results.txt` dosyasında; üretilen test günlükleri repoya eklenmez.
- Compose akışı: öğün kaydı → öğünü listede görme → kullanıcı onayıyla günü kapatma → tam hafıza kopyasının saklanması.
- Kalıcılık: veritabanını kapatıp tekrar açma, 25 eşzamanlı öğünü kaybetmeden yazma, Keystore şifreleme, yeniden açılan güne eski AI yanıtının uygulanmaması ve beklenmedik boş veritabanında kurtarma kopyasının korunması.
- Lint: 0 hata; 20 sürüm/biçim uyarısı. Bağımlılıklar sabit sürümlerdedir; Android 36 hedeflenir.
- Teslim APK'sı: R8 ile küçültülmüş, geliştirme anahtarıyla imzalanmış, Android APK Signature Scheme v2 doğrulaması başarılı. Yerel APK için SHA-256 `dist/SHA256SUMS` içinde; derleme çıktıları repoya eklenmez.

Canlı DeepSeek ve Hevy hesaplarına erişim olmadığı için gerçek hesaplarla test yapılmadı. HTTP sözleşmeleri MockWebServer ile doğrulandı. Gerçek Huawei telefonundan Health Connect aktarımı ve izinleri cihaz üzerinde ayrıca kontrol edilmeli; manuel adım girişi bu bağlantıdan bağımsızdır.

Teslim edilen küçültülmüş APK ayrıca emülatöre kuruldu. Soğuk açılış ve ilk kurulum tamamlandı; günlük ekranı açıldı. Ekran görüntüleri `screenshots/` klasöründedir. Görüntüler ilk kurulumdaki boş kayıt durumunu gösterir; uygulamaya örnek kişisel veri gömülmemiştir.
