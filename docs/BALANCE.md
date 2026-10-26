# Balance dan biaya — profil vanilla 1.1.0

Profil ini memperbaiki baseline konservatif; tes mekanik tidak membuktikan balance melalui playtest jangka panjang. Semua angka di sini adalah default, bukan jaminan pada konfigurasi custom.

## Alur yang dipakai

1. Dua enchanted book biasa digabung pada anvil menjadi **satu buku fusion**.
2. Buku fusion dipasang ke equipment yang cocok melalui anvil.
3. Menambahkan enchanted book biasa ke equipment, atau menggabungkan dua equipment biasa, **tidak otomatis membuat fusion**.

Tidak ada catalyst, crafting station, currency, pilihan lewat command, tombol konfirmasi, atau menu tambahan. Klik kanan, shift-click, hotbar, rename, repair, dan peningkatan level sumber mengikuti anvil vanilla. Command resep hanya bantuan opsional.

## Biaya XP

Pembuatan buku fusion = **biaya anvil vanilla + 2–5 level**. Memasang buku fusion = **biaya vanilla saja**. Prior-work penalty dan Too Expensive tidak dihapus; menggabungkan buku dalam urutan berbeda dapat memberi harga native berbeda.

Contoh yang diukur pada Paper 1.21, buku baru tanpa prior-work penalty: **Sharpness V di kiri + Fire Aspect II di kanan** memberi biaya merge vanilla 4 + tambahan fusion 3 = **7 level**. Buku Infernal Edge yang dihasilkan dipasang ke diamond sword baru seharga **10 level**, seluruhnya dari kalkulasi vanilla. Ini dua transaksi anvil terpisah, bukan satu pembayaran 17 level sekaligus.

## Penghematan durability

| Fusion | Sebelumnya | Default baru | Tambahan umur teoritis |
|---|---|---|---|
| Workhorse | 25% menghindari wear | 15%, food ≥16 | hingga +17,6% saat kondisi terpenuhi |
| Miner's Luck | 40% saat ore | 20% saat ore | hingga +25% pada pemakaian ore |
| Preservation | 35% saat sneak | 20% saat sneak | hingga +25% saat kondisi terpenuhi |
| Fortified | 20% armor | 10% armor | sekitar +11,1% |
| Master Angler | Memperbaiki 2 durability per tangkapan/cooldown | 20% mencegah wear saat tangkapan | sekitar +25%; tidak ada perbaikan gratis |

Untuk peluang menghindari wear `p`, faktor umur tambahan adalah `1/(1-p)` terhadap wear yang masih lolos Unbreaking. Mekanik penghematan tidak pernah mengembalikan durability, sehingga tidak menggantikan perbaikan Mending. Master Angler sekarang mengikuti prinsip yang sama; rod dapat tetap aus.

Brutal Force menambah momentum secara kecil dan terbatas; tidak lagi menimpa knockback vanilla dengan nilai tetap yang justru bisa lebih kecil.

## Batas kekuatan

Satu fusion per equipment. Enchantment sumber tetap menjalankan perilaku vanilla. Bonus tambahan tidak mengalikan seluruh damage, crit, drop Fortune, atau XP; tidak ada duplikasi loot, vein mining, ledakan penghancur blok, atau panah gratis. PvP tambahan default mati.

Batas bonus damage mentah per detik ketika seluruh syarat terus terpenuhi: Infernal Edge ≤0,50; Brutal Force ≤0,20; Executioner ≤0,50 (HP target ≤25%); Holy Flame ≤0,60 (undead); Hellfire ≤0,33; Blast Arrow ≤0,125; Railshot ≤0,50 (satu proc proyektil); Stormcaller ≤0,20 (hujan); Meteor ≤0,375; Comet ≤0,25 (smash ≥3 blok). Armor, shield, miss, charge, dan cooldown menurunkan hasil nyata lebih lanjut.

Efek mobilitas dan potion memakai durasi pendek atau terrain/air tertentu. Fusion armor yang sama tidak memicu empat efek sekaligus. Potion permanen atau lebih kuat tidak dipendekkan. Katalog memakai efek Minecraft yang sudah tersedia, tanpa skill bar atau tombol baru.

## Katalog dan harga yang diukur

Kolom buku adalah total pembuatan buku, termasuk biaya merge vanilla. Kolom pasang adalah biaya memasang buku hasil proses tersebut ke equipment baru. Harga ini contoh terukur pada satu urutan buku, bukan harga tetap untuk semua riwayat item.

