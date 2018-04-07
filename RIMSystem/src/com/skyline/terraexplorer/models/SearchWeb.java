package com.skyline.terraexplorer.models;

import android.content.Context;
import android.graphics.PointF;
import android.text.TextUtils;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.TEAppException;
import com.skyline.terraexplorer.tools.SettingsTool;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

public class SearchWeb {

    public class SearchResult {
        public SearchResult(JSONObject jsonObject) {
            try {
                name = jsonObject.getString("Name");
            } catch (JSONException e) {
                //ignore
            }
            try {
                desc = jsonObject.getString("Description");
            } catch (JSONException e) {
                //ignore
            }
            try {
                lat = jsonObject.getDouble("Lat");
            } catch (JSONException e) {
                //ignore
            }
            try {
                lon = jsonObject.getDouble("Lon");
            } catch (JSONException e) {
                //ignore
            }
        }

        public String name;
        public String desc;
        public double lon;
        public double lat;
        public double altitude;
    }

    public static boolean isEnabled() {
        return TEApp.getAppContext().getSharedPreferences(SettingsTool.PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(TEApp.getAppContext().getString(R.string.key_websearch), true);
    }

    public ArrayList<SearchResult> search(String query, double[] position) {
        String queryString = getQueryString(query, position);
        return downloadAndParseResults(queryString);
    }

    public ArrayList<SearchResult> reverseGeoCode(PointF point) {
        String queryString = String.format(Locale.US, "?latlon=%f,%f", point.y, point.x);
        return downloadAndParseResults(queryString);
    }

    private ArrayList<SearchResult> downloadAndParseResults(String queryString) {
        if (isEnabled() == false)
            return null;
        try

        {
            String searchUrl = TEApp.getAppContext().getSharedPreferences(SettingsTool.PREFERENCES_NAME, Context.MODE_PRIVATE).getString(TEApp.getAppContext().getString(R.string.key_websearch_url), AppLinks.getDefaultSearchServer());
            URL url = new URL(searchUrl);
            // if no path components (default)
            // user is entered SGWP url, need to append mq path
            if (url.getFile() == "") {
                if (searchUrl.endsWith("/") == false)
                    searchUrl = searchUrl + "/";
                searchUrl = searchUrl + AppLinks.getSearchUrlPostfix();
            }
            searchUrl = searchUrl + queryString;
            String jsonData = downloadFile(searchUrl);
            if (jsonData == null)
                return null;
            return parseResults(jsonData);
        } catch (Exception ex) {
            throw new TEAppException(ex);
        }
    }

    private ArrayList<SearchResult> parseResults(String jsonData) throws JSONException {

        JSONArray arr = new JSONArray(jsonData);
        ArrayList<SearchResult> parsedResults = new ArrayList<SearchWeb.SearchResult>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            parsedResults.add(new SearchResult(arr.getJSONObject(i)));
        }
        return parsedResults;
    }

    @SuppressWarnings("deprecation")
    private String getQueryString(String query, double[] position) {
        String[] queryStringParts = new String[]{
                "?q=", URLEncoder.encode(query),
                "&x=", Double.toString(position[0]),
                "&y=", Double.toString(position[2]),
                // distance ->search radius
                "&radius=", Double.toString(position[6] * 2)};
        return TextUtils.join("", queryStringParts);
    }

    private String downloadFile(String url) throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(new HttpGet(url));
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            return out.toString();
        } else {
            throw new IOException(statusLine.getReasonPhrase());
        }
    }
}
