# FusionEnchanting

**Gabungkan enchantment vanilla menjadi 36 fusion melalui anvil biasa.**

FusionEnchanting memperluas enchanting untuk server survival: gabungkan dua enchanted book menjadi buku fusion, lalu pasang ke equipment untuk mendapatkan efek tambahan. Enchantment sumber tetap bekerja seperti biasa, dengan bonus yang memiliki kondisi, peluang, atau cooldown sesuai resep.

**Versi 1.1.1 · Paper 1.21 · Java 21 · Apache-2.0**

[Unduhan](https://github.com/leap-oss/FusionEnchanting/releases) · [Katalog 36 fusion](docs/BALANCE.md) · [Panduan resep](docs/DISCOVERY.md) · [Laporkan bug](https://github.com/leap-oss/FusionEnchanting/issues)

## Fitur utama

- **36 fusion** untuk pedang, bow, crossbow, alat tambang, armor, sepatu, trident, mace, pancing, dan helm.
- **Interaksi anvil yang familiar.** Gabungkan buku, ambil hasil, lalu pasang ke equipment. Klik kanan, shift-click, dan hotbar tetap didukung.
- **Biaya XP mengikuti vanilla.** Pembuatan buku menambah 2–5 level secara default; pemasangan menggunakan biaya anvil vanilla.
- **Resep mudah ditemukan.** Panduan berbentuk buku Minecraft, petunjuk pasangan enchant, dan informasi biaya membantu pemain belajar langsung di dalam game.
- **Satu fusion per item.** Enchantment asli tetap tersedia; rename, repair, dan peningkatan level enchant sumber didukung melalui anvil.
- **Konfigurasi per fusion.** Atur status aktif, biaya tambahan, cooldown, peluang, dan kekuatan efek. Ability tambahan terhadap pemain lain nonaktif secara default.

Plugin dipasang di server. Pemain cukup menggunakan client Minecraft Java biasa; mod client dan resource pack tidak diperlukan. Membuat fusion tidak membutuhkan catalyst, currency tambahan, station baru, command, atau menu pemilihan.

## Cara bermain

```text
Buku enchant A + Buku enchant B
               │
          Anvil + level XP
               ↓
           Buku fusion
               │
     Anvil + equipment + level XP
               ↓
      Equipment dengan fusion
```

1. Buka anvil dan masukkan **dua enchanted book biasa** dengan pasangan serta level yang memenuhi syarat resep.
2. Periksa preview, lalu ambil **buku fusion**.
3. Gabungkan buku fusion dengan equipment yang sesuai melalui anvil.

Contoh: **Sharpness V + Fire Aspect II → Infernal Edge I**, lalu pasang buku tersebut ke pedang. Serangan penuh membakar target selama 3 detik; target yang sudah terbakar menerima tambahan 2 damage, dengan cooldown default 4 detik.

Menggabungkan pedang Sharpness dengan buku Fire Aspect biasa tetap menghasilkan pedang vanilla. Fusion dibuat dari **buku + buku** terlebih dahulu.

### Biaya XP

| Transaksi | Biaya |
|---|---|
| Membuat buku fusion | Biaya anvil vanilla + tambahan resep, default **2–5 level** |
| Memasang buku fusion | Biaya anvil vanilla |

Pada Paper 1.21, buku Sharpness V baru di kiri dan Fire Aspect II baru di kanan menghasilkan Infernal Edge dengan biaya **7 level**: 4 dari vanilla + 3 tambahan fusion. Memasang buku hasilnya ke diamond sword baru memerlukan **10 level** pada transaksi berikutnya.

Riwayat penggunaan anvil dan urutan input dapat mengubah biaya vanilla. Prior-work penalty serta batas **Too Expensive** tetap berlaku.

## Contoh fusion

Semua angka berikut memakai konfigurasi default. Level bahan adalah syarat minimum.

| Fusion | Bahan buku | Efek tambahan |
|---|---|---|
| **Infernal Edge** | Sharpness V + Fire Aspect II | Serangan penuh membakar target; target yang sudah terbakar menerima +2 damage. Cooldown 4 detik. |
| **Hellfire** | Power V + Flame I | Panah tarikan penuh memberi +2 damage dan membakar target selama 5 detik. Cooldown 6 detik. |
| **Prospector** | Efficiency V + Fortune III | Memecahkan ore memberi Night Vision selama 12 detik. Cooldown 10 detik. |
| **Fortified** | Protection IV + Unbreaking III | Peluang 10% mencegah keausan bagian armor yang memiliki fusion ini. |
| **Stormcaller** | Loyalty III + Channeling I | Hit lemparan saat hujan/badai memberi +2 damage dan kilat visual. Cooldown 10 detik. |
| **Meteor** | Density V + Wind Burst I | Smash dari ketinggian minimal 3 blok memberi +3 damage. Cooldown 8 detik. |
| **Master Angler** | Luck of the Sea III + Lure III | Peluang 20% mencegah keausan pancing saat mendapat tangkapan. |
| **Aquanaut** | Respiration III + Aqua Affinity I | Mendapat Night Vision singkat saat berada di dalam air. |

[Lihat seluruh 36 resep, efek, batasan, dan contoh biaya →](docs/BALANCE.md)

## Menemukan resep di dalam game

Saat pertama membuka anvil, pemain mendapat pesan dengan tautan **[Panduan resep]**. Panduan memakai tampilan buku Minecraft dan memuat kategori equipment, bahan beserta level minimum, biaya tambahan, serta detail efek dan batasannya.

Masukkan satu buku enchant ke anvil untuk melihat contoh pasangan yang cocok. Preview fusion menampilkan biaya aktual. Petunjuk dibatasi agar tidak membanjiri chat.

Panduan dapat dibuka kembali dengan:

```text
/fusion guide
/fusion guide sharpness
/fusion guide infernal_edge
```

Membaca panduan bersifat opsional dan tidak mengonsumsi item atau XP. Jika dibuka ketika anvil berisi input, input dikembalikan melalui perilaku native sebelum tampilan buku dibuka.

## Instalasi

**Kebutuhan:** server Paper **1.21** dengan Java **21**. Baseline pengujian adalah Paper 1.21 build 130; dukungan Folia dan patch Minecraft lain belum divalidasi.

1. Unduh JAR dari halaman [Releases](https://github.com/leap-oss/FusionEnchanting/releases), atau [build dari source](#build-dari-source).
2. Hentikan server dan masukkan `fusionenchanting-1.1.1.jar` ke folder `plugins/`.
3. Jalankan server. Konfigurasi akan dibuat di `plugins/FusionEnchanting/config.yml`.
4. Buka anvil untuk mencoba resep, atau jalankan `/fusion guide` di dalam game.

Untuk upgrade, cadangkan data server dan gunakan tepat satu JAR plugin. Pengguna nama lama **EnchantmentFusion** perlu memindahkan config dari `plugins/EnchantmentFusion/` ke `plugins/FusionEnchanting/` saat server berhenti, serta menghapus JAR lama dari `plugins/`. Detail migrasi dan rollback tersedia di [panduan instalasi](docs/INSTALL.md).

## Command

Alias `/fusion`: `/efusion`.

| Command | Fungsi |
|---|---|
| `/fusion help` | Menampilkan bantuan dan tautan panduan |
| `/fusion guide [id/enchant]` | Membuka buku resep; opsional memfilter bahan atau membuka satu fusion |
| `/fusion recipes [halaman]` | Menampilkan katalog resep di chat |
| `/fusion info <id>` | Menampilkan bahan, efek, batasan, dan tuning sebuah fusion |
| `/fusion inspect` | Memeriksa fusion pada item di tangan utama |
| `/fusion give <player> <id>` | Memberikan buku fusion kepada pemain online; khusus admin |
| `/fusion reload` | Memuat ulang konfigurasi plugin; memerlukan izin reload |
| `/fusion selftest` | Menjalankan diagnostik metadata/serialisasi; khusus admin |

### Permission

| Permission | Default | Fungsi |
|---|---|---|
| `enchantmentfusion.use` | Semua pemain | Membuat, memasang, dan menggunakan fusion |
| `enchantmentfusion.recipes` | Semua pemain | Mengakses panduan, resep, info, dan inspect |
| `enchantmentfusion.admin` | OP | Menggunakan give dan selftest |
| `enchantmentfusion.reload` | OP | Memuat ulang konfigurasi |

Prefix permission `enchantmentfusion.*` dipertahankan untuk kompatibilitas dengan instalasi sebelumnya.

## Konfigurasi

Edit `plugins/FusionEnchanting/config.yml`, lalu jalankan `/fusion reload`. Contoh pengaturan:

```yaml
config-version: 2
allow-pvp-abilities: false
disabled-worlds: []

fusions:
  infernal_edge:
    enabled: true
    extra-levels: 3
    cooldown-seconds: 4
    chance: 1.0
    power: 1.0
```

| Pengaturan per fusion | Fungsi | Rentang |
|---|---|---|
| `enabled` | Mengaktifkan resep dan ability | `true` / `false` |
| `extra-levels` | Tambahan level XP saat membuat buku | `0–12` |
| `cooldown-seconds` | Cooldown ability | `0–300` detik |
| `chance` | Peluang memicu efek ketika syaratnya terpenuhi | `0.0–1.0` |
| `power` | Pengali skala efek yang didukung ability | `0.25–2.0` |

`allow-pvp-abilities` mengatur ability tambahan terhadap pemain lain. `disabled-worlds` membatasi penggunaan fusion pada world tertentu. Konfigurasi tidak valid ditolak saat reload; konfigurasi aktif sebelumnya tetap digunakan.

[Lihat konfigurasi default lengkap](src/main/resources/config.yml).

## Aturan item

- Setiap buku atau equipment hanya memiliki **satu fusion level I**. Dua item yang sama-sama memiliki fusion tidak dapat digabung.
- Buku dengan beberapa enchant dapat dipakai jika tepat satu resep cocok. Jika ada beberapa kandidat fusion, hasil tetap buku vanilla.
- Equipment harus menerima seluruh enchant sumber dan sesuai kategori fusion agar buku bisa dipasang.
- Grindstone membersihkan fusion bersama proses disenchant vanilla.
- Lakukan upgrade equipment ke **netherite sebelum memasang fusion**. Smithing dan crafting repair pada item fusion diblokir untuk menjaga identitas item.

## Build dari source

Gunakan **JDK 21** dan **Maven**, lalu jalankan dari root repository plugin:

```bash
mvn clean verify
```

Perintah ini menjalankan core regression test dan menghasilkan `target/fusionenchanting-1.1.1.jar`. Paper API memakai scope `provided` dan tidak dibundel ke dalam JAR. Identitas Maven: `org.leap:fusionenchanting:1.1.1`.

Metode pengujian server, cakupan, dan batas validasi tersedia dalam [panduan pengujian](docs/TESTING.md) dan [laporan validasi](docs/VALIDATION.md). Pengujian client asli, balance survival jangka panjang, dan kompatibilitas dengan kombinasi plugin server perlu dinilai pada server tujuan.

## Bantuan dan kontribusi

Laporkan bug atau usulkan perubahan melalui [GitHub Issues](https://github.com/leap-oss/FusionEnchanting/issues). Untuk laporan bug, sertakan versi plugin, versi Paper/Java, langkah reproduksi, konfigurasi terkait, dan potongan log yang relevan. Pull request untuk perbaikan bug, dokumentasi, dan pengujian juga diterima.

[Instalasi dan upgrade](docs/INSTALL.md) · [Resep dan balance](docs/BALANCE.md) · [Penemuan resep](docs/DISCOVERY.md) · [Changelog](docs/CHANGELOG.md)

## Lisensi

Dikembangkan oleh **LEAP** dan tersedia di bawah [Apache License 2.0](LICENSE).
