package com.example.caxidy.ejemplomapas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.widget.Toast;

public class HttpConnection {
    Context contexto;
    public String readUrl(String mapsApiDirectionsUrl) throws IOException {
        contexto = new MapsActivity().getApplicationContext();
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(mapsApiDirectionsUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Toast.makeText(contexto,contexto.getString(R.string.httperr),Toast.LENGTH_SHORT).show();
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

}
