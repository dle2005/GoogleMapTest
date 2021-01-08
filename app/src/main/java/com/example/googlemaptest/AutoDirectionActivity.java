package com.example.googlemaptest;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AutoDirectionActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap mMap;
    Marker marker;

    LatLng mOrigin;
    LatLng mDest;
    LatLng mWay;

    LatLng point;
    String url;
    MarkerOptions mPoint;

    ArrayList<LatLng> markerPoints;
    TextView tv_info;

    Button bt_origin, bt_dest, bt_way;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autodirection);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        PlacesClient placesClient = Places.createClient(this);

        tv_info = (TextView) findViewById(R.id.info);

        bt_origin = (Button) findViewById(R.id.bt_origin);
        bt_dest = (Button) findViewById(R.id.bt_dest);
        bt_way = (Button) findViewById(R.id.bt_way);

        mPoint = new MarkerOptions();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                if(marker != null) marker.remove();
                mPoint.title(place.getName());
                mPoint.position(place.getLatLng());
                mPoint.snippet(place.getAddress());
                marker = mMap.addMarker(mPoint);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(),15));
            }
            @Override
            public void onError(Status status) {
                Log.i("Main", "An error occurred: " + status);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        bt_origin.setOnClickListener(v -> {
            Log.d("MAIN", mPoint.getTitle());
            mMap.addMarker(mPoint);
            mOrigin = mPoint.getPosition();

            Toast.makeText(getApplicationContext(), "출발지 추가", Toast.LENGTH_LONG).show();
        });

        bt_dest.setOnClickListener(v -> {
            mMap.addMarker(mPoint);
            mDest = mPoint.getPosition();

            Toast.makeText(getApplicationContext(), "도착지 추가", Toast.LENGTH_LONG).show();

            url = getUrl(mOrigin, mDest, mWay);

            RequestTask requestTask = new RequestTask();
            requestTask.execute(url);
        });

        bt_way.setOnClickListener(v -> {
            mMap.addMarker(mPoint);
            mWay = mPoint.getPosition();

            Toast.makeText(getApplicationContext(), "경유지 추가", Toast.LENGTH_LONG).show();
        });
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

    public String getUrl(LatLng origin, LatLng dest, LatLng way) {
         String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
         String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
         String str_way = "waypoints=via:" + way.latitude + "%2C" + way.longitude;

         String mode = "mode=driving";

         String key = "key=" + getString(R.string.google_maps_key);

         String parameter = str_origin + "&" + str_way + "&" + str_dest + "&" + mode + "&" + key;

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