package com.wwecuador.onroad;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.wwecuador.onroad.logic.CategoryAdapter;
import com.wwecuador.onroad.logic.users.User;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.wwecuador.onroad.AlertasCercanasFragment.RANGE_DISTANCE_KEY;
import static com.wwecuador.onroad.AlertasCercanasFragment.RANGE_DISTANCE_VALUE;
import static com.wwecuador.onroad.R.id.nav_view;

public class AlertsListActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = "AlertsListActivity";
    public static final int RC_SIGN_IN = 1;

    User usuarioActual;
    DrawerLayout drawer;
    NavigationView navigationView;
    private double userLat;
    private double userLng;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts_list);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("usuario")) {
                usuarioActual = (User) getIntent().getExtras().getSerializable("usuario");
            }
            if (extras.containsKey("latitud")) {
                userLat = (double) getIntent().getExtras().getSerializable("latitud");
            }
            if (extras.containsKey("longitud")) {
                userLng = (double) getIntent().getExtras().getSerializable("longitud");
            }
        }

        mFirebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser nuevo = FirebaseAuth.getInstance().getCurrentUser();
        View header = ((NavigationView) findViewById(nav_view)).getHeaderView(0);
        ImageView fotoLateral = (ImageView) header.findViewById(R.id.profileImageView);
        TextView username = (TextView) header.findViewById(R.id.usernameTextView);
        TextView email = (TextView) header.findViewById(R.id.emailTextView);

        new DownloadImageTask(fotoLateral).execute(usuarioActual.getPhotoUrl());
        username.setText(usuarioActual.getUsername());
        email.setText(usuarioActual.getEmail());

        // Find the view pager that will allow the user to swipe between fragments
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);

        // Create an adapter that knows which fragment should be shown on each page
        CategoryAdapter adapter = new CategoryAdapter(getSupportFragmentManager(), this);

        // Set the adapter onto the view pager
        viewPager.setAdapter(adapter);

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(3).setChecked(true);

        mAuthStateListener = new FirebaseAuth.AuthStateListener(){
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //El parametro recibido firebaseAuth contiene si el usuario es registrado o no.
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //usuario ingresado
                    //Toast.makeText(MainActivity.this, "You're signed in. Welcome to OnRoad!", Toast.LENGTH_SHORT).show();
                } else {
                    //usuario desconectado.
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

    }

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(AlertsListActivity.this, MainActivity.class);
                finish();
                startActivity(intent);
            } else if (resultCode == RESULT_CANCELED) {
                finishAffinity();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationView.getMenu().getItem(3).setChecked(true);
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent = null;
        boolean salir=false;

        switch (item.getItemId()) {
            case R.id.nav_inicio:
                intent = new Intent(AlertsListActivity.this, MainActivity.class);
                break;
            case R.id.nav_cuenta:
                intent = new Intent(AlertsListActivity.this, CuentaActivity.class);
                intent.putExtra("usuario", usuarioActual);
                intent.putExtra("latitud", userLat);
                intent.putExtra("longitud", userLng);
                break;
            case R.id.nav_enviar_alerta:
                intent = new Intent(AlertsListActivity.this, PickerActivity.class);
                intent.putExtra("usuario", usuarioActual);
                intent.putExtra("desde", "alerts");
                break;
            case R.id.nav_alertas:
                //intent = new Intent(AlertsListActivity.this, PickerActivity.class);
                break;
            case R.id.nav_salir:
                AuthUI.getInstance().signOut(this);
                salir=true;
                break;
            case R.id.nav_contanto:
                intent = new Intent(AlertsListActivity.this, ContactoActivity.class);
                intent.putExtra("usuario", usuarioActual);
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
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}
