package io.bluemoon.gatewayzuul.config;

import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

@Configuration
@EnableOAuth2Sso
@EnableResourceServer
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private static Filter csrfHeaderFilter() {

        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (csrf != null) {
                    Cookie cookie = new Cookie("XSRF-TOKEN", csrf.getToken());
                    cookie.setPath("/");
                    cookie.setSecure(true);
                    response.addCookie(cookie);
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    private static CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-XSRF-TOKEN");
        return repository;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/", "/mk-auth/**", "/login").permitAll().anyRequest().authenticated()
                .and()
                .logout().logoutSuccessUrl("/gateway/logout").logoutRequestMatcher(new AntPathRequestMatcher("/logout")).invalidateHttpSession(true).deleteCookies("JSESSIONID").clearAuthentication(true);

    }

    private RequestMatcher csrfRequestMatcher() {
        return new RequestMatcher() {

            private final Pattern allowedMethods = Pattern.compile("^(GET|HEAD|OPTIONS|TRACE)$");

            // Disable CSFR protection on the following urls:
            private final AntPathRequestMatcher[] requestMatchers = {new AntPathRequestMatcher("/mk-auth/**")};

            @Override
            public boolean matches(HttpServletRequest request) {
                if (allowedMethods.matcher(request.getMethod()).matches()) {
                    return false;
                }

                for (AntPathRequestMatcher matcher : requestMatchers) {
                    if (matcher.matches(request)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }


}
