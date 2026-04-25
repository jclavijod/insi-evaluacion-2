package cl.iplacex.tiendaweb.ext.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
		httpSecurity.authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
				.formLogin(form -> form.loginPage("/ingresar").loginProcessingUrl("/ingresar").permitAll())
                .logout(logout -> logout
                        .logoutRequestMatcher(request ->
                                "GET".equals(request.getMethod()) && "/logout".equals(request.getRequestURI())
                        )
                        .permitAll()
                )
                .anonymous(anonymous -> anonymous.principal("invitado").authorities("ROLE_ANONIMO"))
		;
		return httpSecurity.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {
		var user1 = User.builder()
				.username("juan")
				.password( encoder.encode("1234") )
				.roles("CLIENTE")
				.build()
		;
		var user2 = User.builder()
				.username("andrea")
				.password( encoder.encode("4321") )
				.roles("EMPLEADO")
				.build()
		;
		return new InMemoryUserDetailsManager(user1, user2);
	}
	
}
