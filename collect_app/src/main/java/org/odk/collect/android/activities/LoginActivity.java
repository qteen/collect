package org.odk.collect.android.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import org.odk.collect.android.R;
import org.odk.collect.android.analytics.Analytics;
import org.odk.collect.android.analytics.AnalyticsEvents;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.GeneralKeys;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.preferences.PreferencesProvider;
import org.odk.collect.android.tasks.LoginAsyncTask;
import org.odk.collect.android.utilities.MultiClickGuard;
import org.odk.collect.android.utilities.ThemeUtils;
import org.odk.collect.android.utilities.WebCredentialsUtils;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

public class LoginActivity extends CollectAbstractActivity implements View.OnClickListener, LoginAsyncTask.LoginAsyncTaskListener {
    @Inject
    Analytics analytics;

    private MaterialAutoCompleteTextView url;
    private EditText username;
    private EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Collect.getInstance().getComponent().inject(this);

        GeneralSharedPreferences generalSharedPreferences = Collect.getInstance().getComponent().generalSharedPreferences();
        String serverUrl = (String) generalSharedPreferences.get(GeneralKeys.KEY_SERVER_URL);
        String token = (String) generalSharedPreferences.get(GeneralKeys.KEY_AUTH_TOKEN);
        if(token!=null) {
            try {
                WebCredentialsUtils.parseToken(token);
                WebCredentialsUtils.saveAuthToken(serverUrl, token);

                Intent intent = new Intent(this, FillBlankFormActivity.class);
                startActivity(intent);
                finish();
            } catch (JwtException e) {
                e.printStackTrace();
            }
        }
        // Getting the string array from strings.xml
        String items[] = getResources().getStringArray(R.array.capi_urls);
        // New Arrays list for storing items
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            // Adding items to arary list
            list.add(items[i]);
        }
        // Adapter for holding the data view
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                LoginActivity.this, android.R.layout.simple_list_item_1, list);

        url = findViewById(R.id.url);
        url.setAdapter(adapter);
        url.setThreshold(1);
        url.setDropDownHeight(200);
        url.setDropDownVerticalOffset(-180);
        url.setText(serverUrl);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);

        Button loginBtn = findViewById(R.id.login_button);
        loginBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (MultiClickGuard.allowClick(getClass().getName())) {
            LoginAsyncTask loginAsyncTask = new LoginAsyncTask(this);
            loginAsyncTask.execute(url.getText().toString(), username.getText().toString(), password.getText().toString());
        }
    }

    @Override
    public void onLoginResult(LoginAsyncTask.LoginReturn loginReturn) {
        if(loginReturn.getCode()== HttpsURLConnection.HTTP_OK && !loginReturn.getMessage().startsWith("Error")) {
            analytics.logEvent(AnalyticsEvents.LOGIN_SUCCESSFUL, loginReturn.getUsername());
            Intent intent = new Intent(LoginActivity.this, FillBlankFormActivity.class);
            startActivity(intent);
            finish();
        } else {
            analytics.logEvent(AnalyticsEvents.LOGIN_FAILED, loginReturn.getUsername() + " - " + loginReturn.getMessage());
            Toast.makeText(this, loginReturn.getCode()+" "+loginReturn.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onProgressStep(String stepMessage) {

    }
}