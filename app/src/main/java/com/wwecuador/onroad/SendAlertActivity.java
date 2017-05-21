package com.wwecuador.onroad;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.wwecuador.onroad.logic.Alert;
import com.wwecuador.onroad.logic.users.User;

public class SendAlertActivity extends AppCompatActivity {

    private static final String TAG = "SendAlertActivity";

    //static final int ENVIADO = 1;  // The request code

    private long tipo = 1;

    private Alert estaAlerta;

    private User usuarioActual;
    private long puntos;
    private long numAlerts;
    private double latitud;
    private double longitud;
    private String desde;
    private Intent salida;

    private ImageView icono;
    private Spinner spinnerPrincipal;
    private LinearLayout secondaryLayout;
    private LinearLayout traficoLayout;
    private Spinner spinnerTrafico;
    private EditText direccion;
    private EditText titulo;
    private EditText descripcion;
    private Button aceptar;
    private Button cancelar;
    String key;
    String key_dos;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUserPointsDatabaseReference;
    private DatabaseReference mUserAlertsDatabaseReference;
    private DatabaseReference mGeneralAlertsDatabaseReference;
    private DatabaseReference mUserNumAlertsDatabaseReference;
    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_alert);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("usuario")) {
                usuarioActual = (User)  getIntent().getExtras().getSerializable("usuario");
                puntos = usuarioActual.getPuntos();
                numAlerts = usuarioActual.getNumAlertas();
            }
            if (extras.containsKey("latitud")) {
                latitud = (double) getIntent().getExtras().getSerializable("latitud");
            }
            if (extras.containsKey("latitud")) {
                longitud = (double) getIntent().getExtras().getSerializable("longitud");
            }
            if (extras.containsKey("desde")){
                desde = (String) getIntent().getExtras().getSerializable("desde");
                if (desde.equals("main")){
                    salida = new Intent(SendAlertActivity.this, MainActivity.class);
                } else if (desde.equals("cuenta")){
                    salida = new Intent(SendAlertActivity.this, CuentaActivity.class);
                    salida.putExtra("usuario", usuarioActual);
                } else if (desde.equals("alerts")){
                    salida = new Intent(SendAlertActivity.this, AlertsListActivity.class);
                    salida.putExtra("usuario", usuarioActual);
                } else {
                    salida = new Intent(SendAlertActivity.this, ContactoActivity.class);
                    salida.putExtra("usuario", usuarioActual);
                }
            }
        }

        icono = (ImageView) findViewById(R.id.iconoImageView);
        spinnerPrincipal = (Spinner) findViewById(R.id.tipoPrincipalSpinner);
        traficoLayout = (LinearLayout) findViewById(R.id.traficoLayout);
        spinnerTrafico = (Spinner) findViewById(R.id.traficoSpinner);
        direccion = (EditText) findViewById(R.id.direccionEditText);
        titulo = (EditText) findViewById(R.id.tituloEditText);
        descripcion = (EditText) findViewById(R.id.descripcionEditText);
        aceptar = (Button) findViewById(R.id.enviarButton);
        cancelar = (Button) findViewById(R.id.cancelarButton);
        buildSpinner();
        buildTraficoSpinner();

        //FIREBASE
        mFirebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();


        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUserAlertsDatabaseReference = mFirebaseDatabase.getReference().child("users")
                .child(firebaseUser.getUid()).child("user_alerts").push();
        mGeneralAlertsDatabaseReference = mFirebaseDatabase.getReference().child("alerts").push();
        mUserPointsDatabaseReference = mFirebaseDatabase.getReference().child("users")
                .child(firebaseUser.getUid()).child("puntos");
        mUserNumAlertsDatabaseReference = mFirebaseDatabase.getReference().child("users")
                .child(firebaseUser.getUid()).child("numAlertas");

        key = mGeneralAlertsDatabaseReference.getKey();
        key_dos = mUserAlertsDatabaseReference.getKey();


        aceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviar();
            }
        });

        cancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void buildSpinner(){
// Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_tipo_alerta, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        spinnerPrincipal.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        spinnerPrincipal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.trafico))) {
                        traficoSelected();
                    } else if (selection.equals(getString(R.string.accidente))) {
                        accidenteSelected();
                    } else if (selection.equals(getString(R.string.desvio))){
                        desvioSelected();
                    } else if (selection.equals(getString(R.string.bache))){
                        bacheSelected();
                    } else {
                        policiaSelected();
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //doNothing();
            }
        });
    }

    private void buildTraficoSpinner(){

        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_trafico, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        spinnerTrafico.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        spinnerTrafico.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.leve))) {
                        icono.setImageResource(R.drawable.pin_trafico_leve_2);
                        tipo = 1;
                    } else if (selection.equals(getString(R.string.moderado))) {
                        icono.setImageResource(R.drawable.pin_trafico_moderado_2);
                        tipo = 2;
                    } else {
                        icono.setImageResource(R.drawable.pin_trafico_denso_2);
                        tipo = 3;
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tipo = 1;
            }
        });
    }

    private void enviar(){
        String nuevo = direccion.getText().toString();
        if (nuevo.equals("")){
            Toast.makeText(
                    SendAlertActivity.this,
                    getString(R.string.direction_empty),
                    Toast.LENGTH_LONG
            ).show();
        } else {
            //Log.v(TAG, "PROBANDO: Entra aquí?");
            estaAlerta = new Alert(key, key_dos, firebaseUser.getUid(), firebaseUser.getDisplayName(),
                    latitud, longitud, tipo, 100, direccion.getText().toString(), titulo.getText()
                    .toString(), descripcion.getText().toString());
            mUserAlertsDatabaseReference.setValue(estaAlerta);
            mGeneralAlertsDatabaseReference.setValue(estaAlerta);
            mUserPointsDatabaseReference.setValue(puntos+1);
            usuarioActual.setPuntos(usuarioActual.getPuntos()+1);
            mUserNumAlertsDatabaseReference.setValue(numAlerts+1);
            usuarioActual.setNumAlertas(usuarioActual.getNumAlertas()+1);
            Toast.makeText(
                    SendAlertActivity.this,
                    getString(R.string.alerta_enviada),
                    Toast.LENGTH_LONG
            ).show();
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        finish();
    }

    private void traficoSelected(){
        icono.setImageResource(R.drawable.pin_trafico_leve_2);
        tipo=1;
        spinnerTrafico.setSelection(0);
        traficoLayout.setVisibility(View.VISIBLE);
    }

    private void accidenteSelected(){
        tipo = 4;
        icono.setImageResource(R.drawable.pin_accidente_2);
        traficoLayout.setVisibility(View.GONE);
    }

    private void desvioSelected(){
        tipo = 5;
        icono.setImageResource(R.drawable.pin_desvio_2);
        traficoLayout.setVisibility(View.GONE);
    }

    private void bacheSelected(){
        tipo = 6;
        icono.setImageResource(R.drawable.pin_bache_2);
        traficoLayout.setVisibility(View.GONE);
    }

    private void policiaSelected(){
        tipo = 7;
        icono.setImageResource(R.drawable.pin_police_2);
        traficoLayout.setVisibility(View.GONE);
    }

}
