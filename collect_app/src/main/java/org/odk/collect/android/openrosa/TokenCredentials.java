package org.odk.collect.android.openrosa;

public class TokenCredentials implements HttpCredentialsInterface {
    private final String username;
    private final String token;

    public TokenCredentials(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return null;
    }
}
