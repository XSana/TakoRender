package moe.takochan.takorender.api.ecs;

/**
 * ECS Component 基类
 *
 * <p>
 * Component 是 ECS 架构中的纯数据对象，只保存数据，不包含任何逻辑。
 * 所有业务逻辑都在 {@link GameSystem} 中处理。
 * </p>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     public class TransformComponent extends Component {
 * 
 *         private float x, y, z;
 *
 *         public float getX() {
 *             return x;
 *         }
 * 
 *         public void setX(float x) {
 *             this.x = x;
 *         }
 *     }
 * }
 * </pre>
 */
public abstract class Component {

    private Entity entity;

    /**
     * 获取此 Component 所属的 Entity。
     *
     * @return 所属的 Entity，如果尚未添加到实体则返回 null
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * 设置此 Component 所属的 Entity（内部方法）。
     *
     * <p>
     * 由 {@link Entity#addComponent(Component)} 自动调用，不应手动调用。
     * </p>
     *
     * @param entity 要关联的实体
     */
    void setEntity(Entity entity) {
        this.entity = entity;
    }
}
