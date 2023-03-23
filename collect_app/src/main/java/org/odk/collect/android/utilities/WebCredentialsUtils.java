package org.odk.collect.android.utilities;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.odk.collect.android.BuildConfig;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.logic.PropertyManager;
import org.odk.collect.android.openrosa.HttpCredentials;
import org.odk.collect.android.openrosa.HttpCredentialsInterface;
import org.odk.collect.android.openrosa.TokenCredentials;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.preferences.GeneralKeys;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@Singleton
public class WebCredentialsUtils {

    private static final Map<String, HttpCredentialsInterface> HOST_CREDENTIALS = new HashMap<>();
    private static final Map<String, HttpCredentialsInterface> HOST_AUTH_TOKEN = new HashMap<>();

    public void saveCredentials(@NonNull String url, @NonNull String username, @NonNull String password) {
        if (username.isEmpty()) {
            return;
        }

        String host = Uri.parse(url).getHost();
        HOST_CREDENTIALS.put(host, new HttpCredentials(username, password));
    }

    public void saveCredentialsPreferences(GeneralSharedPreferences generalSharedPreferences, String userName, String password, PropertyManager propertyManager) {
        generalSharedPreferences.save(GeneralKeys.KEY_USERNAME, userName);
        generalSharedPreferences.save(GeneralKeys.KEY_PASSWORD, password);

        propertyManager.reload();
    }

    /**
     * Forgets the temporary credentials saved in memory for a particular host. This is used when an
     * activity that does some work requiring authentication is called with intent extras specifying
     * credentials. Once the work is done, the temporary credentials are cleared so that different
     * ones can be used on a later request.
     *
     * TODO: is this necessary in all cases it's used? Maybe it's needed if we want to be able to do
     * an authenticated call followed by an anonymous one but even then, can't we pass in null
     * username and password if the intent extras aren't set?
     */
    public void clearCredentials(@NonNull String url) {
        if (url.isEmpty()) {
            return;
        }

        String host = Uri.parse(url).getHost();
        if (host != null) {
            HOST_CREDENTIALS.remove(host);
        }
    }

    static void clearAllCredentials() {
        HOST_CREDENTIALS.clear();
    }

    public String getServerUrlFromPreferences() {
        if (GeneralSharedPreferences.getInstance() == null) {
            return "";
        }
        return (String) GeneralSharedPreferences.getInstance().get(GeneralKeys.KEY_SERVER_URL);
    }

    public String getPasswordFromPreferences() {
        if (GeneralSharedPreferences.getInstance() == null) {
            return "";
        }
        return (String) GeneralSharedPreferences.getInstance().get(GeneralKeys.KEY_PASSWORD);
    }

    public String getUserNameFromPreferences() {
        if (GeneralSharedPreferences.getInstance() == null) {
            return "";
        }
        return (String) GeneralSharedPreferences.getInstance().get(GeneralKeys.KEY_USERNAME);
    }

    public String getTokenFromPreferences() {
        if (GeneralSharedPreferences.getInstance() == null) {
            return "";
        }
        return (String) GeneralSharedPreferences.getInstance().get(GeneralKeys.KEY_AUTH_TOKEN);
    }

    public static Claims parseToken(String token) throws JwtException {
        Claims result = null;

        if(token != null) {
            Jws<Claims> jws = Jwts.parserBuilder()  // (1)
                    .requireIssuer(Collect.getAuthIssuer())
                    .setSigningKey(Collect.getAuthSecretKey())         // (2)
                    .build()                    // (3)
                    .parseClaimsJws(token); // (4)

            // we can safely trust the JWT
            result = jws.getBody();
        }

        return result;
    }

    public static void saveAuthToken(@NonNull String url, @NonNull String token) throws JwtException {
        if (token.isEmpty()) {
            return;
        }

        Claims claims = parseToken(token);
        String host = Uri.parse(url).getHost();
        HOST_AUTH_TOKEN.put(host, new TokenCredentials(claims.getSubject(), token));
        GeneralSharedPreferences.getInstance().save(GeneralKeys.KEY_AUTH_TOKEN, token);
    }

    /**
     * Returns a credentials object from the url
     *
     * @param url to find the credentials object
     * @return either null or an instance of HttpCredentialsInterface
     */
    public @Nullable HttpCredentialsInterface getCredentials(@NonNull URI url) {
        String host = url.getHost();
        String serverPrefsUrl = getServerUrlFromPreferences();
        String prefsServerHost = (serverPrefsUrl == null) ? null : Uri.parse(serverPrefsUrl).getHost();

        boolean tokenValid = true;
        try {
            TokenCredentials tokenCredentials = (TokenCredentials) HOST_AUTH_TOKEN.get(host);
            if(tokenCredentials!=null) parseToken(tokenCredentials.getToken());
        } catch (JwtException e) {
            tokenValid = false;

            e.printStackTrace();
        }

        if(!tokenValid) {
            return null;
        } else if (prefsServerHost != null && prefsServerHost.equalsIgnoreCase(host)) {
            // Use the temporary credentials if they exist, otherwise use the credentials saved to preferences
            if (HOST_CREDENTIALS.containsKey(host)) {
                return HOST_CREDENTIALS.get(host);
            } else {
                return new HttpCredentials(getUserNameFromPreferences(), getPasswordFromPreferences());
            }
        } else {
            return HOST_CREDENTIALS.get(host);
        }
    }

}
