# Wellfit · Bugün tasarımı

20 Ekim 2026

Bugün sayfası üç belirgin renk alanına ayrılır:

- **Gün özeti:** adaçayı zemin (`#E5ECE6`). Başlık ve tarih, kalori, makrolar, adım ve kilo aynı bölümün içindedir. Kalori ana göstergesi koyu yeşildir; kalan miktar tüketim hedefine göre hesaplanır.
- **Öğünler:** sıcak taş zemin (`#F1EBE1`). Saat sıralı kayıtlar beyaz satırlarda gösterilir. Her kayıtta kategori, saat, ad, kcal ve makrolar; AI kayıtlarında tahmin etiketi vardır.
- **Antrenmanlar:** mavi-gri zemin (`#E5EBF0`). Seans sayısı ve süre, antrenmanlar, Hevy aktarımı ve manuel kayıt eylemleri birlikte yer alır. Bilinmeyen durum ile dinlenme günü farklı metin ve sembollerle gösterilir.

Sayfa kenarı 16 dp, bölüm arası 20 dp, bölüm içi 16 dp; ana köşeler 20 dp, iç kartlar 12–14 dp. Gün notu ve günü kapatma eylemi ayrı tutulur. Öğün ekleme sabit eylemi korunur. Ayarlar sağ üstte, alt gezinme beyaz zemindedir.

## İkon envanteri

Önceki Material ikon kullanımları tüm ekranlarda tarandı ve `WellfitIcons.kt` içindeki tek aileye taşındı. 24 dp tuval, 1,7 dp çizgi, yuvarlatılmış uç ve birleşimler kullanılır. Vektörler kaynak kodundadır; ikon fontu, emoji veya uzak varlık gerekmez. Material Icons Extended bağımlılığı kaldırıldı.

| Alan | Semboller |
| --- | --- |
| Marka ve gezinme | Wellfit W, günlük/takvim, gidişat, hafıza, ayarlar, geri/ileri |
| Gün özeti | enerji, ayak izleri, tartı, düzenle, eşitle |
| Öğünler | çatal/bıçak, güneş, ay, fincan, AI, ekle |
| Antrenman | dambıl, dinlenme koltuğu, süre, ekle, eşitle |
| Not ve gün kapatma | not, düzenle, tamamla |
| Diğer ekranlar | bağlantı, dış bağlantı, indir, bulut, veri, kalkan, grafik, hafta, saat, trend, hedef, madalya, onay, kapat |

Boş motivasyon cümleleri yerine kayıt durumunu ve yapılabilecek işlemi açıklayan metinler kullanılır. Kullanıcıya görünen ad ve başlatıcı simgesi Wellfit'tir. Mevcut kurulumun verilerini, API anahtarlarını ve yedek uyumluluğunu korumak için Android paket kimliği ile kalıcı veri kimlikleri değiştirilmez.

Diğer sayfaların yerleşimi bu çalışmanın kapsamında yeniden tasarlanmadı; ortak ikonlar ve görünen metinler güncellendi.
