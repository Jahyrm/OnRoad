package com.wwecuador.onroad.logic;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wwecuador.onroad.R;

import java.util.ArrayList;

/**
 * Created by Jahyr on 19/5/2017.
 */

public class AlertAdapter extends ArrayAdapter<Alert>{

    public AlertAdapter(Activity context, ArrayList<Alert> alertas){
        super(context, 0, alertas);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Get if the existing view is being reused, otherwise inflate the view
        View listItemView = convertView;
        if(listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        //Get the Word object located at this position in the list.
        Alert currentAlert = getItem(position);

        View linearLayout = listItemView.findViewById(R.id.text_container);

        String titulo;
        int tipo = (int) currentAlert.getTipo();

        switch(tipo){
            case 1:
                titulo = getContext().getResources().getString(R.string.t1);
                break;
            case 2:
                titulo =  getContext().getString(R.string.t2);
                break;
            case 3:
                titulo =  getContext().getString(R.string.t3);
                break;
            case 4:
                titulo =  getContext().getString(R.string.t4);
                break;
            case 5:
                titulo =  getContext().getString(R.string.t5);
                break;
            case 6:
                titulo =  getContext().getString(R.string.t6);
                break;
            case 7:
                titulo =  getContext().getString(R.string.t7);
                break;
            default:
                titulo = getContext().getString(R.string.t8);
        }

        //Find the TextView in the list_item.xml layout whit ID default_text_view
        TextView tituloTextView = (TextView) listItemView.findViewById(R.id.default_text_view);
        if (currentAlert.getTitulo().equals("")){
            tituloTextView.setText(currentAlert.getDireccion());
        } else {
            tituloTextView.setText(currentAlert.getDireccion()+" - "+currentAlert.getTitulo());
        }

        TextView direccionTextView = (TextView) listItemView.findViewById(R.id.miwok_text_view);
        direccionTextView.setText(titulo+" - "+currentAlert.getNombreDeUsuario());

        ImageView imageView = (ImageView) listItemView.findViewById(R.id.image);
        switch(tipo){
            case 1:
                imageView.setImageResource(R.drawable.pin_trafico_leve_2);
                break;
            case 2:
                imageView.setImageResource(R.drawable.pin_trafico_moderado_2);
                break;
            case 3:
                imageView.setImageResource(R.drawable.pin_trafico_denso_2);
                break;
            case 4:
                imageView.setImageResource(R.drawable.pin_accidente_2);
                break;
            case 5:
                imageView.setImageResource(R.drawable.pin_desvio_2);
                break;
            case 6:
                imageView.setImageResource(R.drawable.pin_bache_2);
                break;
            case 7:
                imageView.setImageResource(R.drawable.pin_police_2);
                break;
            default:
                imageView.setImageResource(R.drawable.error_icon);
        }
        imageView.setVisibility(View.VISIBLE);

        return listItemView;
    }
}
