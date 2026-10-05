package ru.fsp.jobsearcher.infrastructure.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.fsp.jobsearcher.infrastructure.config.AppProperties;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppProperties appProperties;
    private final ObjectProvider<DevAuthFilter> devAuthFilter;

    @Bean
    @Profile("!test")
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean permitAll = appProperties.security().permitAll();
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/actuator/health",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                    ).permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/dictionaries/**").permitAll();
                    if (permitAll) {
                        auth.anyRequest().permitAll();
                    } else {
                        auth.anyRequest().authenticated();
                    }
                });
        Optional.ofNullable(devAuthFilter.getIfAvailable()).ifPresent(filter ->
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));
        if (!permitAll) {
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
        }
        return http.build();
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> roles = new HashSet<>();
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> col) {
            col.forEach(r -> roles.add(String.valueOf(r)));
        }
        Object resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess instanceof Map<?, ?> resources) {
            Object client = resources.get("jobsearcher-api");
            if (client instanceof Map<?, ?> clientMap && clientMap.get("roles") instanceof Collection<?> col) {
                col.forEach(r -> roles.add(String.valueOf(r)));
            }
        }
        return roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(appProperties.cors().allowedOriginPatterns().split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
