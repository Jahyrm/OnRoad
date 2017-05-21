package com.wwecuador.onroad;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationSource;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.navigation.v5.MapboxNavigation;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;
import com.mapbox.services.android.ui.geocoder.GeocoderAutoCompleteView;
import com.mapbox.services.api.ServicesException;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.MapboxDirections;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;
import com.wwecuador.onroad.logic.Alert;
import com.wwecuador.onroad.logic.users.User;
import com.wwecuador.onroad.logic.users.UserAlerts;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.services.Constants.PRECISION_6;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, PermissionsListener {

    private static final String TAG = "MainActivity";

    public static final int RC_SIGN_IN = 1;
    public static final int ELIMINADO = 2;

    private boolean mUsuarioExistente = false;
    private boolean mSeguirComprobando =  true;
    private User mCurrentUser;
    private Alert alertaConZoom;
    private boolean zoom=false;
    private double lat;
    private double lng;
    private double userLat;
    private double userLng;
    private boolean conAlerta;

    //Instacias de Android
    View header;
    CircleImageView profilePhoto;
    TextView usernameTextView;
    TextView emailTextView;
    DrawerLayout drawer;
    private FloatingActionMenu menuGreen;
    private FloatingActionButton fab1;
    private FloatingActionButton fab2;
    private FloatingActionButton fab3;
    NavigationView navigationView;

    //Mapox instance variables
    private MapView mapView;
    private MapboxMap map;
    private LocationEngine locationEngine;
    //private FloatingActionButton floatingActionButton;
    private LocationEngineListener locationEngineListener;
    private PermissionsManager permissionsManager;
    private DirectionsRoute currentRoute;
    private MapboxDirections client;
    private Position origin;
    private Marker marcador_origen;
    private Position destination;
    private MarkerView marcador_destino;
    private MapboxNavigation navigation;
    GeocoderAutoCompleteView autocompleteOrigenView;
    GeocoderAutoCompleteView autocompleteDestinoView;
    LatLng userPosition;

    //Firebase instance variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUsersDatabaseReference;
    private DatabaseReference mUserDatabaseReference;
    private DatabaseReference mUserAlertsDatabaseReference;
    private DatabaseReference mAlertsDatabaseReference;
    private ChildEventListener mListenerDeUsuarios;
    private ChildEventListener mListenerDeAlertas;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        navigation = new MapboxNavigation(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("alerta")) {
                conAlerta = true;
                alertaConZoom = (Alert) getIntent().getExtras().getSerializable("alerta");
                lat = alertaConZoom.getLatitud();
                lng = alertaConZoom.getLongitud();
                zoom = true;
            }
        } else {
            conAlerta = false;
        }

        header = ((NavigationView)findViewById(R.id.nav_view)).getHeaderView(0);
        profilePhoto = ((CircleImageView) header.findViewById(R.id.profileImageView));
        usernameTextView = ((TextView) header.findViewById(R.id.usernameTextView));
        emailTextView = ((TextView) header.findViewById(R.id.emailTextView));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get the location engine object for later use.
        locationEngine = LocationSource.getLocationEngine(this);
        locationEngine.activate();

        //Inflando el mapa
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        //Características del mapa.
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                // Interact with the map using mapboxMap here
                map = mapboxMap;
                map.getUiSettings().setCompassMargins(0, 220, 30, 0);

                // Once map is ready, we want to position the camera above the user location. We
                // first check that the user has granted the location permission, then we call
                // setInitialCamera.
                permissionsManager = new PermissionsManager(MainActivity.this);
                if (!PermissionsManager.areLocationPermissionsGranted(MainActivity.this)) {
                    permissionsManager.requestLocationPermissions(MainActivity.this);
                } else {
                    setInitialCamera();
                }




                map.setInfoWindowAdapter(new MapboxMap.InfoWindowAdapter() {

                    private int tenDp = (int) getResources().getDimension(R.dimen.fab_margin);
                    @Nullable
                    @Override
                    public View getInfoWindow(@NonNull Marker marker) {
                        if (marker.getTitle().equals(getString(R.string.origen_title)) || marker.equals(getString(R.string.destination_title))){
                        } else {
                            String color = marker.getSnippet().substring(0, marker.getSnippet().indexOf("|"));
                            TextView textView = new TextView(MainActivity.this);
                            textView.setText(marker.getTitle());
                            textView.setTextColor(Color.WHITE);
                            textView.setBackgroundColor(Color.parseColor(color));
                            textView.setPadding(tenDp, tenDp, tenDp, tenDp);
                            return textView;
                        }
                        return null;
                    }
                });

                map.setOnInfoWindowClickListener(new MapboxMap.OnInfoWindowClickListener() {
                    @Override
                    public boolean onInfoWindowClick(@NonNull Marker marker) {
                        if (marker.getTitle().equals(getString(R.string.origen_title)) || marker.equals(getString(R.string.destination_title))){
                        } else {
                            Intent intent = new Intent(MainActivity.this, AlertActivity.class);
                            intent.putExtra("usuario", mCurrentUser);
                            intent.putExtra("latitud", userLat);
                            intent.putExtra("longitud", userLng);
                            intent.putExtra("snippet", marker.getSnippet());
                            intent.putExtra("numAlerts", mCurrentUser.getNumAlertas());
                            //finish();
                            //overridePendingTransition(0, 0);
                            startActivityForResult(intent, ELIMINADO);
                            return true;
                        }
                        return false;
                    }
                });

                if (zoom){
                    CameraPosition position = new CameraPosition.Builder()
                            .target(new LatLng(lat, lng)) // Sets the new camera position
                            .zoom(17) // Sets the zoom
                            .bearing(360) // Rotate the camera
                            .tilt(30) // Set the camera tilt
                            .build(); // Creates a CameraPosition from the builder
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 7000);
                }

                /*map.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        Intent intent = new Intent(MainActivity.this, AlertActivity.class);
                        intent.putExtra("snippet", marker.getSnippet());
                        startActivity(intent);
                        return true;
                    }
                });*/
            }
        });

        //Obteniendo acceso a la base de datos
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();

        //Rutas específicas
        mUsersDatabaseReference = mFirebaseDatabase.getReference().child("users");
        mAlertsDatabaseReference = mFirebaseDatabase.getReference().child("alerts");

        // Set up autocomplete widget
        autocompleteOrigenView = (GeocoderAutoCompleteView) findViewById(R.id.origen);
        autocompleteOrigenView.setAccessToken(Mapbox.getAccessToken());
        autocompleteOrigenView.setType(GeocodingCriteria.TYPE_POI);
        autocompleteOrigenView.setOnFeatureListener(new GeocoderAutoCompleteView.OnFeatureListener() {
            @Override
            public void onFeatureClick(CarmenFeature feature) {
                hideOnScreenKeyboard();
                Position position = feature.asPosition();
                updateMap(position.getLatitude(), position.getLongitude(), R.string.origen_title);
                //origin = Position.fromCoordinates(position.getLatitude(), position.getLongitude());
                // Alhambra landmark in Granada, Spain.//From Alhmatched_route.geojson
                origin = Position.fromCoordinates(-3.588098, 37.176164);
                //origin = Position.fromCoordinates(-118.24233, 34.05332);
                autocompleteOrigenView.setEnabled(false);
            }
        });

        // Set up autocomplete widget
        autocompleteDestinoView = (GeocoderAutoCompleteView) findViewById(R.id.destino);
        autocompleteDestinoView.setAccessToken(Mapbox.getAccessToken());
        autocompleteDestinoView.setType(GeocodingCriteria.TYPE_POI);
        autocompleteDestinoView.setOnFeatureListener(new GeocoderAutoCompleteView.OnFeatureListener() {
            @Override
            public void onFeatureClick(CarmenFeature feature) {
                hideOnScreenKeyboard();
                Position position = feature.asPosition();
                //destination = Position.fromCoordinates(position.getLatitude(), position.getLongitude());
                // Jardines del Triunfo in Granada, Spain.
                destination = Position.fromCoordinates(-3.601845, 37.184080);
                //destination = Position.fromCoordinates(-118.49666, 34.01114);
                updateMap(position.getLatitude(), position.getLongitude(), R.string.destination_title);
                autocompleteDestinoView.setEnabled(false);
                fab1.setEnabled(true);
                // Get route from API
                try {
                    getRoute(origin, destination);
                    navigation.getRoute(origin, destination, new Callback<DirectionsResponse>() {
                        @Override
                        public void onResponse(
                                Call<DirectionsResponse> call, Response<DirectionsResponse> response) {

                        }

                        @Override
                        public void onFailure(Call<DirectionsResponse> call, Throwable t) {

                        }
                    });
                } catch (ServicesException servicesException) {
                    servicesException.printStackTrace();
                }
            }
        });


        //Inicializando el verificador de estado - Se adjunta en OnResume y onPause se desadjunta.
        mAuthStateListener = new FirebaseAuth.AuthStateListener(){
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //El parametro recibido firebaseAuth contiene si el usuario es registrado o no.
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //usuario ingresado
                    //Toast.makeText(MainActivity.this, "You're signed in. Welcome to OnRoad!", Toast.LENGTH_SHORT).show();
                    cuandoSignInInicar(user);
                } else {
                    //usuario desconectado.
                    cuandoSignOutLimpiar();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setLogo(R.drawable.logo_onroad_color_144dp)
                                    .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                                            new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build()))
                                    .setTheme(R.style.DarkTheme)
                                    .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };


        //<!--  Botón flotante -- Navegación -- Barra de Navegación -->
        /*
        floatingActionButton = (FloatingActionButton) findViewById(R.id.location_toggle_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                if (map != null) {
                    toggleGps(!map.isMyLocationEnabled());
                }
            }
        });*/

        menuGreen = (FloatingActionMenu) findViewById(R.id.menu_green);
        menuGreen.setClosedOnTouchOutside(true);
        fab1 = (FloatingActionButton) findViewById(R.id.fab1);
        fab2 = (FloatingActionButton) findViewById(R.id.fab2);
        fab3 = (FloatingActionButton) findViewById(R.id.fab3);

        fab1.setEnabled(false);
        fab1.setOnClickListener(clickListener);
        fab2.setOnClickListener(clickListener);
        fab3.setOnClickListener(clickListener);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.fab1:
                    map.removeAnnotations();
                    map.removeMarker(marcador_origen);
                    map.removeMarker(marcador_destino);
                    fab1.setEnabled(false);
                    autocompleteOrigenView.setText("");
                    autocompleteDestinoView.setText("");
                    autocompleteOrigenView.setEnabled(true);
                    autocompleteDestinoView.setEnabled(true);
                    break;
                case R.id.fab2:
                    Intent intent = new Intent(MainActivity.this, PickerActivity.class);
                    intent.putExtra("usuario", mCurrentUser);
                    intent.putExtra("desde", "main");
                    finish();
                    startActivity(intent);
                    break;
                case R.id.fab3:
                    if (map != null) {
                        toggleGps(!map.isMyLocationEnabled());
                    }
                    break;
            }
        }
    };

    private void getRoute(Position origin, Position destination) throws ServicesException {

        client = new MapboxDirections.Builder()
                .setOrigin(origin)
                .setDestination(destination)
                .setOverview(DirectionsCriteria.OVERVIEW_FULL)
                .setProfile(DirectionsCriteria.PROFILE_DRIVING)
                .setAccessToken(Mapbox.getAccessToken())
                .build();

        client.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                // You can get the generic HTTP info about the response
                Log.d(TAG, "Response code: " + response.code());
                if (response.body() == null) {
                    Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().getRoutes().size() < 1) {
                    Log.e(TAG, "No routes found");
                    return;
                }

                // Print some info about the route
                currentRoute = response.body().getRoutes().get(0);
                Log.d(TAG, getString(R.string.distance) + currentRoute.getDistance());
                Toast.makeText(
                        MainActivity.this,
                        getString(R.string.ruta_tiene) + currentRoute.getDistance() +
                                getString(R.string.metros_de_largo),
                        Toast.LENGTH_SHORT).show();

                // Draw the route on the map
                drawRoute(currentRoute);
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Log.e(TAG, "Error: " + throwable.getMessage());
                Toast.makeText(MainActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawRoute(DirectionsRoute route) {
        // Convert LineString coordinates into LatLng[]
        LineString lineString = LineString.fromPolyline(route.getGeometry(), PRECISION_6);
        List<Position> coordinates = lineString.getCoordinates();
        LatLng[] points = new LatLng[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            points[i] = new LatLng(
                    coordinates.get(i).getLatitude(),
                    coordinates.get(i).getLongitude());
        }

        // Draw Points on MapView
        map.addPolyline(new PolylineOptions()
                .add(points)
                .color(Color.parseColor("#000000"))
                .width(5));
    }

    private void updateMap(double latitude, double longitude, int title) {
        // Build marker

        if(getString(title).equals(getString(R.string.origen_title))){
            // Create an Icon object for the marker to use
            IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
            Icon icon = iconFactory.fromResource(R.drawable.blue_marker_view);
            //Adding the marker whit custom icon
            marcador_origen = map.addMarker(new MarkerViewOptions()
                    .position(new LatLng(latitude, longitude))
                    .title(getString(title))
                    .icon(icon));
        } else if (getString(title).equals(getString( R.string.destination_title))){
            // Create an Icon object for the marker to use
            IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
            Icon icon = iconFactory.fromResource(R.drawable.purple_marker_view);

            //Adding the marker whit custom icon
            marcador_destino = map.addMarker(new MarkerViewOptions()
                    .position(new LatLng(latitude, longitude))
                    .title(getString(title))
                    .icon(icon));
        } else {
            map.addMarker(new MarkerViewOptions()
                    .position(new LatLng(latitude, longitude))
                    .title(getString(title)));
        }

        // Animate camera to geocoder result location
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(15)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000, null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, getString(R.string.iniciado_sesion), Toast.LENGTH_SHORT).show();
                IdpResponse idpResponse = IdpResponse.fromResultIntent(data);
                startActivity(new Intent(this, MainActivity.class)
                        .putExtra("my_token", idpResponse.getIdpToken()));
                //Log.v(TAG, "PROBANDO: "+idpResponse.getIdpToken());
            } else if (resultCode == RESULT_CANCELED) {
                finishAffinity();
            }
        } else if(requestCode == ELIMINADO){
            if (resultCode == RESULT_OK) {
                mCurrentUser.setNumAlertas(mCurrentUser.getNumAlertas()+1);
                finish();
                startActivity(getIntent());
            }
        }
    }

    private void setInitialCamera() {
        // Method is used to set the initial map camera position. Should only be called once when
        // the map is ready. We first try using the users last location so we can quickly set the
        // camera as fast as possible.
        if (locationEngine.getLastLocation() != null) {
            userPosition = new LatLng(locationEngine.getLastLocation());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationEngine.getLastLocation()), 16));
            userLat = userPosition.getLatitude();
            userLng = userPosition.getLongitude();
            fab3.setImageResource(R.drawable.ic_location_disabled_24dp);
            fab3.setLabelText(getString(R.string.ocultar_ubicacion));
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
                    userLat = location.getLatitude();
                    userLng = location.getLongitude();
                }
            }
        };
        locationEngine.addLocationEngineListener(locationEngineListener);
        // Enable the location layer on the map and track the user location until they perform a
        // map gesture.
        if (!conAlerta){
            map.setMyLocationEnabled(true);
            fab3.setImageResource(R.drawable.ic_my_location_24dp);
            fab3.setLabelText(getString(R.string.mostrar_ubicacion));
        }
        map.getTrackingSettings().setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
    } // End setInitialCamera

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        Log.v(TAG, "TEST: onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        navigationView.getMenu().getItem(0).setChecked(true);
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        Log.v(TAG, "TEST: onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        Log.v(TAG, "TEST: onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        Log.v(TAG, "TEST: onStop()");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        // Ensure no memory leak occurs if we register the location listener but the call hasn't
        // been made yet.
        if (locationEngineListener != null) {
            locationEngine.removeLocationEngineListener(locationEngineListener);
        }
        Log.v(TAG, "TEST: onDestroy");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void toggleGps(boolean enableGps) {
        if (enableGps) {
            // Check if user has granted location permission
            permissionsManager = new PermissionsManager(this);
            if (!PermissionsManager.areLocationPermissionsGranted(this)) {
                permissionsManager.requestLocationPermissions(this);
            } else {
                enableLocation(true);
            }
        } else {
            enableLocation(false);
        }
    }

    private void enableLocation(boolean enabled) {
        if (enabled) {
            // If we have the last location of the user, we can move the camera to that position.
            Location lastLocation = locationEngine.getLastLocation();
            if (lastLocation != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 16));
                userLat = lastLocation.getLatitude();
                userLng = lastLocation.getLongitude();
            }

            locationEngineListener = new LocationEngineListener() {
                @Override
                public void onConnected() {
                    // No action needed here.
                }

                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16));
                        locationEngine.removeLocationEngineListener(this);
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                    }
                }
            };
            locationEngine.addLocationEngineListener(locationEngineListener);
            if (conAlerta) {
                fab3.setImageResource(R.drawable.ic_my_location_24dp);
                fab3.setLabelText(getString(R.string.mostrar_ubicacion));
            } else {
                fab3.setImageResource(R.drawable.ic_location_disabled_24dp);
                fab3.setLabelText(getString(R.string.ocultar_ubicacion));
            }
        } else {
            fab3.setImageResource(R.drawable.ic_my_location_24dp);
            fab3.setLabelText(getString(R.string.mostrar_ubicacion));
        }
        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(enabled);
    }

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
            enableLocation(true);
        } else {
            Toast.makeText(this, getString(R.string.sin_permisos),
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.sign_out_menu:
                //sign out
                mUsuarioExistente = false;
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent = null;
        boolean salir=false;

        switch (item.getItemId()) {
            case R.id.nav_inicio:
                //intent = new Intent(MainActivity.this, CuentaActivity.class);
                break;
            case R.id.nav_cuenta:
                intent = new Intent(MainActivity.this, CuentaActivity.class);
                intent.putExtra("usuario", mCurrentUser);
                intent.putExtra("latitud", userLat);
                intent.putExtra("longitud", userLng);
                break;
            case R.id.nav_enviar_alerta:
                intent = new Intent(MainActivity.this, PickerActivity.class);
                intent.putExtra("usuario", mCurrentUser);
                intent.putExtra("desde", "main");
                break;
            case R.id.nav_alertas:
                intent = new Intent(MainActivity.this, AlertsListActivity.class);
                intent.putExtra("usuario", mCurrentUser);
                intent.putExtra("latitud", userLat);
                intent.putExtra("longitud", userLng);
                break;
            case R.id.nav_salir:
                mUsuarioExistente = false;
                AuthUI.getInstance().signOut(this);
                salir=true;
                break;
            case R.id.nav_contanto:
                intent = new Intent(MainActivity.this, ContactoActivity.class);
                intent.putExtra("usuario", mCurrentUser);
                intent.putExtra("latitud", userLat);
                intent.putExtra("longitud", userLng);
                break;
        }

        if (!salir){
            if (intent!=null){
                finish();
                overridePendingTransition(0, 0);
                startActivity(intent);
            }
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private void cuandoSignInInicar(FirebaseUser user) {
        // Obtego el Nombre, correo, la URL de la foto de perfil y el ID de usuario.
        String name = user.getDisplayName();
        String email = user.getEmail();
        Uri uriPhotoUrl = user.getPhotoUrl();
        String photoUrl = uriPhotoUrl +"";
        String uid = user.getUid();
        mCurrentUser = new User(uid, name, email, photoUrl, 0, 100, 0, "");

        new DownloadImageTask((CircleImageView) header.findViewById(R.id.profileImageView))
                .execute(photoUrl);
        usernameTextView.setText(mCurrentUser.getUsername());
        emailTextView.setText(mCurrentUser.getEmail());

        mUserAlertsDatabaseReference = mFirebaseDatabase.getReference().child("users").child(uid)
                .child("user_alerts");

        //Pongo el listener que me permite leer desde la base de datos.
        //Segun las reglas de la base de datos que he puesto. Esto solo se puede hacer por usuarios
        //registrados así que es mejor ponerlo aquí.
        attachDatabaseReadListener();
    }

    private void cuandoSignOutLimpiar(){
    }

    private void attachDatabaseReadListener(){

        if (mListenerDeUsuarios == null) {
            //Verificador de cambios en la base de datos. Debajo de usuarios.
            mListenerDeUsuarios = new ChildEventListener() {
                //En todos estos métodos recibo un parámeto con la información actual.
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //Este método se llama cada vez que ingresa algo la base de datos.
                    //También se llama para cada valor que ya existen en la base de datos.

                    //Guardando los datos del usuario en un objeto.
                    User currentUser = dataSnapshot.getValue(User.class);

                    /*
                    Log.v(TAG, "PROBANDO1: " + mUsuarioExistente);
                    Log.v(TAG, "PROBANDO: Usuario obtenido: " + currentUser.getUserId()
                                + "\nComparado con: " + mCurrentUser.getUserId());
                    */
                    if (currentUser.getUserId().equals(mCurrentUser.getUserId())) {
                        //Log.v(TAG, "PROBANDO2: " + mUsuarioExistente);
                        mUsuarioExistente = true;
                        mCurrentUser = currentUser;
                        mSeguirComprobando = false;
                        //Log.v(TAG, "PROBANDO2: " + mUsuarioExistente+ " "+mSeguirComprobando);
                    } else {
                        if (mSeguirComprobando) {
                            mUsuarioExistente = false;
                        }
                    }
                    //Aquí lo que quiero que suceda cuando se agrega ese objeto.
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    //Cuando el contenido de algun valor cambia.
                    //Guardando los datos del usuario en un objeto.
                    mCurrentUser = dataSnapshot.getValue(User.class);
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    //Se llama cuando se borra un valor
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    //Se llama cuando un mensaje cambia de posición en la lista.
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    //Se llama cuando ocurre un error de error cuando se hace cambios.
                    //Casi siempre pasa cuando no tienes permisos para leer los datos.
                }
            };
            //Aquí estoy vinculando el listener a una refencia específica.
            mUsersDatabaseReference.addChildEventListener(mListenerDeUsuarios);
            //Esto se realiza una ves comprobado o cargado todos los elementos de usuario.
            mUsersDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //Log.v(TAG, "PROBANDO3: " + mUsuarioExistente);
                    if (!mUsuarioExistente) {
                        mUsersDatabaseReference.child(mCurrentUser.getUserId()).setValue(mCurrentUser);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }

        if (mListenerDeAlertas == null) {
            //Verificador de cambios en la base de datos. Debajo de usuarios.
            mListenerDeAlertas = new ChildEventListener() {
                //En todos estos métodos recibo un parámeto con la información actual.
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //Este método se llama cada vez que ingresa algo la base de datos.
                    //También se llama para cada valor que ya existen en la base de datos.

                    //Guardando los datos del usuario en un objeto.
                    Alert currentAlert = dataSnapshot.getValue(Alert.class);
                    //Aquí lo que quiero que suceda cuando se agrega ese objeto.
                    IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
                    Icon icon;
                    String title, color="";
                    switch ((int) currentAlert.getTipo()){
                        case 1:
                            icon = iconFactory.fromResource(R.drawable.pin_trafico_leve);
                            title = getString(R.string.t1);
                            color = "#00CC00";
                            break;
                        case 2:
                            icon = iconFactory.fromResource(R.drawable.pin_trafico_moderado);
                            title = getString(R.string.t2);
                            color = "#CC6600";
                            break;
                        case 3:
                            icon = iconFactory.fromResource(R.drawable.pin_trafico_denso);
                            title = getString(R.string.t3);
                            color = "#CC0000";
                            break;
                        case 4:
                            icon = iconFactory.fromResource(R.drawable.pin_accidente);
                            title = getString(R.string.t4);
                            color = "#CC0000";
                            break;
                        case 5:
                            icon = iconFactory.fromResource(R.drawable.pin_desvio);
                            title = getString(R.string.t5);
                            color = "#9900CC";
                            break;
                        case 6:
                            icon = iconFactory.fromResource(R.drawable.pin_bache);
                            title = getString(R.string.t6);
                            color = "#00CCCC";
                            break;
                        default:
                            icon = iconFactory.fromResource(R.drawable.pin_police);
                            title = getString(R.string.t7);
                            color = "#0000CC";
                    }
                    if (map!=null){
                        map.addMarker(new MarkerViewOptions()
                                .position(new LatLng(currentAlert.getLatitud(),
                                        currentAlert.getLongitud()))
                                .title(title+"\n"+currentAlert.getDireccion())
                                .snippet(color+"|"+currentAlert.getAlertID()+"|"
                                +currentAlert.getAlertIDforUser()+"|"
                                +currentAlert.getUserId()+"|"
                                +currentAlert.getNombreDeUsuario()+"|"
                                +currentAlert.getLatitud()+"|"
                                +currentAlert.getLongitud()+"|"
                                +currentAlert.getTipo()+"|"
                                +currentAlert.getConfianza()+"|"
                                +currentAlert.getDireccion()+"|"
                                +currentAlert.getTitulo()+"|"
                                +currentAlert.getDescripcion()+"|")
                                .icon(icon));
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    //Cuando el contenido de algun valor cambia.
                    //Guardando los datos del usuario en un objeto.
                    mCurrentUser = dataSnapshot.getValue(User.class);
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    //Se llama cuando se borra un valor
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    //Se llama cuando un mensaje cambia de posición en la lista.
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    //Se llama cuando ocurre un error de error cuando se hace cambios.
                    //Casi siempre pasa cuando no tienes permisos para leer los datos.
                }
            };
            //Aquí estoy vinculando el listener a una refencia específica.
            mAlertsDatabaseReference.addChildEventListener(mListenerDeAlertas);
        }
    }

    private void detachDatabaseReadListener() {
        if (mListenerDeUsuarios != null){
            mUsersDatabaseReference.removeEventListener(mListenerDeUsuarios);
            mListenerDeUsuarios = null;
        }
        if (mListenerDeAlertas != null){
            mAlertsDatabaseReference.removeEventListener(mListenerDeAlertas);
            mListenerDeAlertas = null;
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                //Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    private void hideOnScreenKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

    }
}
