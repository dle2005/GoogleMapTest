package com.example.googlemaptest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.widget.Button;
import android.widget.EditText;
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
    GoogleMap mMapSearch;

    LatLng mOrigin;
    LatLng mDest;
    LatLng mWay;

    LatLng point;
    String url;
    MarkerOptions temp;

    ArrayList<LatLng> markerPoints;
    TextView tv_info;

    Geocoder geocoder;
    EditText et_search;
    Button bt_search;
    Button bt_origin;
    Button bt_dest;
    Button bt_way;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_info = (TextView) findViewById(R.id.info);

//        mOrigin = new LatLng(41.3949, 2.0086);
//        mDest = new LatLng(41.1258, 1.2035);

//        markerPoints = new ArrayList<LatLng>();
//        markerPoints.add(mOrigin);
//        markerPoints.add(mDest);

        et_search = (EditText) findViewById(R.id.et_search);
        bt_search = (Button) findViewById(R.id.bt_search);

        bt_origin = (Button) findViewById(R.id.bt_origin);
        bt_dest = (Button) findViewById(R.id.bt_dest);
        bt_way = (Button) findViewById(R.id.bt_way);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMapSearch = googleMap;
        geocoder = new Geocoder(this);
        MarkerOptions mOptions = new MarkerOptions();
        temp = new MarkerOptions();


//        mMap.addMarker(new MarkerOptions().position(mOrigin).title("Origin"));
//        mMap.addMarker(new MarkerOptions().position(mDest).title("Dest"));

//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mOrigin, 8f));

        bt_origin.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                temp = mOptions;
                mMap.addMarker(temp);
                mOrigin = point;

                Toast.makeText(getApplicationContext(), "출발지 추가", Toast.LENGTH_LONG).show();
            }
        });

        bt_dest.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                temp = mOptions;
                mMap.addMarker(temp);
                mDest = point;

                Toast.makeText(getApplicationContext(), "도착지 추가", Toast.LENGTH_LONG).show();

                url = getUrl(mOrigin, mDest, mWay);

                RequestTask requestTask = new RequestTask();
                requestTask.execute(url);
            }
        });

        bt_way.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                temp = mOptions;
                mMap.addMarker(temp);
                mWay = point;

                Toast.makeText(getApplicationContext(), "경유지 추가", Toast.LENGTH_LONG).show();
            }
        });

        bt_search.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = et_search.getText().toString();
                List<Address> addressList = null;
                try {
                    addressList = geocoder.getFromLocationName(str, 10);
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Direction not found", Toast.LENGTH_LONG).show();
                    Log.d("Main", "setOnClickListener IOException");
                }

                System.out.println(addressList.get(0).toString());
                String []splitStr = addressList.get(0).toString().split(",");
                String address = splitStr[0].substring(splitStr[0].indexOf("\"") + 1,splitStr[0].length() - 2); // 주소
                Log.d("Main", "address: " + address);

                String latitude = splitStr[10].substring(splitStr[10].indexOf("=") + 1); // 위도
                String longitude = splitStr[12].substring(splitStr[12].indexOf("=") + 1); // 경도
                Log.d("Main", "latitude: " + latitude);
                Log.d("Main", "longitude: " + longitude);

                point = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                Log.d("Main", "point: " + point);

                mOptions.title(str);
                mOptions.snippet(address);
                mOptions.position(point);
                mMapSearch.addMarker(mOptions);
                mMapSearch.moveCamera(CameraUpdateFactory.newLatLngZoom(point,15));
            }
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

         String key = "key=" + getString(R.string.goole_maps_key);

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