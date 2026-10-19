# Doğrulama · 1.1.0 · 19 Ekim 2026

Son kaynaklardan `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug` ve `assembleRelease` başarıyla tamamlandı.

- 36 JVM birim testi: 0 hata. Takvim sınırları, geçmiş hedefler, eksik/sıfır ölçümler, tek tartım, aynı gün birden fazla antrenman, Hevy sayfalama/hesap doğrulaması, tekrarsız aktarım ve eski yedek uyumluluğu dahil.
- Android 17 / API 37 önizleme emülatöründe 8 farklı cihaz testi: başarılı. Beş günlük/kalıcılık testi ve üç istatistik/Hevy arayüz testi doğrudan AndroidJUnitRunner ile doğrulandı; üretilen test günlükleri repoya eklenmez.
- Compose akışı: öğün kaydı → öğünü listede görme → kullanıcı onayıyla günü kapatma → tam hafıza kopyasının saklanması.
- Kalıcılık: veritabanını kapatıp tekrar açma, 25 eşzamanlı öğünü kaybetmeden yazma, Keystore şifreleme, yeniden açılan güne eski AI yanıtının uygulanmaması ve beklenmedik boş veritabanında kurtarma kopyasının korunması.
- İstatistik akışı: hafta/ay gezinmesi, dört bölümün açılması, antrenman takviminden günlük kaydına geçiş; boş Hevy anahtarıyla doğrulamanın başlatılamaması.
- Lint: 0 hata; 20 sürüm/biçim uyarısı. Bağımlılıklar sabit sürümlerdedir; Android 36 hedeflenir.
- Teslim APK'sı: R8 ile küçültülmüş, geliştirme anahtarıyla imzalanmış, Android APK Signature Scheme v2 doğrulaması başarılı. Yerel APK için SHA-256 `dist/SHA256SUMS` içinde; derleme çıktıları repoya eklenmez.

Canlı DeepSeek ve Hevy hesaplarına erişim olmadığı için gerçek hesaplarla test yapılmadı. HTTP sözleşmeleri MockWebServer ile doğrulandı. Gerçek Huawei telefonundan Health Connect aktarımı ve izinleri cihaz üzerinde ayrıca kontrol edilmeli; manuel adım girişi bu bağlantıdan bağımsızdır.

Teslim edilen küçültülmüş 1.1.0 APK ayrıca emülatöre kuruldu; soğuk açılış ve günlük ekranı doğrulandı. Ekran görüntüleri `screenshots/` klasöründedir. İlk beş görüntü 1.0.0 ilk kurulumundandır. `statistics-preview/` altındaki üç yeni görüntü 1.1.0 cihaz testlerinden alınmıştır; istatistik örneği aynı gün iki antrenman içeren geçici test verisini gösterir. Bu veriler uygulamaya gömülmez; test bitince önceki kayıtlar geri yüklenir. Hevy görüntüsünde anahtar alanı boştur.
