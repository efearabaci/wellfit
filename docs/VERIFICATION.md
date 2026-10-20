# Doğrulama · 1.2.0 · 19 Ekim 2026


## Hafıza tasarım güncellemesi · 20 Ekim 2026

- Gün/hafta/ay değerlendirmeleri ve sonraki gün önerileri ayrı renk alanlarına alındı. Tarih çipleri yerine değerlendirme geçmişi menüsü, üstte dışa aktarma eylemi, ayrı kayıt/arşiv/not bölümleri ve yeni simgeler kullanılıyor. Tanıtım açıklaması ve genel sayaç kartı kaldırıldı.
- AI metni görünüm için bölümlenir; saklanan değerlendirme ve gönderim kopyası değiştirilmez. Markdown başlıkları, kalın metin, paragraflar, numaralı/maddeli öneriler ve başlıksız eski çıktılar okunur. Eksik ölçümlerden sahte sıfır üretilmez.
- Bağlantı/izin, sırada, hazırlanıyor, başarısız, eksik değerlendirme ve tamamlanmamış gün durumlarının ayrı eylemleri var. Hazır kayıtlar bağlantı/izin kapalı olsa da okunur. Önceki değerlendirmeler açılabilir; günün kaydına ve profil notları için Ayarlar'a dönülebilir.
- `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug` başarılı. 63 JVM testi: 0 hata; 7 yeni metin biçimleme testi dahil. Lint: 0 hata, mevcut 20 uyarı.
- API 37 test emülatöründe 12 cihaz testi başarılı: Hafıza ekranı/geçmiş/arşiv/boş durum/gönderim kopyası/ayarlar, bağlantı ve yeniden deneme düğmeleri, günlük öğün/kapatma akışı ve depo kalıcılığı. Örnek AI yanıtları geçici test verisidir; testler canlı DeepSeek isteği başlatmaz. Test kayıtları sonrasında geri alınır.
- Son okuma, arşiv, tarih seçimi ve Activity yeniden oluşturma akışı normal ve %130 yazı boyutunda ayrıca doğrulandı. Cihazın yazı ölçeği eski değerine döndürüldü.
- `assembleRelease` başarılı. Kurulum paketi `dist/Wellfit-1.2.1.apk`; APK v2 imzası doğrulandı, SHA-256 kaydı `dist/SHA256SUMS` içinde.
- Önizlemeler `screenshots/memory-redesign/` ve `large-type/` altındadır. Tasarım notları `docs/WELLFIT_MEMORY_DESIGN.md` içinde.


## Gidişat tasarım güncellemesi · 20 Ekim 2026

- Hafta/ay seçimi görünüm menüsüne alındı. Genel/beslenme/hareket/kilo sekmeleri yerine tek sayfada antrenman, adım, beslenme, kilo ve koşullu karşılaştırma bölümleri kullanılıyor.
- Açıklama paragrafları, kayıt devamlılığı ve gün kapatma sayıları arayüzden kaldırıldı. Verisi olmayan bölümler gizleniyor; iki dönemde de ölçüm yoksa karşılaştırma oluşturulmuyor. Tek ölçümde boş grafik veya değişim gösterilmiyor.
- Grafikler tarih seçimini ve günlük kaydına geçişi destekliyor; kilo grafiği gerçek tarih aralıklarını koruyor. Antrenman gün/seans/süre değerleri takvimle birlikte gösteriliyor. Yeni dönem, antrenman, beslenme, kilo ve karşılaştırma simgeleri eklendi.
- `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug` başarılı. 54 birim testi: 0 hata. Lint: 0 hata, mevcut 20 uyarı.
- API 37 emülatöründe son 8 arayüz testi başarılı: tüm bölümler, hafta/ay gezinmesi, takvimden günlük kayda geçiş, boş dönem, eksik makrolar, tek tartım, gerçek sıfır adım, geçerli karşılaştırmalar ve günlük öğün/kapatma akışı. Testler önceki kayıtları geri yüklüyor.
- Gidişat akışı normal ve %130 yazı boyutunda ayrıca doğrulandı. Grafik ekseni genişliği yazı ölçeğiyle büyür; eksen sayılarının tek satıra sığması cihaz testinde kontrol ediliyor. Yazı ölçeği eski değerine geri alındı.
- `assembleRelease` başarılı. Güncel küçültülmüş paket `dist/Wellfit-1.2.0.apk`; APK v2 imzası doğrulandı, SHA-256 kaydı `dist/SHA256SUMS` içinde.
- Güncel görseller `screenshots/progress-redesign/` altında; tamamı geçici test kayıtlarıyla üretildi. Tasarım notları `docs/WELLFIT_PROGRESS_DESIGN.md` içinde.


