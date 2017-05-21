package com.wwecuador.onroad;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import com.wwecuador.onroad.logic.Alert;

import java.util.HashMap;
import java.util.Map;

import static android.R.attr.key;
import static com.wwecuador.onroad.R.string.username;

public class EditAlertActivity extends AppCompatActivity {

    private static final String TAG = "EditAlertActivity";

    private long tipo;

    private Alert estaAlerta;
    private Alert nuevaAlerta;

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

    private FirebaseAuth mFirebaseAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUserPointsDatabaseReference;
    private DatabaseReference mUserAlertsDatabaseReference;
    private DatabaseReference mGeneralAlertsDatabaseReference;
    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_alert);

        estaAlerta = (Alert) getIntent().getExtras().getSerializable("alert");

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

        switch ((int) estaAlerta.getTipo()){
            case 1:
                icono.setImageResource(R.drawable.pin_trafico_leve_2);
                spinnerPrincipal.setSelection(0);
                spinnerTrafico.setSelection(0);
                tipo = 1;
                break;
            case 2:
                icono.setImageResource(R.drawable.pin_trafico_moderado_2);
                spinnerPrincipal.setSelection(0);
                spinnerTrafico.setSelection(1);
                tipo = 2;
                break;
            case 3:
                icono.setImageResource(R.drawable.pin_trafico_denso_2);
                spinnerPrincipal.setSelection(0);
                spinnerTrafico.setSelection(2);
                tipo = 3;
                break;
            case 4:
                icono.setImageResource(R.drawable.pin_accidente_2);
                traficoLayout.setVisibility(View.GONE);
                spinnerPrincipal.setSelection(1);
                tipo = 4;
                break;
            case 5:
                icono.setImageResource(R.drawable.pin_desvio_2);
                traficoLayout.setVisibility(View.GONE);
                spinnerPrincipal.setSelection(2);
                tipo = 5;
                break;
            case 6:
                icono.setImageResource(R.drawable.pin_bache_2);
                traficoLayout.setVisibility(View.GONE);
                spinnerPrincipal.setSelection(3);
                tipo = 6;
                break;
            default:
                icono.setImageResource(R.drawable.pin_police_2);
                traficoLayout.setVisibility(View.GONE);
                spinnerPrincipal.setSelection(4);
                tipo = 7;
        }
        direccion.setText(estaAlerta.getDireccion());
        titulo.setText(estaAlerta.getTitulo());
        descripcion.setText(estaAlerta.getDescripcion());

        //FIREBASE
        mFirebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();


        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUserAlertsDatabaseReference = mFirebaseDatabase.getReference().child("users")
                .child(firebaseUser.getUid()).child("user_alerts")
                .child(estaAlerta.getAlertIDforUser());
        mGeneralAlertsDatabaseReference = mFirebaseDatabase.getReference().child("alerts")
                .child(estaAlerta.getAlertID());

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
                    EditAlertActivity.this,
                    getString(R.string.direction_empty),
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Log.v(TAG, "PROBANDO: Entra aquí?");
            nuevaAlerta = new Alert(estaAlerta.getAlertID(), estaAlerta.getAlertIDforUser(),
                    firebaseUser.getUid(), firebaseUser.getDisplayName(),
                    estaAlerta.getLatitud(), estaAlerta.getLongitud(), tipo,
                    estaAlerta.getConfianza(), direccion.getText().toString(), titulo.getText()
                    .toString(), descripcion.getText().toString());
            mUserAlertsDatabaseReference.child("tipo").setValue(tipo);
            mUserAlertsDatabaseReference.child("direccion").setValue(nuevaAlerta.getDireccion());
            mUserAlertsDatabaseReference.child("titulo").setValue(nuevaAlerta.getTitulo());
            mUserAlertsDatabaseReference.child("descripcion").setValue(nuevaAlerta.getDescripcion());
            mGeneralAlertsDatabaseReference.child("tipo").setValue(tipo);
            mGeneralAlertsDatabaseReference.child("direccion").setValue(nuevaAlerta.getDireccion());
            mGeneralAlertsDatabaseReference.child("titulo").setValue(nuevaAlerta.getTitulo());
            mGeneralAlertsDatabaseReference.child("descripcion").setValue(nuevaAlerta.getDescripcion());
            Toast.makeText(
                    EditAlertActivity.this,
                    getString(R.string.alerta_editada),
                    Toast.LENGTH_SHORT
            ).show();
            finish();
            Intent intent = new Intent(EditAlertActivity.this, MainActivity.class);
            startActivity(intent);
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
