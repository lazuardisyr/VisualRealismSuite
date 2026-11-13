# 3D Visual Realism Suite (Grafika Komputer - Minggu 11)
Java 17+ | LWJGL 3.3.3 | JOML | OpenGL 3.3 core (GLSL 330)

## Cara menjalankan di Apache NetBeans
1. File > Open Project > pilih folder `VisualRealismSuite` (ikon Maven).
2. Pastikan NetBeans memakai JDK 17+ (Tools > Java Platforms).
3. Klik kanan project > Build (pertama kali butuh INTERNET untuk unduh LWJGL & JOML).
4. Tekan F6 / Run Project. Main class: com.grafkom.viewing3d.Main

macOS: tambahkan VM option -XstartOnFirstThread.

## Kontrol
B Phong/Blinn-Phong | F Flat/Smooth | Z Z-Buffer | C Back-Face Culling
T Tekstur | M Nearest/Bilinear+Mipmap | H Shadow Mapping
1/2/3 Sphere/Torus/Rumah | R putar objek | L putar cahaya | V VSync
Drag kiri = orbit, scroll = zoom, ESC = keluar. Status & FPS tampil di title bar.

Ganti tekstur: timpa src/main/resources/textures/brick.png dengan gambar sendiri.
