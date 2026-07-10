package com.friendlywings.automation.model;

import java.time.LocalDate;

public class HotelBooking extends Booking {

    private String bookingId;
    private String bookingRef;
    private String client;
    private String memberId;
    private String countryOfResidence;
    private String property;
    private String hotelName;
    private String address;
    private String propertyContactNumber;
    private int rooms = 1;
    private int extraBeds = 0;
    private int adults = 1;
    private int children = 0;
    private String roomType;
    private String promotion;
    private String mealsIncluded;
    private String cancellationPolicy;
    private String benefitsIncluded;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String checkInTime = "After 12:00 am";
    private String checkOutTime = "After 12:00 am";
    private String paymentMethod;
    private String cardNo;
    private String guestRef;
    private String remarks;
    private String guestList;

    public HotelBooking() {
        setBookingType("HOTEL");
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }
    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getCountryOfResidence() { return countryOfResidence; }
    public void setCountryOfResidence(String countryOfResidence) { this.countryOfResidence = countryOfResidence; }
    public String getProperty() { return property; }
    public void setProperty(String property) { this.property = property; }
    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPropertyContactNumber() { return propertyContactNumber; }
    public void setPropertyContactNumber(String propertyContactNumber) { this.propertyContactNumber = propertyContactNumber; }
    public int getRooms() { return rooms; }
    public void setRooms(int rooms) { this.rooms = rooms; }
    public int getExtraBeds() { return extraBeds; }
    public void setExtraBeds(int extraBeds) { this.extraBeds = extraBeds; }
    public int getAdults() { return adults; }
    public void setAdults(int adults) { this.adults = adults; }
    public int getChildren() { return children; }
    public void setChildren(int children) { this.children = children; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getPromotion() { return promotion; }
    public void setPromotion(String promotion) { this.promotion = promotion; }
    public String getMealsIncluded() { return mealsIncluded; }
    public void setMealsIncluded(String mealsIncluded) { this.mealsIncluded = mealsIncluded; }
    public String getCancellationPolicy() { return cancellationPolicy; }
    public void setCancellationPolicy(String cancellationPolicy) { this.cancellationPolicy = cancellationPolicy; }
    public String getBenefitsIncluded() { return benefitsIncluded; }
    public void setBenefitsIncluded(String benefitsIncluded) { this.benefitsIncluded = benefitsIncluded; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public String getCheckInTime() { return checkInTime; }
    public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }
    public String getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(String checkOutTime) { this.checkOutTime = checkOutTime; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getCardNo() { return cardNo; }
    public void setCardNo(String cardNo) { this.cardNo = cardNo; }
    public String getGuestRef() { return guestRef; }
    public void setGuestRef(String guestRef) { this.guestRef = guestRef; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getGuestList() { return guestList; }
    public void setGuestList(String guestList) { this.guestList = guestList; }
}
