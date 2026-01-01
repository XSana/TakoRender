package moe.takochan.takorender.api.component;

import moe.takochan.takorender.api.ecs.Component;

/**
 * Component that holds position, rotation, and scale data.
 */
public class TransformComponent extends Component {

    // Position
    public double x;
    public double y;
    public double z;

    // Rotation (in degrees)
    public float pitch;
    public float yaw;
    public float roll;

    // Scale
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;
    public float scaleZ = 1.0f;

    public TransformComponent() {
        this(0, 0, 0);
    }

    public TransformComponent(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setRotation(float pitch, float yaw, float roll) {
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }

    public void setScale(float scale) {
        this.scaleX = scale;
        this.scaleY = scale;
        this.scaleZ = scale;
    }

    public void setScale(float scaleX, float scaleY, float scaleZ) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }
}
