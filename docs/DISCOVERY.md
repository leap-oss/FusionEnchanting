# Menemukan resep fusion

Saat pertama membuka **anvil**, pemain mendapat satu pesan penjelasan alur dan tautan **[Panduan resep]**. Klik tautan tersebut untuk membaca katalog dalam tampilan buku Minecraft. Pesan pengenalan ditandai pada data pemain sehingga tidak berulang setiap membuka anvil atau masuk server.

Panduan memiliki daftar kategori, resep aktif dengan level minimum kedua bahan, kategori equipment, biaya tambahan, dan tautan **[Efek & detail]**. Detail memuat efek dasar, batasan, serta cooldown, peluang, dan skala efek dari konfigurasi server saat panduan dibuka. Resep nonaktif tidak ditawarkan. Nama enchant memakai nama Inggris agar konsisten dengan ID resep; resource pack/lokalisasi client dapat menampilkan nama enchant vanilla yang berbeda.

Contoh percobaan pemain:

1. Masukkan buku Sharpness di salah satu slot anvil. Petunjuk menunjukkan contoh **Sharpness V + Fire Aspect II → Infernal Edge I**, dengan penanda bahwa angka tersebut adalah level minimal.
2. Klik **[Lihat resep]** di chat untuk melihat pasangan yang memakai Sharpness. Jika input masih berada di anvil, anvil ditutup terlebih dahulu dan input dikembalikan melalui perilaku native.
3. Setelah panduan ditutup, buka anvil kembali dan gabungkan kedua buku sesuai syarat. Preview buku fusion menampilkan nama/efek pada tooltip; petunjuk menampilkan biaya level yang sedang dihitung anvil.
4. Ambil buku fusion, lalu pasang ke sword melalui anvil biasa.

Membaca buku bersifat opsional. Panduan bukan item yang diberikan, tidak mengonsumsi buku atau XP, dan tidak dipakai untuk memilih hasil crafting. Gabungan equipment dengan buku biasa tetap mengikuti vanilla. Input dengan beberapa kandidat fusion tetap menghasilkan buku vanilla; petunjuk menjelaskan perlunya satu pasangan yang jelas.

Panduan bisa diakses kembali melalui tautan `/fusion help` atau `/fusion recipes`, serta command berikut:

| Akses | Isi |
|---|---|
| `/fusion guide` | Katalog seluruh resep aktif |
| `/fusion guide sharpness` | Pasangan dengan bahan Sharpness |
| `/fusion guide infernal_edge` | Bahan, efek, batasan, dan tuning Infernal Edge |
| `/fusion info infernal_edge` | Detail singkat di chat |
| `/fusion inspect` | Informasi fusion pada item yang dipegang |

Petunjuk anvil memerlukan izin `enchantmentfusion.use` dan `enchantmentfusion.recipes`, serta world yang tidak diblokir. Katalog memerlukan izin recipes; keduanya default aktif untuk pemain. Petunjuk konteks hanya diperbarui saat berubah, maksimal sekali per detik; tautan konteks maksimal sekali per 10 detik selama sesi anvil. Pengenalan tidak membuka layar buku secara paksa.

[Seluruh 36 resep dan efek](BALANCE.md) · [Cara bermain](../README.md).

Pengujian server memeriksa isi/paginasi buku, packet pembukaan buku, izin, petunjuk, pembatasan pesan, serta pengembalian input tanpa konsumsi XP. Kejelasan tampilan dan kemudahan menemukannya masih perlu dicoba melalui client Minecraft asli, termasuk jika memakai resource pack.
