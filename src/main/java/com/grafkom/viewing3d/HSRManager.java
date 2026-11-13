package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/** Praktikum 2: konfigurasi Hidden Surface Removal (Z-Buffer & Back-Face Culling). */
public class HSRManager {

    public static void setupDepthTest(boolean enable) {
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS); // Pixel lebih dekat lolos uji depth
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    public static void setupBackFaceCulling(boolean enable) {
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);   // Buang polygon belakang
            glFrontFace(GL_CCW);   // Counter-Clockwise adalah tampak depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }
}
