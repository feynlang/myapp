package com.example.myapp;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;


@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
            .csrf(csrf->csrf.ignoringRequestMatchers(PathRequest.toH2Console()))
            .headers(headers->headers.frameOptions(frame->frame.sameOrigin()))
            .formLogin(formLogin->formLogin
                .loginPage("/user/login").defaultSuccessUrl("/"))
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/")
                .failureUrl("/user/login?error"))
            .logout(logout->logout
                .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher("/user/logout"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
        );
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    //AuthenticationManager는 사용자 인증 시 UserSecurityService와 PasswordEncoder를 내부적으로 사용 -> 인증&권한부여 프로세스
}