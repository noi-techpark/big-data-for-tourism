package controllers;

import controllers.domainmodel.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;

import java.util.List;

import controllers.domainmodel.User;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private Environment env;

    @Autowired
    private Users users;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                    .antMatchers("/css/**", "/images/**", "/js/**", "/lost").permitAll()
                    .anyRequest().fullyAuthenticated()
                    .and()
                .formLogin()
                    .loginPage("/login")
                    .permitAll()
                    .and()
                .csrf()
                    .disable()
                .logout()
                    .logoutSuccessUrl("/login");
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .userDetailsService(users)
            .passwordEncoder(new Md5PasswordEncoder());
    }

    @Bean
    WebMvcConfigurer myWebMvcConfigurer() {
        return new WebMvcConfigurerAdapter() {

            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                registry.addViewController("/login").setViewName("login");
            }
        };
    }
}

@Service
class Users implements UserDetailsService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Environment env;

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        try {
            User user = userRepository.findByUsername(username);

            List<GrantedAuthority> auth = AuthorityUtils
                    .commaSeparatedStringToAuthorityList("ROLE_USER");
            if(user.getAuthority().equals("ROLE_ADMIN")) {
                auth = AuthorityUtils
                        .commaSeparatedStringToAuthorityList("ROLE_ADMIN");
            }
            String password = user.getPassword();

            log.info("username: " + username);
            log.info("role: " + auth.toString());

            return new org.springframework.security.core.userdetails.User(username, password, auth);
        } catch (Exception e) {
            throw new UsernameNotFoundException(username);
        }
    }

}
