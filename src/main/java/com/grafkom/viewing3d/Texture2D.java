package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

/**
 * Praktikum 3: Texture loader (STBImage) + UV mapping + texture filtering.
 * Gambar dibaca dari classpath (src/main/resources/textures/...).
 * Jika gagal, dipakai tekstur cadangan (checkerboard magenta-hitam).
 */
public class Texture2D {
    private final int textureID;

    public Texture2D(String resourcePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Texture Filtering & Wrap Parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        stbi_set_flip_vertically_on_load(true); // v=0 di bawah gambar (konvensi OpenGL)

        boolean loaded = false;
        ByteBuffer fileData = readResource(resourcePath);
        if (fileData != null) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                ByteBuffer data = stbi_load_from_memory(fileData, width, height, channels, 4);
                if (data != null) {
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                            GL_RGBA, GL_UNSIGNED_BYTE, data);
                    stbi_image_free(data);
                    loaded = true;
                } else {
                    System.err.println("Failed to decode texture: " + resourcePath
                            + " (" + stbi_failure_reason() + ")");
                }
            }
            MemoryUtil.memFree(fileData);
        } else {
            System.err.println("Failed to load texture: " + resourcePath);
        }

        if (!loaded) {
            uploadFallback();
        }
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    /** true = Bilinear + Mipmapping (trilinear), false = Nearest Neighbor. */
    public void setFilter(boolean mipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (mipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void dispose() {
        glDeleteTextures(textureID);
    }

    private static ByteBuffer readResource(String path) {
        try (InputStream in = Texture2D.class.getResourceAsStream(path)) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes);
            buf.flip();
            return buf;
        } catch (IOException e) {
            return null;
        }
    }

    private void uploadFallback() {
        final int size = 64;
        ByteBuffer px = MemoryUtil.memAlloc(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean on = ((x / 8) + (y / 8)) % 2 == 0;
                px.put((byte) (on ? 255 : 0)).put((byte) 0).put((byte) (on ? 255 : 0)).put((byte) 255);
            }
        }
        px.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, px);
        MemoryUtil.memFree(px);
    }
}
