# Wellfit · Hafıza tasarımı

20 Ekim 2026

Hafıza bir değerlendirme okuma ekranıdır. Üstte yalnızca başlık ve dışa aktarma eylemi bulunur. Tarih alanındaki oklar kayıtlı değerlendirmeler arasında geçer; tarihe dokunmak durumlarıyla birlikte geçmiş listesini açar. Bugün de listede yer alır. Seçilen tarih diğer günlük akışlarıyla korunur.

## Bölümler

| İçerik | Zemin | Gösterim |
| --- | --- | --- |
| Tarih | Adaçayı | Seçili tarih, gün; hazır değerlendirmede AI/model/zaman bilgisi |
| Gün değerlendirmesi | Adaçayı | Paragraflar ve vurgulanan gerçek değerler |
| Bu hafta | Mavi-gri | Haftalık değerlendirme |
| Bu ay | Sıcak taş | Aylık değerlendirme |
| Sonraki gün | Açık mor | Numaralı veya maddeli öneriler |
| Günün kayıtları | Mavi-gri | Gerçek öğün, kalori, adım, kilo ve antrenman verileri; açılabilir gün notu |
| Önceki değerlendirmeler | Açık mor | Oluşturulma zamanıyla açılan geçmiş metinler |
| AI için notların | Sıcak taş | Kullanıcının kendi profil notları; uzun metinde tümünü okuma ve Ayarlar'dan düzenleme |

Kayıt kapsamını sayan tanıtım kartı, yatay tarih çipleri, genel açıklama paragrafları ve eksik ölçümlerin yer tutucu istatistikleri kaldırıldı. Günün kayıt başlığı ilgili günlük sayfasına döner. Eksik ölçümler sıfır yapılmaz; gerçek sıfır korunur. Kayıt ve kişisel not bölümleri yalnızca içerik olduğunda görünür.

## Durumlar ve eylemler

- Tamamlanmamış gün: günlük kaydına dönme; varsa son hazır değerlendirmeyi açma.
- AI kapalı veya DeepSeek anahtarı eksik: doğrudan AI ayarlarına geçme.
- Sırada: kısa durum başlığı. Bağlantı bekleniyorsa yeniden deneme.
- Hazırlanıyor: ilerleme göstergesi; yinelenen istek düğmesi gösterilmez.
- Başarısız: gerçek hata ve yeniden deneme; işlem sürerken düğme devre dışıdır.
- Hazır: saklanan değerlendirme, AI/model/zaman bilgisi; bağlantı ve paylaşım kapalı olsa da okunur.
- Eski bir kayıtta değerlendirme nesnesi yoksa geçersiz yeniden deneme sunulmaz.

Gönderilen veriyi inceleme ve tüm hafızayı Markdown olarak dışa aktarma korunmuştur. Gönderim incelemesi, yalnızca saklanmış istek kopyası olduğunda görünür. Eski boş isteklerin yeniden hazırlanması için mevcut depo/worker akışı korunur.

AI metni yalnızca görünüm için bölümlenir; saklanan metin, istek kopyası ve dışa aktarma içeriği değiştirilmez. Standart başlıklar, Markdown başlıkları, satır içi kalın metin, numaralı/maddeli listeler ve başlıksız eski metinler desteklenir. Tanınmayan başlıklar ve metinler korunur.

Sayfa kenarı 16 dp, bölüm arası 20 dp, bölüm içi 16 dp; ana köşeler 20 dp, iç satırlar 12 dp. Hafıza, değerlendirme, arşiv, not, uyarı ve dışa aktarma simgeleri ortak 24 dp vektör ailesindedir.
