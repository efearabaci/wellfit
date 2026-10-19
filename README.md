# Denge

Kotlin + Jetpack Compose ile geliştirilmiş, Türkçe ve önce çevrimdışı çalışan kişisel beslenme ve hareket günlüğü. Android 9 (API 28) ve üzeri.

## Kurulum

Yerel kurulum paketi: `dist/Denge-1.0.0.apk`. Derleme çıktıları Git reposuna eklenmez. Paketi `./gradlew assembleRelease` ile üretip `app/build/outputs/apk/release/app-release.apk` konumundan telefona aktararak yükleyebilirsin. Bu paket R8 ile küçültülmüş ve geliştirme anahtarıyla imzalanmış kişisel kurulum sürümüdür; mağaza dağıtımı için özel imzalama anahtarı gerekir.

Android Studio ile bu klasörü aç, Gradle eşitlemesini tamamla ve **app** yapılandırmasını çalıştır. JDK 17 veya 21, Android SDK 36 gerekir. Bilgisayara özel `local.properties` dosyasını kendi SDK yolunla oluştur; dosya sürüm kontrolüne dahil değildir.

```sh
./gradlew assembleDebug assembleRelease
./gradlew testDebugUnitTest lintDebug
# Açık emülatör veya bağlı bir test telefonu ile:
./gradlew connectedDebugAndroidTest
```

## Kullanım

1. İlk açılışta adını ve kendi planındaki günlük kalori, makro ve adım hedeflerini gir. Başlangıç değerleri kişisel bir beslenme önerisi değildir.
2. **Bugün** ekranında her öğünü yedikten sonra adını, saatini, kalorisini ve makrolarını ekle. Kayıtlar hemen telefona yazılır. Öğüne dokunarak düzenleyebilir veya silebilirsin.
3. Adım/kilo kartına dokunup Huawei Sağlık'taki günlük toplamı ve istersen tartı kaydını gir. Boş adım alanı bilinmeyendir; `0` gerçek sıfır kaydıdır.
4. Antrenmanını kendin ekle veya **Hevy'den al** düğmesini kullan. Antrenmansız günlerde **Bugün dinleniyorum** seçeneği bulunur.
5. Son öğününden sonra **Günü kapat**. Gün kendiliğinden kapanmaz. Kapalı gün yeniden açılıp düzenlenebilir; eski AI sonucu arşivlenir.
6. **Gidişat** ekranında 7/30 günlük istatistikleri ve kilo grafiğini, **Hafıza** ekranında AI değerlendirmelerini gör.
7. **Ayarlar → JSON yedeği kaydet** ile düzenli dış yedek al. **Hafıza** ekranından okunabilir Markdown çıktısı da alabilirsin.

## Bağlantılar

### DeepSeek

**Ayarlar** bölümüne kendi DeepSeek API anahtarını gir ve AI paylaşımını açıp kaydet. Varsayılan model `deepseek-flash`; model adını değiştirebilirsin. Kullanım kendi API hesabından ücretlendirilir.

Günü kapatmak günlük kayıtlarını ve tam istek kopyasını **tek SQLite işlemiyle** saklar. WorkManager, ağ varken isteği gönderir. Ağ/429/5xx hatalarında artan bekleme süresiyle toplam beş girişime kadar tekrar dener. Uygulama veya işlem kapanınca iş kaybolmaz; uygulama açılışında bekleyen işler yeniden kontrol edilir. Yetki/bakiye hataları Ayarlar'dan düzeltilip Hafıza'da yeniden denenebilir. Anahtar olmadan da gün kapatılabilir; bağlantı sonradan etkinleştirildiğinde bekleyen özetler hazırlanır.

AI için veri paylaşımı isteğe bağlıdır. API anahtarları Android Keystore AES-GCM ile şifrelenir ve yedeklere dahil edilmez. Ağ günlüklerine anahtar veya kişisel veri yazılmaz. Çevrimdışı istatistikler AI özeti gibi gösterilmez.

### Huawei / Health Connect

Manuel giriş her zaman kullanılabilir. İsteğe bağlı **Health Connect adım okuma** entegrasyonu hazırdır: Ayarlar'dan izin ver ve otomatik güncellemeyi aç. İzin ve veri varsa uygulama açılışında, tarih değişiminde ve günü kapatmadan önce günlük toplam okunur. Manuel girdiğin toplam otomatik olarak değiştirilmez; Bugün ekranındaki açık eşitleme düğmesiyle kaynağı tekrar Health Connect'e geçirebilirsin. Kaynak ve son eşitleme zamanı saklanır.

**Huawei Sağlık'ın Health Connect'e yazdığı varsayılmaz.** Huawei verisi bu platforma aktarılmıyorsa, desteklenen bir veri aktarımı kurman veya günlük toplamı manuel girmen gerekir. Huawei Health Kit'e doğrudan bağlantı bu projede yoktur; Huawei geliştirici hesabı, uygulama kimliği ve veri erişim yetkilendirmesi olmadan böyle bir bağlantı çalışır gösterilmez.

Health Connect adım verileri Android'in `aggregate` API'siyle okunur; farklı kaynaklardan gelen kayıtlar uygulama içinde tekrar toplanmaz. Android 13 ve altında Health Connect uygulaması ayrıca kurulmalıdır. Health Connect'siz/GMS'siz cihazlarda manuel akış devam eder. Arka planda sürekli sensör takibi yapılmaz.

### Hevy

