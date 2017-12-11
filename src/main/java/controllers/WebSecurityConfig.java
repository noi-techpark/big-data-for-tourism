package controllers;

import de.bytefish.elasticutils.elasticsearch5.client.bulk.configuration.BulkProcessorConfiguration;
import de.bytefish.elasticutils.elasticsearch5.client.bulk.options.BulkProcessingOptions;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;

import java.net.InetAddress;
import java.util.List;

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
        http.authorizeRequests().anyRequest().fullyAuthenticated();
        http.httpBasic();
        http.csrf().disable();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .userDetailsService(users)
            .passwordEncoder(new Md5PasswordEncoder());
    }
}

@Service
class Users implements UserDetailsService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Environment env;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        String indexNameUserdetails = env.getProperty("es.userdetails");

        BulkProcessorConfiguration bulkConfiguration = new BulkProcessorConfiguration(BulkProcessingOptions.builder()
                .build());

        boolean enableSsl = Boolean.parseBoolean(System.getProperty("ssl", env.getProperty("es.ssl")));
        String cluster = env.getProperty("es.cluster");
        String user = env.getProperty("es.user");

        Settings settings = Settings.builder()
                .put("client.transport.nodes_sampler_interval", "5s")
                .put("client.transport.sniff", false)
                .put("transport.tcp.compress", true)
                .put("cluster.name", cluster)
                .put("xpack.security.transport.ssl.enabled", enableSsl)
                .put("request.headers.X-Found-Cluster", cluster)
                .put("xpack.security.user", user)
                .build();

        try (TransportClient transportClient = new PreBuiltXPackTransportClient(settings)) {

            String endpoint = env.getProperty("es.endpoint");
            int port = Integer.parseInt(env.getProperty("es.port"));

            try {
                transportClient
                        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(endpoint), port));
            } catch (Exception e) {
                log.error("could not resolve es endpoint: " + endpoint + ":" + port);
            }

            SearchResponse response = null;
            try {
                response = transportClient.prepareSearch(indexNameUserdetails)
                        .setSource(new SearchSourceBuilder().size(1).query(QueryBuilders.termQuery("_id", username)))
                        .get();
            } catch (Exception e) {
                log.error("search response error: " + e.toString());
            }

            log.info("total hits: " + response.getHits().getTotalHits());

            if(response.getHits().getTotalHits() > 0) {
                List<GrantedAuthority> auth = AuthorityUtils
                        .commaSeparatedStringToAuthorityList("ROLE_USER");
                if(response.getHits().getAt(0).getSource().get("authority").toString().equals("ROLE_ADMIN")) {
                    auth = AuthorityUtils
                            .commaSeparatedStringToAuthorityList("ROLE_ADMIN");
                }
                String password = response.getHits().getAt(0).getSource().get("password").toString();

                log.info("username: " + username);
                log.info("role: " + auth.toString());

                return new org.springframework.security.core.userdetails.User(username, password, auth);
            }

        }
        throw new UsernameNotFoundException(username);
    }

}
