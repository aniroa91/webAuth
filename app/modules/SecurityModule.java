package modules;

import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import controllers.SecureHttpActionAdapter;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.play.CallbackController;
import org.pac4j.play.LogoutController;
import org.pac4j.play.store.PlayCacheSessionStore;
import org.pac4j.play.store.PlaySessionStore;
import play.Environment;
import com.nimbusds.jose.JWSAlgorithm;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.Configure;

public class SecurityModule extends AbstractModule {

    private final Config configuration;
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SecurityModule(final Environment environment, final Config configuration) {
        this.configuration = configuration;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
        System.setProperty("http.proxyHost", "proxy.hcm.fpt.vn");
        System.setProperty("http.proxyPort", "80");
        System.setProperty("https.proxyHost", "proxy.hcm.fpt.vn");
        System.setProperty("https.proxyPort", "80");

//        String env_host = System.getenv(Configure.SVC_USER_HOST());

        String env_host = Configure.SVC_USER_HOST();
        System.setProperty("http.nonProxyHosts", "127.0.0.1|10.0.0.0/8|172.16.0.0/12|192.168.0.0/16|*.cluster.local|*.local|172.27.11.151|"+env_host);
//        System.setProperty("http.nonProxyHosts", "127.0.0.1|10.0.0.0/8|172.16.0.0/12|192.168.0.0/16|*.cluster.local|*.local|172.27.11.151|");
        try {
            bind(PlaySessionStore.class).to(PlayCacheSessionStore.class);
            final OidcConfiguration oidcConfiguration = new OidcConfiguration();
            oidcConfiguration.setDiscoveryURI(configuration.getString("oidc.discoveryUri"));
            oidcConfiguration.setClientId(configuration.getString("oidc.clientId"));
            oidcConfiguration.setSecret(configuration.getString("oidc.clientSecret"));
            oidcConfiguration.setPreferredJwsAlgorithm(JWSAlgorithm.RS256);


            final OidcClient oidcClient = new OidcClient(oidcConfiguration);

            final String baseUrl = configuration.getString("baseUrl");
            final Clients clients = new Clients(baseUrl + "/login/callback", oidcClient);

            final org.pac4j.core.config.Config config = new org.pac4j.core.config.Config(clients);
            config.setHttpActionAdapter(new SecureHttpActionAdapter());
            bind(org.pac4j.core.config.Config.class).toInstance(config);
        }
        catch (Exception e){
            logger.info(e.toString());
        }
    }

}
