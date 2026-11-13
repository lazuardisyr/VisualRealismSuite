package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Kamera orbit sederhana (drag mouse = putar, scroll = zoom). */
public class Camera {
    private float yaw = 45f;      // derajat
    private float pitch = 28f;    // derajat
    private float distance = 7.5f;

    public void rotate(float dx, float dy) {
        yaw -= dx * 0.3f;
        pitch += dy * 0.3f;
        pitch = Math.max(3f, Math.min(88f, pitch)); // tetap di atas lantai
    }

    public void zoom(float amount) {
        distance -= amount * 0.5f;
        distance = Math.max(2.5f, Math.min(14f, distance));
    }

    public Vector3f getPosition() {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vector3f(
                (float) (distance * Math.cos(p) * Math.cos(y)),
                (float) (distance * Math.sin(p)),
                (float) (distance * Math.cos(p) * Math.sin(y)));
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
    }
}
