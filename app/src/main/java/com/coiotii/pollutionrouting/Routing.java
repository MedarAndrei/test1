package com.coiotii.pollutionrouting;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class Routing extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMarkerClickListener {


    private GoogleMap mMap;
    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private Location lastlocation;
    private Marker currentLocationMarker;
    private Marker lastSearchMarker = null;
    public static final int REQUEST_LOCATION_CODE = 99;
    int PROXIMITY_RADIUS = 10000;
    double latitude, longitude;
    double end_latitutde, end_longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();

        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        if (client == null) {
                            bulidGoogleApiClient();
                        }
                        //mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();
                }
        }
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            bulidGoogleApiClient();
            //mMap.setMyLocationEnabled(true);
        }
    }


    protected synchronized void bulidGoogleApiClient() {
        client = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        client.connect();

    }

    @Override
    public void onLocationChanged(Location location) {

//        mMap.clear();
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        lastlocation = location;
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        Log.d("lat = ", "" + latitude);
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Location");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        markerOptions.draggable(true);
        if (currentLocationMarker == null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
        }
        currentLocationMarker = mMap.addMarker(markerOptions);


        if (client != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
        }
    }

    public void searchTo() {
        EditText tf_location = findViewById(R.id.B_location);
        String location = tf_location.getText().toString();
        List<Address> addressList;

        if (!location.equals("")) {
            Geocoder geocoder = new Geocoder(this);

            if (lastSearchMarker != null) {
                lastSearchMarker.remove();
            }


            try {
                addressList = geocoder.getFromLocationName(location, 1);
//                        mMap.getMyLocation()

                if (addressList != null) {
                    for (int i = 0; i < addressList.size(); i++) {
                        LatLng latLng = new LatLng(addressList.get(i).getLatitude(), addressList.get(i).getLongitude());
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng);
                        markerOptions.title(location);
                        lastSearchMarker = mMap.addMarker(markerOptions.draggable(true));
                        mMap.setOnMarkerDragListener(new OnMarkerDragListener() {
                            @Override
                            public void onMarkerDragStart(Marker marker) {

                            }

                            @Override
                            public void onMarkerDrag(Marker marker) {
                                //marker.setDraggable(true);
                            }

                            @Override
                            public void onMarkerDragEnd(Marker marker) {
                                //marker.getPosition();
                                Toast.makeText(Routing.this, marker.getId() + " " + lastSearchMarker.getId() + " " + currentLocationMarker.getId(), Toast.LENGTH_SHORT).show();
                                if (marker.getId().equals(lastSearchMarker.getId())) {
                                    lastSearchMarker.setPosition(marker.getPosition());
                                } else if (marker.getId().equals(currentLocationMarker.getId())) {
                                    currentLocationMarker.setPosition(marker.getPosition());
                                }
                            }
                        });
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
//                        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void searchFrom() {
        EditText tf_location = findViewById(R.id.A_location);
        String location = tf_location.getText().toString();
        List<Address> addressList;

        if (!location.equals("")) {
            Geocoder geocoder = new Geocoder(this);

            if (currentLocationMarker != null) {
                currentLocationMarker.remove();
            }


            try {
                addressList = geocoder.getFromLocationName(location, 1);
//                        mMap.getMyLocation()

                if (addressList != null) {
                    for (int i = 0; i < addressList.size(); i++) {
                        LatLng latLng = new LatLng(addressList.get(i).getLatitude(), addressList.get(i).getLongitude());
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng);
                        markerOptions.title(location);
                        currentLocationMarker = mMap.addMarker(markerOptions.draggable(true)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                        mMap.setOnMarkerDragListener(new OnMarkerDragListener() {
                            @Override
                            public void onMarkerDragStart(Marker marker) {

                            }

                            @Override
                            public void onMarkerDrag(Marker marker) {
                                //marker.setDraggable(true);
                            }

                            @Override
                            public void onMarkerDragEnd(Marker marker) {
                                //marker.getPosition();
                                Toast.makeText(Routing.this, marker.getId() + " " + lastSearchMarker.getId() + " " + currentLocationMarker.getId(), Toast.LENGTH_SHORT).show();
                                if (marker.getId().equals(lastSearchMarker.getId())) {
                                    lastSearchMarker.setPosition(marker.getPosition());
                                } else if (marker.getId().equals(currentLocationMarker.getId())) {
                                    currentLocationMarker.setPosition(marker.getPosition());
                                }
                            }
                        });
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
//                        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onClick(View v) {
        Object dataTransfer[] = new Object[2];
        GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();

        switch (v.getId()) {
            case R.id.B_search:
                searchTo();
                break;
            case R.id.A_search:
                searchFrom();
                break;
            case R.id.A_current_location:
                currentLocationMarker.setPosition(new LatLng(latitude, longitude));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocationMarker.getPosition(), 14));
                break;
            case R.id.B_to:

                end_latitutde = lastSearchMarker.getPosition().latitude;
                end_longitude = lastSearchMarker.getPosition().longitude;

                double start_latitude = currentLocationMarker.getPosition().latitude;
                double start_longitude = currentLocationMarker.getPosition().longitude;

                float[] res = new float[1];
                Location.distanceBetween(start_latitude, start_longitude, end_latitutde, end_longitude, res);
//                markerOptions.snippet("Distance " + res[0]);
                Toast.makeText(this, "Distance " + res[0], Toast.LENGTH_LONG).show();
//                mMap.addMarker(markerOptions);
                break;
        }
    }


    private String getUrl(double latitude, double longitude, String nearbyPlace) {

        StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlaceUrl.append("location=" + latitude + "," + longitude);
        googlePlaceUrl.append("&radius=" + PROXIMITY_RADIUS);
        googlePlaceUrl.append("&type=" + nearbyPlace);
        googlePlaceUrl.append("&sensor=true");
        googlePlaceUrl.append("&key=" + "AIzaSyBLEPBRfw7sMb73Mr88L91Jqh3tuE4mKsE");

        Log.d("MapsActivity", "url = " + googlePlaceUrl.toString());

        return googlePlaceUrl.toString();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        locationRequest = new LocationRequest();
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
        }
    }


    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
            }
            return false;

        } else
            return true;
    }


    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.setDraggable(true);
        return false;
    }
}


