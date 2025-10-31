package com.justine.enums;

public enum ServiceType {
    SPA(3000.0),
    LAUNDRY(500.0),
    AIRPORT_PICKUP(2500.0),
    ROOM_SERVICE(1500.0),
    PARKING(300.0);

    private final double defaultPriceKES;

    ServiceType(double defaultPriceKES) {
        this.defaultPriceKES = defaultPriceKES;
    }

    public double getDefaultPriceKES() {
        return defaultPriceKES;
    }
}
