package com.t4skforce.mailtest.annitation.wait;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface Wait {
	Header[] headers() default {};
	Body[] body() default {};
	String message() default "@Wait('{'{0}'}') condition not met";
}
