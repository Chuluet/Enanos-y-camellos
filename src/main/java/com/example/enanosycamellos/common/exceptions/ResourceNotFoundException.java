package com.example.enanosycamellos.common.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Shortcut for the typical message: "There is no Cow with id X".
     *
     * @param resource entity name, e.g., "Cow"
     * @param id       identifier that was searched
     */
    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException(
                "There is no %s with id %s".formatted(resource, id));
    }
}