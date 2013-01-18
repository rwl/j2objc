package com.google.j2objc.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface Register {

  String name() default "";

  boolean isWrapper() default false;

  String header() default "";
}
