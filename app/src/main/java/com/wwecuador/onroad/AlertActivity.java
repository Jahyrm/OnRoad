package com.wwecuador.onroad;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.wwecuador.onroad.logic.Alert;
import com.wwecuador.onroad.logic.users.User;

public class AlertActivity extends AppCompatActivity {

    private final String TAG = "AlertActivity";

    User usuarioDeAlerta;

    public static final int EDITAR = 1;

    private Alert alertaActual;
    private long numAlerts;
    private long reputacion;
    private double userLat;
    private double userLng;

    private ImageView marker;
    private TextView tituloView;
    private TextView direccion;
    private TextView descripcion;
    private FrameLayout greenButtonBg;
    private FrameLayout redButtonBg;
    private ImageView greenButton;
    private ImageView redButton;
    private TextView usernameTextView;
    private ImageView reputationImage;

    private View.OnClickListener verPerfil;
    private View.OnClickListener verPropioPerfil;
    private View.OnClickListener votarPositivo;
    private View.OnClickListener votarNegativo;
    private View.OnClickListener editarListener;
    private View.OnClickListener eliminarListener;

    //Firebase
    private FirebaseUser firebaseUser;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mReferenciaAlUsuarioActual;
    private DatabaseReference mReferenciaUserNumAlerts;
    private DatabaseReference mReferenciaAlertaDeUsuario;
    private DatabaseReference mReferenciaAlertaEnGenerales;
    private DatabaseReference mReferenciaAPuntosDeUsuario;
    private ChildEventListener mListenerDeUsuarios;
    private ChildEventListener mListenerDeUsuariosDos;
    private ChildEventListener mListenerDeAlertasVotadas;
    private ChildEventListener mListenerDeUserAlerts;
    private ChildEventListener mListenerDeAlertasGeneral;

