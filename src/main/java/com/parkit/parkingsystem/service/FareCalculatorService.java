package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {
    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    public void calculateFare(Ticket ticket, boolean isDiscount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        long inHourMillis = ticket.getInTime().getTime();
        long outHourMillis = ticket.getOutTime().getTime();

        double durationInHours = (double) (outHourMillis - inHourMillis) / (1000 * 60 * 60);

        if (durationInHours <= 0.5) {
            ticket.setPrice(0);
            return;
        }

        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                ticket.setPrice(Math.round(durationInHours * Fare.CAR_RATE_PER_HOUR * 1000.0) / 1000.0);
                break;
            }
            case BIKE: {
                ticket.setPrice(Math.round(durationInHours * Fare.BIKE_RATE_PER_HOUR * 1000.0) / 1000.0);
                break;
            }
            default:
                throw new IllegalArgumentException("Unkown Parking Type");
        }

        if (isDiscount) {
            ticket.setPrice(ticket.getPrice() * 0.95);
        }
    }
}