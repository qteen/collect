package org.odk.collect.android.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import org.odk.collect.android.R;
import org.odk.collect.android.utilities.MultiClickGuard;
import org.odk.collect.android.utilities.ThemeUtils;

public class LoginActivity extends CollectAbstractActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button loginBtn = findViewById(R.id.login_button);
        loginBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (MultiClickGuard.allowClick(getClass().getName())) {
            Intent intent = new Intent(LoginActivity.this, FillBlankFormActivity.class);
            startActivity(intent);
            finish();
        }
    }
}