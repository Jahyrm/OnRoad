package com.wwecuador.onroad;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.wwecuador.onroad.logic.Alert;
import com.wwecuador.onroad.logic.AlertAdapter;
import com.wwecuador.onroad.logic.users.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jahyr on 19/5/2017.
 */

public class AlertasCercanasFragment extends Fragment {

    public static final String RANGE_DISTANCE_KEY = "rango";
    public static double RANGE_DISTANCE_VALUE = 0.031550;

    View rootView;
    User usuario;
    private double latitud;
    private double longitud;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mAlertsDatabaseReference;
    private ChildEventListener mListenerDeAlertas2;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    //Create an arrayList (Casi Pila) de Alertas
    final ArrayList<Alert> alertas = new ArrayList<Alert>();

    public AlertasCercanasFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.alert_list, container, false);

        Intent intent = getActivity().getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            usuario = (User) getActivity().getIntent().getExtras().getSerializable("usuario");
            if (extras.containsKey("latitud")) {
                latitud = (double) getActivity().getIntent().getExtras().getSerializable("latitud");
            }
            if (extras.containsKey("longitud")) {
                longitud = (double) getActivity().getIntent().getExtras().getSerializable("longitud");
            }
        }

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mAlertsDatabaseReference = mFirebaseDatabase.getReference().child("alerts");
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(RANGE_DISTANCE_KEY, RANGE_DISTANCE_VALUE);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
        fetchConfig();


        if (mListenerDeAlertas2 == null) {
            //Verificador de cambios en la base de datos. Debajo de usuarios.
            mListenerDeAlertas2 = new ChildEventListener() {
                //En todos estos métodos recibo un parámeto con la información actual.
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //Este método se llama cada vez que ingresa algo la base de datos.
                    //También se llama para cada valor que ya existen en la base de datos.

                    //Guardando los datos del usuario en un objeto.
                    Alert currentAlert = dataSnapshot.getValue(Alert.class);

                    if (latitud!=0 && longitud!=0){
                        double userLat = (double)
                                Math.round(latitud * 1000000d) / 1000000d;
                        double positionLat = (double)
                                Math.round(currentAlert.getLatitud() * 1000000d) / 1000000d;
                        double userLong = (double)
                                Math.round(longitud * 1000000d) / 1000000d;
                        double positionLong = (double)
                                Math.round(currentAlert.getLongitud() * 1000000d) / 1000000d;
                        double distanciaLatitud = (double)
                                Math.round((userLat-positionLat) * 1000000d) / 1000000d;
                        double distanciaLongitud = (double)
                                Math.round((userLong-positionLong) * 1000000d) / 1000000d;

                        if (((distanciaLatitud <= RANGE_DISTANCE_VALUE) && (distanciaLatitud >=
                                -RANGE_DISTANCE_VALUE)) &&
                                ((distanciaLongitud <= RANGE_DISTANCE_VALUE) &&
                                        (distanciaLongitud >= -RANGE_DISTANCE_VALUE))){
                            alertas.add(currentAlert);
                        }
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
                }
            };
        }
        return rootView;
    }

    private void fetchConfig() {
        long cacheExpiration = 3600;
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()){
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
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
        RANGE_DISTANCE_VALUE = mFirebaseRemoteConfig.getDouble("rango");
        mAlertsDatabaseReference.addChildEventListener(mListenerDeAlertas2);
        mAlertsDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (latitud==0 && longitud==0){
                    String errorUno = getString(R.string.errorUno);
                    String errorDos = getString(R.string.errorDos);
                    alertas.add(new Alert("Error", "Admin", "0", errorUno, 0, 0, 0, 0, errorDos,
                            "", ""));
                }

                AlertAdapter adapter = new AlertAdapter(getActivity(), alertas);

                // Find the {@link ListView} object in the view hierarchy of the {@link Activity}.
                // There should be a {@link ListView} with the view ID called list, which is declared in the
                // alert_listt.xml file.
                ListView listView = (ListView) rootView.findViewById(R.id.list);

                // Make the {@link ListView} use the {@link ArrayAdapter} we created above, so that the
                // {@link ListView} will display list items for each word in the list of words.
                // Do this by calling the setAdapter method on the {@link ListView} object and pass in
                // 1 argument, which is the {@link Word} with the variable name itemsAdapter.
                listView.setAdapter(adapter);

                if(latitud!=0 && longitud!=0) {
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                            Alert alerta = alertas.get(position);
                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            intent.putExtra("alerta", alerta);
                            startActivity(intent);
                        }
                    });
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mListenerDeAlertas2 != null){
            mAlertsDatabaseReference.removeEventListener(mListenerDeAlertas2);
            alertas.clear();
            mListenerDeAlertas2 = null;
        }
    }
}