## Bugün tasarım güncellemesi · 20 Ekim 2026

- Tarih, kalori, makrolar, adım ve kilo tek bir adaçayı özet alanına alındı. Öğünler sıcak taş, antrenmanlar mavi-gri zeminle ayrıldı; bölüm araları, iç boşluklar, yazı hiyerarşisi ve eylemler yenilendi.
- Tüm ekranların ikon kullanımları yerel `WellfitIcons` vektör ailesine taşındı; eski ikon bağımlılığı kaldırıldı. Marka ve başlatıcı simgesi Wellfit W olarak güncellendi. Karşılama ve dolgu metinleri kaldırıldı.
- `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` ve `lintDebug` başarılı. 54 birim testi: 0 hata. Lint: 0 hata, mevcut 20 uyarı.
- API 37 emülatöründe 8 arayüz testi geçti: yeni Bugün bölümleri, bilinmeyen/sıfır ölçüm ayrımı, dinlenme durumu, kapalı günün düzenlemeleri engellemesi, öğün/gün kapatma/AI önizleme ve istatistik gezinmesi. Test verileri sonrasında eski kayıtlar geri yüklendi.
- Görsel önizlemeler `screenshots/today-redesign/` içinde; örnek kayıtlar test verisidir. Tasarım ve ikon envanteri `docs/WELLFIT_TODAY_DESIGN.md` dosyasında.
- `assembleRelease` başarılı. Son küçültülmüş kurulum paketi `dist/Wellfit-1.2.0.apk`; SHA-256 kaydı `dist/SHA256SUMS` içinde.
- Ek %130 yazı boyutu doğrulaması, emülatör kapandıktan sonra tekrar açılamadığı için tamamlanamadı. Cihazın yazı ölçeği değiştirilmedi.


## Wellfit ad ve metin güncellemesi · 20 Ekim 2026

- Kullanıcıya görünen uygulama adı, AI asistan kimliği, Markdown başlığı ve dışa aktarma dosya adları Wellfit olarak güncellendi. Motivasyon sloganları kaldırıldı; günlük değerlendirme talimatı somut kayıtlara odaklanacak şekilde güncellendi.
- Mevcut kurulumun kayıtlarını ve anahtarlarını korumak için Android paket kimliği, veritabanı adı, Keystore kimliği ve eski yedek biçimi korundu.
- `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` ve `lintDebug` başarılı. 54 birim testi: 0 hata.
- Android 17 / API 37 emülatöründe `DailyJourneyTest` ve `StatisticsJourneyTest`: 4 test başarılı. Öğün kaydı, gün kapatma, hafıza kopyası, istatistik gezinmesi, takvimden güne geçiş ve Hevy anahtar girişi akışları doğrulandı.
- Görsel tasarımdan bağımsız ürün ve ekran açıklamaları `docs/WELLFIT_DESIGN_PROMPT.md` dosyasında.


## Önceki sürüm doğrulaması

Son kaynaklardan `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug` ve `assembleRelease` başarıyla tamamlandı.

