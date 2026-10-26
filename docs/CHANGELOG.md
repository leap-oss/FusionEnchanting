# Changelog

## 1.1.1 — 25 Oktober 2026

- Tambah panduan baca-saja memakai tampilan buku Minecraft: daftar kategori, seluruh resep aktif, level minimum bahan, biaya tambahan, efek, batasan, dan tuning aktif.
- Tambah pesan pengenalan satu kali saat pertama memakai anvil, tautan panduan yang dapat diklik, serta petunjuk pasangan saat satu buku dimasukkan. Preview menunjukkan biaya aktual; hint tidak mengubah transaksi.
- Tambah `/fusion guide [id/enchant]`, filter bahan, dan tautan dari help/recipes. Membuka panduan mengembalikan input anvil secara native tanpa memberi item panduan atau memotong XP.
- Tambah regresi discovery pada Paper: isi seluruh 36 resep, paginasi, konfigurasi/izin, pesan, paket pembukaan, pengembalian input, serta debounce dan pembatalan hint.

## 1.1.0 — 25 Oktober 2026

- Ganti alur menjadi buku enchant + buku enchant → buku fusion, lalu buku fusion + equipment. Equipment biasa tidak auto-fuse.
- Hapus catalyst dan biaya tetap tinggi; pembuatan memakai biaya vanilla + 2–5 level, pemasangan sepenuhnya vanilla.
- Pulihkan kontrol inventory native (klik kanan/shift/hotbar) dan upgrade level source.
- Turunkan chance durability, hilangkan perbaikan gratis Master Angler, dan pertahankan knockback vanilla pada Brutal Force.
- Tambah migrasi config schema 2 dengan backup, dukungan PDC/serialisasi buku, serta refresh lore otomatis.
- Tambah kontrol harga native untuk seluruh 36 pembuatan dan 36 pemasangan serta persistensi 72 identitas buku/equipment.


## 1.0.1 — 24 Oktober 2026

- Take fusion memakai transaksi anvil native. Validasi sumber, output, catalyst, XP, game mode, izin, world, dan tuning dilakukan sebelum take; pembatalan listener lain dihormati. Tidak ada lagi tugas tertunda yang memberi item dan memotong XP sendiri.
- Tambah FusionCreateEvent dan FusionAbilityEvent cancellable untuk integrasi proteksi; secondary target diperiksa satu per satu.
- Perbaiki Scorched Venom menjadi Wither I, karena spider kebal Poison. `/fusion inspect` menyegarkan lore lama tanpa mengubah identitas item.
- Pertahankan potion permanen dan potion yang lebih kuat; hindari tamed pets dan NPC.
- Pencarian recipe menggunakan indeks catalyst; enchantment ber-namespace lain tidak dapat menimbulkan benturan nama. Source enchant di atas maksimum native ditolak sebelum preview.
- PDC membatasi ukuran ID/UUID dan menolak primitive/schema salah. Preview dibersihkan saat config reload dan disable.
- Tambah regresi dalam Paper dengan ServerPlayer, menu, dan container-click packet handler native; skenario efek 36 fusion; lifecycle; penyimpanan item lintas restart.
- Tambah seluruh default dalam config, CI Maven/core, pemeriksaan isi JAR, paket checksum dengan gerbang hasil pengujian untuk byte JAR yang sama, serta deployment atomik dengan backup/rollback.

## 1.0.0 — build alpha

Katalog 36 fusion dan implementasi awal. Validasi awal hanya core rules, metadata, dan bootstrap server; belum memiliki rangkaian pengujian gameplay/menu native yang sekarang tersedia.