Hevy'nin resmi API'si **Hevy Pro** ve kişisel API anahtarı gerektirir. Anahtarı [Hevy geliştirici ayarlarından](https://hevy.com/settings?developer) alıp uygulamaya kaydet.

Antrenman yaptığında **Hevy'den al** düğmesine dokun. Sayfalanmış geçmişin tüm sayfaları okunur; seçili günün kaydedilmiş saat dilimine göre o gün başlayan antrenmanlar alınır. Başlık, süre, hareketler, setler, tekrarlar, ağırlıklar, mesafeler, süreler ve kaynak JSON saklanır. Ham API verisi AI hafızasına dahil edilir. Aynı kaynak kimliğiyle eşitleme tekrarlandığında kayıt çoğalmaz; yerel manuel kayıtların korunur. Bir sayfa başarısız olursa kısmi veri kaydedilmez. Güvenlik sınırı 500 sayfadır; sınır aşılırsa kayıtlar kesilerek aktarılmaz.

## Hafıza ve veri koruması

- Room / SQLite WAL ile sürümlenmiş bir günlük belgesi tutulur. Değişiklikler mutex ve veritabanı transaction'ı içinde uygulanır; eşzamanlı kayıtlar birbirini ezmez. Destructive migration yoktur; ilk şema `app/schemas/` altında bulunur.
- Her başarılı değişiklikten sonra uygulamanın özel alanına atomik bir JSON kurtarma kopyası yazılır. Bu, telefon kaybına karşı dış yedek değildir.
- AI bağlamı: profil/hedefler, kalıcı notlar, o tarihe kadarki **tüm kayıtlı günlerin** öğünleri, makroları, adım kaynağı, kiloları, notları, antrenmanları ve ham Hevy setleri; önceki AI çıktıları; hesaplanmış haftalık/aylık özetler. Gelecek tarihler dahil edilmez. Bilinmeyen ve açık günler eksiksiz gün/sıfır sayılmaz.
- Önceki istek metinleri yeni istek içine tekrar gömülmez; kaynak kayıtlar ve önceki sonuçlar taşınır. Gönderilen her tam istek ayrıca kayıpsız GZIP ile saklanır ve Hafıza'dan incelenebilir.
- Veri sessizce kesilmez. 600.000 karakterlik koruyucu bağlam sınırı aşılırsa gün yine kaydedilir, AI isteği gönderilmez ve açıklama gösterilir. Modelin kendi daha düşük bağlam sınırı varsa servis hatası gösterilir. Çok büyük geçmiş için gelecekte model sınırlarına uygun arşiv erişimi gerekir.
- Yeniden açılan güne ait eski bir ağ isteği sonradan dönse bile yeni kayda uygulanmaz; sonuç benzersiz istek kimliğiyle eşleştirilir.
- JSON yedeği SHA-256 bütünlük kontrolü ve şema doğrulaması içerir. İçe aktarımda aynı tarihli yerel kayıtlar korunur; yalnızca eksik günler eklenir. Kurtarma ekranı bozuk günlük belgesinin kopyasını da korur.
- Yedek ve Markdown dosyaları **şifresiz kişisel veri** içerir. Kaydetme konumunu Android dosya seçicisiyle sen seçersin. Otomatik Android bulut/cihaz taşıma yedeği kapalıdır; anahtarlar taşınmaz. Uygulamayı kaldırmak veya telefonunu kaybetmek yerel kayıtları kaybettirir: dış JSON yedeği gerekir.

## Teknik yapı

`domain/`: doğrulanan veri modelleri, tam bağlam üretimi, kayıpsız istek sıkıştırma.

`data/`: Room, atomik günlük işlemleri, Keystore anahtar kasası, doğrulanan dışa/içe aktarım.

`integration/`: DeepSeek, Hevy, Health Connect ve kalıcı WorkManager işleri.

`ui/`: Compose Material 3, tek yönlü state akışı, günlük ekranı, grafikler, hafıza ve ayarlar. Sıcak beyaz/orman yeşili palet, sade kartlar, açık boş/yükleniyor/hata durumları. Formlar ekran dönüşünde korunur; API anahtarı taslakları instance state'e yazılmaz.

## Doğrulama sınırları

Otomatik testler ham geçmişin ve Hevy detaylarının bağlamda korunmasını, eksik gün hesaplarını, tam/yarım DeepSeek yanıtlarını, HTTP hata gizliliğini, yedek bütünlüğünü, Keystore saklamasını, veritabanı yeniden açılışını, eşzamanlı kayıtları ve Compose günlük akışını kapsar. Canlı DeepSeek/Hevy hesapların ve gerçek Huawei telefonun olmadığı için bu hesaplarla uçtan uca bağlantı doğrulanamaz; ağ sözleşmeleri yerel test sunucusuyla kontrol edilir.

## Resmi kaynaklar

- [DeepSeek API](https://api-docs.deepseek.com/)
- [Hevy Public API](https://api.hevyapp.com/docs/)
- [Health Connect başlangıç rehberi](https://developer.android.com/health-and-fitness/health-connect/get-started)
- [Health Connect veri okuma](https://developer.android.com/health-and-fitness/health-connect/read-data)
- [Huawei Health Kit](https://developer.huawei.com/consumer/en/hms/huaweihealth/)

## Ekran görüntüleri ve test kaydı

Son kurulum paketinden alınan görüntüler: [Bugün](screenshots/02-today.png), [Gidişat](screenshots/03-progress.png), [Hafıza](screenshots/04-memory.png), [Ayarlar](screenshots/05-settings.png).

[Doğrulama ayrıntıları](docs/VERIFICATION.md): 12 birim testi ve 5 Android cihaz testi başarılı; lint 0 hata.
