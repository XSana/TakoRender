package moe.takochan.takorender.api.ecs;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Basic Entity implementation for the ECS framework.
 * An Entity is a container for components and represents a game object.
 */
public class Entity {

    private final long id;
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();
    private boolean active = true;

    public Entity(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Add a component to this entity.
     */
    public <T extends Component> Entity addComponent(T component) {
        components.put(component.getClass(), component);
        component.setEntity(this);
        return this;
    }

    /**
     * Get a component from this entity.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> getComponent(Class<T> componentClass) {
        return Optional.ofNullable((T) components.get(componentClass));
    }

    /**
     * Check if this entity has a specific component.
     */
    public <T extends Component> boolean hasComponent(Class<T> componentClass) {
        return components.containsKey(componentClass);
    }

    /**
     * Remove a component from this entity.
     */
    public <T extends Component> void removeComponent(Class<T> componentClass) {
        components.remove(componentClass);
    }
}
