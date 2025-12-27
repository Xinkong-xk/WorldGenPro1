package io.xk.worldgenpro.core.configure.plugin.java;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Java 插件注解，用于标记插件入口类
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface plugin {
    /**
     * 插件名称（必填，没有默认值）
     */
    String name() default "";

    /**
     * 插件版本（可选，默认 1.0.0）
     */
    String version() default "1.0.0";

    /**
     * 插件作者（可选，默认空字符串）
     */
    String author() default "";

    /**
     * 插件描述（可选，默认空字符串）
     */
    String description() default "";

    /**
     * 可扩展的键值对（可选，可以不写）
     */
    String[] attributes() default {};
}
