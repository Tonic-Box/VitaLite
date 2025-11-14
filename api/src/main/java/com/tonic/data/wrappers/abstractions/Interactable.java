package com.tonic.data.wrappers.abstractions;

public interface Interactable
{
    /**
     * Interacts with the entity using the specified action
     *
     * @param action The action(s) to perform on the object.
     */
    void interact(String action);

    /**
     * Interacts with the entity using the specified action index
     *
     * @param action The action index to perform on the object.
     */
    void interact(int action);

    /**
     * Gets the available actions for this entity
     *
     * @return An array of action strings.
     */
    String[] getActions();
}
