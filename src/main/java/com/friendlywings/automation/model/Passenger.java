package com.friendlywings.automation.model;

public class Passenger {

    private String name;
    private String type;
    private String pnr;
    private String ticketNumber;
    private String passportNumber;
    private String cabinBaggage;
    private String checkInBaggage;

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

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber;
    }

    public String getCabinBaggage() {
        return cabinBaggage;
    }

    public void setCabinBaggage(String cabinBaggage) {
        this.cabinBaggage = cabinBaggage;
    }

    public String getCheckInBaggage() {
        return checkInBaggage;
    }

    public void setCheckInBaggage(String checkInBaggage) {
        this.checkInBaggage = checkInBaggage;
    }
}
