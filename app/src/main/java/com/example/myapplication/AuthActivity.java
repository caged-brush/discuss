package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One screen for both logging in and signing up; the mode can be switched in place. */
public class AuthActivity extends AppCompatActivity {
    private static final String EXTRA_SIGN_UP = "sign_up";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private AuthRepository auth;
    private boolean signUpMode;

    private View signUpFields;
    private TextInputLayout tilPassword, tilConfirmPassword;
    private EditText etUsername, etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    private TextView tvTitle, tvSubtitle, tvError, tvSwitch;
    private Button btnSubmit;
    private ProgressBar progress;

    public static Intent intent(Context context, boolean signUp) {
        return new Intent(context, AuthActivity.class).putExtra(EXTRA_SIGN_UP, signUp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = new AuthRepository(DBHandler.getInstance(this));

        signUpFields = findViewById(R.id.signUpFields);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etUsername = findViewById(R.id.etUsername);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tvTitle = findViewById(R.id.tvAuthTitle);
        tvSubtitle = findViewById(R.id.tvAuthSubtitle);
        tvError = findViewById(R.id.tvAuthError);
        tvSwitch = findViewById(R.id.tvAuthSwitch);
        btnSubmit = findViewById(R.id.btnAuthSubmit);
        progress = findViewById(R.id.progressAuth);

        boolean startInSignUp = savedInstanceState != null
                ? savedInstanceState.getBoolean(EXTRA_SIGN_UP)
                : getIntent().getBooleanExtra(EXTRA_SIGN_UP, false);
        setMode(startInSignUp);

        findViewById(R.id.btnAuthBack).setOnClickListener(v -> finish());
        tvSwitch.setOnClickListener(v -> setMode(!signUpMode));
        btnSubmit.setOnClickListener(v -> submit());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_SIGN_UP, signUpMode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void setMode(boolean signUp) {
        signUpMode = signUp;
        int visibility = signUp ? View.VISIBLE : View.GONE;
        signUpFields.setVisibility(visibility);
        tilConfirmPassword.setVisibility(visibility);
        tilPassword.setHelperText(signUp ? getString(R.string.auth_password_helper) : null);
        tvTitle.setText(signUp ? R.string.auth_signup_title : R.string.auth_login_title);
        tvSubtitle.setText(signUp ? R.string.auth_signup_subtitle : R.string.auth_login_subtitle);
        btnSubmit.setText(signUp ? R.string.auth_signup_title : R.string.auth_login_title);
        tvSwitch.setText(signUp ? R.string.auth_switch_to_login : R.string.auth_switch_to_signup);
        showError(null);
    }

    private void submit() {
        String username = text(etUsername);
        String firstName = text(etFirstName);
        String lastName = text(etLastName);
        String email = text(etEmail);
        char[] password = chars(etPassword.getText());
        char[] confirm = chars(etConfirmPassword.getText());

        if (signUpMode && !Arrays.equals(password, confirm)) {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
            showError(getString(R.string.auth_password_mismatch));
            return;
        }
        Arrays.fill(confirm, '\0');

        setBusy(true);
        boolean signUp = signUpMode;
        // PBKDF2 is deliberately slow, so never run it on the UI thread
        executor.execute(() -> {
            String uid = null;
            String error = null;
            try {
                uid = signUp
                        ? auth.signUp(username, firstName, lastName, email, password)
                        : auth.logIn(email, password);
                if (uid == null) {
                    error = getString(R.string.auth_wrong_credentials);
                }
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
            } catch (Exception e) {
                error = getString(R.string.auth_failed);
            } finally {
                Arrays.fill(password, '\0');
            }
            String finalUid = uid;
            String finalError = error;
            mainHandler.post(() -> onAuthResult(finalUid, finalError));
        });
    }

    private void onAuthResult(String uid, String error) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        setBusy(false);
        if (uid == null) {
            showError(error);
            return;
        }
        Session.logIn(this, uid);
        Intent intent = new Intent(this, ArticleBrowse.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setBusy(boolean busy) {
        btnSubmit.setEnabled(!busy);
        tvSwitch.setEnabled(!busy);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (busy) {
            showError(null);
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(message == null ? View.GONE : View.VISIBLE);
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private static char[] chars(Editable editable) {
        if (editable == null) {
            return new char[0];
        }
        char[] out = new char[editable.length()];
        editable.getChars(0, editable.length(), out, 0);
        return out;
    }
}
