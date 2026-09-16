# Panduan Setup Server WireGuard di Google Cloud (Free Tier)

Panduan ini bikin server WireGuard di **Google Cloud** untuk **tes gratis** aplikasi VPN Android lo.

> ⚠️ **Baca dulu:** Free tier GCP kasih VM gratis selamanya, TAPI **bandwidth keluar cuma ~1 GB/bulan**.
> Ini **cukup buat TES** (buktiin app + tunnel jalan), tapi **tidak untuk pemakaian harian**.
> Buat pemakaian nyata, pindah ke Vultr/DigitalOcean nanti — cukup ganti config, app tetap sama.

---

## 0. Yang Dibutuhkan

- Akun Google.
- **Kartu kredit** (wajib buat verifikasi; dapat juga kredit trial $300/90 hari).
- ~20 menit.

---

## 1. Bikin VM yang Masuk Kategori "Always Free"

Supaya **gratis beneran**, VM-nya HARUS sesuai spesifikasi Always Free ini — jangan sampai salah pilih:

1. Buka https://console.cloud.google.com → buat project baru (mis. `vpn-lab`).
2. Menu ▸ **Compute Engine** ▸ **VM instances** ▸ **Create Instance**.
3. Setelan yang **wajib** biar masuk Always Free:
   | Setelan | Pilih |
   |---------|-------|
   | **Region** | `us-west1`, `us-central1`, **atau** `us-east1` (HANYA ini yang free) |
   | **Machine type** | Seri **E2**, tipe **`e2-micro`** (HANYA ini yang free) |
   | **Boot disk** | Ubuntu 22.04 LTS, **Standard persistent disk** (bukan SSD), **≤ 30 GB** |
   | **Firewall** | centang **Allow HTTP** & **Allow HTTPS** (opsional; UDP kita atur manual) |
4. Klik **Create**. Catat **External IP** VM (mis. `34.120.0.10`).

> 💡 Region VPN di US artinya latensi dari Indonesia agak tinggi — wajar, ini cuma buat tes.

---

## 2. Buka Firewall UDP 51820 (VPC Firewall Rule)

GCP mem-block semua port kecuali yang diizinkan. Tambah rule untuk WireGuard:

1. Menu ▸ **VPC network** ▸ **Firewall** ▸ **Create firewall rule**.
2. Isi:
   | Field | Nilai |
   |-------|-------|
   | Name | `allow-wireguard` |
   | Direction | **Ingress** |
   | Targets | All instances in the network |
   | Source IPv4 ranges | `0.0.0.0/0` |
   | Protocols and ports | **UDP** ▸ `51820` |
3. **Create**.

---

## 3. SSH ke VM & Install WireGuard

Klik tombol **SSH** di sebelah VM (browser SSH), lalu:

```bash
sudo apt update && sudo apt install -y wireguard
```

## 4. Aktifkan IP Forwarding

```bash
echo 'net.ipv4.ip_forward=1' | sudo tee /etc/sysctl.d/99-wireguard.conf
sudo sysctl -p /etc/sysctl.d/99-wireguard.conf
```

## 5. Generate Kunci (server + client)

```bash
cd /etc/wireguard
umask 077
wg genkey | tee server_private.key | wg pubkey > server_public.key
wg genkey | tee client_private.key | wg pubkey > client_public.key
echo "SERVER_PRIVATE=$(cat server_private.key)"
echo "SERVER_PUBLIC=$(cat server_public.key)"
echo "CLIENT_PRIVATE=$(cat client_private.key)"
echo "CLIENT_PUBLIC=$(cat client_public.key)"
```

Simpan keempat nilai di atas.

## 6. Config Server: `/etc/wireguard/wg0.conf`

Cari nama interface publik dulu:
```bash
ip route get 1.1.1.1 | grep -oP 'dev \K\S+'   # biasanya ens4 di GCP
```

Buat file (`sudo nano /etc/wireguard/wg0.conf`), ganti placeholder & nama interface (`ens4`):

```ini
[Interface]
Address = 10.8.0.1/24
ListenPort = 51820
PrivateKey = SERVER_PRIVATE

PostUp   = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o ens4 -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o ens4 -j MASQUERADE

[Peer]
PublicKey = CLIENT_PUBLIC
AllowedIPs = 10.8.0.2/32
```

## 7. Jalankan Server

```bash
sudo systemctl enable --now wg-quick@wg0
sudo wg show
```

---

## 8. Client Config untuk Aplikasi Android

Tempel ini ke text box di aplikasi. Ganti placeholder:

```ini
[Interface]
PrivateKey = CLIENT_PRIVATE
Address = 10.8.0.2/32
DNS = 1.1.1.1

[Peer]
PublicKey = SERVER_PUBLIC
Endpoint = 34.120.0.10:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
```

| Placeholder | Dari |
|-------------|------|
| `CLIENT_PRIVATE` | langkah 5 |
| `SERVER_PUBLIC` | langkah 5 |
| `34.120.0.10` | External IP VM (langkah 1) |

---

## 9. Tes (tujuan utama free tier GCP)

1. Build & install app (lihat `README.md`).
2. Tempel client config ▸ **Connect** ▸ setujui dialog VPN.
3. Buka `https://ifconfig.me` di browser HP → IP harus jadi IP VM GCP. ✅
4. Buka 1-2 web ringan buat mastiin traffic jalan. ✅
5. **Disconnect** setelah puas (hemat kuota 1 GB).

📸 Screenshot bukti IP berubah = bagus buat portfolio.

---

## 10. ⚠️ Biar Aman dari Tagihan (WAJIB)

1. **Set Budget Alert:** Billing ▸ Budgets & alerts ▸ Create budget → set $1, alert di 50%/90%/100%. Lo bakal diemail sebelum kena charge.
2. **Pantau bandwidth:** ingat batas ~1 GB/bulan. Jangan streaming/download lewat VPN ini.
3. **Matiin VM kalau nggak dipakai:** VM instances ▸ pilih VM ▸ **Stop**. VM yang di-stop nggak makan kuota (disk tetap kecil & gratis).
4. **Hapus kalau udah selesai tes:** Delete VM + firewall rule kalau nggak lanjut.

---

## 11. Pindah Provider Nanti (App TIDAK berubah)

Kalau mau upgrade ke Vultr/DigitalOcean (bandwidth lega):
1. Setup WireGuard di server baru (langkah 3-7 sama persis).
2. Di client config, cukup ganti **`Endpoint`** (IP baru) & **`PublicKey`** peer (server baru).
3. Tempel config baru ke app ▸ Connect.

**Aplikasi Android-nya nggak perlu di-rebuild.** WireGuard = config-driven. 🎉

## Troubleshooting

- **Handshake nggak muncul (`sudo wg show` di server):** firewall UDP 51820 belum kebuka (cek langkah 2).
- **Connect tapi no internet:** cek `ip_forward` aktif & nama interface di rule MASQUERADE benar (`ens4`).
- **Lambat:** wajar, server di US + free tier. Buat pemakaian nyata pindah ke region Asia (Vultr/DO Singapore).
