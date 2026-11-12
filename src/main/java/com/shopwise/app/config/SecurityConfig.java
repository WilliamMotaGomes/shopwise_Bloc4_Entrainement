package com.shopwise.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.shopwise.app.security.ApiRole;
import com.shopwise.app.security.RestAccessDeniedHandler;
import com.shopwise.app.security.RestAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtConverter,
			RestAuthenticationEntryPoint entryPoint, RestAccessDeniedHandler accessDeniedHandler) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/products").hasAnyRole(ApiRole.ADMIN, ApiRole.MERCHANT)
						.requestMatchers(HttpMethod.PUT, "/api/products/**")
						.hasAnyRole(ApiRole.ADMIN, ApiRole.MERCHANT)
						.requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole(ApiRole.ADMIN)
						.requestMatchers("/api/sales", "/api/sales/**").hasAnyRole(ApiRole.ADMIN, ApiRole.MERCHANT)
						.requestMatchers(HttpMethod.POST, "/api/recommendations/retrain").hasRole(ApiRole.ADMIN)
						.requestMatchers("/api/recommendations", "/api/recommendations/**")
						.hasAnyRole(ApiRole.ADMIN, ApiRole.MERCHANT)
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
						.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler));
		return http.build();
	}
}
