package org.nzvirtual.api.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.nzvirtual.api.dto.BookingPersist;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="bookings")
public class Booking {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
    @Column(name = "airline")
    private String airline;
    @Column(name = "flight_number")
    private String flightNumber;
    @OneToOne
    @JoinColumn(name = "departure", referencedColumnName = "icao")
    private Airport departure;
    @OneToOne
    @JoinColumn(name = "arrival", referencedColumnName = "icao")
    private Airport arrival;
    @OneToOne
    @JoinColumn(name = "equipment", referencedColumnName = "icao")
    private Equipment equipment;
    @Column(name = "departure_time")
    private String departureTime;
    @Column(name = "arrival_time")
    private String arrivalTime;
    @Column(name = "duration")
    private String duration;
    @Column(name = "atcident")
    private String atcident;
    @Column(name = "planned_departure", columnDefinition = "TIMESTAMP")
    private LocalDateTime plannedDeparture;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public Airport getDeparture() {
        return departure;
    }

    public void setDeparture(Airport departure) {
        this.departure = departure;
    }

    public Airport getArrival() {
        return arrival;
    }

    public void setArrival(Airport arrival) {
        this.arrival = arrival;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getAtcident() {
        return atcident;
    }

    public void setAtcident(String atcident) {
        this.atcident = atcident;
    }

    public LocalDateTime getPlannedDeparture() {
        return plannedDeparture;
    }

    public void setPlannedDeparture(LocalDateTime plannedDeparture) {
        this.plannedDeparture = plannedDeparture;
    }
}
