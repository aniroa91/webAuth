package controllers;

import org.pac4j.core.config.Config;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.play.store.PlaySessionStore;
import play.mvc.Controller;
import play.mvc.Result;
import play.libs.concurrent.HttpExecutionContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import static org.pac4j.core.util.CommonHelper.assertNotNull;
import org.pac4j.play.PlayWebContext;

/**
 * <p>This filter finishes the login process for an indirect client, based on the {@link #callbackLogic}.</p>
 *
 * <p>The configuration can be provided via setters: {@link #setDefaultUrl(String)} (default url after login if none was requested) and
 * {@link #setMultiProfile(boolean)} (whether multiple profiles should be kept).</p>
 *
 * @author Jerome Leleu
 * @author Michael Remond
 * @since 1.5.0
 */
public class CallbackController extends Controller {

    private CallbackLogic<Result, PlayWebContext> callbackLogic = new DefaultCallbackLogic<>();

    private String defaultUrl;

    private Boolean multiProfile;

    @Inject
    protected Config config;
    @Inject
    protected PlaySessionStore playSessionStore;
    @Inject
    protected HttpExecutionContext ec;

    public CompletionStage<Result> callback() {
//        String env_host = System.getenv(Configure.SVC_USER_HOST());
//        System.setProperty("http.proxyHost", "proxy.hcm.fpt.vn");
//        System.setProperty("http.proxyPort", "80");
//        System.setProperty("https.proxyHost", "proxy.hcm.fpt.vn");
//        System.setProperty("https.proxyPort", "80");
//        System.setProperty("http.nonProxyHosts", "127.0.0.1|10.0.0.0/8|172.16.0.0/12|192.168.0.0/16|*.cluster.local|*.local|172.27.11.151|"+env_host);

        assertNotNull("callbackLogic", callbackLogic);

        assertNotNull("config", config);
        final PlayWebContext playWebContext = new PlayWebContext(ctx(), playSessionStore);

        return CompletableFuture.supplyAsync(() -> callbackLogic.perform(playWebContext, config, config.getHttpActionAdapter(), this.defaultUrl, this.multiProfile, false), ec.current());
    }

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public void setDefaultUrl(final String defaultUrl) {
        this.defaultUrl = defaultUrl;
    }

    public boolean isMultiProfile() {
        return multiProfile;
    }

    public void setMultiProfile(final boolean multiProfile) {
        this.multiProfile = multiProfile;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(final Config config) {
        this.config = config;
    }
}