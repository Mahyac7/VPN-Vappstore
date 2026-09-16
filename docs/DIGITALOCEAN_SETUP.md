# Panduan Setup Server WireGuard di DigitalOcean

Panduan ini bikin server WireGuard di **DigitalOcean** untuk aplikasi VPN Android lo.
Lebih ramah daripada Google Cloud: **tidak ada syarat bayar di muka**, dan user baru
sering dapat **kredit gratis $200** (cukup buat berbulan-bulan).

> 💡 App Android lo **tidak perlu diubah** sama sekali. WireGuard itu config-driven —
> cukup ganti `Endpoint` (IP server baru) & `PublicKey` (server baru) di config.

---

## 0. Yang Dibutuhkan

- Email (buat daftar DigitalOcean).
- **Kartu kredit/debit ATAU PayPal** (verifikasi awal cuma di-hold ~$1, biasanya balik).
- ~20 menit.

> Droplet termurah **$4/bulan** (dibayar per jam ~$0.006/jam). Kalau dapat kredit $200,
> praktis gratis berbulan-bulan. Bisa dimatiin/dihapus kapan saja biar hemat.

---

## 1. Daftar DigitalOcean

1. Buka https://www.digitalocean.com → **Sign up**.
2. Daftar pakai email / Google / GitHub.
3. Verifikasi email, lalu tambahkan metode bayar (kartu atau PayPal).
   - Cari **kode kredit gratis** ("DigitalOcean $200 credit") saat daftar bila tersedia.

---

## 2. Bikin Droplet (Server)

1. Klik **Create ▸ Droplets**.
2. **Choose Region:** pilih yang dekat Indonesia → **Singapore (SGP1)**. (Latensi rendah.)
3. **Choose an image:** tab **OS** ▸ **Ubuntu** ▸ versi **24.04 (LTS) x64**.
4. **Choose Size:**
   - Type: **Basic**
   - CPU option: **Regular** (Disk SSD)
   - Pilih paket termurah: **$4/mo** (1 GB RAM / 1 vCPU / 25 GB SSD / 500 GB transfer).
     > 500 GB transfer/bulan itu **50x lebih besar** dari free tier Google Cloud (1 GB). Lega buat pemakaian pribadi.
5. **Choose Authentication Method:**
   - **SSH Key** (lebih aman, disarankan) — atau **Password** (lebih gampang buat pemula).
   - Kalau pilih Password, set root password yang kuat & catat.
6. **Hostname:** `wg-server`.
7. Klik **Create Droplet**. Tunggu ~1 menit sampai muncul **IPv4 address** (mis. `159.xx.xx.xx`). **Catat IP ini.**

---

## 3. Login ke Droplet (SSH)

Dari terminal komputer lo (Linux/macOS) atau **PowerShell/Windows Terminal**:

```bash
ssh root@IP_DROPLET_LO
```

- Kalau pakai password: masukkan password root.
- Kalau pakai SSH key: otomatis masuk.
- Ketik `yes` bila ditanya fingerprint pertama kali.

> Nggak punya terminal SSH? DigitalOcean punya **"Console"** (tombol di halaman droplet) — akses browser langsung.

---

## 4. Install WireGuard

```bash
apt update && apt install -y wireguard
```

## 5. Aktifkan IP Forwarding

```bash
echo 'net.ipv4.ip_forward=1' | tee /etc/sysctl.d/99-wireguard.conf
sysctl -p /etc/sysctl.d/99-wireguard.conf
```

## 6. Generate Kunci (server + client)

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

**Simpan keempat nilai di atas** (nanti dipakai di config).

## 7. Config Server: `/etc/wireguard/wg0.conf`

Cari nama interface publik dulu:
```bash
ip route get 1.1.1.1 | grep -oP 'dev \K\S+'   # biasanya eth0 di DigitalOcean
```

Buat file:
```bash
nano /etc/wireguard/wg0.conf
```

Isi (ganti `SERVER_PRIVATE`, `CLIENT_PUBLIC`, dan `eth0` bila beda):

```ini
[Interface]
Address = 10.8.0.1/24
ListenPort = 51820
PrivateKey = SERVER_PRIVATE

PostUp   = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE

[Peer]
PublicKey = CLIENT_PUBLIC
AllowedIPs = 10.8.0.2/32
```

Simpan (`Ctrl+O`, `Enter`, `Ctrl+X`).

## 8. Buka Firewall untuk WireGuard

DigitalOcean tidak mem-block port secara default di droplet, tapi kalau lo pakai `ufw`:

```bash
ufw allow 22/tcp
ufw allow 51820/udp
ufw --force enable
```

> Kalau lo juga bikin **Cloud Firewall** di panel DigitalOcean, tambahkan inbound rule: **UDP port 51820**.

## 9. Jalankan Server WireGuard

```bash
systemctl enable --now wg-quick@wg0
wg show     # harus menampilkan interface wg0
```

---

## 10. Client Config untuk Aplikasi Android

Tempel ini ke text box di aplikasi. Ganti placeholder:

```ini
[Interface]
PrivateKey = CLIENT_PRIVATE
Address = 10.8.0.2/32
DNS = 1.1.1.1

[Peer]
PublicKey = SERVER_PUBLIC
Endpoint = IP_DROPLET_LO:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
```

| Placeholder | Dari |
|-------------|------|
| `CLIENT_PRIVATE` | langkah 6 |
| `SERVER_PUBLIC` | langkah 6 |
| `IP_DROPLET_LO` | IPv4 droplet (langkah 2) |

---

## 11. Tes Koneksi

1. Build & install APK (lewat GitHub Actions — lihat `README.md`).
2. Buka app ▸ tempel client config ▸ **Connect** ▸ setujui dialog VPN Android.
3. Buka `https://ifconfig.me` di browser HP → IP harus jadi IP droplet (Singapore). ✅
4. Cek di server: `wg show` → ada `latest handshake` setelah HP connect.

---

## 12. Hemat Biaya / Aman dari Tagihan

- **Matiin kalau nggak dipakai:** halaman droplet ▸ **Power Off** (masih kena biaya kecil untuk storage). Untuk benar-benar berhenti tagihan → **Destroy** droplet.
- **Pantau billing:** menu **Billing** di DigitalOcean, set alert.
- **500 GB transfer/bulan** sudah termasuk; lewat itu ~$0.01/GB (jauh lebih murah & lega dari GCP).

## Troubleshooting

- **Nggak ada handshake:** cek firewall UDP 51820 (langkah 8) dan `Endpoint` IP benar.
- **Connect tapi no internet:** cek `ip_forward` aktif (langkah 5) & nama interface di MASQUERADE benar (`eth0`).
- **SSH ditolak:** pakai **Console** di panel DigitalOcean sebagai gantinya.

---

## Pindah Provider Lagi Nanti?

Sama seperti sebelumnya: cukup setup WireGuard di server baru, lalu ganti `Endpoint` &
`PublicKey` peer di client config. **Aplikasi Android tidak perlu di-build ulang.** 🎉
