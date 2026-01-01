package moe.takochan.takorender.api.ecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ECS World that manages entities and systems.
 */
public class World {

    private final AtomicLong entityIdCounter = new AtomicLong(0);
    private final Map<Long, Entity> entities = new HashMap<>();
    private final List<GameSystem> systems = new ArrayList<>();

    /**
     * Create a new entity.
     */
    public Entity createEntity() {
        long id = entityIdCounter.incrementAndGet();
        Entity entity = new Entity(id);
        entities.put(id, entity);
        return entity;
    }

    /**
     * Get an entity by ID.
     */
    public Optional<Entity> getEntity(long id) {
        return Optional.ofNullable(entities.get(id));
    }

    /**
     * Remove an entity from the world.
     */
    public void removeEntity(long id) {
        entities.remove(id);
    }

    /**
     * Get all entities.
     */
    public List<Entity> getEntities() {
        return new ArrayList<>(entities.values());
    }

    /**
     * Get all entities with a specific component.
     */
    public <T extends Component> List<Entity> getEntitiesWith(Class<T> componentClass) {
        List<Entity> result = new ArrayList<>();
        for (Entity entity : entities.values()) {
            if (entity.isActive() && entity.hasComponent(componentClass)) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * Add a system to the world.
     */
    public <T extends GameSystem> T addSystem(T system) {
        system.setWorld(this);
        systems.add(system);
        system.onInit();
        return system;
    }

    /**
     * Remove a system from the world.
     */
    public void removeSystem(GameSystem system) {
        system.onDestroy();
        systems.remove(system);
    }

    /**
     * Update all systems.
     *
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        for (GameSystem system : systems) {
            if (system.isEnabled()) {
                system.update(deltaTime);
            }
        }
    }

    /**
     * Clear all entities and systems.
     */
    public void clear() {
        for (GameSystem system : new ArrayList<>(systems)) {
            removeSystem(system);
        }
        entities.clear();
    }
}
