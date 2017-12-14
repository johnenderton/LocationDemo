package com.example.user.locationdemo;

/**
 * Created by USER on 11/27/2017.
 */

public class location {
    private double lat;
    private double lng;
    private String name;
    private String type;
    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public location() {

    }
    public location (double lat, double lng, String name, String type, String address) {
        this.lat = lat;
        this.lng = lng;
        this.name = name;
        this.type = type;
        this.address = address;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
