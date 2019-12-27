package controllers;

import com.google.inject.Inject;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.play.PlayWebContext;
import org.pac4j.play.java.Secure;
import org.pac4j.play.store.PlaySessionStore;
import play.mvc.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import modules.SecurityModule;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import common.services.AuthorUtil;

public class HomeController extends Controller {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    String version = "1.0.1";
    public Result version() {
        logger.info(session("username"));

        return ok(version);
    }

    public Result adminLogin(String user) {
        String username = "hoangnh";
        session("username", username);
        session("role", "*");
        session("location", "");
        session("verifiedLocation", "");

        return redirect("/");
    }
    public Result index() {
        return ok(views.html.index.render());
    }

    @Secure(clients = "OidcClient")
    public Result oidcIndex() {
        String profile = getProfiles().toString();
        logger.info(profile);
        Pattern p = Pattern.compile("nickname=(.*?),");
        Matcher m = p.matcher(profile);
        String username = "";
        if(m.find()){
            username = m.group(1);
            logger.info(username);
            session("username", username);
            session("role", "*");
            session("location", "");
            session("verifiedLocation", "");
        }
        session("username", username);
        session("role", "*");
        session("location", "");
        session("verifiedLocation", "");

        return redirect("/");
    }

    @Inject
    private PlaySessionStore playSessionStore;

    @SuppressWarnings("unchecked")
    private List<CommonProfile> getProfiles() {
        final PlayWebContext context = new PlayWebContext(ctx(), playSessionStore);
        final ProfileManager<CommonProfile> profileManager = new ProfileManager(context);
        return profileManager.getAll(true);
    }

}
