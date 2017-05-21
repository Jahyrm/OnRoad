package com.wwecuador.onroad;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.wwecuador.onroad.logic.users.User;

import org.w3c.dom.Text;

import java.io.InputStream;

public class PerfilActivity extends AppCompatActivity {

    private static final String TAG = "PerfilActivity";

    private User userVisitado;
    private int reputacionNumeros;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUsersDatabaseReference;
    private ChildEventListener mListenerDeUsuarios;

    private ImageView foto;
    private EditText username;
    private EditText email;
    private TextView puntos;
    private ImageView reputacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        userVisitado = (User) getIntent().getExtras().getSerializable("usuario");

        foto = (ImageView) findViewById(R.id.foto);
        username = (EditText) findViewById(R.id.username);
        email = (EditText) findViewById(R.id.email);
        puntos = (TextView) findViewById(R.id.puntos);
        reputacion = (ImageView) findViewById(R.id.reputacion);
        actualizarInfo();

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUsersDatabaseReference = mFirebaseDatabase.getReference().child("users");
    }

    private void actualizarInfo(){
        new DownloadImageTask(foto).execute(userVisitado.getPhotoUrl());
        username.setText(userVisitado.getUsername());
        email.setText(userVisitado.getEmail());
        puntos.setText(userVisitado.getPuntos()+"");
        reputacionNumeros = (int) userVisitado.getReputacion();
        if (reputacionNumeros<=20){
            reputacion.setImageResource(R.drawable.reputation_1);
        } else if (reputacionNumeros>20 && reputacionNumeros<=40){
            reputacion.setImageResource(R.drawable.reputation_2);
        } else if (reputacionNumeros>40 && reputacionNumeros<=60){
            reputacion.setImageResource(R.drawable.reputation_3);
        } else if (reputacionNumeros>60 && reputacionNumeros<=80){
            reputacion.setImageResource(R.drawable.reputation_4);
        } else {
            reputacion.setImageResource(R.drawable.reputation_5);
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
