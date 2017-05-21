package com.wwecuador.onroad.logic.users;

import java.io.Serializable;

/**
 * Created by Jahyr on 13/5/2017.
 */

public class User implements Serializable {
    private String userId;
    private String username;
    private String email;
    private String photoUrl;
    private long puntos;
    private long reputacion;
    private long numAlertas;
    private String alertasVotadas;

    public User() {
        //Constructor requerido para las llamadas a DataSnapshot.getValue(User.class);
    }

    public User(String userId, String username, String email, String photourl, long puntos,
                long reputacion, long numAlertas, String alertasVotadas) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.photoUrl = photourl;
        this.puntos = puntos;
        this.reputacion = reputacion;
        this.numAlertas = numAlertas;
        this.alertasVotadas = alertasVotadas;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public long getPuntos() {
        return puntos;
    }

    public void setPuntos(long puntos) {
        this.puntos = puntos;
    }

    public long getReputacion() {
        return reputacion;
    }

    public void setReputacion(long reputacion) {
        this.reputacion = reputacion;
    }

    public long getNumAlertas() { return numAlertas; }

    public void setNumAlertas(long numAlertas) { this.numAlertas = numAlertas; }

    public String getAlertasVotadas() { return alertasVotadas; }

    public void setAlertasVotadas(String alertasVotadas) { this.alertasVotadas = alertasVotadas; }

}
