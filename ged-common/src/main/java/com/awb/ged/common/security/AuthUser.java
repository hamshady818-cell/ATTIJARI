package com.awb.ged.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <h1>AuthUser</h1>
 * <p>
 * Custom parameter annotation used in Spring REST Controllers to inject the currently authenticated
 * {@link CurrentUser} context directly into controller method parameters.
 * </p>
 * <p>
 * This keeps the API layer clean from framework-specific classes like {@code Authentication} or {@code Jwt}.
 * </p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthUser {
}
