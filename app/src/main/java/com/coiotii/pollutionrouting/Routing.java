package com.coiotii.pollutionrouting;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private Polyline currentPolyline = null;
    private ArrayList<Polyline> drawnPolylines = new ArrayList<Polyline>();
    private ArrayList<Integer> scoreSum = new ArrayList<Integer>();
    private ArrayList<Integer> routeScores = new ArrayList<Integer>();
    private ArrayList<Integer> amountOfPointsPerRoute = new ArrayList<Integer>();
    private int totalAmountOfPoints = 0;
    private int amountOfPointsSoFar = 0;
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(100, 5000,
                                                    1, TimeUnit.MINUTES,
                                                    new LinkedBlockingDeque<Runnable>());


    //directions api key
    //AIzaSyDhs2hwfjjmR1w3r-mNg95wvFc55qyGE_I
    private GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext();
        return geoApiContext.setQueryRateLimit(3)
                .setApiKey("AIzaSyDhs2hwfjjmR1w3r-mNg95wvFc55qyGE_I")
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS);
    }

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
                                //TODO check if they're null
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

                DateTime now = new DateTime();
                DirectionsResult result = null;
                try {
                    LatLng orig = currentLocationMarker.getPosition();
                    String originString = orig.latitude + "," + orig.longitude;

                    LatLng dest = lastSearchMarker.getPosition();
                    String destString = dest.latitude + "," + dest.longitude;

                    result = DirectionsApi.newRequest(getGeoContext())
                            .mode(TravelMode.DRIVING).origin(originString)
                            .destination(destString)
                            .departureTime(now)
                            .alternatives(true)
                            .await();
                } catch (ApiException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (result != null) {
                    //delete the old  routes
                    for (Polyline l: drawnPolylines) {
                        l.remove();
                    }

                    //get the new routes
                    Toast.makeText(this, new Integer(result.routes.length).toString(), Toast.LENGTH_LONG).show();
                    for (int i = result.routes.length - 1; i >= 1; i--) {
                        List<LatLng> decodedPath = decodePoly(result.routes[i].overviewPolyline.getEncodedPath());
                        Polyline poly = mMap.addPolyline(new PolylineOptions().addAll(decodedPath).color(0xffaaaaaa));
                        poly.setClickable(true);
                        poly.setZIndex(0);
                        drawnPolylines.add(poly);
                    }
                    List<LatLng> decodedPath = decodePoly(result.routes[0].overviewPolyline.getEncodedPath());
                    currentPolyline = mMap.addPolyline(new PolylineOptions().addAll(decodedPath).color(0xffff6c00));
                    currentPolyline.setClickable(true);
                    currentPolyline.setZIndex(10);
                    drawnPolylines.add(currentPolyline);

                    mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
                        @Override
                        public void onPolylineClick(Polyline polyline) {
                            if (polyline.getId().equals(currentPolyline.getId())) {
                                return;
                            }

                            currentPolyline.setColor(0xffaaaaaa);
                            currentPolyline.setZIndex(0);
                            polyline.setColor(0xffff6c00);
                            currentPolyline = polyline;
                            currentPolyline.setZIndex(10);

                            int index = drawnPolylines.indexOf(currentPolyline);
                            if (routeScores.size() != 0) {
                                Toast.makeText(Routing.this, new Integer(routeScores.get(index)).toString(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
                break;
            case R.id.breeze:




                scoreSum.clear();
                routeScores.clear();
                amountOfPointsPerRoute.clear();
                amountOfPointsSoFar = 0;
                totalAmountOfPoints = 0;

                for (int i = 0; i < drawnPolylines.size(); i++) {
                    scoreSum.add(0);
                    routeScores.add(0);
                    amountOfPointsPerRoute.add(drawnPolylines.get(i).getPoints().size() / 8);
                    totalAmountOfPoints = i * 8 + 8;
//                    for (int j = 0; j < drawnPolylines.get(i).getPoints().size(); j += 8) {
//                        totalAmountOfPoints++;
//                        amountOfPointsPerRoute.set(i, amountOfPointsPerRoute.get(i) + 1);
//                    }
//                    totalAmountOfPoints += drawnPolylines.get(i).getPoints().size() / 8;
                }



                for (int i = 0; i < drawnPolylines.size(); i++) {
                    for (int j = 0; j < 8; j++) {
                        URI uri = null;
                        double olatitude = drawnPolylines.get(i).getPoints().get(j * amountOfPointsPerRoute.get(i)).latitude;
                        double olongitude = drawnPolylines.get(i).getPoints().get(j * amountOfPointsPerRoute.get(i)).longitude;
                        try {
                            uri = new URI("https://api.breezometer.com/baqi/?lat="+olatitude+"&lon="+olongitude+"&fields=breezometer_aqi&key=deeb4795f89f4d15a2723566069d9568");
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }

                        GetAQPair param = new GetAQPair();
                        param.uri = uri;
                        param.latitude = drawnPolylines.get(i).getPoints().get(j * amountOfPointsPerRoute.get(i)).latitude;
                        param.longitude = drawnPolylines.get(i).getPoints().get(j * amountOfPointsPerRoute.get(i)).longitude;
                        param.route = i;

                        AsyncTask<GetAQPair, Void, GetAQRet> a = new AsyncTask<GetAQPair, Void, GetAQRet>() {

                            @Override
                            protected GetAQRet doInBackground(GetAQPair... pairs) {
                                return getAQ(pairs[0]);
                            }


                            @Override
                            protected void onPostExecute(GetAQRet s) {
                                //Toast.makeText(Routing.this, new Integer(s.retVal).toString(), Toast.LENGTH_LONG).show();
                                scoreSum.set(s.route, scoreSum.get(s.route) + s.retVal);
                                Log.d("ASYNC ", amountOfPointsSoFar + " " + totalAmountOfPoints);
                                amountOfPointsSoFar++;

                                mMap.addMarker(new MarkerOptions().position(new LatLng(s.latitude, s.longitude)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                                if (amountOfPointsSoFar == totalAmountOfPoints) {
//                                    we are done
                                    for (int i = 0; i < routeScores.size(); i++) {
                                        Log.d("End ASYNC sums", scoreSum.get(i).toString());
                                        Log.d("END ASYNC ", amountOfPointsPerRoute.get(i) + " " + scoreSum.get(i));
                                        routeScores.set(i, scoreSum.get(i) / 8 );
                                    }

                                    Toast.makeText(Routing.this, "Boss de boss, barosan", Toast.LENGTH_LONG).show();
                                }
                            }
                        };
//                        a.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, param);
                        a.executeOnExecutor(tpe, param);
//                        a.execute(param);
                    }
                }

                //Toast.makeText(this, response, Toast.LENGTH_LONG).show();
                break;
        }
    }

    private class GetAQPair {
        public URI uri;
        public double longitude;
        public double latitude;
        public int route;
        public int route_index;
    }

    private class GetAQRet {
        public int retVal;
        public double longitude;
        public double latitude;
        public int route;
        public int route_index;
    }

    public GetAQRet getAQ(GetAQPair pair) {
        URI uri = pair.uri;
        HttpResponse response;
        String responseString = "";

        try{
            //Toast.makeText(this, uri.toString(), Toast.LENGTH_LONG).show();
            HttpGet request = new HttpGet(uri);
            HttpClient client = new DefaultHttpClient();
            response = client.execute(request);
            HttpEntity httpEntity = response.getEntity();
            responseString = EntityUtils.toString(httpEntity);
        } catch (Exception e){
            //Toast.makeText(this, "CRASHES SOMEWHERE OMG", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        GetAQRet ret = new GetAQRet();
        ret.route = pair.route;
        ret.route_index = pair.route_index;
        ret.latitude = pair.latitude;
        ret.longitude = pair.longitude;
        String num = responseString.substring(responseString.indexOf(' ')+1, responseString.lastIndexOf('}'));
        ret.retVal = new Integer(num);
        return ret;

    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
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


