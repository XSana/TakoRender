package moe.takochan.takorender.api.system;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;

/**
 * 变换系统 - 负责更新所有 TransformComponent 的矩阵和方向向量
 *
 * <p>
 * TransformSystem 在 UPDATE 阶段最先执行，确保其他系统可以使用最新的变换数据。
 * </p>
 *
 * <p>
 * <b>职责</b>:
 * </p>
 * <ul>
 * <li>计算世界变换矩阵（Translation * Rotation * Scale）</li>
 * <li>计算方向向量（forward, up, right）</li>
 * <li>清除脏标记</li>
 * </ul>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     World world = new World();
 *     world.addSystem(new TransformSystem()); // 最先添加
 *     world.addSystem(new CameraSystem());
 *     world.addSystem(new MeshRenderSystem());
 * }
 * </pre>
 */
@RequiresComponent(TransformComponent.class)
public class TransformSystem extends GameSystem {

    private final Quaternionf tempQuaternion = new Quaternionf();

    @Override
    public Phase getPhase() {
        return Phase.UPDATE;
    }

    @Override
    public int getPriority() {
        return -1000;
    }

    @Override
    public void update(float deltaTime) {
        for (Entity entity : getRequiredEntities()) {
            TransformComponent transform = entity.getComponent(TransformComponent.class)
                .orElse(null);

            if (transform == null) {
                continue;
            }

            if (transform.isDirty()) {
                updateMatrices(transform);
                transform.clearDirty();
            }
        }
    }

    /**
     * 更新变换矩阵和方向向量
     */
    private void updateMatrices(TransformComponent transform) {
        Vector3f position = transform.getPositionRef();
        Vector3f rotation = transform.getRotationRef();
        Vector3f scale = transform.getScaleRef();
        Matrix4f worldMatrix = transform.getWorldMatrix();

        worldMatrix.identity()
            .translate(position)
            .rotateY((float) Math.toRadians(rotation.y))
            .rotateX((float) Math.toRadians(rotation.x))
            .rotateZ((float) Math.toRadians(rotation.z))
            .scale(scale);

        tempQuaternion.identity()
            .rotateY((float) Math.toRadians(rotation.y))
            .rotateX((float) Math.toRadians(rotation.x))
            .rotateZ((float) Math.toRadians(rotation.z));

        Vector3f forward = transform.getForward();
        forward.set(0, 0, -1);
        tempQuaternion.transform(forward);

        Vector3f up = transform.getUp();
        up.set(0, 1, 0);
        tempQuaternion.transform(up);

        Vector3f right = transform.getRight();
        right.set(1, 0, 0);
        tempQuaternion.transform(right);
    }
}
