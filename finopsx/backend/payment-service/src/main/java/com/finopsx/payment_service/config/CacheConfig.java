package com.finopsx.payment_service.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Enables Spring's cache abstraction; Redis is configured in application.properties. */
@Configuration
@EnableCaching
public class CacheConfig {
}
