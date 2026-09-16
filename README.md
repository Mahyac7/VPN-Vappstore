# WireGuard VPN — Android (Beta)

Aplikasi VPN Android sederhana yang **jalan beneran**, memakai library resmi WireGuard
(`com.wireguard.android:tunnel`) dan `VpnService` Android. Dibuat untuk **belajar & portfolio**,
terhubung ke **server WireGuard milik lo sendiri** (lihat [`docs/SERVER_SETUP.md`](docs/SERVER_SETUP.md)).

> Tidak perlu root. Backend userspace Go dari WireGuard menangani tunnel & routing.

---

## Fitur

- Connect / disconnect ke tunnel WireGuard dengan satu tap.
- Tempel & simpan config `.conf` langsung di aplikasi.
- Validasi config sebelum connect.
- Foreground service + notifikasi status saat VPN aktif (wajib di Android modern).
- UI Jetpack Compose (Material 3).

## Arsitektur

```
MainActivity ──▶ VpnService.prepare() (dialog izin VPN Android)
     │
     ▼
VpnViewModel ──▶ VpnManager (singleton)
                    │
                    ├─ GoBackend  (userspace WireGuard, no root)
                    ├─ WgTunnel   (implementasi Tunnel: nama + callback state)
                    └─ Config.parse(raw .conf)

WireGuardVpnService : GoBackend.VpnService  (foreground notification)
ConfigStore : SharedPreferences (simpan config)
```

| File | Peran |
|------|------|
| `MainActivity.kt` | Entry point, minta izin VPN & notifikasi |
| `ui/VpnScreen.kt` | UI Compose (status, input config, tombol) |
| `ui/VpnViewModel.kt` | State, validasi, connect/disconnect |
| `vpn/VpnManager.kt` | Wrapper GoBackend, up/down tunnel |
| `vpn/WgTunnel.kt` | Implementasi `Tunnel` |
| `vpn/WireGuardVpnService.kt` | Foreground service + notifikasi |
| `data/ConfigStore.kt` | Simpan config di SharedPreferences |

---

## Cara Build & Jalankan

### Prasyarat
- **Android Studio** (Ladybug atau lebih baru) + Android SDK (compileSdk 34).
- JDK 17 (dibundel Android Studio).
- HP Android (API 24+) atau emulator. **Catatan:** tunnel VPN paling andal diuji di **device fisik**.

### Langkah
1. Buka folder project ini di Android Studio (`File ▸ Open`).
2. Biarkan Gradle sync (mengunduh dependency, termasuk library WireGuard).
3. Sambungkan HP (USB debugging aktif) atau jalankan emulator.
4. Klik **Run ▶**. Aplikasi ter-install.

Atau via command line (butuh Android SDK ter-set di `local.properties`):
```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Build di Cloud lewat GitHub Actions (tanpa Android SDK lokal) ☁️

Nggak punya Android SDK di komputer? Build APK-nya langsung di GitHub — runner-nya udah
punya Android SDK. Workflow-nya ada di [`.github/workflows/android-build.yml`](.github/workflows/android-build.yml).

Cara pakai:
1. Buka tab **Actions** di repo GitHub lo.
2. Pilih workflow **"Build Android APK"** ▸ klik **Run workflow** (atau otomatis jalan tiap push ke `main`).
3. Tunggu build selesai (centang hijau ✅).
4. Buka run tersebut ▸ scroll ke bagian **Artifacts** ▸ download **`wireguard-vpn-debug-apk`** (berupa `.zip`).
5. Extract zip-nya → dapat `app-debug.apk`.

**Install APK ke HP:**
- Transfer `app-debug.apk` ke HP (USB / Google Drive / email).
- Buka file-nya di HP ▸ izinkan **"Install from unknown sources"** ▸ Install.
- Buka app ▸ tempel WireGuard config ▸ Connect. ✅

### Menghubungkan
1. Siapkan server WireGuard lo:
   - **Umum (VPS apa pun):** [`docs/SERVER_SETUP.md`](docs/SERVER_SETUP.md)
   - **DigitalOcean (mudah, tanpa bayar di muka):** [`docs/DIGITALOCEAN_SETUP.md`](docs/DIGITALOCEAN_SETUP.md) ⭐
   - **Google Cloud free tier:** [`docs/GOOGLE_CLOUD_SETUP.md`](docs/GOOGLE_CLOUD_SETUP.md)
2. Salin **client config** dari panduan itu.
3. Buka aplikasi ▸ tempel config ▸ **Connect** ▸ setujui dialog izin VPN Android.
4. Verifikasi: buka `https://ifconfig.me` di browser HP — IP harus jadi IP VPS lo.

---

## Catatan Penting

- **Environment build:** project ini disiapkan lengkap, tapi APK harus di-compile di mesin lo
  dengan Android SDK (SDK tidak tersedia di sandbox tempat project ini dibuat).
- **Keamanan:** `ConfigStore` menyimpan config (berisi private key) dalam plain SharedPreferences
  demi kejelasan belajar. Untuk produk nyata, pakai `EncryptedSharedPreferences`.
- **Lisensi:** library `com.wireguard.android:tunnel` berlisensi **GPLv2**. Kalau lo distribusikan
  aplikasinya, patuhi ketentuan GPLv2.
- **Google Play:** aplikasi VPN punya kebijakan khusus (deklarasi VpnService, privacy policy, dll).

## Roadmap Ide (opsional buat portfolio)
- QR code scanner untuk import config.
- Multiple server profiles + pemilih server.
- Statistik transfer (rx/tx) realtime dari `backend.getStatistics()`.
- Kill-switch (blokir traffic saat tunnel down).
- Enkripsi penyimpanan config.
