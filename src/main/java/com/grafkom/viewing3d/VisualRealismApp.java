package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite (Minggu 11).
 *
 * KONTROL
 *   B : Phong <-> Blinn-Phong        (Indikator 1)
 *   F : Flat <-> Smooth shading      (Indikator 2)
 *   Z : Z-Buffer (Depth Test) ON/OFF (Indikator 3)
 *   C : Back-Face Culling ON/OFF     (Indikator 3)
 *   T : Texture ON/OFF               (Indikator 4)
 *   M : Filtering Nearest <-> Bilinear+Mipmap (Indikator 4)
 *   H : Shadow Mapping ON/OFF        (Indikator 5)
 *   1/2/3 : Sphere / Torus / Rumah 3D
 *   R : putar objek otomatis    L : putar cahaya otomatis    V : VSync
 *   Mouse drag kiri : orbit kamera, Scroll : zoom, ESC : keluar
 */
public class VisualRealismApp {

    private static final int WIDTH = 1280, HEIGHT = 720;

    private long window;
    private final Camera camera = new Camera();

    // State fitur
    private boolean blinnPhong = true;
    private boolean smooth = true;
    private boolean depthTest = true;
    private boolean cullFace = true;
    private boolean useTexture = true;
    private boolean mipmapFilter = true;
    private boolean shadows = true;
    private boolean autoRotate = true;
    private boolean lightOrbit = false;
    private boolean vsync = true;
    private int meshIndex = 0;

    private float objectAngle = 0f;
    private float lightAngle = 0.6f;
    private final Vector3f lightPos = new Vector3f();

    // Resource GPU
    private ShaderProgram mainShader, depthShader;
    private ShadowMapFBO shadowFbo;
    private Texture2D brickTex, groundTex;
    private Mesh[] meshes;
    private final String[] meshNames = {"Sphere", "Torus", "Rumah 3D"};
    private Mesh ground;

    // Input & FPS
    private boolean dragging = false;
    private double lastX, lastY;
    private int frames = 0;
    private float fpsTimer = 0f, fps = 0f;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // diperlukan di macOS
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(WIDTH, HEIGHT, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new IllegalStateException("Gagal membuat window (butuh OpenGL 3.3+)");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
        glEnable(GL_MULTISAMPLE);

        setupCallbacks();

        mainShader = new ShaderProgram("phong.vert", "phong.frag");
        depthShader = new ShaderProgram("depth.vert", "depth.frag");
        shadowFbo = new ShadowMapFBO();
        brickTex = new Texture2D("/textures/brick.png");
        groundTex = new Texture2D("/textures/checker.png");

        meshes = new Mesh[]{
                new Mesh(MeshData.createSphere(0.8f, 32, 48)),
                new Mesh(MeshData.createTorus(0.7f, 0.3f, 48, 24)),
                new Mesh(MeshData.createHouse())
        };
        ground = new Mesh(MeshData.createGround(8f, -1.0f, 6f));

        updateLight();
        updateTitle();
        printHelp();
    }

