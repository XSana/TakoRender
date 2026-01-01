package moe.takochan.takorender.api.component;

import moe.takochan.takorender.api.ecs.Component;

/**
 * Component that represents a camera for rendering.
 */
public class CameraComponent extends Component {

    // Field of view in degrees
    public float fov = 70.0f;

    // Near and far clipping planes
    public float nearPlane = 0.05f;
    public float farPlane = 1000.0f;

    // Viewport dimensions (0 = use window dimensions)
    public int viewportWidth = 0;
    public int viewportHeight = 0;

    // Render priority (lower values render first)
    public int priority = 0;

    // Whether this camera is currently active
    public boolean active = true;

    public CameraComponent() {}

    public CameraComponent(float fov) {
        this.fov = fov;
    }

    public CameraComponent(float fov, float nearPlane, float farPlane) {
        this.fov = fov;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
    }
}
