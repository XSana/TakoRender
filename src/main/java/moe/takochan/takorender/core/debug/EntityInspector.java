package moe.takochan.takorender.core.debug;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import moe.takochan.takorender.api.component.LayerComponent;
import moe.takochan.takorender.api.component.VisibilityComponent;
import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.Layer;
import moe.takochan.takorender.api.ecs.World;

/**
 * Entity 检查器
 *
 * <p>
 * EntityInspector 用于调试时查看 Entity 和 Component 数据。
 * 支持反射获取 Component 字段值。
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
 *     EntityInspector inspector = new EntityInspector(world);
 *
 *     // 获取所有实体摘要
 *     for (EntityInfo info : inspector.getAllEntities()) {
 *         System.out.println(info.getId() + ": " + info.getComponentCount() + " components");
 *     }
 *
 *     // 获取实体详情
 *     EntityDetail detail = inspector.getEntityDetail(entity);
 *     for (ComponentInfo comp : detail.getComponents()) {
 *         System.out.println(comp.getTypeName());
 *         for (var entry : comp.getFields()
 *             .entrySet()) {
 *             System.out.println("  " + entry.getKey() + " = " + entry.getValue());
 *         }
 *     }
 * }
 * </pre>
 */
public class EntityInspector {

    private final World world;

    /**
     * 创建检查器
     *
     * @param world ECS World
     */
    public EntityInspector(World world) {
        this.world = world;
    }

    /**
     * Entity 摘要信息
     */
    public static class EntityInfo {

        private final long id;
        private final Layer layer;
        private final boolean visible;
        private final boolean active;
        private final int componentCount;

        EntityInfo(Entity entity) {
            this.id = entity.getId();
            this.active = entity.isActive();
            this.componentCount = entity.getComponentTypes()
                .size();

            // 获取 Layer
            this.layer = entity.getComponent(LayerComponent.class)
                .map(LayerComponent::getLayer)
                .orElse(Layer.WORLD_3D);

            // 获取可见性
            this.visible = entity.getComponent(VisibilityComponent.class)
                .map(VisibilityComponent::shouldRender)
                .orElse(true);
        }

        public long getId() {
            return id;
        }

        public Layer getLayer() {
            return layer;
        }

        public boolean isVisible() {
            return visible;
        }

        public boolean isActive() {
            return active;
        }

        public int getComponentCount() {
            return componentCount;
        }
    }

    /**
     * Component 信息
     */
    public static class ComponentInfo {

        private final String typeName;
        private final Map<String, Object> fields;

        ComponentInfo(Component component) {
            this.typeName = component.getClass()
                .getSimpleName();
            this.fields = extractFields(component);
        }

