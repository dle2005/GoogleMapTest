package com.example.googlemaptest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap mMap;
    LatLng mOrigin;
    LatLng mDest;

    ArrayList<LatLng> markerPoints;
    TextView tv_info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_info = (TextView) findViewById(R.id.info);

        mOrigin = new LatLng(41.3949, 2.0086);
        mDest = new LatLng(41.1258, 1.2035);

        markerPoints = new ArrayList<LatLng>();
        markerPoints.add(mOrigin);
        markerPoints.add(mDest);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.addMarker(new MarkerOptions().position(mOrigin).title("Origin"));
        mMap.addMarker(new MarkerOptions().position(mDest).title("Dest"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mOrigin, 8f));

        String url = getUrl(mOrigin, mDest);

        RequestTask requestTask = new RequestTask();
        requestTask.execute(url);
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream inputStream = null;
        HttpURLConnection httpUrlConnection = null;
        try{
            URL url = new URL(strUrl);

            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.connect();

            inputStream = httpUrlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Main", "downloadUrl Exception e");
        }finally{
            inputStream.close();
            httpUrlConnection.disconnect();
        }

        return data;
    }

    public String getUrl(LatLng origin, LatLng dest) {
         String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
         String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

         String mode = "mode=driving";

         String key = "key=" + getString(R.string.goole_maps_key);

         String parameter = str_origin + "&" + str_dest + "&" + mode + "&" + key;

         String url = "https://maps.googleapis.com/maps/api/directions/json" + "?" + parameter;

         return url;
    }

    public class RequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String data = "";
            try {
                data = downloadUrl(strings[0]);
            } catch (Exception e) {
                Log.d("Main", " doInBackground");
            }
            return data;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            //Json object parsing
            ParserTask parserTask = new ParserTask();
            parserTask.execute(data);
        }
    }

    public class ParserTask extends AsyncTask<String, Void, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonString) {
            List<List<HashMap<String, String>>> routes = null;
            JSONObject jsonObject = null;

            try {
                jsonObject = new JSONObject(jsonString[0]);
                DirectionParser parser = new DirectionParser();
                routes = parser.parse(jsonObject);
            } catch (JSONException e) {
                Log.d("Main", "ParserTask doInBackground Exception e");
            }

            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            super.onPostExecute(lists);
            ArrayList points = null;
            PolylineOptions polylineOptions = null;

            String distance = "";
            String duration = "";

            for (int i = 0; i < lists.size(); i++) {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = lists.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    if(j == 0) {
                        distance = (String) point.get("distance");
                        continue;
                    }
                    else if (j == 1) {
                        duration = (String) point.get("duration");
                        continue;
                    }

                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lng"));

                    points.add(new LatLng(lat, lon));
                }
                polylineOptions.addAll(points);
                polylineOptions.width(15f);
                polylineOptions.color(Color.BLUE);
                polylineOptions.geodesic(true);
            }
            if (polylineOptions != null) {
                mMap.addPolyline(polylineOptions);
                tv_info.setText("Distance: " + distance + ", Duration: " + duration);
            } else {
                Toast.makeText(getApplicationContext(), "Direction not found", Toast.LENGTH_LONG).show();
            }
        }
    }
}