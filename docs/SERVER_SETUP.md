# Panduan Setup Server WireGuard (VPS)

Panduan ini bikin server WireGuard di sebuah VPS Linux (Ubuntu 22.04/24.04) dan menghasilkan
**client config** yang bisa lo tempel ke aplikasi Android.

> Ini untuk pemakaian pribadi / belajar. Jangan buka ke publik tanpa memahami biaya bandwidth & risiko.

---

## 0. Yang Dibutuhkan

- 1 VPS (mis. Hetzner, Contabo, IONOS, Cloudzy) dengan **IPv4 publik**.
- Akses SSH ke VPS sebagai root / sudo.
- Port UDP **51820** dibuka di firewall/panel provider.

Catat **IP publik VPS** lo, misalnya `203.0.113.10`.

---

## 1. Install WireGuard di Server

```bash
sudo apt update && sudo apt install -y wireguard
```

## 2. Aktifkan IP Forwarding

```bash
echo 'net.ipv4.ip_forward=1' | sudo tee /etc/sysctl.d/99-wireguard.conf
sudo sysctl -p /etc/sysctl.d/99-wireguard.conf
```

## 3. Generate Kunci Server

```bash
cd /etc/wireguard
umask 077
wg genkey | tee server_private.key | wg pubkey > server_public.key
cat server_private.key   # <- SERVER_PRIVATE_KEY
cat server_public.key    # <- SERVER_PUBLIC_KEY
```

## 4. Generate Kunci Client

```bash
wg genkey | tee client_private.key | wg pubkey > client_public.key
cat client_private.key   # <- CLIENT_PRIVATE_KEY
cat client_public.key    # <- CLIENT_PUBLIC_KEY
```

## 5. Buat Config Server: `/etc/wireguard/wg0.conf`

Ganti `SERVER_PRIVATE_KEY`, `CLIENT_PUBLIC_KEY`, dan `eth0` (nama interface publik lo — cek dengan `ip route get 1.1.1.1`).

```ini
[Interface]
Address = 10.8.0.1/24
ListenPort = 51820
PrivateKey = SERVER_PRIVATE_KEY

# NAT: forward traffic client ke internet lewat interface publik
PostUp   = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE

[Peer]
# Client (HP Android lo)
PublicKey = CLIENT_PUBLIC_KEY
AllowedIPs = 10.8.0.2/32
```

## 6. Jalankan & Enable Service

```bash
sudo systemctl enable --now wg-quick@wg0
sudo wg show    # cek interface aktif
```

## 7. Buka Firewall

```bash
# UFW
sudo ufw allow 51820/udp
# ATAU cloud firewall provider: allow inbound UDP 51820
```

---

## 8. Client Config untuk Aplikasi Android

Ini yang lo **tempel ke text box di aplikasi**. Ganti placeholder-nya:

```ini
[Interface]
PrivateKey = CLIENT_PRIVATE_KEY
Address = 10.8.0.2/32
DNS = 1.1.1.1

[Peer]
PublicKey = SERVER_PUBLIC_KEY
Endpoint = 203.0.113.10:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
```

| Placeholder | Ambil dari |
|-------------|-----------|
| `CLIENT_PRIVATE_KEY` | langkah 4 (`client_private.key`) |
| `SERVER_PUBLIC_KEY` | langkah 3 (`server_public.key`) |
| `203.0.113.10` | IP publik VPS lo |

- `AllowedIPs = 0.0.0.0/0, ::/0` artinya **semua traffic** dirutekan lewat VPN (full tunnel).
- `PersistentKeepalive = 25` membantu koneksi tetap hidup di belakang NAT seluler.

---

## 9. Uji Coba

1. Build & install aplikasi Android (lihat `README.md`).
2. Tempel client config di atas ke aplikasi, tap **Connect**, setujui dialog VPN Android.
3. Cek IP publik lo di HP (buka `https://ifconfig.me` di browser) — harus jadi IP VPS.

## Troubleshooting

- **Connect gagal / no internet:** cek `net.ipv4.ip_forward=1` aktif dan aturan `MASQUERADE` benar (nama interface).
- **Handshake tapi tak ada data:** biasanya `AllowedIPs`/routing salah, atau firewall provider belum allow UDP 51820.
- **`sudo wg show`** di server harus menampilkan `latest handshake` setelah client connect.
