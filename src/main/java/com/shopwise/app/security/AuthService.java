package com.shopwise.app.security;

import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.shopwise.app.dto.request.LoginRequest;
import com.shopwise.app.dto.response.LoginResponse;

@Service
public class AuthService {
	private final UserDetailsService userDetailsService;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userDetailsService = userDetailsService;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public LoginResponse login(LoginRequest request) {
		UserDetails user;
		try {
			user = userDetailsService.loadUserByUsername(request.username());
		} catch (UsernameNotFoundException e) {
			throw new BadCredentialsException("Invalid credentials");
		}
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BadCredentialsException("Invalid credentials");
		}
		List<String> roles = user.getAuthorities().stream().map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
				.toList();
		return new LoginResponse(jwtService.issue(user.getUsername(), roles), "Bearer", jwtService.getTtlSeconds(),
				roles);
	}
}
