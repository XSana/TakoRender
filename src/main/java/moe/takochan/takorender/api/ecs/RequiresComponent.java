package moe.takochan.takorender.api.ecs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明 System 依赖的 Component 类型
 *
 * <p>
 * 此注解用于声明一个 GameSystem 需要处理的 Component 类型。
 * 它提供了自动查询实体的便利方法。
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
 *     &#64;RequiresComponent({ TransformComponent.class, MeshComponent.class })
 *     public class MeshRenderSystem extends GameSystem {
 *
 *         &#64;Override
 *         public void update(float deltaTime) {
 *             // 使用 getRequiredEntities() 自动查询拥有所需 Component 的实体
 *             for (Entity entity : getRequiredEntities()) {
 *                 TransformComponent transform = entity.getComponent(TransformComponent.class)
 *                     .get();
 *                 MeshComponent mesh = entity.getComponent(MeshComponent.class)
 *                     .get();
 *                 // 渲染逻辑
 *             }
 *         }
 *     }
 * }
 * </pre>
 *
 * @see GameSystem#getRequiredEntities()
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresComponent {

    /**
     * 系统依赖的 Component 类型数组
     *
     * @return Component 类型数组
     */
    Class<? extends Component>[] value();
}