- 54 JVM birim testi: 0 hata. Takvim sınırları, geçmiş hedefler, eksik/sıfır ölçümler, tek tartım, aynı gün birden fazla antrenman, Hevy sayfalama/hesap doğrulaması, tekrarsız aktarım ve eski yedek uyumluluğu dahil. Yeni öğün testleri: isteğe bağlı makroların kayıpsız saklanması, eksik makroların toplam/ortalamalara sıfır olarak katılmaması, kaynak metnin hafızaya taşınması, AI JSON sözleşmesi ve hatalı/yarım yanıtın reddedilmesi. Saat maskesi testleri: `1220`→`12:20`, `930`→`9:30`, tek tek yazma, silme, yapıştırma ve geçersiz saatlerin (`24:00`, `12:60`) reddedilmesi.
- Arayüz: öğün ekleme sağ alttaki **Öğün ekle** düğmesiyle açılıyor; protein/karb./yağ girişleri eşit yükseklikte; Ayarlar alt sekmeden alınıp üst çubuğun soluna taşındı ve geri dönüşü var. Emülatörde öğün kaydı (`300 kcal`, `12:20`), öğün listesinde doğru saat, Ayarlar geçişi ve sekme geçişlerinde düğmenin görünürlüğü elle doğrulandı.
- Android 17 / API 37 önizleme emülatöründe 10 cihaz testi: başarılı. Günlük/kalıcılık, istatistik/Hevy ve AI öğün akışları tek pakette doğrudan AndroidJUnitRunner ile doğrulandı; üretilen test günlükleri repoya eklenmez.
- Compose akışı: öğün kaydı → öğünü listede görme → kullanıcı onayıyla günü kapatma → tam hafıza kopyasının saklanması.
- Kalıcılık: veritabanını kapatıp tekrar açma, 25 eşzamanlı öğünü kaybetmeden yazma, Keystore şifreleme, yeniden açılan güne eski AI yanıtının uygulanmaması ve beklenmedik boş veritabanında kurtarma kopyasının korunması.
- İstatistik akışı: hafta/ay gezinmesi, dört bölümün açılması, antrenman takviminden günlük kaydına geçiş; boş Hevy anahtarıyla doğrulamanın başlatılamaması. Ayarlar ekranı artık üst çubuktaki dişli simgesinden açılıyor.
- Öğün akışı: **Öğün ekle** düğmesinden sonra karbonhidrat/yağ girmeden kayıt ve günü kapatma; serbest metin taslağının Activity yeniden oluşturulmasında korunması; önizlemede düzenleme sırasında günlüğün değişmemesi ve son kaydetme düğmesiyle iki öğünün birlikte kalıcılaşması. AI önizleme testi ücretli API çağrısı yerine doğrulanan model yanıtı biçiminde geçici sonuç kullanır.
- Lint: 0 hata; 20 sürüm/biçim uyarısı. Bağımlılıklar sabit sürümlerdedir; Android 36 hedeflenir.
- Teslim APK'sı: R8 ile küçültülmüş, geliştirme anahtarıyla imzalanmış, Android APK Signature Scheme v2 doğrulaması başarılı. Yerel APK için SHA-256 `dist/SHA256SUMS` içinde; derleme çıktıları repoya eklenmez.

Canlı DeepSeek ve Hevy hesaplarına erişim olmadığı için gerçek hesaplarla test yapılmadı. HTTP sözleşmeleri MockWebServer ile doğrulandı. Gerçek Huawei telefonundan Health Connect aktarımı ve izinleri cihaz üzerinde ayrıca kontrol edilmeli; manuel adım girişi bu bağlantıdan bağımsızdır.

AI öğün servisi testleri yalnızca yemek metni ve varsayılan öğün/saatin gönderildiğini, isteklerin JSON çıktı istediğini, eksik bilgiler için netleştirme yanıtının kayıt üretmediğini ve HTTP hata metninin anahtar/kişisel içerik sızdırmadığını doğrular. Gerçek modelin besin tahmini doğruluğu otomatik testlerle garanti edilmez; kullanıcı önizlemeden değerleri düzenleyebilir.

Teslim edilen küçültülmüş 1.2.0 APK ayrıca emülatöre kuruldu; soğuk açılış ve günlük ekranı doğrulandı. Ekran görüntüleri `screenshots/` klasöründedir. İlk beş görüntü 1.0.0 ilk kurulumundandır. `statistics-preview/` altındaki üç görüntü 1.1.0 cihaz testlerinden alınmıştır; istatistik örneği aynı gün iki antrenman içeren geçici test verisini gösterir. `meal-preview/09-ai-meal-review.png` 1.2.0 testindeki geçici AI önizlemesidir. Bu veriler uygulamaya gömülmez; test bitince önceki kayıtlar geri yüklenir. Hevy görüntüsünde anahtar alanı boştur.
