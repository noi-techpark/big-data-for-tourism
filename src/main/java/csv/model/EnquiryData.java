package csv.model;

import de.bytefish.jtinycsvparser.mapping.CsvMappingResult;
import elastic.model.Category;
import elastic.model.GeoLocation;
import elastic.model.Location;
import controllers.Preloader;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EnquiryData {

    private LocalDate arrival;
    private LocalDate departure;
    private Location country;
    private Integer adults;
    private Integer children;
    private Location destination;
    private Category category;
    private Integer booking;
    private Integer cancellation;
    private LocalDateTime submittedOn;
    private long lengthOfStay;

    private ArrayList<String> categories;

    public LocalDate getArrival() {
        return arrival;
    }

    public void setArrival(LocalDate arrival) {
        this.arrival = arrival;
    }

    public LocalDate getDeparture() {
        return departure;
    }

    public void setDeparture(LocalDate departure) {
        this.departure = departure;

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(this.getArrival(), this.getDeparture());
        this.setLengthOfStay(daysBetween);
    }

    public Location getCountry() {
        return country;
    }

    public void setCountry(String country) {
        CsvMappingResult<CountryData> _country = Preloader.countries.stream()
                .filter(x -> country.equals(x.getResult().getName()))
                .findAny()
                .orElse(null);

        Location _location = new Location();
        if(_country == null) {
            _location.code = "";
            _location.name = "";
            _location.latlon = null;
        } else {
            _location.code = _country.getResult().getName();
            _location.name = _country.getResult().getFullname();
            _location.latlon = new GeoLocation(Double.parseDouble(_country.getResult().getLatitude()),
                    Double.parseDouble(_country.getResult().getLongitude()));
        }
        this.country = _location;
    }

    public Integer getAdults() {
        return adults;
    }

    public void setAdults(Integer adults) {
        this.adults = adults;
    }

    public Integer getChildren() {
        return children;
    }

    public void setChildren(Integer children) {
        this.children = children;
    }

    public Location getDestination() {
        return destination;
    }

    public void setDestination(Integer destination) {
        CsvMappingResult<MunicipalityData> _municipality = Preloader.municipalities.stream()
                .filter(x -> destination.toString().equals(x.getResult().getCode()))
                .findAny()
                .orElse(null);

        Location _location = new Location();
        if(_municipality == null) {
            _location.code = "";
            _location.name = "";
            _location.latlon = null;
        } else {
            _location.code = _municipality.getResult().getCode();
            _location.name = _municipality.getResult().getName();
            _location.latlon = new GeoLocation(Double.parseDouble(_municipality.getResult().getLatitude()),
                    Double.parseDouble(_municipality.getResult().getLongitude()));
        }
        this.destination = _location;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Integer category) {
        this.categories = new ArrayList<String>(Arrays.asList("Gastgewerbliche Betriebe (1-3)", "Gastgewerbliche Betriebe (4-5)", "Privatvermieter", "Bauernhöfe", "Sonstiges", "Altro"));

        Category _category = new Category();
        _category.code = category.toString();
        _category.name = this.categories.get(category - 1);
        this.category = _category;
    }

    public Integer getBooking() {
        return booking;
    }

    public void setBooking(Integer booking) {
        this.booking = booking;
    }

    public Integer getCancellation() {
        return cancellation;
    }

    public void setCancellation(Integer cancellation) {
        this.cancellation = cancellation;
    }

    public LocalDateTime getSubmittedOn() {
        return submittedOn;
    }

    public void setSubmittedOn(LocalDateTime submittedOn) {
        this.submittedOn = submittedOn;
    }

    public long getLengthOfStay() {
        return lengthOfStay;
    }

    public void setLengthOfStay(long lengthOfStay) { this.lengthOfStay = lengthOfStay; }

    public String getHash() {
        String s = getSubmittedOn().toString().concat(getArrival().toString()).concat(getDeparture().toString()).concat(getAdults().toString()).concat(getCountry().toString()).concat(getCategory().toString()).concat(getBooking().toString()).concat(getCancellation().toString());
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) { }
        m.update(s.getBytes(),0,s.length());
        return new BigInteger(1,m.digest()).toString(16);
    }
}

