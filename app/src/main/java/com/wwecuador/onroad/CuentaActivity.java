package com.wwecuador.onroad;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.wwecuador.onroad.logic.Alert;
import com.wwecuador.onroad.logic.ValidatorUtil;
import com.wwecuador.onroad.logic.users.User;

import java.io.InputStream;
import java.util.Arrays;

import de.hdodenhof.circleimageview.CircleImageView;

public class CuentaActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "CuentaActivity";

    private static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER =  2;

    private boolean clickedPicker = false;
    private boolean editado = false;
    private double userLat;
    private double userLng;

    private User usuarioActual;

    private View header;
    private CircleImageView profilePhoto;
    private TextView usernameTextView;
    private TextView emailTextView;
    private String fotoAnterior;
    private String nuevaFoto;
    private ImageView fotoPrincipal;
    private EditText nombrePrincipal;
    private EditText emailPrincipal;
    private ImageView uploadImage;
    private TableRow newPassRowOne;
    private TableRow newPassRowTwo;
    private TableRow puntosRow;
    private TextView puntosTextView;
    private long reputation;
    private ImageView reputacionImage;
    private TableRow reputationRow;
    private EditText nuevaPassUno;
    private EditText nuevaPassDos;
    private FloatingActionButton fab;
    private LinearLayout buttons;
    private Button aceptar;
    private Button cancelar;
    private View.OnClickListener pickerListener;
    private View.OnClickListener editarLister;
    private View.OnClickListener eliminarListener;
    private Menu menuSuperior;
    private MenuItem editItem;
    private MenuItem deleteItem;
    private MenuItem confirmEditItem;
    private MenuItem confirmDeleteItem;
    private MenuItem cancelItem;
    NavigationView navigationView;
    private FrameLayout clickable;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUserDatabaseReference;
    private DatabaseReference mAlertsDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mUserPhotosStorageReference;
    private FirebaseUser firebaseUser;
    private StorageReference lastPhoto;
    private ChildEventListener listenerDeAlertas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cuenta);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        fotoAnterior = usuarioActual.getPhotoUrl();

        header = ((NavigationView)findViewById(R.id.nav_view)).getHeaderView(0);
        profilePhoto = ((CircleImageView) header.findViewById(R.id.profileImageView));
        usernameTextView = ((TextView) header.findViewById(R.id.usernameTextView));
        emailTextView = ((TextView) header.findViewById(R.id.emailTextView));
        fotoPrincipal = (ImageView) findViewById(R.id.imageView2);
        nombrePrincipal = (EditText) findViewById(R.id.edittext_username);
        emailPrincipal = (EditText) findViewById(R.id.edittext_email);
        uploadImage = (ImageView) findViewById(R.id.uploadImage);
        newPassRowOne = (TableRow) findViewById(R.id.row_new_pass_one);
        newPassRowTwo = (TableRow) findViewById(R.id.row_new_pass_two);
        puntosRow = (TableRow) findViewById(R.id.row_puntos);
        puntosTextView = (TextView) findViewById(R.id.puntosEditText);
        reputationRow = (TableRow) findViewById(R.id.row_reputation);
        buttons = (LinearLayout) findViewById(R.id.buttons);
        aceptar = (Button) findViewById(R.id.aceptar_button);
        cancelar = (Button) findViewById(R.id.cancel_button);
        nuevaPassUno = (EditText) findViewById(R.id.new_pass_one);
        nuevaPassDos = (EditText) findViewById(R.id.new_pass_two);
        clickable = (FrameLayout) findViewById(R.id.clickable);

        new DownloadImageTask(profilePhoto).execute(usuarioActual.getPhotoUrl());
        usernameTextView.setText(usuarioActual.getUsername());
        emailTextView.setText(usuarioActual.getEmail());
        new DownloadImageTask(fotoPrincipal).execute(usuarioActual.getPhotoUrl());
        nombrePrincipal.setText(usuarioActual.getUsername());
        emailPrincipal.setText(usuarioActual.getEmail());
        puntosTextView.setText(String.valueOf(usuarioActual.getPuntos()));
        reputation = usuarioActual.getReputacion();
        reputacionImage = (ImageView) findViewById(R.id.reputationImage);
        if (reputation<=20){
            reputacionImage.setImageResource(R.drawable.reputation_1);
        } else if (reputation>20 && reputation<=40){
            reputacionImage.setImageResource(R.drawable.reputation_2);
        } else if (reputation>40 && reputation<=60){
            reputacionImage.setImageResource(R.drawable.reputation_3);
        } else if (reputation>60 && reputation<=80){
            reputacionImage.setImageResource(R.drawable.reputation_4);
        } else {
            reputacionImage.setImageResource(R.drawable.reputation_5);
        }

        //FIREBASE
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserPhotosStorageReference = mFirebaseStorage.getReference().child("users_photos");
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUserDatabaseReference = mFirebaseDatabase.getReference().child("users")
                .child(firebaseUser.getUid());
        mAlertsDatabaseReference = mFirebaseDatabase.getReference().child("alerts");



        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickEditar();
            }
        });


        cancelar = (Button) findViewById(R.id.cancel_button);
        cancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickCancelar();
            }
        });

        pickerListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.complete_action_using)), RC_PHOTO_PICKER);
            }
        };

        editarLister = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editar();
            }
        };

        eliminarListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eliminar();
            }
        };

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(1).setChecked(true);

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
    public void onResume() {
        super.onResume();
        navigationView.getMenu().getItem(1).setChecked(true);
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        if (listenerDeAlertas != null){
            mAlertsDatabaseReference.removeEventListener(listenerDeAlertas);
            listenerDeAlertas = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();
            lastPhoto = mUserPhotosStorageReference
                    .child(selectedImageUri.getLastPathSegment());

            //Upload file fo Firebase Storage
            lastPhoto.putFile(selectedImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUri = taskSnapshot.getDownloadUrl();
                    nuevaFoto = downloadUri +"";
                    new DownloadImageTask(fotoPrincipal).execute(nuevaFoto);
                    //Log.v(TAG, nuevaFoto);
                    clickedPicker = true;
                    //Log.v(TAG, "PROBANDO: ¿Qué pasó?");
                }
            });
        }
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(CuentaActivity.this, MainActivity.class);
                finish();
                startActivity(intent);
            } else if (resultCode == RESULT_CANCELED) {
                finishAffinity();
            }
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.cuenta, menu);
        menuSuperior = menu;
        editItem = menu.getItem(0);
        deleteItem = menu.getItem(1);
        confirmEditItem = menu.getItem(2);
        confirmDeleteItem = menu.getItem(3);
        cancelItem = menu.getItem(4);
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
            case R.id.edit_account:
                clickEditar();
                return true;
            case R.id.delete_account:
                clickEliminar();
                return true;
            case R.id.edit_action:
                editar();
                return true;
            case R.id.delete_action:
                eliminar();
                return true;
            case R.id.cancel_action:
                clickCancelar();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        Intent intent = null;

        switch (item.getItemId()) {
            case R.id.nav_inicio:
                intent = new Intent(CuentaActivity.this, MainActivity.class);
                break;
            case R.id.nav_cuenta:
                //intent = new Intent(CuentaActivity.this, ContactoActivity.class);
                break;
            case R.id.nav_enviar_alerta:
                intent = new Intent(CuentaActivity.this, PickerActivity.class);
                intent.putExtra("usuario", usuarioActual);
                intent.putExtra("desde", "cuenta");
                break;
            case R.id.nav_alertas:
                intent = new Intent(CuentaActivity.this, AlertsListActivity.class);
                intent.putExtra("usuario", usuarioActual);
                intent.putExtra("latitud", userLat);
                intent.putExtra("longitud", userLng);
                break;
            case R.id.nav_salir:
                salir();
                break;
            case R.id.nav_contanto:
                intent = new Intent(CuentaActivity.this, ContactoActivity.class);
                intent.putExtra("usuario", usuarioActual);
                intent.putExtra("latitud", userLat);
                intent.putExtra("longitud", userLng);
                break;
        }

            if (intent!=null){
                finish();
                overridePendingTransition(0, 0);
                startActivity(intent);
            }
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
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

    private void clickCancelar(){
        if (clickedPicker && !editado) {
                new DownloadImageTask(fotoPrincipal).execute(fotoAnterior);
                lastPhoto.delete();
        } else if (clickedPicker && editado){
            new DownloadImageTask(profilePhoto).execute(nuevaFoto);
            usernameTextView.setText(usuarioActual.getUsername());
            emailTextView.setText(usuarioActual.getEmail());
        }
        clickable.setVisibility(View.GONE);
        clickable.setOnClickListener(null);
        editItem.setVisible(true);
        deleteItem.setVisible(true);
        confirmDeleteItem.setOnMenuItemClickListener(null);
        confirmDeleteItem.setVisible(false);
        confirmEditItem.setOnMenuItemClickListener(null);
        confirmEditItem.setVisible(false);
        cancelItem.setVisible(false);
        fotoPrincipal.setOnClickListener(null);
        uploadImage.setOnClickListener(null);
        uploadImage.setVisibility(View.GONE);
        newPassRowOne.setVisibility(View.GONE);
        newPassRowTwo.setVisibility(View.GONE);
        nuevaPassUno.setText("");
        nuevaPassDos.setText("");
        puntosRow.setVisibility(View.VISIBLE);
        reputationRow.setVisibility(View.VISIBLE);
        buttons.setVisibility(View.GONE);
        fab.setVisibility(View.VISIBLE);
        nombrePrincipal.setText(usuarioActual.getUsername());
        emailPrincipal.setText(usuarioActual.getEmail());
        nombrePrincipal.setEnabled(false);
        emailPrincipal.setEnabled(false);
        clickedPicker = false;
        nuevaFoto = "";
    }

    private void clickEditar(){
        clickable.setVisibility(View.VISIBLE);
        clickable.setOnClickListener(pickerListener);
        editItem.setVisible(false);
        deleteItem.setVisible(false);
        confirmEditItem.setVisible(true);
        cancelItem.setVisible(true);
        uploadImage.setVisibility(View.VISIBLE);
        fotoPrincipal.setOnClickListener(pickerListener);
        uploadImage.setOnClickListener(pickerListener);
        nombrePrincipal.setEnabled(true);
        emailPrincipal.setEnabled(true);
        puntosRow.setVisibility(View.GONE);
        reputationRow.setVisibility(View.GONE);
        newPassRowOne.setVisibility(View.VISIBLE);
        newPassRowTwo.setVisibility(View.VISIBLE);
        fab.setVisibility(View.GONE);
        aceptar.setText(R.string.just_edit);
        buttons.setVisibility(View.VISIBLE);
        aceptar.setOnClickListener(editarLister);
    }

    private void clickEliminar(){
        editItem.setVisible(false);
        deleteItem.setVisible(false);
        confirmDeleteItem.setVisible(true);
        cancelItem.setVisible(true);
        fab.setVisibility(View.GONE);
        aceptar.setText(R.string.just_delete);
        buttons.setVisibility(View.VISIBLE);
        aceptar.setOnClickListener(eliminarListener);
    }

    private void editar() {

        if (ValidatorUtil.validateEmail(emailPrincipal.getText().toString())){
            Log.v(TAG, "PROBANDO: EMAIL VALIDADO.");
            UserProfileChangeRequest profileUpdates;

            Log.v(TAG, "PROBANDO5: "+clickedPicker);
            if (clickedPicker){
                profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nombrePrincipal.getText().toString())
                        .setPhotoUri(Uri.parse(nuevaFoto))
                        .build();
            } else{
                profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nombrePrincipal.getText().toString())
                        .build();
            }

            Log.v(TAG, "PROBANDO5: "+clickedPicker);
            Log.v(TAG, "PROBANDO5: "+nuevaFoto);
            String newPassword = nuevaPassUno.getText().toString();
            if (newPassword.equals("")){
                if (clickedPicker){
                    Log.v(TAG, "PROBANDO6: "+nuevaFoto);
                    mUserDatabaseReference.child("photoUrl")
                            .setValue(nuevaFoto);
                }
                //Actualizar datos.
                firebaseUser.updateProfile(profileUpdates)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                        mUserDatabaseReference.child("username")
                                                .setValue(nombrePrincipal.getText().toString());
                                    Log.v(TAG, "PROBANDO5:User profile updated.");
                                }
                            }
                        });

                firebaseUser.updateEmail(emailPrincipal.getText().toString())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    mUserDatabaseReference.child("email")
                                            .setValue(emailPrincipal.getText().toString());
                                    Log.v(TAG, "PROBANDO: User email address updated.");
                                }
                            }
                        });

                usuarioActual.setUsername(nombrePrincipal.getText().toString());
                if (clickedPicker){
                    usuarioActual.setPhotoUrl(nuevaFoto);
                }
                usuarioActual.setPhotoUrl(nuevaFoto);
                usuarioActual.setEmail(emailPrincipal.getText().toString());
                editado = true;
                Toast.makeText(this, getString(R.string.datos_editados), Toast.LENGTH_SHORT).show();
                clickCancelar();
            } else {
                if (ValidatorUtil.validatePass(newPassword)) {
                    if (nuevaPassUno.getText().toString().equals(nuevaPassDos.getText().toString())) {
                        Log.v(TAG, "PROBANDO6: "+clickedPicker+nuevaFoto);
                        if (clickedPicker){
                            mUserDatabaseReference.child("photoUrl")
                                    .setValue(nuevaFoto);
                        }
                        firebaseUser.updatePassword(newPassword)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.v(TAG, "User password updated.");
                                        }
                                    }
                                });
                        //Actualizar datos.
                        firebaseUser.updateProfile(profileUpdates)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                                mUserDatabaseReference.child("username")
                                                        .setValue(nombrePrincipal.getText().toString());
                                            Log.v(TAG, "User profile updated.");
                                        }
                                    }
                                });

                        firebaseUser.updateEmail(emailPrincipal.getText().toString())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            mUserDatabaseReference.child("email")
                                                    .setValue(emailPrincipal.getText().toString());
                                            Log.v(TAG, "PROBANDO: User email address updated.");
                                        }
                                    }
                                });
                        usuarioActual.setUsername(nombrePrincipal.getText().toString());
                        if (clickedPicker){
                            usuarioActual.setPhotoUrl(nuevaFoto);
                        }
                        usuarioActual.setEmail(emailPrincipal.getText().toString());
                        editado = true;
                        Toast.makeText(this, getString(R.string.datos_editados), Toast.LENGTH_SHORT).show();
                        clickCancelar();
                    } else {
                        Toast.makeText(this, getString(R.string.pass_same), Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(this, getString(R.string.pass_invalido), Toast.LENGTH_SHORT).show();
                }
            }

        } else {
            Toast.makeText(this, getString(R.string.email_invalido), Toast.LENGTH_SHORT).show();
        }
    }

    private void eliminar(){
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.just_delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                firebaseUser.delete()
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "User account deleted.");
                                }
                            }
                        });
                mUserDatabaseReference.removeValue();
                eliminarAlertas();
                Log.v(TAG, "PROBANDO: Llega aquí");
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                    clickCancelar();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        Snackbar.make(clickable, getString(R.string.delete_snackbar_msg), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    private void salir(){
        AuthUI.getInstance().signOut(this);
    }

    private void eliminarAlertas() {
        if (listenerDeAlertas == null) {
            //Verificador de cambios en la base de datos. Debajo de usuarios.
            listenerDeAlertas = new ChildEventListener() {
                //En todos estos métodos recibo un parámeto con la información actual.
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //Este método se llama cada vez que ingresa algo la base de datos.
                    //También se llama para cada valor que ya existen en la base de datos.

                    //Guardando los datos del usuario en un objeto.
                    Alert currentAlert = dataSnapshot.getValue(Alert.class);
                    if (usuarioActual.getUserId().equals(currentAlert.getUserId())){
                        mFirebaseDatabase.getReference().child("alerts")
                                .child(currentAlert.getAlertID()).removeValue();
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
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
            mAlertsDatabaseReference.addChildEventListener(listenerDeAlertas);
            mAlertsDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    salir();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

}