| Fusion | Dua enchant minimum | Tambahan level | Total buku | Pasang | Efek default |
|---|---|---:|---:|---:|---|
| Infernal Edge | Sharpness 5 + Fire Aspect 2 | 3 | 7 | 10 | Serangan penuh membakar 3 detik; target yang sudah terbakar menerima +2 damage. |
| Blade Storm | Sharpness 5 + Sweeping Edge 3 | 3 | 8 | 12 | Serangan penuh memberi Slowness I selama 2 detik pada target dan maksimal 3 monster sekitar radius 3 blok. |
| Brutal Force | Sharpness 5 + Knockback 2 | 2 | 4 | 8 | Serangan penuh memberi +1 damage dan mendorong target menjauh. |
| Executioner | Sharpness 5 + Looting 3 | 4 | 10 | 12 | Serangan penuh pada target dengan HP paling banyak 25% memberi +4 damage. |
| Holy Flame | Smite 5 + Fire Aspect 2 | 3 | 7 | 10 | Serangan penuh pada undead memberi +3 damage dan efek api jiwa visual. |
| Soul Reaper | Smite 5 + Looting 3 | 3 | 9 | 12 | Membunuh undead memberi Regeneration I selama 4 detik. |
| Scorched Venom | Bane Of Arthropods 5 + Fire Aspect 2 | 3 | 8 | 10 | Serangan penuh pada arthropoda memberi Wither I selama 4 detik. |
| Flame Burst | Fire Aspect 2 + Knockback 2 | 2 | 6 | 7 | Serangan penuh pada target yang terbakar memberi Weakness I selama 3 detik. |
| Hellfire | Power 5 + Flame 1 | 3 | 5 | 8 | Panah tarikan penuh memberi +2 damage dan membakar target selama 5 detik. |
| Impact | Power 5 + Punch 2 | 2 | 7 | 10 | Panah tarikan penuh memberi Slowness II selama 2 detik. |
| Blast Arrow | Flame 1 + Punch 2 | 3 | 5 | 7 | Panah tarikan penuh memberi +1 damage dan Weakness I 3 detik pada target. |
| Volley | Multishot 1 + Quick Charge 3 | 3 | 5 | 6 | Salah satu hit dari tembakan Multishot memberi Slowness I 3 detik; penembak mendapat Speed I 3 detik. |
| Railshot | Piercing 4 + Quick Charge 3 | 4 | 8 | 8 | Hit pertama proyektil piercing memberi +3 damage. |
| Prospector | Efficiency 5 + Fortune 3 | 2 | 7 | 12 | Memecahkan ore memberi Night Vision selama 12 detik. |
| Excavator | Efficiency 5 + Silk Touch 1 | 3 | 8 | 10 | Menambang sambil sneak memberi Haste I selama 5 detik. |
| Workhorse | Efficiency 5 + Unbreaking 3 | 2 | 7 | 9 | Saat food level minimal 16, peluang 15% mencegah keausan alat. |
| Miner’s Luck | Fortune 3 + Unbreaking 3 | 2 | 8 | 10 | Saat menambang ore, peluang 20% mencegah keausan alat. |
| Preservation | Silk Touch 1 + Unbreaking 3 | 2 | 6 | 8 | Saat sneak, peluang 20% mencegah keausan alat. |
| Prosperity | Fortune 3 + Mending 1 | 3 | 9 | 9 | Mengalihkan maksimal 2 XP dari ore untuk memperbaiki hingga 4 durability alat. |
| Retribution | Protection 4 + Thorns 3 | 3 | 7 | 17 | Penyerang melee mendapat Weakness I selama 3 detik. |
| Molten Guard | Fire Protection 4 + Thorns 3 | 3 | 7 | 17 | Penyerang melee dibakar selama 3 detik. |
| Reactive Armor | Blast Protection 4 + Thorns 3 | 3 | 11 | 21 | Setelah terkena ledakan, mendapat Resistance I selama 3 detik. |
| Deflection | Projectile Protection 4 + Thorns 3 | 2 | 6 | 17 | Setelah terkena proyektil, mendapat Speed I selama 3 detik. |
| Fortified | Protection 4 + Unbreaking 3 | 2 | 6 | 8 | Peluang 10% mencegah keausan bagian armor ini. |
| Bloodforged | Thorns 3 + Mending 1 | 3 | 5 | 15 | Kill melee langsung terhadap monster memperbaiki hingga 4 durability pada satu bagian armor Bloodforged yang paling rusak. |
| Amphibious | Feather Falling 4 + Depth Strider 3 | 2 | 8 | 11 | Keluar dari air ke darat memberi Speed I selama 4 detik. |
| Glacial Step | Feather Falling 4 + Frost Walker 2 | 2 | 6 | 9 | Mendarat setelah jatuh minimal 3 blok memberi Speed I selama 3 detik. |
| Soulbound Steps | Feather Falling 4 + Soul Speed 3 | 2 | 6 | 17 | Saat berdiri di soul sand atau soul soil, mendapat Jump Boost I singkat. |
| Stormcaller | Loyalty 3 + Channeling 1 | 4 | 8 | 8 | Lemparan yang mengenai target saat hujan/badai memberi +2 damage dan kilat visual. |
| Tempest | Impaling 5 + Channeling 1 | 4 | 8 | 15 | Lemparan yang mengenai target saat badai memberi Slowness II selama 3 detik. |
| Wave Rider | Riptide 3 + Unbreaking 3 | 3 | 9 | 10 | Mengaktifkan Riptide memberi Dolphin’s Grace selama 4 detik. |
| Meteor | Density 5 + Wind Burst 1 | 5 | 10 | 8 | Smash dari ketinggian minimal 3 blok memberi +3 damage dan partikel impact. |
| Shockwave | Breach 4 + Wind Burst 1 | 5 | 13 | 11 | Smash dari ketinggian minimal 3 blok memberi Weakness I 3 detik pada target. |
| Comet | Density 5 + Fire Aspect 2 | 4 | 9 | 10 | Smash dari ketinggian minimal 3 blok membakar target 5 detik dan memberi +2 damage. |
| Master Angler | Luck Of The Sea 3 + Lure 3 | 2 | 8 | 13 | Peluang 20% mencegah keausan rod saat mendapat tangkapan. |
| Aquanaut | Respiration 3 + Aqua Affinity 1 | 2 | 4 | 9 | Saat berada di dalam air, mendapat Night Vision singkat. |

## Yang masih perlu playtest

Pilihan antara utility (Night Vision, mobility) dan efisiensi durability dipengaruhi kebiasaan pemain. Ekonomi XP farm, jenis mob, plugin combat, dan PvP server dapat mengubah nilai relatif tiap fusion. Evaluasi baseline ini pada server tujuan sebelum mengubah chance/power; tes otomatis membuktikan aturan dan angka bekerja, bukan preferensi atau meta komunitas.
