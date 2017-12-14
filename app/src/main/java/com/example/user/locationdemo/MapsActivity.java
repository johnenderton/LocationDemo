package com.example.user.locationdemo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener,
        GoogleMap.OnCameraMoveCanceledListener, GoogleMap.OnCameraMoveListener, SensorEventListener{

    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private List<Marker> markerList;
    private Polyline line = null;
    private Button btnchangemap;

    private final String LOG_TAG = "TestApp................";
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double Lat, Long;
    private Location mLastLocation;
    private Marker mCurrentPosMarker;
    private FusedLocationProviderClient mfusedLocationProviderClient;
    private LatLngBounds latLngBounds;
    private GeoDataClient mGeoDataClient;

    // Firebase variables
    private FirebaseDatabase mFirebaseDatabse;
    private DatabaseReference mMessageDatabaseReference;
    private ChildEventListener mChileEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // set up google api client
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        markerList = new ArrayList<>();
        markerList.clear();
        btnchangemap = (Button)findViewById(R.id.btn_change_map);

        // Initialize firebase variables
        mFirebaseDatabse = FirebaseDatabase.getInstance();
        mMessageDatabaseReference = mFirebaseDatabse.getReference().child("markers");

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setZoomControlsEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
        }

        // set up listener. When a new marker is added or there's a change in firebase, all markers are shown in google map
        mChileEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                // TODO: handle the post
                location mylocation = dataSnapshot.getValue(location.class);
                LatLng ll = new LatLng(mylocation.getLat(), mylocation.getLng());
                BitmapDescriptor icon = myIcon(mylocation.getType());
                MarkerOptions markerOptions = new MarkerOptions().position(ll)
                        .title(mylocation.getName())
                        .icon(icon);
                if (mylocation.getAddress() != null) {
                    markerOptions.snippet(mylocation.getAddress());
                }
                //mMap.addMarker(markerOptions);
                markerList.add(mMap.addMarker(markerOptions));
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                // TODO: handle the post
                location mylocation = dataSnapshot.getValue(location.class);
                LatLng ll = new LatLng(mylocation.getLat(), mylocation.getLng());
                BitmapDescriptor icon = myIcon(mylocation.getType());
                MarkerOptions markerOptions = new MarkerOptions().position(ll)
                        .title(mylocation.getName())
                        .icon(icon);
                if (mylocation.getAddress() != null) {
                    markerOptions.snippet(mylocation.getAddress());
                }
                //mMap.addMarker(markerOptions);
                markerList.add(mMap.addMarker(markerOptions));
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        mMessageDatabaseReference.addChildEventListener(mChileEventListener);
        mMap.setOnCameraMoveCanceledListener(this);
        mMap.setOnCameraMoveListener(this);

        try {
            // search a place
            AutoSearch();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        }
        mMap.setOnMarkerClickListener(this);
    }

    // click a marker event
    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.i(LOG_TAG, "a Marker is clicked");
        LatLng origin = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(marker.getPosition(), origin);
        DownloadTask downloadTask = new DownloadTask();
        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
        return false;
    }

    // get lat lng bound when move map end
    @Override
    public void onCameraMoveCanceled() {
        latLngBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
//        Log.i(LOG_TAG, "LatLng: " + latLngBounds.toString());
        for (Marker marker: markerList) {
            if (latLngBounds.contains(marker.getPosition())) {
                marker.setVisible(true);
            } else {
                marker.setVisible(false);
            }
        }
    }

    // get lat lng bound when move map
    @Override
    public void onCameraMove() {
        latLngBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
//        Log.i(LOG_TAG, "LatLng: " + latLngBounds.toString());
        for (Marker marker: markerList) {
            if (latLngBounds.contains(marker.getPosition())) {
                marker.setVisible(true);
            } else {
                marker.setVisible(false);
            }
        }
    }

    // search a place function
    public void AutoSearch() throws GooglePlayServicesNotAvailableException, GooglePlayServicesRepairableException {
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setCountry("VN")
                .build();
        autocompleteFragment.setFilter(typeFilter);

        Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                .setFilter(typeFilter)
                .build(this);
        startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i(LOG_TAG, "Place: " + place.getName());
                LatLng latLng = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(place.getName().toString())
                        .snippet(place.getAddress().toString()));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
            }
            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(LOG_TAG, "An error occurred: " + status);
            }
        });
    }

    public void OnMapSearch(View view) {
//        EditText locationSearch = (EditText) findViewById(R.id.editText_search);
//        String location = locationSearch.getText().toString();
//        List<Address> addressList = null;
//
//        if (location != null || !location.equals("")) {
//            Geocoder geocoder = new Geocoder(this);
//            try {
//                addressList = geocoder.getFromLocationName(location, 1);
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            Address address = addressList.get(0);
//            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
//            mMap.addMarker(new MarkerOptions().position(latLng).title("Search Location"));
//            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
//        }
    }

    // set map type
    public void onClicked_mapType_change(View view) {
        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(this, btnchangemap);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getTitle().toString()) {
                    case "normal":
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        btnchangemap.setText("Normal");
                        return true;
                    case "satellite":
                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        btnchangemap.setText("Satellite");
                        return true;
                    case "hybrid":
                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        btnchangemap.setText("Hybrid");
                        return true;
                    case "terrain":
                        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        btnchangemap.setText("Terrain");
                        return true;
                    default:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        btnchangemap.setText("Normal");
                        return true;
                }
            }
        });
        popup.show();//showing popup menu
    }

    // set up icon for marker
    public BitmapDescriptor myIcon (String icon_name) {
        BitmapDescriptor icon;
        switch (icon_name) {
            case "phuclong":
                icon = BitmapDescriptorFactory.fromResource(R.drawable.phuclong);
                break;
            case "aeonmall":
                icon = BitmapDescriptorFactory.fromResource(R.drawable.aeonmall);
                break;
            case "coopmart":
                icon = BitmapDescriptorFactory.fromResource(R.drawable.coopmart);
                break;
            case "lottemart":
                icon = BitmapDescriptorFactory.fromResource(R.drawable.lottemart);
                break;
            case "mcdonald":
                icon = BitmapDescriptorFactory.fromResource(R.drawable.mcdonald);
                break;
            case "rbtea":
                icon = BitmapDescriptorFactory.fromResource(R.drawable.rbtea);
                break;
            default:
                icon = BitmapDescriptorFactory.defaultMarker();
                break;
        }
        return icon;
    }

    // update lat lng for marker that show current location
    @Override
    public void onLocationChanged(Location location) {
        Log.i(LOG_TAG, location.toString());
        if (mCurrentPosMarker != null) {
            mCurrentPosMarker.remove();
        }
        if (mLastLocation != location) {
            mLastLocation = location;
            Lat = mLastLocation.getLatitude();
            Long = mLastLocation.getLongitude();
            LatLng currentPos = new LatLng(Lat, Long);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(currentPos);
            markerOptions.title("Current Position");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            mCurrentPosMarker = mMap.addMarker(markerOptions);
            if (mGoogleApiClient == null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPos));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
            }
        }
        //stop location updates
        if (mGoogleApiClient != null) {
//            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted.
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
            // other 'case' lines to check for other permissions this app might request.
            //You can add here other case statements according to your requirement.
        }
    }

    // get last known location
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(LOG_TAG, "Device Connected");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            Log.i(LOG_TAG, "Permission Checked");
            mfusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            mfusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                Lat = location.getLatitude();
                                Long = location.getLongitude();
                                LatLng currentPos = new LatLng(Lat, Long);
                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(currentPos);
                                markerOptions.title("Last Position");
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                                mCurrentPosMarker = mMap.addMarker(markerOptions);
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPos));
                                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                            }
                        }
                    });
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "GoogleApiClient connection has been suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "GoogleApiClient connection has failed");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "Activity start");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "Activity pause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "Activity Resume");
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
        Log.i(LOG_TAG, "Activity Stop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Activity Destroy");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(LOG_TAG, "Activity Restart");
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=driving";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
    /**
     * A class to parse the Google Places in JSON format
     */
    class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);
            }
//      Drawing polyline in the Google Map for the i-th route
            if (line != null) {
                line.remove();
            }
            line = mMap.addPolyline(lineOptions);
        }

    }
    class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }
}