        private static Map<String, Object> extractFields(Component component) {
            Map<String, Object> result = new LinkedHashMap<>();
            Class<?> clazz = component.getClass();

            // 遍历所有字段（包括父类）
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(component);
                        result.put(field.getName(), formatValue(value));
                    } catch (Exception e) {
                        result.put(field.getName(), "<error: " + e.getMessage() + ">");
                    }
                }
                clazz = clazz.getSuperclass();
            }

            return result;
        }

        private static Object formatValue(Object value) {
            if (value == null) {
                return "null";
            }

            // 基本类型和字符串直接返回
            if (value instanceof Number || value instanceof Boolean
                || value instanceof String
                || value instanceof Enum) {
                return value;
            }

            // 数组
            if (value.getClass()
                .isArray()) {
                return value.getClass()
                    .getComponentType()
                    .getSimpleName() + "["
                    + java.lang.reflect.Array.getLength(value)
                    + "]";
            }

            // 集合
            if (value instanceof java.util.Collection) {
                return value.getClass()
                    .getSimpleName() + "["
                    + ((java.util.Collection<?>) value).size()
                    + "]";
            }

            // 其他对象返回类型名
            return value.getClass()
                .getSimpleName() + "@"
                + Integer.toHexString(System.identityHashCode(value));
        }

        public String getTypeName() {
            return typeName;
        }

        public Map<String, Object> getFields() {
            return fields;
        }
    }

    /**
     * Entity 详细信息
     */
    public static class EntityDetail {

        private final EntityInfo info;
        private final List<ComponentInfo> components;

        EntityDetail(Entity entity) {
            this.info = new EntityInfo(entity);
            this.components = new ArrayList<>();

            for (Class<? extends Component> type : entity.getComponentTypes()) {
                entity.getComponent(type)
                    .ifPresent(c -> components.add(new ComponentInfo(c)));
            }
        }

        public EntityInfo getInfo() {
            return info;
        }

        public List<ComponentInfo> getComponents() {
            return components;
        }
    }

    /**
     * 获取所有实体摘要
     */
    public List<EntityInfo> getAllEntities() {
        List<EntityInfo> result = new ArrayList<>();
        for (Entity entity : world.getEntities()) {
            result.add(new EntityInfo(entity));
        }
        return result;
    }

    /**
     * 获取指定层的实体
     */
    public List<EntityInfo> getEntitiesByLayer(Layer layer) {
        List<EntityInfo> result = new ArrayList<>();
        for (Entity entity : world.getEntitiesWith(LayerComponent.class)) {
            Layer entityLayer = entity.getComponent(LayerComponent.class)
                .map(LayerComponent::getLayer)
                .orElse(Layer.WORLD_3D);
            if (entityLayer == layer) {
                result.add(new EntityInfo(entity));
            }
        }
        return result;
    }

    /**
     * 获取实体详情
     */
    public EntityDetail getEntityDetail(Entity entity) {
        return new EntityDetail(entity);
    }

    /**
     * 获取实体详情（通过 ID）
     */
    public EntityDetail getEntityDetail(long entityId) {
        return world.getEntity(entityId)
            .map(EntityDetail::new)
            .orElse(null);
    }

    /**
     * 生成文本报告
     */
    public String getReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Entity Inspector Report ===\n");
        sb.append(String.format("Total Entities: %d\n\n", world.getEntityCount()));

        List<EntityInfo> entities = getAllEntities();

        sb.append(String.format("%-10s %-12s %-8s %-8s %-10s\n", "ID", "Layer", "Visible", "Active", "Components"));
        sb.append(String.format("%-10s %-12s %-8s %-8s %-10s\n", "--", "-----", "-------", "------", "----------"));

        for (EntityInfo info : entities) {
            sb.append(
                String.format(
                    "%-10d %-12s %-8s %-8s %-10d\n",
                    info.getId(),
                    info.getLayer(),
                    info.isVisible() ? "yes" : "no",
                    info.isActive() ? "yes" : "no",
                    info.getComponentCount()));
        }

        return sb.toString();
    }

    /**
     * 生成实体详细报告
     */
    public String getDetailReport(Entity entity) {
        EntityDetail detail = getEntityDetail(entity);
        StringBuilder sb = new StringBuilder();

        sb.append(
            String.format(
                "=== Entity %d ===\n",
                detail.getInfo()
                    .getId()));
        sb.append(
            String.format(
                "Layer: %s\n",
                detail.getInfo()
                    .getLayer()));
        sb.append(
            String.format(
                "Visible: %s\n",
                detail.getInfo()
                    .isVisible()));
        sb.append(
            String.format(
                "Active: %s\n",
                detail.getInfo()
                    .isActive()));
        sb.append("\nComponents:\n");

        for (ComponentInfo comp : detail.getComponents()) {
            sb.append(String.format("  [%s]\n", comp.getTypeName()));
            for (Map.Entry<String, Object> entry : comp.getFields()
                .entrySet()) {
                sb.append(String.format("    %s = %s\n", entry.getKey(), entry.getValue()));
            }
        }

        return sb.toString();
    }
}
