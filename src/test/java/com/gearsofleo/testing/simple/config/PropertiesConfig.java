package com.gearsofleo.testing.simple.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.gearsofleo.rhino.core.bootstrap.AppProfile;

/**
 * Configuration class of property source(s) for integration testing.
 */
@Configuration
@Profile(AppProfile.NORMAL)
public class PropertiesConfig {

	private static final String TEST_PROPERTIES_FILE = "simple.properties";

	/**
	 * Creates the property placeholder configurer that can gather properties
	 * from a bunch of different sources.
	 *
	 * @return The property placeholder configurer.
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {

		final PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();

		pspc.setFileEncoding("UTF-8");
		pspc.setIgnoreUnresolvablePlaceholders(true);
		// pspc.setIgnoreResourceNotFound(true);

		// Properties defined in later files will override properties
		// defined in earlier files, in case of overlapping keys. Hence, make
		// sure that the most specific files are the last ones in the given list
		// of locations. So our "integration test" properties will override the
		// default server properties when the server is run in
		// "integration test" mode.

		final Resource[] resources = new ClassPathResource[] { new ClassPathResource(TEST_PROPERTIES_FILE) };

		pspc.setLocations(resources);

		return pspc;
	}
}
