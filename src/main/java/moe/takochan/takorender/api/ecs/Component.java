package moe.takochan.takorender.api.ecs;

/**
 * Base class for all components in the ECS framework.
 * Components hold data and have no logic.
 */
public abstract class Component {

    private Entity entity;

    /**
     * Get the entity this component is attached to.
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Set the entity this component is attached to.
     * This is called automatically by Entity.addComponent().
     */
    void setEntity(Entity entity) {
        this.entity = entity;
    }
}
