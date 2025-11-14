package com.tonic.data.wrappers.abstractions;

public interface Interactable
{
    /**
     * Interacts with the entity by first matching action
     *
     * @param actions The action(s) to perform on the entity.
     */
    void interact(String... actions);

    /**
     * Interacts with the entity using the specified action index
     *
     * @param action The action index to perform on the entity.
     */
    void interact(int action);

    /**
     * Gets the available actions for this entity
     *
     * @return An array of action strings.
     */
    String[] getActions();
}
