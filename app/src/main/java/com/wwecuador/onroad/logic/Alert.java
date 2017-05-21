package com.wwecuador.onroad.logic;

import java.io.Serializable;

/**
 * Created by Jahyr on 18/5/2017.
 */

public class Alert implements Serializable {
    private String alertID;
    private String alertIDforUser;
    private String userId;
    private String nombreDeUsuario;
    private double latitud;
    private double longitud;
    private long tipo;
    private long confianza;
    private String direccion;
    private String titulo;
    private String descripcion;

    public Alert() {
        //Constructor requerido para las llamadas a DataSnapshot.getValue(User.class);
    }

    public Alert(String alertID, String alertIDforUser, String userId, String nombreDeUsuario, double latitud,
                 double longitud, long tipo, long confianza, String direccion,
                 String titulo, String descripcion) {
        this.alertID = alertID;
        this.alertIDforUser = alertIDforUser;
        this.userId = userId;
        this.nombreDeUsuario = nombreDeUsuario;
        this.latitud = latitud;
        this.longitud = longitud;
        this.tipo = tipo;
        this.confianza = confianza;
        this.direccion = direccion;
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

    public String getAlertID() {
        return alertID;
    }

    public void setAlertID(String alertID) {
        this.alertID = alertID;
    }

    public String getAlertIDforUser() { return alertIDforUser; }

    public void setAlertIDforUser(String alertIDforUser) { this.alertIDforUser = alertIDforUser; }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNombreDeUsuario() {
        return nombreDeUsuario;
    }

    public void setNombreDeUsuario(String nombreDeUsuario) {
        this.nombreDeUsuario = nombreDeUsuario;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public long getTipo() {
        return tipo;
    }

    public void setTipo(long tipo) {
        this.tipo = tipo;
    }

    public long getConfianza() { return confianza; }

    public void setConfianza(long confianza) { this.confianza = confianza; }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
