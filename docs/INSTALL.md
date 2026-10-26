# Instalasi dan upgrade 1.1.1

Target yang diuji: Paper 1.21 build 130, Java 21. JAR: `fusionenchanting-1.1.1.jar`. Versi 1.1.1 menambah panduan opsional; alur dan balance 1.1.0 tetap berlaku.

1. Hentikan server dengan `stop` dan cadangkan world/playerdata/config.
2. Pasang tepat satu JAR FusionEnchanting di `plugins/`; pindahkan versi lama ke luar folder itu.
3. Jalankan server. Config lama schema 1 dibackup ke `config.yml.v1-backup-*` lalu dimigrasi secara atomik ke schema 2.
4. Cari log 36 recipes/36 handlers, jalankan `/fusion selftest` dari konsol, dan uji buku + buku pada anvil.
5. Untuk akses Codespaces, playit tetap diarahkan ke `127.0.0.1:25565`.

Pada workspace pengembangan lengkap, `python3 scripts/deploy-plugin.py` dijalankan dari folder `minecraft/` untuk menyiapkan JAR secara atomik dan membackup versi lama ke `.tools/plugin-backups/`. Script ini tidak disertakan dalam repo plugin mandiri. Server tidak direstart otomatis. Hindari hot-reload/PlugMan.

## Perubahan nama plugin

Nama plugin sekarang `FusionEnchanting`, dengan folder config `plugins/FusionEnchanting/`. Untuk instalasi lama, saat server berhenti pindahkan config dari `plugins/EnchantmentFusion/` ke folder baru sebelum menjalankan server. Namespace data item dan permission `enchantmentfusion.*` tetap dipertahankan agar item dan izin lama kompatibel.

## Perubahan dari 1.0

- Pembuatan hanya lewat **buku + buku → buku fusion**. Equipment + buku biasa tetap vanilla.
- Buku fusion dipasang ke equipment melalui anvil normal. Admin `/fusion give` kini memberi buku.
- Tidak ada catalyst atau biaya tetap 16–30 level. Config baru memakai `extra-levels` (0–12, default 2–5), hanya untuk pembuatan buku.
- Legacy `levels`/`catalyst-amount` tidak dikonversi menjadi surcharge besar. Default chance durability lama diturunkan; custom tuning yang berbeda dari default lama dipertahankan.
- Workhorse 15%, Miner's Luck 20%, Preservation 20%, Fortified 10%. Master Angler menghemat wear 20% ketika tangkapan; tidak lagi memperbaiki rod.
- Item equipment lama tetap berlaku. Source level dapat naik secara vanilla. Lore diperbarui otomatis melalui join/held/inventory.

Untuk rollback ke 1.0, kembalikan backup server/config yang konsisten. Binary lama tidak dapat memakai buku fusion baru, meskipun tidak menghapusnya. Jangan memuat dua JAR bersamaan atau mendowngrade world Minecraft.

## Konfigurasi

`config-version: 2`, `allow-pvp-abilities` default false, `disabled-worlds`, serta tuning per ID: enabled, extra-levels, cooldown-seconds, chance, power. `/fusion reload` menolak nilai salah tanpa mengganti config aktif.

FusionCreateEvent memiliki operasi CREATE_BOOK/APPLY_BOOK/MAINTAIN dan getter clone untuk sumber/input kanan/hasil. FusionAbilityEvent dapat dibatalkan per target. Integrasi plugin proteksi tertentu tetap perlu diuji pada stack tujuan.

## Validasi ulang

Dari repo plugin mandiri, jalankan `mvn clean verify` untuk build dan core regression test. Pipeline integrasi berikut memerlukan workspace pengembangan lengkap dan dijalankan dari folder `minecraft/`:

```bash
./scripts/production-check.sh --offline --headless --deploy
```

EULA harus sudah diterima sendiri. Server test memakai world terpisah dan tidak menjalankan playit atau mengubah autentikasi server utama. `--headless` meniadakan TCP bind hanya di JVM test. Hapus flag itu bila lingkungan mendukung socket dan ingin menguji listener lokal. Jaringan/client eksternal tetap memerlukan pemeriksaan terpisah.
