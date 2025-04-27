package com.example.locationtracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private RequestQueue requestQueue;
    private final String showUrl = "http://192.168.1.174/LocationTrackingBackend/src/api/showPosition.php";
    private final String insertUrl = "http://192.168.1.174/LocationTrackingBackend/src/api/createPosition.php";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1003;

    private SearchView map_search;
    private AppCompatButton save_position;

    private LatLng lastSearchedPosition = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        requestQueue = Volley.newRequestQueue(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        map_search = findViewById(R.id.map_search);
        save_position = findViewById(R.id.save_position);
        SearchView searchView = findViewById(R.id.map_search);

        int searchSrcTextId = searchView.getContext().getResources()
                .getIdentifier("android:id/search_src_text", null, null);

        View searchPlate = searchView.findViewById(searchSrcTextId);

        if (searchPlate != null) {
            searchPlate.setBackgroundColor(Color.TRANSPARENT);
        }

        int plateId = searchView.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);

        View plate = searchView.findViewById(plateId);
        if (plate != null) {
            plate.setBackgroundColor(Color.TRANSPARENT); // or plate.setVisibility(View.GONE);
        }



        map_search.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        String location = map_search.getQuery().toString();
                        List<Address> addressList = null;
                        if(location != null){
                            Geocoder geocoder = new Geocoder(MapsActivity.this);
                            try{
                                addressList = geocoder.getFromLocationName(location, 1);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            assert addressList != null;
                            Address address = addressList.get(0);
                            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                            lastSearchedPosition = latLng;
                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(location));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10 ));
                        }
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return false;
                    }
                }
        );

        save_position = findViewById(R.id.save_position);

        save_position.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastSearchedPosition != null) {
                    double lat = lastSearchedPosition.latitude;
                    double lng = lastSearchedPosition.longitude;

                    sendPosition(lat, lng);
                    loadSavedPositions();

                    Toast.makeText(MapsActivity.this, "Position saved!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MapsActivity.this, "No position selected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mapFragment.getMapAsync(MapsActivity.this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        View mapView = mapFragment.getView();

        if (mapView != null) {
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
            rlp.setMargins(0, 215, 0, 0);
            locationButton.setLayoutParams(rlp);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc != null) {
                LatLng myLoc = new LatLng(loc.getLatitude(), loc.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLoc, 15));
            }

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        loadSavedPositions();

        mMap.setOnMarkerClickListener(
                marker -> {
                    Object tag = marker.getTag();
                    if(tag != null){
                        String id = tag.toString();
                        new AlertDialog.Builder(MapsActivity.this)
                                .setTitle("Delete Position")
                                .setMessage("Are you sure you want to delete this position?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    deletePositionFromServer(id, marker);
                                })
                                .setNegativeButton("No", null)
                                .show();
                    }
                    return true;
                }
        );
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private void loadSavedPositions() {
        mMap.clear();
        JsonObjectRequest jsonReq = new JsonObjectRequest(Request.Method.GET, showUrl, null,
                response -> {
                    try {
                        JSONObject data = response.getJSONObject("data");
                        JSONArray arr = data.getJSONArray("positions");

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            int id = obj.getInt("id");
                            LatLng locPos = new LatLng(obj.getDouble("latitude"), obj.getDouble("longitude"));
                            Log.d("Positions", response.toString());
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(locPos)
                                    .title("Saved Position"));

                            assert marker != null;
                            marker.setTag(String.valueOf(id));
                        }

                        if (arr.length() > 0) {
                            JSONObject last = arr.getJSONObject(arr.length() - 1);
                            LatLng lastPos = new LatLng(last.getDouble("latitude"), last.getDouble("longitude"));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPos, 10));
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MapsActivity.this, "Erreur de chargement des positions", Toast.LENGTH_SHORT).show();

                    }
                },
                error -> Log.e("VolleyShowError", error.toString())
        );
        requestQueue.add(jsonReq);
    }


    private void deletePositionFromServer(String id, Marker marker) {
        String url = "http://192.168.1.174/LocationTrackingBackend/src/api/deletePosition.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getBoolean("success")) {
                            marker.remove();
                            loadSavedPositions();
                            Log.d("DELETE_RESPONSE", response);
                            Toast.makeText(this, "Position deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("DELETE_RESPONSE", response);
                            Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Error deleting position", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("id", id);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
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
                    Toast.makeText(MapsActivity.this, "Position saved successfully", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Log.e("NetworkRequest", "Error: " + error.toString());
                    Toast.makeText(MapsActivity.this, "Failed to save position", Toast.LENGTH_SHORT).show();
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
