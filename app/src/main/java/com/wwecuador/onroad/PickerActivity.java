package com.wwecuador.onroad;

import android.content.Intent;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationSource;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.telemetry.*;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;
import com.mapbox.services.api.ServicesException;
import com.mapbox.services.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.services.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.services.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.services.commons.models.Position;
import com.wwecuador.onroad.logic.users.User;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PickerActivity extends AppCompatActivity implements PermissionsListener {

    private final int ENVIADO = 1;

    User usuarioActual;
    private long puntos;
    private long numAlerts;
    private double latitud;
    private double longitud;
    private Intent salida;
    String desde;

    private MapView mapView;
    private MapboxMap map;
    private LocationEngine locationEngine;
    private Marker droppedMarker;
    private ImageView hoveringMarker;
    private Button selectLocationButton;
    private LinearLayout buttons;
    private Button enviar;
    private Button cancelar;
    private LocationEngineListener locationEngineListener;
    private PermissionsManager permissionsManager;
    LatLng userPosition;

    private static final String TAG = "LocationPickerActivity";

    public static final String ALLOWED_DISTANCE = "allowed_distance";
    public static double ALLOWED_DISTANCE_VALUE = 0.001550;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the account manager
        setContentView(R.layout.activity_picker);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("usuario")) {
                usuarioActual = (User)  getIntent().getExtras().getSerializable("usuario");
                puntos = usuarioActual.getPuntos();
                numAlerts = usuarioActual.getNumAlertas();
            }
            if (extras.containsKey("desde")){
                desde = (String) getIntent().getExtras().getSerializable("desde");
                if (desde.equals("main")){
                    salida = new Intent(PickerActivity.this, MainActivity.class);
                } else if (desde.equals("cuenta")){
                    salida = new Intent(PickerActivity.this, CuentaActivity.class);
                    salida.putExtra("usuario", usuarioActual);
                } else if (desde.equals("alerts")){
                    salida = new Intent(PickerActivity.this, AlertsListActivity.class);
                    salida.putExtra("usuario", usuarioActual);
                } else {
                    salida = new Intent(PickerActivity.this, ContactoActivity.class);
                    salida.putExtra("usuario", usuarioActual);
                }
            }
        }

        // Get the location engine object for later use.
        locationEngine = LocationSource.getLocationEngine(this);
        locationEngine.activate();

        // Initialize the map view
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;

                // Once map is ready, we want to position the camera above the user location. We
                // first check that the user has granted the location permission, then we call
                // setInitialCamera.
                permissionsManager = new PermissionsManager(PickerActivity.this);
                if (!PermissionsManager.areLocationPermissionsGranted(PickerActivity.this)) {
                    permissionsManager.requestLocationPermissions(PickerActivity.this);
                } else {
                    setInitialCamera();
                }

                // Toast instructing user to tap on the map
                Toast.makeText(
                        PickerActivity.this,
                        getString(R.string.move_map_instruction),
                        Toast.LENGTH_LONG
                ).show();

            }
        });


        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        //mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(ALLOWED_DISTANCE, ALLOWED_DISTANCE_VALUE);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
        fetchConfig();


        buttons = (LinearLayout) findViewById(R.id.buttons);

        // When user is still picking a location, we hover a marker above the map in the center.
        // This is done by using an image view with the default marker found in the SDK. You can
        // swap out for your own marker image, just make sure it matches up with the dropped marker.
        hoveringMarker = new ImageView(this);
        hoveringMarker.setImageResource(R.drawable.green_picker);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        hoveringMarker.setLayoutParams(params);
        mapView.addView(hoveringMarker);

        // Button for user to drop marker or to pick marker back up.
        selectLocationButton = (Button) findViewById(R.id.select_location_button);
        selectLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map != null) {
                    if (droppedMarker == null) {
                        // We first find where the hovering marker position is relative to the map.
                        // Then we set the visibility to gone.
                        float coordinateX = hoveringMarker.getLeft() + (hoveringMarker.getWidth() / 2);
                        float coordinateY = hoveringMarker.getBottom();
                        float[] coords = new float[] {coordinateX, coordinateY};
                        final LatLng latLng = map.getProjection().fromScreenLocation(new PointF(coords[0], coords[1]));
                        hoveringMarker.setVisibility(View.GONE);


                        /*
                        selectLocationButton.setVisibility(View.GONE);
                        buttons.setVisibility(View.VISIBLE);

                        // Transform the appearance of the button to become the cancel button
                        selectLocationButton.setBackgroundColor(
                                ContextCompat.getColor(PickerActivity.this, R.color.colorAccent));
                        selectLocationButton.setText("Cancel");*/
                        // Create the marker icon the dropped marker will be using.
                        Icon icon = IconFactory.getInstance(PickerActivity.this).fromResource(R.drawable.green_picker);

                        // Placing the marker on the map as soon as possible causes the illusion
                        // that the hovering marker and dropped marker are the same.
                        droppedMarker = map.addMarker(new MarkerViewOptions().position(latLng).icon(icon));

                        // Finally we get the geocoding information
                        reverseGeocode(latLng);


                        //COMPRONADO SI ESTÁ LEJOS DE SU UBICACICÓN.
                        if (userPosition!=null){
                            Log.v(TAG, "PROBANDO1: "+ALLOWED_DISTANCE_VALUE+"\n"+userPosition.getLatitude()+"\n"
                                    +userPosition.getLongitude()+"\n"+latLng.getLatitude()+"\n"+latLng.getLongitude());
                            double userLat = (double)
                                    Math.round(userPosition.getLatitude() * 1000000d) / 1000000d;
                            double positionLat = (double)
                                    Math.round(latLng.getLatitude() * 1000000d) / 1000000d;
                            double userLong = (double)
                                    Math.round(userPosition.getLongitude() * 1000000d) / 1000000d;
                            double positionLong = (double)
                                    Math.round(latLng.getLongitude() * 1000000d) / 1000000d;
                            double distanciaLatitud = (double)
                                    Math.round((userLat-positionLat) * 1000000d) / 1000000d;
                            double distanciaLongitud = (double)
                                    Math.round((userLong-positionLong) * 1000000d) / 1000000d;

                            Log.v(TAG, "PROBANDO2: "+ALLOWED_DISTANCE_VALUE+"\n"+userLat+"\n"
                                    +userLong+"\n"+positionLat+"\n"+positionLong);

                            Log.v(TAG, "PROBANDO3: "+ALLOWED_DISTANCE_VALUE+"\n"+distanciaLatitud+"\n"
                            +distanciaLongitud);
                            if ((distanciaLatitud<=ALLOWED_DISTANCE_VALUE && distanciaLatitud >=
                                    -ALLOWED_DISTANCE_VALUE) &&
                                    (distanciaLongitud<=ALLOWED_DISTANCE_VALUE &&
                                            distanciaLongitud>=-ALLOWED_DISTANCE_VALUE)){
                                latitud = latLng.getLatitude();
                                longitud = latLng.getLongitude();
                                selectLocationButton.setVisibility(View.GONE);
                                buttons.setVisibility(View.VISIBLE);
                            } else {
                                Toast.makeText(PickerActivity.this, getString(R.string.far),
                                        Toast.LENGTH_SHORT).show();
                                map.removeMarker(droppedMarker);

                                // Switch the button apperance back to select a location.
                                buttons.setVisibility(View.GONE);
                                selectLocationButton.setVisibility(View.VISIBLE);

                                // Lastly, set the hovering marker back to visible.
                                hoveringMarker.setVisibility(View.VISIBLE);
                                droppedMarker = null;
                            }
                        }

                    }
                }
            }
        });

        enviar = (Button) findViewById(R.id.send_button);
        enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PickerActivity.this, SendAlertActivity.class);
                intent.putExtra("latitud", latitud);
                intent.putExtra("longitud", longitud);
                intent.putExtra("usuario", usuarioActual);
                intent.putExtra("desde", desde);
                startActivityForResult(intent, ENVIADO);
            }
        });

        cancelar = (Button) findViewById(R.id.cancel_button);
        cancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // When the marker is dropped, the user has clicked the button to cancel.
                // Therefore, we pick the marker back up.
                map.removeMarker(droppedMarker);

                // Switch the button apperance back to select a location.
                buttons.setVisibility(View.GONE);
                selectLocationButton.setVisibility(View.VISIBLE);

                // Lastly, set the hovering marker back to visible.
                hoveringMarker.setVisibility(View.VISIBLE);
                droppedMarker = null;
            }
        });


    } // End onCreate

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ENVIADO) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                map.removeMarker(droppedMarker);

                // Switch the button apperance back to select a location.
                buttons.setVisibility(View.GONE);
                selectLocationButton.setVisibility(View.VISIBLE);

                // Lastly, set the hovering marker back to visible.
                hoveringMarker.setVisibility(View.VISIBLE);
                droppedMarker = null;
            } else {
                usuarioActual.setNumAlertas(usuarioActual.getNumAlertas()+1);
                finish();
                overridePendingTransition(0, 0);
                startActivity(salida);
                overridePendingTransition(0, 0);
            }
        }
    }

    private void fetchConfig() {
        long cacheExpiration = 3600;
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()){
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            /*Toast.makeText(PickerActivity.this, "Fetch Succeeded",
                                    Toast.LENGTH_SHORT).show();
                            // After config data is successfully fetched, it must be activated before newly fetched
                            // values are returned.*/
                            mFirebaseRemoteConfig.activateFetched();
                        } else {
                            /*Toast.makeText(PickerActivity.this, "Fetch Failed",
                                    Toast.LENGTH_SHORT).show();*/
                        }
                        hacerAlgoConLosNuevosValores();
                    }
                });
    }

    public void hacerAlgoConLosNuevosValores(){
        ALLOWED_DISTANCE_VALUE = mFirebaseRemoteConfig.getDouble("allowed_distance");
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        finish();
        overridePendingTransition(0, 0);
        startActivity(salida);
        overridePendingTransition(0, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        if (locationEngine != null && locationEngineListener != null) {
            locationEngine.activate();
            locationEngine.addLocationEngineListener(locationEngineListener);
            locationEngine.requestLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        if (locationEngine != null && locationEngineListener != null) {
            locationEngine.removeLocationUpdates();
            locationEngine.removeLocationEngineListener(locationEngineListener);
            locationEngine.deactivate();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void setInitialCamera() {
        // Method is used to set the initial map camera position. Should only be called once when
        // the map is ready. We first try using the users last location so we can quickly set the
        // camera as fast as possible.
        if (locationEngine.getLastLocation() != null) {
            userPosition = new LatLng(locationEngine.getLastLocation());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationEngine.getLastLocation()), 16));
        }

        // This location listener is used in a very specific use case. If the users last location is
        // unknown we wait till the GPS locates them and position the camera above.
        locationEngineListener = new LocationEngineListener() {
            @Override
            public void onConnected() {
                locationEngine.requestLocationUpdates();
            }

            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    // Move the map camera to where the user location is
                    map.setCameraPosition(new CameraPosition.Builder()
                            .target(new LatLng(location))
                            .zoom(16)
                            .build());
                    locationEngine.removeLocationEngineListener(this);
                    userPosition = new LatLng(location);
                }
            }
        };
        locationEngine.addLocationEngineListener(locationEngineListener);
        // Enable the location layer on the map and track the user location until they perform a
        // map gesture.
        map.setMyLocationEnabled(true);
        map.getTrackingSettings().setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
    } // End setInitialCamera

    private void reverseGeocode(final LatLng point) {
        // This method is used to reverse geocode where the user has dropped the marker.
        try {
            MapboxGeocoding client = new MapboxGeocoding.Builder()
                    .setAccessToken(getString(R.string.access_token))
                    .setCoordinates(Position.fromCoordinates(point.getLongitude(), point.getLatitude()))
                    .setGeocodingType(GeocodingCriteria.TYPE_ADDRESS)
                    .build();

            client.enqueueCall(new Callback<GeocodingResponse>() {
                @Override
                public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {

                    List<CarmenFeature> results = response.body().getFeatures();
                    if (results.size() > 0) {
                        CarmenFeature feature = results.get(0);
                        // If the geocoder returns a result, we take the first in the list and update
                        // the dropped marker snippet with the information. Lastly we open the info
                        // window.
                        if (droppedMarker != null) {
                            droppedMarker.setSnippet(feature.getPlaceName());
                            map.selectMarker(droppedMarker);
                        }

                    } else {
                        if (droppedMarker != null) {
                            droppedMarker.setSnippet(getString(R.string.no_results));
                            map.selectMarker(droppedMarker);
                        }
                    }
                }

                @Override
                public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                    //Log.e(TAG, "PRUEBA: Falla en el Geocoding: " + throwable.getMessage());
                }
            });
        } catch (ServicesException servicesException) {
            //Log.e(TAG, "PRUEBA: Error geocoding: " + servicesException.toString());
            servicesException.printStackTrace();
        }
    } // reverseGeocode

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, getString(R.string.necesita_explicacion),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            setInitialCamera();
        } else {
            Toast.makeText(this, getString(R.string.sin_permisos),
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