    String votosUsuarioActual = "";
    boolean alertaVotada;
    boolean posONeg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        marker = (ImageView) findViewById(R.id.pinImageView);
        tituloView = (TextView) findViewById(R.id.tipoTitulo);
        direccion = (TextView) findViewById(R.id.direccionTextView);
        descripcion = (TextView) findViewById(R.id.descripcionTextView);
        greenButtonBg = (FrameLayout) findViewById(R.id.greenButtonBg);
        redButtonBg = (FrameLayout) findViewById(R.id.redButtonBg);
        greenButton = (ImageView) findViewById(R.id.greenButton);
        redButton = (ImageView) findViewById(R.id.redButton);
        usernameTextView = (TextView) findViewById(R.id.usernameTextView);
        reputationImage = (ImageView) findViewById(R.id.reputationImageView);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("snippet")){
                String snippet = (String) getIntent().getExtras().getSerializable("snippet");
                String vector[] = snippet.split("\\|");
                if (vector.length==10){
                    alertaActual = new Alert(vector[1], vector[2], vector[3], vector[4], Double.parseDouble(vector[5]),
                            Double.parseDouble(vector[6]), Long.valueOf(vector[7]), Long.valueOf(vector[8]),
                            vector[9], "", "");
                } else if (vector.length==11) {
                    alertaActual = new Alert(vector[1], vector[2], vector[3], vector[4], Double.parseDouble(vector[5]),
                            Double.parseDouble(vector[6]), Long.valueOf(vector[7]), Long.valueOf(vector[8]),
                            vector[9], vector[10], "");
                } else {
                    alertaActual = new Alert(vector[1], vector[2], vector[3], vector[4], Double.parseDouble(vector[5]),
                            Double.parseDouble(vector[6]), Long.valueOf(vector[7]), Long.valueOf(vector[8]),
                            vector[9], vector[10], vector[11]);
                }
            }
            if (extras.containsKey("numAlerts")){
                numAlerts = (long) getIntent().getExtras().getSerializable("numAlerts");
            }
            if (extras.containsKey("latitud")) {
                userLat = (double) getIntent().getExtras().getSerializable("latitud");
            }
            if (extras.containsKey("longitud")) {
                userLng = (double) getIntent().getExtras().getSerializable("longitud");
            }
        }


        //FIREBASE
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mReferenciaAlUsuarioActual =  mFirebaseDatabase.getReference().child("users")
                .child(firebaseUser.getUid()).child("alertasVotadas");
        mReferenciaUserNumAlerts = mFirebaseDatabase.getReference().child("users")
                .child(firebaseUser.getUid()).child("numAlertas");
        mReferenciaAlertaDeUsuario = mFirebaseDatabase.getReference().child("users")
                .child(alertaActual.getUserId()).child("user_alerts").child(alertaActual.getAlertIDforUser());
        mReferenciaAlertaEnGenerales = mFirebaseDatabase.getReference().child("alerts")
                .child(alertaActual.getAlertID());
        mReferenciaAPuntosDeUsuario = mFirebaseDatabase.getReference().child("users")
                .child(alertaActual.getUserId()).child("reputacion");

        String titulo;
        switch((int) alertaActual.getTipo()){
            case 1:
                titulo = getString(R.string.t1);
                marker.setImageResource(R.drawable.pin_trafico_leve_2);
                break;
            case 2:
                titulo = getString(R.string.t2);
                marker.setImageResource(R.drawable.pin_trafico_moderado_2);
                break;
            case 3:
                titulo = getString(R.string.t3);
                marker.setImageResource(R.drawable.pin_trafico_denso_2);
                break;
            case 4:
                titulo = getString(R.string.t4);
                marker.setImageResource(R.drawable.pin_accidente_2);
                break;
            case 5:
                titulo = getString(R.string.t5);
                marker.setImageResource(R.drawable.pin_desvio_2);
                break;
            case 6:
                titulo = getString(R.string.t6);
                marker.setImageResource(R.drawable.pin_bache_2);
                break;
            default:
                titulo = getString(R.string.t7);
                marker.setImageResource(R.drawable.pin_police_2);
        }

        usernameTextView.setText(alertaActual.getNombreDeUsuario());
        tituloView.setText(titulo);
        direccion.setText(alertaActual.getDireccion());
        if (alertaActual.getTitulo().equals("") && alertaActual.getDescripcion().equals("")){
            descripcion.setText(getString(R.string.no_desc));
        } else{
            descripcion.setText(alertaActual.getTitulo()+"\n"+alertaActual.getDescripcion());
        }

        attachDatabaseReadListener();
        userDatos();

        if (alertaActual.getConfianza()<=20){
            reputationImage.setImageResource(R.drawable.reputation_1);
        } else if (alertaActual.getConfianza()>20 && alertaActual.getConfianza()<=40){
            reputationImage.setImageResource(R.drawable.reputation_2);
        } else if (alertaActual.getConfianza()>40 && alertaActual.getConfianza()<=60){
            reputationImage.setImageResource(R.drawable.reputation_3);
        } else if (alertaActual.getConfianza()>60 && alertaActual.getConfianza()<=80){
            reputationImage.setImageResource(R.drawable.reputation_4);
        } else {
            reputationImage.setImageResource(R.drawable.reputation_5);
        }

        votarPositivo = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertaVotada) {
                    if (posONeg) {
                        alertaActual.setConfianza(alertaActual.getConfianza()-30);
                        reputacion = reputacion - 20;
                        votosUsuarioActual = votosUsuarioActual.replace(alertaActual.getAlertID()+":P|", "");
                        mReferenciaAlUsuarioActual.setValue(votosUsuarioActual);
                        mReferenciaAPuntosDeUsuario.setValue(reputacion);
                        mReferenciaAlertaDeUsuario.child("confianza").setValue(alertaActual.getConfianza());
                        mReferenciaAlertaEnGenerales.child("confianza").setValue(alertaActual.getConfianza());
                        greenButtonBg.setBackgroundResource(R.drawable.up_bg);
                        alertaVotada=false;
                    } else {
                        votosUsuarioActual = votosUsuarioActual.replace(alertaActual.getAlertID()+":N|", alertaActual.getAlertID()+":P|");
                        mReferenciaAlUsuarioActual.setValue(votosUsuarioActual);
                        alertaActual.setConfianza(alertaActual.getConfianza()+60);
                        reputacion = reputacion + 40;
                        mReferenciaAPuntosDeUsuario.setValue(reputacion);
                        mReferenciaAlertaDeUsuario.child("confianza").setValue(alertaActual.getConfianza());
                        mReferenciaAlertaEnGenerales.child("confianza").setValue(alertaActual.getConfianza());
                        greenButtonBg.setBackgroundResource(R.drawable.up_marked_bg);
                        redButtonBg.setBackgroundResource(R.drawable.down_bg);
                        redButton.setOnClickListener(votarNegativo);
                        posONeg=true;
                    }
                } else {
                    votosUsuarioActual = votosUsuarioActual+alertaActual.getAlertID()+":P|";
                    mReferenciaAlUsuarioActual.setValue(votosUsuarioActual);
                    alertaActual.setConfianza(alertaActual.getConfianza()+30);
                    reputacion = reputacion + 20;
                    mReferenciaAPuntosDeUsuario.setValue(reputacion);
                    mReferenciaAlertaDeUsuario.child("confianza").setValue(alertaActual.getConfianza());
                    mReferenciaAlertaEnGenerales.child("confianza").setValue(alertaActual.getConfianza());
                    Toast.makeText(AlertActivity.this, getString(R.string.has_votado_correctamente),
                            Toast.LENGTH_SHORT).show();
                    greenButtonBg.setBackgroundResource(R.drawable.up_marked_bg);
                    alertaVotada=true;
                    posONeg=true;
                }
            }
        };

        votarNegativo = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long confianza = alertaActual.getConfianza();

                if (alertaVotada) {
                    if (!posONeg) {
                        alertaActual.setConfianza(alertaActual.getConfianza()+30);
                        votosUsuarioActual = votosUsuarioActual.replace(alertaActual.getAlertID()+":N|", "");
                        mReferenciaAlUsuarioActual.setValue(votosUsuarioActual);
                        reputacion = reputacion + 20;
                        mReferenciaAPuntosDeUsuario.setValue(reputacion);
                        mReferenciaAlertaDeUsuario.child("confianza").setValue(alertaActual.getConfianza());
                        mReferenciaAlertaEnGenerales.child("confianza").setValue(alertaActual.getConfianza());
                        redButtonBg.setBackgroundResource(R.drawable.down_bg);
                        alertaVotada=false;
                    } else {
                        votosUsuarioActual = votosUsuarioActual.replace(alertaActual.getAlertID()+":P|", alertaActual.getAlertID()+":N|");
                        mReferenciaAlUsuarioActual.setValue(votosUsuarioActual);
                        alertaActual.setConfianza(alertaActual.getConfianza()-60);
                        reputacion = reputacion - 40;
                        mReferenciaAPuntosDeUsuario.setValue(reputacion);
                        mReferenciaAlertaDeUsuario.child("confianza").setValue(alertaActual.getConfianza());
                        mReferenciaAlertaEnGenerales.child("confianza").setValue(alertaActual.getConfianza());
                        redButtonBg.setBackgroundResource(R.drawable.down_marked_bg);
                        greenButtonBg.setBackgroundResource(R.drawable.up_bg);
                        greenButton.setOnClickListener(votarPositivo);
                        posONeg=false;
                    }
                } else {
                    votosUsuarioActual = votosUsuarioActual+alertaActual.getAlertID()+":N|";
                    mReferenciaAlUsuarioActual.setValue(votosUsuarioActual);
                    alertaActual.setConfianza(alertaActual.getConfianza()-30);
                    reputacion = reputacion - 20;
                    mReferenciaAPuntosDeUsuario.setValue(reputacion);
                    mReferenciaAlertaDeUsuario.child("confianza").setValue(alertaActual.getConfianza());
                    mReferenciaAlertaEnGenerales.child("confianza").setValue(alertaActual.getConfianza());
                    redButtonBg.setBackgroundResource(R.drawable.down_marked_bg);
                    alertaVotada=true;
                    posONeg=false;
                }
            }
        };


        editarListener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //cuadro de dialogo
                Intent intent = new Intent(AlertActivity.this, EditAlertActivity.class);
                intent.putExtra("alert", alertaActual);
                startActivityForResult(intent, EDITAR);
                setResult(RESULT_OK);
            }
        };

        eliminarListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //cuadro de dialogo
                // Create an AlertDialog.Builder and set the message, and click listeners
                // for the postivie and negative buttons on the dialog.
                eliminar(v);
            }
        };

        verPerfil = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AlertActivity.this, PerfilActivity.class);
                intent.putExtra("usuario", usuarioDeAlerta);
                startActivity(intent);
            }
        };

        verPropioPerfil = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AlertActivity.this, CuentaActivity.class);
                intent.putExtra("usuario", usuarioDeAlerta);
                intent.putExtra("latitud", userLat);
                intent.putExtra("longitud", userLng);
                finishAffinity();
                startActivity(intent);
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == EDITAR) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
            } else {
                setResult(RESULT_OK);
                finish();
                Intent intent = new Intent(AlertActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
    }

    private void attachDatabaseReadListener(){

        if (mListenerDeAlertasVotadas == null) {
            //Verificador de cambios en la base de datos. Debajo de usuarios.
            mListenerDeAlertasVotadas = new ChildEventListener() {
                //En todos estos métodos recibo un parámeto con la información actual.
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //Este método se llama cada vez que ingresa algo la base de datos.
                    //También se llama para cada valor que ya existen en la base de datos.

                    //Guardando los datos del usuario en un objeto.
                    User currentUser = dataSnapshot.getValue(User.class);
                    if(currentUser.getUserId().equals(firebaseUser.getUid())){
                        votosUsuarioActual = currentUser.getAlertasVotadas();
                        comprobar();
                        Log.v(TAG, "PROBANDO ME DA VALOR?: "+votosUsuarioActual);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    //Cuando el contenido de algun valor cambia.
                    //Guardando los datos del usuario en un objeto.
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
                    Log.v(TAG, "PROBANDO:"+databaseError.getMessage());
                }
            };
            //Aquí estoy vinculando el listener a una refencia específica.
            mFirebaseDatabase.getReference().child("users").addChildEventListener(mListenerDeAlertasVotadas);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        detachDatabaseReadListener();
        Log.v(TAG, "TEST: onPause()");
    }

    private void comprobar(){
        if (firebaseUser.getUid().equals(alertaActual.getUserId())){
            usernameTextView.setOnClickListener(verPropioPerfil);
            greenButton.setImageResource(R.drawable.ic_edit);
            redButton.setImageResource(R.drawable.ic_close);
            greenButton.setOnClickListener(editarListener);
            redButton.setOnClickListener(eliminarListener);
        } else {
            usernameTextView.setOnClickListener(verPerfil);
            Log.v(TAG, "PROBANDO: "+votosUsuarioActual);
            Log.v(TAG, "PROBANDO: "+alertaActual.getAlertID());
            if (votosUsuarioActual.contains(alertaActual.getAlertID())) {
                alertaVotada = true;
                int inicio = votosUsuarioActual.indexOf(alertaActual.getAlertID());
                String cadena = votosUsuarioActual.substring(inicio);
                Log.v(TAG, "PROBANDO:"+cadena);
                String voto = cadena.substring(0, cadena.indexOf("|"));
                Log.v(TAG, "PROBANDO: "+voto);
                String positivoONegativo = voto.substring((voto.indexOf(":")+1));
                Log.v(TAG, "PROBANDO: "+positivoONegativo);
                if (positivoONegativo.equals("P")){
                    posONeg = true;
                    greenButtonBg.setBackgroundResource(R.drawable.up_marked_bg);
                    greenButton.setOnClickListener(votarPositivo);
                    redButton.setOnClickListener(votarNegativo);
                } else {
                    posONeg = false;
                    redButtonBg.setBackgroundResource(R.drawable.down_marked_bg);
                    greenButton.setOnClickListener(votarPositivo);
                    redButton.setOnClickListener(votarNegativo);
                }
            } else {
                alertaVotada = false;
                greenButton.setOnClickListener(votarPositivo);
                redButton.setOnClickListener(votarNegativo);
            }
        }
    }

    private void eliminar(View view){
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.eliminar_alerta);
        builder.setPositiveButton(R.string.just_delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mReferenciaUserNumAlerts.setValue(numAlerts-1);
                mReferenciaAlertaDeUsuario.removeValue();
                mReferenciaAlertaEnGenerales.removeValue();
                Toast.makeText(AlertActivity.this, getString(R.string.alerta_eliminada),
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                attachOtraConsultaBaseDeDatos();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void attachOtraConsultaBaseDeDatos(){
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
                    if(currentUser.getAlertasVotadas().contains(alertaActual.getAlertID()+":P|")){
                        votosUsuarioActual = currentUser.getAlertasVotadas().replace(alertaActual.getAlertID()+":P|", "");
                        mFirebaseDatabase.getReference().child("users").child(currentUser.getUserId()).child("alertasVotadas").setValue(votosUsuarioActual);
                        Log.v(TAG, "PROBANDO ME DA VALOR?: "+votosUsuarioActual);
                    } else if (currentUser.getAlertasVotadas().contains(alertaActual.getAlertID()+":N|")){
                        votosUsuarioActual = currentUser.getAlertasVotadas().replace(alertaActual.getAlertID()+":N|", "");
                        mFirebaseDatabase.getReference().child("users").child(currentUser.getUserId()).child("alertasVotadas").setValue(votosUsuarioActual);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    //Cuando el contenido de algun valor cambia.
                    //Guardando los datos del usuario en un objeto.
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
                    Log.v(TAG, "PROBANDO:"+databaseError.getMessage());
                }
            };
            //Aquí estoy vinculando el listener a una refencia específica.
            mFirebaseDatabase.getReference().child("users").addChildEventListener(mListenerDeUsuarios);
            mFirebaseDatabase.getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    finish();
                    Intent intent = new Intent(AlertActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }


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
                    if(currentUser.getAlertasVotadas().contains(alertaActual.getAlertID()+":P|")){
                        votosUsuarioActual = currentUser.getAlertasVotadas().replace(alertaActual.getAlertID()+":P|", "");
                        mFirebaseDatabase.getReference().child("users").child(currentUser.getUserId()).child("alertasVotadas").setValue(votosUsuarioActual);
                        Log.v(TAG, "PROBANDO ME DA VALOR?: "+votosUsuarioActual);
                    } else if (currentUser.getAlertasVotadas().contains(alertaActual.getAlertID()+":N|")){
                        votosUsuarioActual = currentUser.getAlertasVotadas().replace(alertaActual.getAlertID()+":N|", "");
                        mFirebaseDatabase.getReference().child("users").child(currentUser.getUserId()).child("alertasVotadas").setValue(votosUsuarioActual);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    //Cuando el contenido de algun valor cambia.
                    //Guardando los datos del usuario en un objeto.
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
                    Log.v(TAG, "PROBANDO:"+databaseError.getMessage());
                }
            };
            //Aquí estoy vinculando el listener a una refencia específica.
            mFirebaseDatabase.getReference().child("users").addChildEventListener(mListenerDeUsuarios);
            mFirebaseDatabase.getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    finish();
                    Intent intent = new Intent(AlertActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
    }

    public void userDatos(){
        //if (mListenerDeUsuariosDos == null) {
            //Verificador de cambios en la base de datos. Debajo de usuarios.
            mListenerDeUsuariosDos = new ChildEventListener() {
                //En todos estos métodos recibo un parámeto con la información actual.
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //Este método se llama cada vez que ingresa algo la base de datos.
                    //También se llama para cada valor que ya existen en la base de datos.

                    //Guardando los datos del usuario en un objeto.
                    User currentUser = dataSnapshot.getValue(User.class);
                    if (currentUser.getUserId().equals(alertaActual.getUserId())){
                        usuarioDeAlerta = currentUser;
                        reputacion = currentUser.getReputacion();
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    //Cuando el contenido de algun valor cambia.
                    //Guardando los datos del usuario en un objeto.
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
                    Log.v(TAG, "PROBANDO:"+databaseError.getMessage());
                }
            };
            //Aquí estoy vinculando el listener a una refencia específica.
            mFirebaseDatabase.getReference().child("users").addChildEventListener(mListenerDeUsuariosDos);
            mFirebaseDatabase.getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //reputacion = usuarioDeAlerta.getReputacion();
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        //}
    }

    private void detachDatabaseReadListener() {
        if (mListenerDeUsuarios != null){
            mFirebaseDatabase.getReference().child("users").removeEventListener(mListenerDeUsuarios);
            mListenerDeUsuarios = null;
        }
        if (mListenerDeUsuariosDos != null){
            mFirebaseDatabase.getReference().child("users").removeEventListener(mListenerDeUsuariosDos);
            mListenerDeUsuariosDos = null;
        }
    }

}
