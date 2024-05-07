package com.aws.spring;

import java.sql.Date;

public class DataModel {
    private long id;
    private String name;
    private Date reservationDate;
    private String productName;
    private String address;

    public DataModel(long id, String name, Date reservationDate, String productName, String address) {
        this.id = id;
        this.name = name;
        this.reservationDate = reservationDate;
        this.productName = productName;
        this.address = address;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(Date reservationDate) {
        this.reservationDate = reservationDate;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
