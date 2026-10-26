# Pengujian 1.1.1 — alur buku native dan discovery

Untuk build dan core regression test dari repo plugin mandiri, jalankan `mvn clean verify` dengan JDK 21 dan Maven. Pipeline integrasi di bawah dijalankan dari folder `minecraft/` pada workspace pengembangan lengkap; `scripts/` dan server test tidak disertakan dalam repo plugin.

```bash
./scripts/production-check.sh --offline --headless --deploy
```

Pipeline menjalankan core rules, compiler Java 21, pemeriksaan JAR, server Paper test, restart JVM, packaging ber-checksum, dan deployment atomik bila diminta. EULA harus sudah diterima pemilik. World utama, autentikasi, playit, dan folder web tidak dipakai dalam test.

## Cakupan

- Seluruh 36 recipe: dua buku sumber diproses oleh AnvilMenu/packet handler native menjadi buku fusion.
- Seluruh 36 pemasangan: buku hasil proses nyata dipasang ke equipment; UUID tetap sama, dua input dikonsumsi sekali, dan biaya persis sama dengan kontrol vanilla.
- Biaya pembuatan dibanding kontrol merge vanilla + extra-levels. Urutan buku terbalik didukung; riwayat prior work dan Too Expensive dipertahankan.
- Buku biasa + equipment dan equipment + equipment tetap vanilla. Input ambigu tidak memilih recipe tersembunyi. Buku fusion tidak aktif saat hanya dipegang.
- Klik kanan, shift-click, dan hotbar memakai perilaku native. Inventory penuh, XP kurang, replay 300 klik, preview/harga palsu, dua pemain, veto plugin, reload, dan disable diperiksa.
- Seluruh 36 ability beserta guard, native combat/mace, projectile snapshot, mining, durability, potion, PvP, pet, world, dan permission.
- Master Angler memblokir wear saat catch pada rod yang sama; tidak memperbaiki durability. Default penghematan durability diuji lewat batas balance pada core rules.
- Rename, repair, source upgrade, grindstone, crafting/smithing guards, lore update otomatis, PDC salah, serialisasi, dan migrasi config dengan backup.
- Server kedua memeriksa kembali 36 buku dan 36 equipment dari chest/world tersimpan: total 72 identitas.
- Panduan: seluruh 36 bahan/level/efek/batasan, paginasi, filter, tuning dan resep nonaktif; packet pembukaan buku, item tangan dan pengembalian input anvil tanpa XP/item tambahan; pesan pengenalan satu kali, izin/world, petunjuk sumber kiri/kanan, biaya preview, debounce, serta pembatalan saat anvil ditutup.

Test memakai ServerPlayer dan entity nyata dengan channel dalam memori. Sebagian pemicu ability menggunakan event terstruktur agar deterministik. `--headless` meniadakan TCP bind hanya di JVM test; tidak sama dengan pengujian client eksternal.

## Bukti dan batas

Hasil terakhir ditulis ke VALIDATION.md. `book-costs.tsv` berisi harga terukur pada satu urutan buku baru dan equipment baru. `test-results.txt` mencatat tiap skenario. Paket tidak dibuat jika hash JAR/source/test berbeda dari hasil yang lolos, atau ada recipe/ability yang tidak teruji.

Balance.md menjelaskan formula, penurunan chance, dan tradeoff default. Tes memeriksa perilaku dan angka; balance ekonomi/XP farm, preferensi pemain, meta PvP, lag jaringan, soak test, serta plugin pihak ketiga tetap memerlukan playtest pada server tujuan. Tidak ada klaim Folia atau semua patch 1.21.x.
