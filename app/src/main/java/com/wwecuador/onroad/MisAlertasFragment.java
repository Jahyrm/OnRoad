package com.wwecuador.onroad;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.wwecuador.onroad.logic.Alert;
import com.wwecuador.onroad.logic.AlertAdapter;
import com.wwecuador.onroad.logic.users.User;

import java.util.ArrayList;

/**
 * Created by Jahyr on 19/5/2017.
 */

public class MisAlertasFragment extends Fragment {

    View rootView;
    User usuario;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mAlertsDatabaseReference;
    private ChildEventListener mListenerDeAlertas;

    //Create an arrayList (Casi Pila) de Alertas
    final ArrayList<Alert> alertas = new ArrayList<Alert>();

    public MisAlertasFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.alert_list, container, false);

        usuario = (User) getActivity().getIntent().getExtras().getSerializable("usuario");

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mAlertsDatabaseReference = mFirebaseDatabase.getReference().child("alerts");


        if (mListenerDeAlertas == null) {
            Log.v("AllAlertsFragment", "PROBANDO: O AQUÍ?");
            //Verificador de cambios en la base de datos. Debajo de usuarios.
            mListenerDeAlertas = new ChildEventListener() {
                //En todos estos métodos recibo un parámeto con la información actual.
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //Este método se llama cada vez que ingresa algo la base de datos.
                    //También se llama para cada valor que ya existen en la base de datos.

                    //Guardando los datos del usuario en un objeto.
                    Alert currentAlert = dataSnapshot.getValue(Alert.class);
                    if (usuario.getUserId().equals(currentAlert.getUserId())){
                        alertas.add(currentAlert);
                    }
                    Log.v("AllAlertsFragment", currentAlert.getAlertID());
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
            //Aquí estoy vinculando el listener a una refencia específica.
            mAlertsDatabaseReference.addChildEventListener(mListenerDeAlertas);
            mAlertsDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
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
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mListenerDeAlertas != null){
            mAlertsDatabaseReference.removeEventListener(mListenerDeAlertas);
            alertas.clear();
            mListenerDeAlertas = null;
        }
    }
}
