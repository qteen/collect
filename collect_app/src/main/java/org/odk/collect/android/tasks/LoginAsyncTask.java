package org.odk.collect.android.tasks;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.openrosa.HttpGetResult;
import org.odk.collect.android.openrosa.OpenRosaHttpInterface;
import org.odk.collect.android.preferences.GeneralKeys;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.utilities.WebCredentialsUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import io.jsonwebtoken.JwtException;
import timber.log.Timber;

public class LoginAsyncTask extends AsyncTask<String, String, LoginAsyncTask.LoginReturn> {
    private static final String HTTP_CONTENT_TYPE_APP_JSON = "application/json";

    @Inject
    OpenRosaHttpInterface httpInterface;
    @Inject
    WebCredentialsUtils webCredentialsUtils;

    private LoginAsyncTaskListener loginAsyncTaskListener;

    public LoginAsyncTask(LoginAsyncTaskListener loginAsyncTaskListener) {
        this.loginAsyncTaskListener = loginAsyncTaskListener;
        Collect.getInstance().getComponent().inject(this);
    }

    @Override
    protected LoginReturn doInBackground(String... params) {
        String url = params[0];
        String username = params[1];
        String password = params[2];

        Collect app = Collect.getInstance();
        publishProgress(app.getString(R.string.wait_loading));

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
                Collect.getInstance());
        String authPath = settings.getString(GeneralKeys.KEY_AUTH_URL,
                app.getString(R.string.default_moda_auth));
        if (!authPath.startsWith("/")) {
            authPath = "/" + authPath;
        }
        String fullurl = url + authPath + "?username=" + username;

        Uri authUri = Uri.parse(fullurl);
        webCredentialsUtils.saveCredentials(fullurl, username, password);

        URI uri;
        try {
            uri = URI.create(authUri.toString());
        } catch (IllegalArgumentException e) {
            Timber.d(e.getMessage() != null ? e.getMessage() : e.toString());
            return new LoginReturn(0, "Error Auth = " + e.getMessage(), username);
        }

        HttpGetResult result;
        try {
            result = httpInterface.executeGetRequest(uri, HTTP_CONTENT_TYPE_APP_JSON, webCredentialsUtils.getCredentials(uri));
            if (result.getStatusCode() == HttpsURLConnection.HTTP_UNAUTHORIZED) {
                webCredentialsUtils.clearCredentials(authUri.getHost());
                return new LoginReturn(result.getStatusCode(), "Error Auth: Username/Password not recognize", username);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new LoginReturn(0, "Error Auth: " + e.getMessage(), username);
        }

        if(result.getStatusCode() != HttpsURLConnection.HTTP_OK) {
            return new LoginReturn(result.getStatusCode(), "Error Auth: Something went wrong", username);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(result.getInputStream()));
        StringBuilder out = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }

            String token = out.toString().replace("\"","");
            webCredentialsUtils.saveAuthToken(fullurl, token);
            savePreferences(url, username, password, token);
        } catch (IOException e) {
            new LoginReturn(result.getStatusCode(), "Error: "+e.getMessage(), username);
        } catch (JwtException e) {
            new LoginReturn(result.getStatusCode(), "Error JWT: "+e.getMessage(), username);
        }

        return new LoginReturn(result.getStatusCode(), "OK", username);
    }

    private void savePreferences(String url, String username, String password, String token) {
        GeneralSharedPreferences generalSharedPreferences = Collect.getInstance().getComponent().generalSharedPreferences();
        generalSharedPreferences.save(GeneralKeys.KEY_AUTH_TOKEN, token);
        generalSharedPreferences.save(GeneralKeys.KEY_SERVER_URL, url);
        generalSharedPreferences.save(GeneralKeys.KEY_USERNAME, username);
        generalSharedPreferences.save(GeneralKeys.KEY_PASSWORD, password);
        Collect.getInstance().getComponent().propertyManager().reload();
    }

    @Override
    protected void onPostExecute(LoginReturn loginReturn) {
        loginAsyncTaskListener.onLoginResult(loginReturn);
    }

    public interface LoginAsyncTaskListener extends ProgressNotifier{
        void onLoginResult(LoginReturn i);
    }

    public class LoginReturn{
        int code;
        String message;
        String username;

        public LoginReturn(int code, String message, String username) {
            this.code = code;
            this.message = message;
            this.username = username;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getUsername() {
            return username;
        }
    }
}
