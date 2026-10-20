# Wellfit · Gidişat tasarımı

20 Ekim 2026

Gidişat tek bir kaydırılabilir sayfadır. Hafta/ay seçimi sağ üstteki tarih simgesinden açılan görünüm menüsündedir. Tarih aralığı önceki/sonraki oklarla değiştirilir; gelecek dönemlere geçilmez. Geçmişten mevcut hafta veya aya tek eylemle dönülür.

## Bölümler

| Bölüm | Zemin | Birlikte gösterilen veriler |
| --- | --- | --- |
| Antrenman | Mavi-gri `#E5EBF0` | Gün, seans, süre ve antrenman/dinlenme takvimi |
| Adımlar | Adaçayı `#E5ECE6` | Günlük ortalama, günlük grafik, hedefe ulaşılan günler ve toplam |
| Beslenme | Sıcak taş `#F1EBE1` | Günlük kalori, makrolar, kişisel hedefler ve öğün dağılımı |
| Kilo | Açık mor `#EBE7F0` | Son ölçüm, ilk-son değişim, gerçek tarihlerle grafik ve hedef kilo |
| Karşılaştırma | Beyaz | Önceki ve seçili dönemin ortalamaları, her iki ortalamanın kayıt sayısı ve fark |

Seanslar, öğün dağılımı, ölçüm listesi ve günlük kayıt listesi ihtiyaç halinde genişletilir. Dört görünüm düğmesi, bağımsız istatistik kartları, kayıt serisi ve gün kapatma sayıları kaldırılmıştır. Varsayılan görünümde açıklama paragrafları bulunmaz. Metinler bölüm/ölçüm adı, tarih, birim veya gerçek kayıt bilgisidir.

Verisi olmayan bir bölüm oluşturulmaz. Bütün dönem boşsa tek boş durum ve bugünün kaydını açma eylemi gösterilir. Karşılaştırma yalnızca aynı ölçümün iki dönemde de hesaplanabildiği durumda görünür. Eksik makro ortalamaları, tek tartımdan değişim ve bilinmeyen ölçümlerden sıfır üretilmez. Tek günlük ölçüm veya tek tartım varsa boş grafik yerine yalnızca gerçek kayıt gösterilir. Kilo ortalamaları ve farkları iki ondalığa kadar gösterilir. Açık günlerin kalori değerleri grafikte görülebilir; beslenme ortalamaları mevcut kurala göre tamamlanan günlerden hesaplanır. Ortalama hesabına katılan gün sayısı değerle birlikte gösterilir.

Günlük çubuklara dokunmak tarihi seçer; seçili satır o günün kaydını açar. Kilo grafiğinde en yakın gerçek ölçüm seçilir. Grafikler erişilebilirlik menüsünden önceki/sonraki gün veya ölçüm eylemlerini destekler. Eksen genişliği yazı ölçeğiyle büyür; normal ve %130 yazı boyutunda doğrulanmıştır. Takvim günleri doğrudan günlük kaydına gider.

Gidişat gezinme simgesi ile dönem, antrenman, beslenme, kilo eğrisi, karşılaştırma ve yön simgeleri ortak `WellfitIcons` ailesine eklendi. Çizgi kalınlığı ve ölçek Bugün sayfasıyla tutarlıdır. Sayfa kenarı 16 dp, bölüm arası 20 dp, bölüm içi boşluk 18 dp; bölüm köşeleri 20 dp'dir.
