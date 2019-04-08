package com.example.bicimap.FBDataFiles;

import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FBData {
    private double lat;
    private double lon;
    private String dh;
    private  List<Integer> PMlist;
    private double speed;
    private double aceleracion;
    private String observaciones;

    public FBData(){

    }

    public FBData(double lat, double lon, Integer [] pm, String dh, List<Integer> PMlist, double speed, String observaciones )
    {
        this.lat = lat;
        this.lon = lon;
        this.dh = dh;
        this.PMlist=PMlist;
        this.speed = speed;
        this.aceleracion=aceleracion;
        this.observaciones=observaciones;
    }

    public double getLat(){
        return lat;
    }

    public void setLat(double lat){
        this.lat = lat;
    }

    public double getLon(){ return lon;}

    public void setLon(double lon){
        this.lon = lon;
    }

    public List<Integer> getPMlist(){
        return PMlist;
    }
    public void setPMlist(List<Integer> PMlist) { this.PMlist = PMlist; }

    public double getSpeed(){ return speed;}
    public void setSpeed(double speed){this.speed=speed;}

    public double getAceleracion(){ return aceleracion;}
    public void setAceleracion( double aceleracion){this.aceleracion=aceleracion;}

    public String getObservaciones(){ return observaciones;}
    public void setObservaciones( String observaciones ){this.observaciones=observaciones;}



    public String dataDh() { return dh;}

    public void setDh(String dh) {this.dh = dh;}

    public String dataDate(){
        String Date= dataDh().substring(0,10);
        return Date;
    }

    public String dataTime(){
        String Time= dataDh().substring(11,19);
        return Time;

    }


    @Override
    public String toString() {
        return "Localizacion{" +
                ", latitud='" + lat + '\'' +
                ", longitud=" + lon +
                '}';
    }


}






