# Denge

Kotlin + Jetpack Compose ile geliştirilmiş, Türkçe ve önce çevrimdışı çalışan kişisel beslenme ve hareket günlüğü. Android 9 (API 28) ve üzeri.

## Kurulum

Yerel kurulum paketi: `dist/Denge-1.1.0.apk`. Derleme çıktıları Git reposuna eklenmez. Paketi `./gradlew assembleRelease` ile üretip `app/build/outputs/apk/release/app-release.apk` konumundan telefona aktararak yükleyebilirsin. Bu paket R8 ile küçültülmüş ve geliştirme anahtarıyla imzalanmış kişisel kurulum sürümüdür; mağaza dağıtımı için özel imzalama anahtarı gerekir.

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
6. **Gidişat** ekranında gerçek takvim haftaları ve aylar arasında gezin. Genel, Beslenme, Hareket ve Kilo bölümlerinden istatistiklerini incele. Takvimde bir güne dokunarak o günün kayıtlarını aç. **Hafıza** ekranında AI değerlendirmelerini gör.
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

Hevy'nin resmi API'si **Hevy Pro** ve kişisel API anahtarı gerektirir. **Ayarlar → Hevy → Hevy API anahtarını al** bağlantısını aç; anahtarı uygulamadaki alana gir ve **Bağlan ve doğrula** düğmesine dokun. Hesaba erişim `GET /v1/workouts/count` ile doğrulanır; başarılı olmadan yeni Hevy anahtarı kaydedilmez. Anahtar, sohbete veya Git reposuna yazılmaz.

**Antrenman geçmişini içe aktar** ile bütün Hevy geçmişini tek seferde aktar. İlerleme sayfa sayfa gösterilir. İstek başındaki ve sonundaki hesap sayısı, alınan benzersiz antrenman sayısıyla eşleşmeden tek bir günlük kayıt bile değiştirilmez. Tekrarlanan aktarım antrenmanları çoğaltmaz; açık günler güncellenir, manuel öğün/not/antrenmanlar korunur. Kapalı günlere ait kayıtlar ve AI kopyaları değiştirilmez; kaç günün korunduğu gösterilir. Kapalı bir günü güncellemek için önce yeniden aç. Hevy'de hiç kayıt bulunmayan tarihler otomatik dinlenme olarak işaretlenmez. Kaynaktan silinmiş geçmiş kayıtlar başka günlerden otomatik temizlenmez; bu işlem tam bir silme aynalaması değildir.

Sonraki antrenmanlarında Bugün ekranındaki **Hevy'den al** düğmesini de kullanabilirsin. Sayfalanmış geçmişin tüm sayfaları okunur; seçili günün kaydedilmiş saat dilimine göre o gün başlayan antrenmanlar alınır. Başlık, süre, hareketler, setler, tekrarlar, ağırlıklar, mesafeler, süreler ve kaynak JSON saklanır. Ham API verisi AI hafızasına dahil edilir. Aynı kaynak kimliğiyle eşitleme tekrarlandığında kayıt çoğalmaz; yerel manuel kayıtların korunur. Bir sayfa başarısız olursa kısmi veri kaydedilmez. Güvenlik sınırı 500 sayfadır; sınır aşılırsa kayıtlar kesilerek aktarılmaz.

## İstatistikler · 1.1.0

- **Gerçek hafta / ay seçimi:** Pazartesi başlayan haftalar, ayın tamamını gösteren takvim, önceki/sonraki dönem gezinmesi; gelecek dönemlere geçiş yok.
- **Antrenman takvimi:** antrenman, açıkça bildirilen dinlenme, bilinmeyen ve gelecek günler ayrı; tarih seçimi günlük kaydını açar. Bir günde iki seans, bir antrenman günü ve iki seans sayılır.
- **Genel:** antrenman gün ve seans sayısı, toplam süre, kalori/adım ortalamaları, kayıt devamlılığı, kişisel hedeflere ulaşma ve önceki dönemle ortalama farkları. Karşılaştırmaların kayıt kapsamı ayrıca gösterilir.
- **Beslenme:** enerji toplamı/ortalaması, günlük enerji grafiği, protein/karbonhidrat/yağ ortalamaları, öğün sayısı ve öğün türü dağılımı. Kalori hedefine yakınlık o günkü hedefin ±%10 aralığıyla açıkça tanımlanır.
- **Hareket:** antrenman günü/seansı/süresi, ortalama seans, haftanın hangi günlerinde spor yaptığın, dinlenme ve bilinmeyen gün sayısı, toplam/ortalama/en yüksek adım ve günlük adım grafiği. Kas veya egzersiz hacmi analizi eklenmez.
- **Kilo:** ilk-son ölçüm farkı, dönem ortalaması, min/maks, hedef kilo, tarih aralıklarını koruyan ölçüm grafiği ve haftalık tartı ortalamaları. Tek tartımdan kilo değişimi üretilmez.
- **Doğru paydalar:** beslenme ortalamaları kapalı günlerden, adım ortalamaları adım girilmiş günlerden hesaplanır. Bilinmeyen günler sıfır sayılmaz; girilmiş `0` ise gerçek bir ölçümdür. Geçmiş günlerin kendi hedefleri kullanılır. Hedefi 0 olan protein günleri başarı paydasına katılmaz.

Hevy bağlantı/aktarım zamanları kalıcıdır; gerçek bağlantı durumu anahtarın varlığı ve son doğrulama zamanıyla birlikte gösterilir. Yeni alanlar varsayılan değerlere sahiptir; 1.0.0 kayıt ve yedekleri okunmaya devam eder.

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

1.1.0 cihaz testlerinden: [Hareket istatistikleri](screenshots/statistics-preview/06-statistics-activity.png), [Antrenman takvimi](screenshots/statistics-preview/07-training-calendar.png), [Hevy bağlantısı](screenshots/statistics-preview/08-hevy-connection.png). İstatistik görüntülerindeki iki antrenman geçici test verisidir; uygulamaya örnek veri gömülmez.

İlk sürümün boş kayıt akışı: [Bugün](screenshots/02-today.png), [Hafıza](screenshots/04-memory.png), [Ayarlar](screenshots/05-settings.png).

[Doğrulama ayrıntıları](docs/VERIFICATION.md) test kapsamını ve cihaz doğrulamasını içerir. Derleme çıktıları, günlükler, yerel SDK yolu, anahtarlar ve kişisel JSON/Markdown yedekleri Git dışında tutulur. Gradle dağıtımı resmi SHA-256 değeriyle doğrulanır.
