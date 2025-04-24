package com.example.locationtracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_READ_PHONE_STATE = 1002;
    private static final int PERMISSION_REQUEST_LOCATION = 1001;
    private RequestQueue requestQueue;
    private final String insertUrl = "http://192.168.1.174/LocationTrackingBackend/src/api/createPosition.php";
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestQueue = Volley.newRequestQueue(this);

        // Request READ_PHONE_STATE permission if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    PERMISSION_REQUEST_READ_PHONE_STATE);
        }

        Button btnMap = findViewById(R.id.btnShowMap);
        btnMap.setOnClickListener(v -> startActivity(new Intent(this, MapsActivity.class)));

        // Initialize location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Check and request location permissions
        checkLocationPermissions();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }

    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            // Request both location permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    PERMISSION_REQUEST_LOCATION);
        } else {
            // Permissions already granted, start location updates
            startLocationUpdates();
        }
    }

    @SuppressLint("MissingPermission") // We check permission before calling this
    private void startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    60000,
                    0,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            Log.d("LOCATION_UPDATE", "Location changed: " + location);
                            double lat = location.getLatitude();
                            double lon = location.getLongitude();
                            float acc = location.getAccuracy();
                            String msg = String.format(getString(R.string.new_location),
                                    lat, lon, location.getAltitude(), acc);
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                            sendPosition(lat, lon);
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                            String newStatus = "";
                            switch (status) {
                                case LocationProvider.OUT_OF_SERVICE:
                                    newStatus = "OUT_OF_SERVICE";
                                    break;
                                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                                    newStatus = "TEMPORARILY_UNAVAILABLE";
                                    break;
                                case LocationProvider.AVAILABLE:
                                    newStatus = "AVAILABLE";
                                    break;
                            }
                            String msg = String.format(getResources().getString(R.string.provider_new_status),
                                    provider, newStatus);
                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                            String msg = String.format(getResources().getString(R.string.provider_enabled),
                                    provider);
                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                            String msg = String.format(getResources().getString(R.string.provider_disabled),
                                    provider);
                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error starting location updates: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_READ_PHONE_STATE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission de lecture d'IMEI accordée", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission de lecture d'IMEI refusée", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, start updates
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission is required for this app to work", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
    private void sendPosition(double lat, double lon) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("NetworkRequest", "Attempting to send position to: " + insertUrl);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("latitude", lat);
            jsonBody.put("longitude", lon);
            jsonBody.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            jsonBody.put("imei", getDeviceIdentifier());
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.POST,
                insertUrl,
                jsonBody,
                response -> {
                    Log.d("NetworkRequest", "Success! Response: " + response.toString());
                    Toast.makeText(MainActivity.this, "Position saved successfully", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Log.e("NetworkRequest", "Error: " + error.toString());
                    Toast.makeText(MainActivity.this, "Failed to save position", Toast.LENGTH_SHORT).show();
                    if (error.networkResponse != null) {
                        Log.e("NetworkError", "Status code: " + error.networkResponse.statusCode);
                        Log.e("NetworkError", "Response: " + new String(error.networkResponse.data));
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(jsonRequest);
    }


    private String getDeviceIdentifier() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                return "unknown";
            }

            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, use a different identifier
                return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return tm.getImei();
            } else {
                return tm.getDeviceId();
            }
        } catch (Exception e) {
            Log.e("DeviceID", "Error getting device identifier", e);
            return "unknown";
        }
    }
}