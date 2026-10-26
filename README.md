# Enchantment Fusion 1.1.1

Modifikasi enchanting vanilla untuk **Paper 1.21 / Java 21**, dengan 36 fusion. Package `org.leap.enchantmentfusion`.

## Cara bermain

1. Buka anvil dan gabungkan **dua enchanted book biasa** dengan pasangan/level yang cocok.
2. Ambil **buku fusion**; biaya memakai kalkulasi vanilla dengan tambahan kecil 2–5 level.
3. Pasang buku fusion ke equipment melalui anvil; biaya pemasangan memakai vanilla tanpa tambahan kedua.

Tidak ada catalyst, station, currency, menu pilihan, command wajib, atau klik konfirmasi khusus. Shift-click dan klik kanan tetap bekerja. Sword Sharpness + buku Fire Aspect biasa tetap menjadi sword vanilla, tanpa otomatis berubah menjadi fusion.

Contoh terukur pada buku/item baru: buku Sharpness V (kiri) + Fire Aspect II (kanan) → Infernal Edge I, **7 level** (4 vanilla + 3 tambahan). Pasang buku tersebut ke diamond sword baru: **10 level**, sesuai vanilla. Riwayat anvil dan urutan buku dapat mengubah biaya native.

[Lihat 36 efek dan balance](docs/BALANCE.md).

## Bantuan opsional

Saat pertama membuka anvil, pemain mendapat pesan singkat dengan tautan **[Panduan resep]**. Membacanya membuka buku Minecraft berisi seluruh resep aktif, kategori equipment, level minimum bahan, biaya tambahan, dan tautan ke efek/batasannya. Panduan tidak memberi item atau memotong XP.

Menaruh satu buku enchant di anvil menampilkan contoh pasangan beserta level minimumnya dan tautan resep yang sesuai. Preview fusion menampilkan biaya aktual. Pesan dibatasi agar tidak membanjiri chat.

Panduan dapat dibuka lagi dengan `/fusion guide`; `/fusion guide sharpness` memfilter bahan, dan `/fusion guide infernal_edge` membuka detail. `/fusion recipes [page]`, `/fusion info <id>`, dan `/fusion inspect` tetap tersedia. Pemain tidak perlu membaca panduan atau memakai command untuk membuat atau memasang fusion.

[Cara menemukan resep](docs/DISCOVERY.md) · [Katalog seluruh efek](docs/BALANCE.md).

Admin: `/fusion give <player> <id>` memberi buku fusion; `/fusion reload` memuat config valid; `/fusion selftest` memeriksa data/API server. Permission use/recipes default true; admin/reload default op.

Satu fusion per buku/equipment. Source enchant tetap native; level source dapat ditingkatkan melalui anvil biasa. Grindstone membersihkan fusion. Lore item lama diperbarui otomatis. Item versi 1.0 tetap kompatibel.

## Build, rilis, dan operasi

Repo plugin ini membutuhkan JDK 21 dan Maven. Dari root repo:

```bash
mvn clean verify
```

JAR hasil build tersedia di `target/enchantment-fusion-1.1.1.jar`. Folder `target/` tidak masuk Git; distribusikan JAR melalui GitHub Releases. Source, resource default plugin, core regression test, dan dokumentasi tetap masuk repo.

Pipeline integrasi berikut hanya tersedia di workspace pengembangan lengkap, dijalankan dari folder `minecraft/` (script dan server lokal tidak disertakan dalam repo plugin):

```bash
./scripts/production-check.sh --offline --headless --deploy
```

Baseline pengujian: Paper 1.21 build 130 dan Java 21. Tanpa `--deploy`, runtime utama tidak disentuh. Paket dibuat di `release/` hanya setelah validasi untuk byte JAR/source/test yang sama lolos.

[Instalasi/upgrade](docs/INSTALL.md) · [Hasil validasi](docs/VALIDATION.md) · [Pengujian](docs/TESTING.md) · [Changelog](docs/CHANGELOG.md).