    private void setupCallbacks() {
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS) return;
            switch (key) {
                case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(win, true);
                case GLFW_KEY_B -> blinnPhong = !blinnPhong;
                case GLFW_KEY_F -> smooth = !smooth;
                case GLFW_KEY_Z -> depthTest = !depthTest;
                case GLFW_KEY_C -> cullFace = !cullFace;
                case GLFW_KEY_T -> useTexture = !useTexture;
                case GLFW_KEY_M -> {
                    mipmapFilter = !mipmapFilter;
                    brickTex.setFilter(mipmapFilter);
                    groundTex.setFilter(mipmapFilter);
                }
                case GLFW_KEY_H -> shadows = !shadows;
                case GLFW_KEY_R -> autoRotate = !autoRotate;
                case GLFW_KEY_L -> lightOrbit = !lightOrbit;
                case GLFW_KEY_V -> {
                    vsync = !vsync;
                    glfwSwapInterval(vsync ? 1 : 0);
                }
                case GLFW_KEY_1 -> meshIndex = 0;
                case GLFW_KEY_2 -> meshIndex = 1;
                case GLFW_KEY_3 -> meshIndex = 2;
                default -> { return; }
            }
            updateTitle();
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) dragging = (action == GLFW_PRESS);
        });

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (dragging) camera.rotate((float) (x - lastX), (float) (y - lastY));
            lastX = x;
            lastY = y;
        });

        glfwSetScrollCallback(window, (win, xoff, yoff) -> camera.zoom((float) yoff));
    }

    private void loop() {
        double last = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            update(dt);
            render();

            glfwSwapBuffers(window);
            glfwPollEvents();

            frames++;
            fpsTimer += dt;
            if (fpsTimer >= 0.5f) {
                fps = frames / fpsTimer;
                frames = 0;
                fpsTimer = 0f;
                updateTitle();
            }
        }
    }

    private void update(float dt) {
        if (autoRotate) objectAngle += dt * 0.6f;
        if (lightOrbit) lightAngle += dt * 0.8f;
        updateLight();
    }

    private void updateLight() {
        lightPos.set(5f * (float) Math.cos(lightAngle), 6f, 5f * (float) Math.sin(lightAngle));
    }

    private void render() {
        int[] w = new int[1], h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        int fbW = w[0], fbH = h[0];
        if (fbW == 0 || fbH == 0) return; // window di-minimize

        Matrix4f objectModel = new Matrix4f().rotateY(objectAngle);
        Matrix4f identity = new Matrix4f();

        // Matriks ruang cahaya (Directional light => proyeksi ortografik)
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        Matrix4f lightProj = new Matrix4f().ortho(-9f, 9f, -9f, 9f, 1f, 25f);
        Matrix4f lightSpace = new Matrix4f(lightProj).mul(lightView);

        // ---------- PASS 1: Depth pass dari sudut pandang cahaya ----------
        if (shadows) {
            shadowFbo.bindForWriting();
            HSRManager.setupDepthTest(true);
            HSRManager.setupBackFaceCulling(false);
            depthShader.use();
            depthShader.setMat4("lightSpaceMatrix", lightSpace);
            depthShader.setMat4("model", objectModel);
            meshes[meshIndex].draw(smooth);
            shadowFbo.unbind(fbW, fbH);
        }

        // ---------- PASS 2: Render normal dari sudut pandang kamera ----------
        glViewport(0, 0, fbW, fbH);
        glClearColor(0.55f, 0.70f, 0.88f, 1f);
        glDepthMask(true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(cullFace);

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0),
                (float) fbW / fbH, 0.1f, 50f);

        mainShader.use();
        mainShader.setMat4("view", camera.getViewMatrix());
        mainShader.setMat4("projection", projection);
        mainShader.setMat4("lightSpaceMatrix", lightSpace);
        mainShader.setVec3("lightPos", lightPos);
        mainShader.setVec3("viewPos", camera.getPosition());
        mainShader.setVec3("lightColor", 1f, 1f, 1f);
        mainShader.setBool("useBlinnPhong", blinnPhong);
        mainShader.setBool("useShadow", shadows);
        mainShader.setBool("useTexture", useTexture);
        mainShader.setInt("textureSampler", 0);
        mainShader.setInt("shadowMap", 1);

        // Shadow map di texture unit 1
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, shadowFbo.getDepthTextureID());

        // Lantai (penerima bayangan)
        groundTex.bind(0);
        mainShader.setMat4("model", identity);
        mainShader.setVec3("objectColor", 0.72f, 0.72f, 0.72f);
        mainShader.setFloat("specularStrength", 0.1f);
        ground.draw(false);

        // Objek utama
        brickTex.bind(0);
        mainShader.setMat4("model", objectModel);
        mainShader.setVec3("objectColor", 0.85f, 0.45f, 0.30f);
        mainShader.setFloat("specularStrength", 0.8f);
        meshes[meshIndex].draw(smooth);

        glActiveTexture(GL_TEXTURE0);
    }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }

    private void updateTitle() {
        String title = String.format(
                "3D Visual Realism Suite | FPS %.0f | %s | %s | Z-Buffer:%s | Cull:%s | Tekstur:%s (%s) | Bayangan:%s | %s",
                fps,
                blinnPhong ? "Blinn-Phong" : "Phong",
                smooth ? "Smooth" : "Flat",
                onOff(depthTest), onOff(cullFace),
                onOff(useTexture), mipmapFilter ? "Bilinear+Mipmap" : "Nearest",
                onOff(shadows),
                meshNames[meshIndex]);
        glfwSetWindowTitle(window, title);
    }

    private void printHelp() {
        System.out.println("=== 3D Visual Realism Suite ===");
        System.out.println("B : Phong <-> Blinn-Phong");
        System.out.println("F : Flat <-> Smooth shading");
        System.out.println("Z : Z-Buffer ON/OFF      C : Back-Face Culling ON/OFF");
        System.out.println("T : Tekstur ON/OFF       M : Nearest <-> Bilinear+Mipmap");
        System.out.println("H : Shadow Mapping ON/OFF");
        System.out.println("1/2/3 : Sphere / Torus / Rumah 3D");
        System.out.println("R : putar objek   L : putar cahaya   V : VSync");
        System.out.println("Mouse drag kiri : orbit kamera   Scroll : zoom   ESC : keluar");
    }

    private void cleanup() {
        for (Mesh m : meshes) m.dispose();
        ground.dispose();
        brickTex.dispose();
        groundTex.dispose();
        shadowFbo.dispose();
        mainShader.dispose();
        depthShader.dispose();
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
