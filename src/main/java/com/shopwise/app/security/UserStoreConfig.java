package com.shopwise.app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class UserStoreConfig {
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public UserDetailsService userDetailsService(PasswordEncoder encoder,
			@Value("${shopwise.security.users.admin.password}") String adminPassword,
			@Value("${shopwise.security.users.merchant.password}") String merchantPassword,
			@Value("${shopwise.security.users.clerk.password}") String clerkPassword) {
		return new InMemoryUserDetailsManager(
				User.withUsername("admin").password(encoder.encode(adminPassword)).roles(ApiRole.ADMIN).build(),
				User.withUsername("marie").password(encoder.encode(merchantPassword)).roles(ApiRole.MERCHANT).build(),
				User.withUsername("clerk").password(encoder.encode(clerkPassword)).roles(ApiRole.USER).build());
	}
}
