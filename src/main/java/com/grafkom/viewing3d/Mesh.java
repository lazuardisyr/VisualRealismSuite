package com.grafkom.viewing3d;

import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh di GPU. Dari satu MeshData dibuat DUA VAO:
 *  - flatVao   : tiap segitiga punya 3 vertex dengan normal face yang sama (Flat Shading)
 *  - smoothVao : normal vertex = rata-rata normal face yang berbagi posisi yang sama
 *                N_vertex = sum(N_face_i) / ||sum(N_face_i)||  (Smooth Shading)
 * Layout vertex: pos(3) | normal(3) | uv(2).
 */
public class Mesh {

    private record Key(int x, int y, int z) {}

    private static Key key(float[] p) {
        return new Key(Math.round(p[0] * 10000f), Math.round(p[1] * 10000f), Math.round(p[2] * 10000f));
    }

    private final int flatVao, flatVbo, smoothVao, smoothVbo;
    private final int vertexCount;

    public Mesh(MeshData data) {
        List<int[]> tris = new ArrayList<>();
        List<Vector3f> faceNormals = new ArrayList<>();
        Map<Key, Vector3f> accum = new HashMap<>();

        // Pass 1: normal tiap face + akumulasi per posisi vertex
        for (int[] t : data.triangles) {
            float[] a = data.positions.get(t[0]);
            float[] b = data.positions.get(t[1]);
            float[] c = data.positions.get(t[2]);
            Vector3f e1 = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
            Vector3f e2 = new Vector3f(c[0] - a[0], c[1] - a[1], c[2] - a[2]);
            Vector3f n = e1.cross(e2, new Vector3f());
            if (n.lengthSquared() < 1e-12f) continue; // segitiga degenerate (kutub bola)
            n.normalize();
            tris.add(t);
            faceNormals.add(n);
            for (int idx : t) {
                Key k = key(data.positions.get(idx));
                Vector3f sum = accum.get(k);
                if (sum == null) {
                    sum = new Vector3f();
                    accum.put(k, sum);
                }
                sum.add(n);
            }
        }

        vertexCount = tris.size() * 3;
        FloatBuffer flatBuf = BufferUtils.createFloatBuffer(vertexCount * 8);
        FloatBuffer smoothBuf = BufferUtils.createFloatBuffer(vertexCount * 8);

        // Pass 2: isi buffer
        for (int i = 0; i < tris.size(); i++) {
            int[] t = tris.get(i);
            Vector3f fn = faceNormals.get(i);
            for (int idx : t) {
                float[] pos = data.positions.get(idx);
                float[] uv = data.uvs.get(idx);

                Vector3f sn = new Vector3f(accum.get(key(pos)));
                if (sn.lengthSquared() < 1e-12f) sn.set(fn); else sn.normalize();

                put(flatBuf, pos, fn, uv);
                put(smoothBuf, pos, sn, uv);
            }
        }
        flatBuf.flip();
        smoothBuf.flip();

        int[] f = upload(flatBuf);
        int[] s = upload(smoothBuf);
        flatVao = f[0];
        flatVbo = f[1];
        smoothVao = s[0];
        smoothVbo = s[1];
    }

    private static void put(FloatBuffer fb, float[] pos, Vector3f n, float[] uv) {
        fb.put(pos[0]).put(pos[1]).put(pos[2]);
        fb.put(n.x).put(n.y).put(n.z);
        fb.put(uv[0]).put(uv[1]);
    }

    private static int[] upload(FloatBuffer buf) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
        return new int[]{vao, vbo};
    }

    public void draw(boolean smooth) {
        glBindVertexArray(smooth ? smoothVao : flatVao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void dispose() {
        glDeleteBuffers(flatVbo);
        glDeleteBuffers(smoothVbo);
        glDeleteVertexArrays(flatVao);
        glDeleteVertexArrays(smoothVao);
    }
}
