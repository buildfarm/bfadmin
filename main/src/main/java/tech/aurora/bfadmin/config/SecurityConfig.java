package tech.aurora.bfadmin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity
@Order(1)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private static final String principalRequestHeader = "SecurityKey";

    //Unique security key used by UI, changes with every request
    public static String securityKey = "";

    @Value("${security.http.auth-token}")
    private String principalRequestValue;

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .requestMatcher(new AntPathRequestMatcher("/api/**"))
                .authorizeRequests(authz -> authz.anyRequest().authenticated())
                .addFilterBefore(apiKeyAuthFilter(), BasicAuthenticationFilter.class)
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter() {
        return new ApiKeyAuthFilter(principalRequestHeader);
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
