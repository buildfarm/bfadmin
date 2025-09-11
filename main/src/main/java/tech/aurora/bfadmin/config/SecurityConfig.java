package tech.aurora.bfadmin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity
@Order(1)
public class SecurityConfig {
    private static final String principalRequestHeader = "SecurityKey";

    //Unique security key used by UI, changes with every request
    public static String securityKey = "";

    @Value("${security.http.auth-token}")
    private String principalRequestValue;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .securityMatcher("/api/**")
                .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
                .addFilterBefore(apiKeyAuthFilter(), BasicAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return httpSecurity.build();
    }

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter() {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(principalRequestHeader);
        filter.setAuthenticationManager(authenticationManager());
        return filter;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return authentication -> {
            if (principalRequestValue.equals(authentication.getPrincipal())) {
                authentication.setAuthenticated(true);
                return authentication;
            } else {
                throw new BadCredentialsException("Invalid API Key");
            }
        };
    }

    private class ApiKeyAuthFilter extends AbstractPreAuthenticatedProcessingFilter {

        private String principalRequestHeader;

        public ApiKeyAuthFilter(String principalRequestHeader) {
            this.principalRequestHeader = principalRequestHeader;
        }

        @Override
        protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
            return request.getHeader(principalRequestHeader);
        }

        @Override
        protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
            return "N/A";
        }
    }
}
