# HaneCüzdan

**HaneCüzdan**, kişisel ve aile bütçesini tek yerden yönetmek için geliştirilmiş modern bir Android finans uygulamasıdır.

- **Geliştirici:** ByTuna
- **Sürüm:** v1.1.0
- **Platform:** Android (Min SDK 26 / Target SDK 35)

---

## Özellikler

### Genel Bakış & Analiz
- **Ana Sayfa:** Dönemsel gelir, gider ve bakiye özeti
- **Aylık Harcama & Gelecek Ödemeler:** Harcama takibi ve yaklaşan ödeme planları
- **Akıllı Danışman:** Harcama hızı ve tasarruf önerileri

### Hane & Planlama
- **Hane & Kişisel:** Bulut senkronizasyonlu **Aile Havuzu** ve cihaz içi **Kişisel Profil** arasında tek ekrandan geçiş
- **Masraf Bölüş:** Ortak harcamaların paylaşımı
- **Bütçe Limitleri & Hedefler:** Kategori bazlı bütçe sınırları ve tasarruf hedefleri

### Taahhüt & Arşiv
- **Abonelikler:** Aylık abonelik takibi ve fatura günü hatırlatmaları
- **Krediler & Taksitler:** Kredi ve taksit planı yönetimi
- **Fiş Arşivi:** ML Kit OCR destekli fiş tarama ve arşivleme

### Yönetim, Güvenlik & Arayüz
- **Kavisli Alt Menü:** Tüm sekmelerde aktif, animasyonlu dalgalı oyuk ve yüzen ikon balonuna sahip alt navigasyon çubuğu
- **Gruplandırılmış Yan Menü:** Mantıksal kategorilere ayrılmış sade gezinme menüsü
- **Ayarlar & Güvenlik:** Açılır/kapanır kartlar halinde Görünüm Teması (Sistem/Açık/Koyu), Profil & Aile Havuzu, Güvenlik & Uygulama Kilidi (Parola, Gizli Soru, Biyometrik Giriş) ve CSV / PDF / JSON dışa aktarma & yedekleme

---

## Teknoloji Yığını

- **Dil & Arayüz:** Kotlin, Jetpack Compose, Material 3
- **Yerel Veritabanı:** Room + KSP
- **Bulut Senkronizasyonu:** Firebase Firestore
- **Güvenlik:** AndroidX Biometric, Security Crypto
- **OCR:** Google ML Kit Text Recognition
- **Diğer:** Kotlinx Serialization, Coroutines, WorkManager, OkHttp

---


