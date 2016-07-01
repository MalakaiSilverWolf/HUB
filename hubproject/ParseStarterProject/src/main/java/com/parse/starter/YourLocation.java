package com.parse.starter;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class YourLocation extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationManager locationManager;
    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;

    TextView infoTextView;
    Button requestHUBbutton;

    Boolean requestActive = false;
    String driverUsername = "";
    ParseGeoPoint driverLocation = new ParseGeoPoint(0, 0);

    Handler handler = new Handler();


    public void requestRide(View view) {

        if (!requestActive) {

            Log.i("MyApp", "HUB requested");

            ParseObject request = new ParseObject("Requests");

            request.put("requesterUsername", ParseUser.getCurrentUser().getUsername());

            ParseACL parseACL = new ParseACL();
            parseACL.setPublicWriteAccess(true);
            parseACL.setPublicReadAccess(true);
            request.setACL(parseACL);

            request.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {

                    if (e == null) {

                        infoTextView.setText("Finding HUB driver...");
                        requestHUBbutton.setText("Cancel HUB");
                        requestActive = true;
                    }
                }
            });
        } else {

            infoTextView.setText("HUB Cancelled.");
            requestHUBbutton.setText("Request HUB");
            requestActive = false;

            ParseQuery<ParseObject> query = new ParseQuery<>("Requests");

            query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {

                    if (e == null) {

                        if (objects.size() > 0) {

                            for (ParseObject object : objects) {

                                object.deleteInBackground();

                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_location);
        String username = getIntent().getStringExtra("Username");
        TextView tv = (TextView) findViewById(R.id.TVusername);
        tv.setText(username);

        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMapAsync(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
        } else {
            Toast.makeText(this, "You have to accept to enjoy all app's services!", Toast.LENGTH_LONG).show();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

            }
        }

        infoTextView = (TextView) findViewById(R.id.infoTextView);
        requestHUBbutton = (Button) findViewById(R.id.requestHUB);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setUpMap();
    }

    public void updateLocation(final Location location) {

        mMap.clear();

        if (!requestActive) {

            ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
            query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {

                    if (e == null) {

                        if (objects.size() > 0) {

                            for (ParseObject object : objects) {

                                requestActive = true;
                                infoTextView.setText("Finding HUB driver...");
                                requestHUBbutton.setText("Cancel HUB");

                                if (object.get("driverUsername") != null) {

                                    driverUsername = object.getString("driverUsername");
                                    infoTextView.setText("Your driver is on their way!");
                                    requestHUBbutton.setVisibility(View.INVISIBLE);

                                    Log.i("AppInfo", driverUsername);
                                }
                            }
                        }
                    }
                }
            });
        }

        if (driverUsername.equals("")) {

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 10));

            mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title("Your Location"));

        }

        if (requestActive) {

            if (!driverUsername.equals("")) {

                ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
                userQuery.whereEqualTo("username", driverUsername);
                userQuery.findInBackground(new FindCallback<ParseUser>() {
                    @Override
                    public void done(List<ParseUser> objects, ParseException e) {
                        if (e == null) {
                            if (objects.size() > 0) {
                                for (ParseUser driver : objects) {
                                    driverLocation = driver.getParseGeoPoint("location");
                                }
                            }
                        }
                    }
                });

                if (driverLocation.getLatitude() != 0 && driverLocation.getLongitude() != 0) {

                    Log.i("AppInfo", driverLocation.toString());

                    Double distanceInMiles = driverLocation.distanceInMilesTo(new ParseGeoPoint(location.getLatitude(), location.getLongitude()));

                    Double distanceOneDP = (double) Math.round(distanceInMiles * 10) / 10;

                    String dist = "Your driver is " + distanceOneDP.toString() + " miles away ";
                    infoTextView.setText(dist);

                    LatLngBounds.Builder builder = new LatLngBounds.Builder();

                    ArrayList<Marker> markers = new ArrayList<>();

                    markers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude())).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)).title("Driver Location")));

                    markers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title("Your Location")));

                    for (Marker marker : markers) {
                        builder.include(marker.getPosition());
                    }

                    LatLngBounds bounds = builder.build();

                    int padding = 100;
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                    mMap.animateCamera(cu);
                }
            }

            final ParseGeoPoint userLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

            ParseQuery<ParseObject> query = new ParseQuery<>("Requests");

            query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {

                    if (e == null) {

                        if (objects.size() > 0) {

                            for (ParseObject object : objects) {

                                object.put("requesterLocation", userLocation);
                                object.saveInBackground();
                            }
                        }
                    }
                }
            });
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateLocation(location);
            }
        }, 5000);
    }

    private void setUpMap() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            Toast.makeText(this, "You have to accept to enjoy all app's services!", Toast.LENGTH_LONG).show();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        mMap.animateCamera(cameraUpdate);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
        } else {
            Toast.makeText(this, "You have to accept to enjoy all app's services!", Toast.LENGTH_LONG).show();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
            }
        }
        locationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }
}



