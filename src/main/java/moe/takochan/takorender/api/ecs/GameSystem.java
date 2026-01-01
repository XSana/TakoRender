package moe.takochan.takorender.api.ecs;

/**
 * Base class for all systems in the ECS framework.
 * Systems contain logic and operate on entities with specific components.
 */
public abstract class GameSystem {

    private World world;
    private boolean enabled = true;

    /**
     * Get the world this system belongs to.
     */
    public World getWorld() {
        return world;
    }

    /**
     * Set the world this system belongs to.
     * Called automatically when the system is added to a world.
     */
    void setWorld(World world) {
        this.world = world;
    }

    /**
     * Check if this system is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable this system.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Called when the system is added to the world.
     */
    public void onInit() {}

    /**
     * Called every tick to update the system.
     *
     * @param deltaTime Time since last update in seconds
     */
    public abstract void update(float deltaTime);

    /**
     * Called when the system is removed from the world.
     */
    public void onDestroy() {}
}
