package com.walt;

import com.walt.dao.*;
import com.walt.model.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class WaltServiceImpl implements WaltService {
    @Resource
    CityRepository cityRepository;

    @Resource
    CustomerRepository customerRepository;

    @Resource
    DriverRepository driverRepository;

    @Resource
    DeliveryRepository deliveryRepository;

    @Resource
    RestaurantRepository restaurantRepository;

    @Override
    public Delivery createOrderAndAssignDriver(Customer customer, Restaurant restaurant, Date deliveryTime) throws WaltException {
        if (customer == null || restaurant == null || deliveryTime == null) {
            throw new IllegalArgumentException("None of the arguments can be null");
        }

        if(!customer.getCity().getId().equals(restaurant.getCity().getId())) {
            throw new WaltException("Customer's city and Restaurant's city must be the same");
        }

        List<Driver> cityDrivers = driverRepository.findAllDriversByCity(customer.getCity());
        List<Driver> availableDrivers = new ArrayList<>();
        for(Driver driver : cityDrivers) {
            if(this.isDriverAvailable(driver, deliveryTime)) {
                availableDrivers.add(driver);
            }
        }

        if (availableDrivers.size() == 0) {
            throw new WaltException("No available drivers were found");
        }
        Driver driver = this.getLeastBusyDriver(availableDrivers);

        Delivery delivery = new Delivery(driver, restaurant, customer, deliveryTime);
        delivery.setDistance(ThreadLocalRandom.current().nextDouble(0, 20));
        deliveryRepository.save(delivery);

        return delivery;
    }

    @Override
    public List<DriverDistance> getDriverRankReport() {
        return deliveryRepository.findDistancesByDriver();
    }

    @Override
    public List<DriverDistance> getDriverRankReportByCity(City city) {
        return deliveryRepository.findCityDistancesByDriver(city);
    }

    private boolean isDriverAvailable(Driver driver, Date deliveryTime) {
        List<Delivery> deliveries = driverRepository.getAllDeliveries(driver);

        for(Delivery delivery : deliveries) {
            long timeDiffInMs = deliveryTime.getTime() - delivery.getDeliveryTime().getTime();
            if (timeDiffInMs < 60 * 60 * 1000) {
                return false;
            }
        }

        return true;
    }

    private Driver getLeastBusyDriver(List<Driver> availableDrivers) {
        Driver leastBusyDriver = availableDrivers.get(0);
        int minDeliveries = driverRepository.getNumberOfDeliveries(leastBusyDriver);

        for(Driver driver : availableDrivers) {
            int numDeliveries = driverRepository.getNumberOfDeliveries(driver);
            if(numDeliveries < minDeliveries) {
                minDeliveries = numDeliveries;
                leastBusyDriver = driver;
            }
        }

        return leastBusyDriver;
    }
}
