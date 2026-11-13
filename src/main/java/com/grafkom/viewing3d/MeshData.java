package com.grafkom.viewing3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Data geometri di sisi CPU: posisi + koordinat UV + daftar segitiga (CCW dilihat dari luar).
 * Vektor normal TIDAK disimpan di sini: Mesh menghitungnya sendiri
 *  - Flat   : normal tunggal per face
 *  - Smooth : rata-rata normal semua face yang berbagi posisi vertex yang sama
 */
public class MeshData {
    public final List<float[]> positions = new ArrayList<>(); // {x, y, z}
    public final List<float[]> uvs = new ArrayList<>();       // {u, v}
    public final List<int[]> triangles = new ArrayList<>();   // {i0, i1, i2}

    public int addVertex(float x, float y, float z, float u, float v) {
        positions.add(new float[]{x, y, z});
        uvs.add(new float[]{u, v});
        return positions.size() - 1;
    }

    public void addTriangle(int a, int b, int c) {
        triangles.add(new int[]{a, b, c});
    }

    /** Quad CCW (p0..p3 berurutan berlawanan jarum jam dari luar), UV 0..uMax / 0..vMax. */
    private void addQuad(float[] p0, float[] p1, float[] p2, float[] p3, float uMax, float vMax) {
        int a = addVertex(p0[0], p0[1], p0[2], 0, 0);
        int b = addVertex(p1[0], p1[1], p1[2], uMax, 0);
        int c = addVertex(p2[0], p2[1], p2[2], uMax, vMax);
        int d = addVertex(p3[0], p3[1], p3[2], 0, vMax);
        addTriangle(a, b, c);
        addTriangle(a, c, d);
    }

    /** Segitiga CCW dengan UV (0,0) (1,0) (0.5,1). */
    private void addTri(float[] p0, float[] p1, float[] p2) {
        int a = addVertex(p0[0], p0[1], p0[2], 0f, 0f);
        int b = addVertex(p1[0], p1[1], p1[2], 1f, 0f);
        int c = addVertex(p2[0], p2[1], p2[2], 0.5f, 1f);
        addTriangle(a, b, c);
    }

    private static float[] p(float x, float y, float z) { return new float[]{x, y, z}; }

    // ------------------------------------------------------------------
    // Sphere
    // ------------------------------------------------------------------
    public static MeshData createSphere(float radius, int stacks, int slices) {
        MeshData m = new MeshData();
        for (int i = 0; i <= stacks; i++) {
            double theta = Math.PI * i / stacks;           // 0 (kutub atas) .. PI (kutub bawah)
            float y = (float) Math.cos(theta);
            float r = (float) Math.sin(theta);
            for (int j = 0; j <= slices; j++) {
                double phi = 2.0 * Math.PI * j / slices;
                float x = r * (float) Math.cos(phi);
                float z = r * (float) Math.sin(phi);
                m.addVertex(radius * x, radius * y, radius * z,
                        (float) j / slices, 1f - (float) i / stacks);
            }
        }
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int a = i * (slices + 1) + j;
                int b = a + slices + 1;
                m.addTriangle(a, a + 1, b);
                m.addTriangle(a + 1, b + 1, b);
            }
        }
        return m;
    }

    // ------------------------------------------------------------------
    // Torus
    // ------------------------------------------------------------------
    public static MeshData createTorus(float majorR, float minorR, int rings, int sides) {
        MeshData m = new MeshData();
        for (int i = 0; i <= rings; i++) {
            double a = 2.0 * Math.PI * i / rings;
            for (int j = 0; j <= sides; j++) {
                double b = 2.0 * Math.PI * j / sides;
                float x = (float) ((majorR + minorR * Math.cos(b)) * Math.cos(a));
                float y = (float) (minorR * Math.sin(b));
                float z = (float) ((majorR + minorR * Math.cos(b)) * Math.sin(a));
                m.addVertex(x, y, z, 2f * i / rings, (float) j / sides);
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < sides; j++) {
                int a00 = i * (sides + 1) + j;
                int a01 = a00 + 1;
                int a10 = a00 + sides + 1;
                int a11 = a10 + 1;
                m.addTriangle(a00, a01, a10);
                m.addTriangle(a10, a01, a11);
            }
        }
        return m;
    }

    // ------------------------------------------------------------------
    // Rumah 3D: badan kotak + atap pelana (prisma)
    // ------------------------------------------------------------------
    public static MeshData createHouse() {
        MeshData m = new MeshData();
        float x0 = -0.7f, x1 = 0.7f;   // lebar
        float y0 = -0.6f, y1 = 0.2f;   // tinggi badan
        float z0 = -0.5f, z1 = 0.5f;   // kedalaman
        float ry = 0.8f;               // tinggi bubungan atap (di z = 0)

        // Dinding (CCW dilihat dari luar)
        m.addQuad(p(x0, y0, z1), p(x1, y0, z1), p(x1, y1, z1), p(x0, y1, z1), 1, 1); // depan  (+z)
        m.addQuad(p(x1, y0, z0), p(x0, y0, z0), p(x0, y1, z0), p(x1, y1, z0), 1, 1); // belakang (-z)
        m.addQuad(p(x1, y0, z1), p(x1, y0, z0), p(x1, y1, z0), p(x1, y1, z1), 1, 1); // kanan (+x)
        m.addQuad(p(x0, y0, z0), p(x0, y0, z1), p(x0, y1, z1), p(x0, y1, z0), 1, 1); // kiri (-x)
        m.addQuad(p(x0, y0, z0), p(x1, y0, z0), p(x1, y0, z1), p(x0, y0, z1), 1, 1); // alas (-y)

        // Atap: dua bidang miring
        m.addQuad(p(x0, y1, z1), p(x1, y1, z1), p(x1, ry, 0), p(x0, ry, 0), 2, 1);   // atap depan
        m.addQuad(p(x1, y1, z0), p(x0, y1, z0), p(x0, ry, 0), p(x1, ry, 0), 2, 1);   // atap belakang

        // Dinding segitiga (gable)
        m.addTri(p(x1, y1, z1), p(x1, y1, z0), p(x1, ry, 0));                        // kanan
        m.addTri(p(x0, y1, z0), p(x0, y1, z1), p(x0, ry, 0));                        // kiri
        return m;
    }

    // ------------------------------------------------------------------
    // Ground plane (menghadap ke atas +Y), UV di-tile 'tiles' kali
    // ------------------------------------------------------------------
    public static MeshData createGround(float half, float y, float tiles) {
        MeshData m = new MeshData();
        m.addQuad(p(-half, y, half), p(half, y, half), p(half, y, -half), p(-half, y, -half), tiles, tiles);
        return m;
    }
}
