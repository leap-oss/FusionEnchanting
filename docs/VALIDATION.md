# Hasil validasi 1.1.1 — 2026-10-25

Baseline: Paper **1.21 build 130**, Java **21**. SHA256 JAR: `90055ab631cc7e4406ae06b9211233772e037157fe3b410f949429e33a012c65`.

| Pemeriksaan | Hasil |
|---|---|
| Rules buku + buku, XP, batas balance, cooldown | PASS — 349 pemeriksaan |
| PDC, buku/equipment, migrasi config, serialisasi | PASS — 532 pemeriksaan native |
| Menu/packet handler, ability dan lifecycle | PASS — 187 skenario server |
| Buku enchant + buku enchant → buku fusion | PASS — seluruh 36 recipe |
| Equipment + buku fusion → fusion terpasang | PASS — seluruh 36 recipe |
| Equipment + buku biasa tetap vanilla | PASS |
| Shift-click, klik kanan, hotbar | PASS — mengikuti kontrol anvil native |
| Biaya membuat buku | Biaya vanilla + surcharge kecil yang tercatat |
| Biaya memasang buku | Sama dengan kontrol vanilla; tidak ada surcharge kedua |
| Pemicu seluruh 36 ability | PASS |
| Restart JVM dan penyimpanan | PASS — 72 identitas buku/equipment |
| Metadata, izin, world, PvP, veto plugin, replay | PASS |
| Panduan resep dan petunjuk anvil | PASS — isi 36 resep, filter/config, izin, pesan, packet buku dan pengembalian input |
| Data distribusi | Source/test/JAR hash harus sesuai dengan hasil tes |

## Metode dan batas

Test menggunakan ServerPlayer dan container-click packet handler Paper dengan channel dalam memori. Biaya setiap recipe dibandingkan dengan kontrol anvil vanilla, lalu pembuatan dan pemasangan dijalankan melalui handler native. Server kedua memeriksa kembali 36 buku dan 36 equipment yang disimpan pada world.

TCP bind ditiadakan hanya pada JVM test karena lingkungan melarang socket. Client eksternal, playit, lag jaringan nyata, soak test lama, serta stack plugin lain belum diuji. Cakupan ini memeriksa perilaku dan memberi baseline balance yang konservatif, bukan membuktikan semua ekonomi server seimbang melalui playtest jangka panjang.

Harga yang diukur tersedia di `book-costs.tsv` dan BALANCE.md. Daftar setiap skenario ada di `test-results.txt`. Log lokal berada di `.tools/fusion-smoke/integration.log` dan `persistence.log`. Folder web tidak termasuk sumber build, pengujian, atau deployment plugin.
