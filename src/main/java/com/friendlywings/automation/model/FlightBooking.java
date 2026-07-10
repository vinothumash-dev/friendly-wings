package com.friendlywings.automation.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FlightBooking extends Booking {

    private String tripId;
    private String route;
    private LocalDate travelDate;
    private List<FlightSegment> segments = new ArrayList<>();
    private List<Traveller> travellers = new ArrayList<>();
    private String pnr;

    public FlightBooking() {
        setBookingType("FLIGHT");
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public List<FlightSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<FlightSegment> segments) {
        this.segments = segments;
    }

    public List<Traveller> getTravellers() {
        return travellers;
    }

    public void setTravellers(List<Traveller> travellers) {
        this.travellers = travellers;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }
}
