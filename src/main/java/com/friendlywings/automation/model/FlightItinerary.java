package com.friendlywings.automation.model;

import java.util.ArrayList;
import java.util.List;

public class FlightItinerary extends Booking {

    private String bookingId;
    private String pnr;
    private List<Passenger> passengers = new ArrayList<>();
    private List<Trip> trips = new ArrayList<>();

    public FlightItinerary() {
        setBookingType("FLIGHT");
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public void setTrips(List<Trip> trips) {
        this.trips = trips;
    }
}
