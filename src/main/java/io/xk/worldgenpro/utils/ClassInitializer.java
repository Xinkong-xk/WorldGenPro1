package io.xk.worldgenpro.utils;

import static io.xk.worldgenpro.WorldGenPro.LOGGER;

public final class ClassInitializer {

    private ClassInitializer() {}

    public static void forceInitialize(Class<?> clazz) {
        try {
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
            LOGGER.info("[ClassInitializer] Initialized: " + clazz.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Failed to initialize class: " + clazz.getName(), e
            );
        }
    }
}
