package com.skyline.terraexplorer.controllers;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.skyline.terraexplorer.R;

public class AboutActivity extends MatchParentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        Button button = (Button) findViewById(R.id.about_close);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            TextView version = (TextView) findViewById(R.id.about_version);
            version.setText(String.format(getString(R.string.about_version), pInfo.versionName, pInfo.versionCode));
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        underlineEmail(findViewById(R.id.about_info_email));
        underlineEmail(findViewById(R.id.about_support_email));
    }

    private void underlineEmail(View view) {
        final TextView textView = (TextView) view;
        String text = textView.getText().toString();
        final String[] textParts = text.split(":");
        text = textParts[0] + ": <u>" + textParts[1] + "</u>";
        textView.setText(Html.fromHtml(text));

        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmail(textParts[1]);
            }
        });
    }

    private void sendEmail(String email) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null));
        //emailIntent.putExtra(Intent.EXTRA_SUBJECT, "EXTRA_SUBJECT");
        startActivity(Intent.createChooser(emailIntent, getString(R.string.about_send_email)));
    }

}
