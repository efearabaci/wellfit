# Doğrulama · 1.2.0 · 19 Ekim 2026

Son kaynaklardan `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug` ve `assembleRelease` başarıyla tamamlandı.

- 47 JVM birim testi: 0 hata. Takvim sınırları, geçmiş hedefler, eksik/sıfır ölçümler, tek tartım, aynı gün birden fazla antrenman, Hevy sayfalama/hesap doğrulaması, tekrarsız aktarım ve eski yedek uyumluluğu dahil. Yeni öğün testleri: isteğe bağlı makroların kayıpsız saklanması, eksik makroların toplam/ortalamalara sıfır olarak katılmaması, kaynak metnin hafızaya taşınması, AI JSON sözleşmesi ve hatalı/yarım yanıtın reddedilmesi.
- Android 17 / API 37 önizleme emülatöründe 10 cihaz testi: başarılı. Günlük/kalıcılık, istatistik/Hevy ve AI öğün akışları tek pakette doğrudan AndroidJUnitRunner ile doğrulandı; üretilen test günlükleri repoya eklenmez.
- Compose akışı: öğün kaydı → öğünü listede görme → kullanıcı onayıyla günü kapatma → tam hafıza kopyasının saklanması.
- Kalıcılık: veritabanını kapatıp tekrar açma, 25 eşzamanlı öğünü kaybetmeden yazma, Keystore şifreleme, yeniden açılan güne eski AI yanıtının uygulanmaması ve beklenmedik boş veritabanında kurtarma kopyasının korunması.
- İstatistik akışı: hafta/ay gezinmesi, dört bölümün açılması, antrenman takviminden günlük kaydına geçiş; boş Hevy anahtarıyla doğrulamanın başlatılamaması.
- Öğün akışı: karbonhidrat/yağ girmeden kayıt ve günü kapatma; serbest metin taslağının Activity yeniden oluşturulmasında korunması; önizlemede düzenleme sırasında günlüğün değişmemesi ve son kaydetme düğmesiyle iki öğünün birlikte kalıcılaşması. AI önizleme testi ücretli API çağrısı yerine doğrulanan model yanıtı biçiminde geçici sonuç kullanır.
- Lint: 0 hata; 20 sürüm/biçim uyarısı. Bağımlılıklar sabit sürümlerdedir; Android 36 hedeflenir.
- Teslim APK'sı: R8 ile küçültülmüş, geliştirme anahtarıyla imzalanmış, Android APK Signature Scheme v2 doğrulaması başarılı. Yerel APK için SHA-256 `dist/SHA256SUMS` içinde; derleme çıktıları repoya eklenmez.

Canlı DeepSeek ve Hevy hesaplarına erişim olmadığı için gerçek hesaplarla test yapılmadı. HTTP sözleşmeleri MockWebServer ile doğrulandı. Gerçek Huawei telefonundan Health Connect aktarımı ve izinleri cihaz üzerinde ayrıca kontrol edilmeli; manuel adım girişi bu bağlantıdan bağımsızdır.

AI öğün servisi testleri yalnızca yemek metni ve varsayılan öğün/saatin gönderildiğini, isteklerin JSON çıktı istediğini, eksik bilgiler için netleştirme yanıtının kayıt üretmediğini ve HTTP hata metninin anahtar/kişisel içerik sızdırmadığını doğrular. Gerçek modelin besin tahmini doğruluğu otomatik testlerle garanti edilmez; kullanıcı önizlemeden değerleri düzenleyebilir.

Teslim edilen küçültülmüş 1.2.0 APK ayrıca emülatöre kuruldu; soğuk açılış ve günlük ekranı doğrulandı. Ekran görüntüleri `screenshots/` klasöründedir. İlk beş görüntü 1.0.0 ilk kurulumundandır. `statistics-preview/` altındaki üç görüntü 1.1.0 cihaz testlerinden alınmıştır; istatistik örneği aynı gün iki antrenman içeren geçici test verisini gösterir. `meal-preview/09-ai-meal-review.png` 1.2.0 testindeki geçici AI önizlemesidir. Bu veriler uygulamaya gömülmez; test bitince önceki kayıtlar geri yüklenir. Hevy görüntüsünde anahtar alanı boştur.
