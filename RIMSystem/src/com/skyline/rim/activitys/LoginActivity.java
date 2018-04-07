package com.skyline.rim.activitys;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.skyline.rim.globaldata.UserInfo;
import com.skyline.rim.webapiTools.WebServiceHelper;
import com.skyline.terraexplorer.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.skyline.rim.globaldata.systemInfo.saveLogonInfo;

/**
 * A login screen that offers login via account/password.
 */
public class LoginActivity extends Activity {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    //登陆成功后取得的信息
    private List<Map<String, String>> mLoginInfos;
    // UI references.
    private EditText mLoginUrlView;
    private EditText mAccountView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        TextView blankView = (TextView) findViewById(R.id.blankview);
        Display display = getWindowManager().getDefaultDisplay(); // 为获取屏幕宽、高
        blankView.setHeight((int) (display.getHeight() * 0.1));
        // Set up the login form.
        mLoginUrlView = (EditText) findViewById(R.id.loginurl);
        mAccountView = (EditText) findViewById(R.id.account);
        mPasswordView = (EditText) findViewById(R.id.password);
        mLoginUrlView.setText(UserInfo.mProjectID);
        mAccountView.setText(UserInfo.mLoginname);
        //mPasswordView.setText(UserInfo.mPassword);

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

    }

    /**
     * 登录验证的调用函数，按键和密码框的响应时间调用attemptLogin来做用户验证，他主要的功能
     * 是验证用户输入密码和账户的格式的正确与否， 如果格式错误，在输入框中显示格式错误信息类型，格
     * 式正确后，调用showProgress显示用户验证延时等待对话框和启动mAuthTask异步处理用户信息验证。
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }
        // Reset errors.
        mAccountView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String projectID = mLoginUrlView.getText().toString();
        String account = mAccountView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid loginurl address.
        if (TextUtils.isEmpty(projectID)) {
            mAccountView.setError(getString(R.string.error_loginuirl_required));
            focusView = mLoginUrlView;
            cancel = true;
        } else if (!isUriValid(projectID)) {
            mLoginUrlView.setError(getString(R.string.error_invalid_uri));
            focusView = mLoginUrlView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(account)) {
            mAccountView.setError(getString(R.string.error_account_required));
            focusView = mAccountView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(projectID, account, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isUriValid(String uri) {
        //TODO: Replace this with your own logic
        return uri.length() > 0;
        //return uri.contains("http://");
    }

    /***
     * 检查password是不是空
     * @param password 密码
     * @return 有效=true ;无效=false
     */
    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 0;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        //项目标识，用以区分不同的项目
        private final String mProjectID;
        //用户名
        private final String mAccount;
        //密码private
        private final String mPassword;

        List<Map<String, String>> slist = new ArrayList<Map<String, String>>();

        UserLoginTask(String projectID, String account, String password) {
            mProjectID = projectID;
            mAccount = account;
            mPassword = password;
        }
        /***
         *
         * @param params 保留
         * @return 验证成功返回true，否则false
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            boolean result = true;
            try {
                //webapi请求，根据 mUrlLogin,mAccount,mPassword进行登录验证，并取得项目的配置信息
                final String url = "http://192.168.4.4:83/api/rim/login/GetUserInfo?XMID=" + mProjectID + "&YHM=" + mAccount + "&MM=" + mPassword;
                JSONObject jsonObject = WebServiceHelper.getJSONObject(url);
                Map<String, String> map = null;
                Log.i("tsdi", jsonObject.toString());
                String code1 = jsonObject.getString("code");
                //根据code判断是否登陆成功
                if (Integer.parseInt(code1) != 1) {
                    result = false;
                }
                else {
                    //从返回的json数据里，可以用getJSONArray获取相关参数，并记录下列信息到缓存
                    saveLogonInfo(LoginActivity.this, jsonObject);
                    UserInfo.saveUserInfo(LoginActivity.this, mProjectID, mAccount, mPassword, jsonObject);
                }
            } catch (Exception e) {
                result = false;
                Log.e("tsdi", e.getMessage());
            }
            // TODO: register the new account here.
            return result;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);
            if (success) {//验证成功
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                LoginActivity.this.finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

