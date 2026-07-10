package com.friendlywings.automation.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class FlightSegment {

    private String airline;
    private String operatedBy;
    private String flightNumber;
    private String fareType;
    private String departureAirportCode;
    private String departureAirportName;
    private String departureTerminal;
    private LocalTime departureTime;
    private LocalDate departureDate;
    private String arrivalAirportCode;
    private String arrivalAirportName;
    private String arrivalTerminal;
    private LocalTime arrivalTime;
    private LocalDate arrivalDate;
    private String duration;
    private String travelClass;
    private String baggage;
    private String layover;

    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }
    public String getOperatedBy() { return operatedBy; }
    public void setOperatedBy(String operatedBy) { this.operatedBy = operatedBy; }
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public String getFareType() { return fareType; }
    public void setFareType(String fareType) { this.fareType = fareType; }
    public String getDepartureAirportCode() { return departureAirportCode; }
    public void setDepartureAirportCode(String departureAirportCode) { this.departureAirportCode = departureAirportCode; }
    public String getDepartureAirportName() { return departureAirportName; }
    public void setDepartureAirportName(String departureAirportName) { this.departureAirportName = departureAirportName; }
    public String getDepartureTerminal() { return departureTerminal; }
    public void setDepartureTerminal(String departureTerminal) { this.departureTerminal = departureTerminal; }
    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public String getArrivalAirportCode() { return arrivalAirportCode; }
    public void setArrivalAirportCode(String arrivalAirportCode) { this.arrivalAirportCode = arrivalAirportCode; }
    public String getArrivalAirportName() { return arrivalAirportName; }
    public void setArrivalAirportName(String arrivalAirportName) { this.arrivalAirportName = arrivalAirportName; }
    public String getArrivalTerminal() { return arrivalTerminal; }
    public void setArrivalTerminal(String arrivalTerminal) { this.arrivalTerminal = arrivalTerminal; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public LocalDate getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(LocalDate arrivalDate) { this.arrivalDate = arrivalDate; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getTravelClass() { return travelClass; }
    public void setTravelClass(String travelClass) { this.travelClass = travelClass; }
    public String getBaggage() { return baggage; }
    public void setBaggage(String baggage) { this.baggage = baggage; }
    public String getLayover() { return layover; }
    public void setLayover(String layover) { this.layover = layover; }
}
