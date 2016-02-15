package com.gearsofleo.testing.simple.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.gearsofleo.testing.dsl.config.TestDslConfigMarker;

@Configuration
@ComponentScan(basePackageClasses = {TestDslConfigMarker.class}, 
	basePackages = { TestConfig.BASE_PACKAGE })
public class TestConfig {
	public static final String BASE_PACKAGE = "com.gearsofleo.testing.simple";

}
