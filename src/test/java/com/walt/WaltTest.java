package com.walt;

import com.walt.dao.*;
import com.walt.model.*;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@SpringBootTest()
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WaltTest {

    @TestConfiguration
    static class WaltServiceImplTestContextConfiguration {

        @Bean
        public WaltService waltService() {
            return new WaltServiceImpl();
        }
    }

    @Autowired
    WaltService waltService;

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

    @BeforeEach()
    public void prepareData(){

        City jerusalem = new City("Jerusalem");
        City tlv = new City("Tel-Aviv");
        City bash = new City("Beer-Sheva");
        City haifa = new City("Haifa");

        cityRepository.save(jerusalem);
        cityRepository.save(tlv);
        cityRepository.save(bash);
        cityRepository.save(haifa);

        createDrivers(jerusalem, tlv, bash, haifa);

        createCustomers(jerusalem, tlv, haifa);

        createRestaurant(jerusalem, tlv);
    }

    private void createRestaurant(City jerusalem, City tlv) {
        Restaurant meat = new Restaurant("meat", jerusalem, "All meat restaurant");
        Restaurant vegan = new Restaurant("vegan", tlv, "Only vegan");
        Restaurant cafe = new Restaurant("cafe", tlv, "Coffee shop");
        Restaurant chinese = new Restaurant("chinese", tlv, "chinese restaurant");
        Restaurant mexican = new Restaurant("restaurant", tlv, "mexican restaurant ");

        restaurantRepository.saveAll(Lists.newArrayList(meat, vegan, cafe, chinese, mexican));
    }

    private void createCustomers(City jerusalem, City tlv, City haifa) {
        Customer beethoven = new Customer("Beethoven", tlv, "Ludwig van Beethoven");
        Customer mozart = new Customer("Mozart", jerusalem, "Wolfgang Amadeus Mozart");
        Customer chopin = new Customer("Chopin", haifa, "Frédéric François Chopin");
        Customer rachmaninoff = new Customer("Rachmaninoff", tlv, "Sergei Rachmaninoff");
        Customer bach = new Customer("Bach", tlv, "Sebastian Bach. Johann");
        Customer sheldon = new Customer("Sheldon", jerusalem, "Wolfgang Sheldon");
        Customer cooper = new Customer("Cooper", jerusalem, "Wolfgang  Cooper");

        customerRepository.saveAll(Lists.newArrayList(beethoven, mozart, chopin, rachmaninoff, bach,sheldon,cooper));
    }

    private void createDrivers(City jerusalem, City tlv, City bash, City haifa) {
        Driver mary = new Driver("Mary", tlv);
        Driver patricia = new Driver("Patricia", tlv);
        Driver jennifer = new Driver("Jennifer", haifa);
        Driver james = new Driver("James", bash);
        Driver john = new Driver("John", bash);
        Driver robert = new Driver("Robert", jerusalem);
        Driver david = new Driver("David", jerusalem);
        Driver daniel = new Driver("Daniel", tlv);
        Driver noa = new Driver("Noa", haifa);
        Driver ofri = new Driver("Ofri", haifa);
        Driver nata = new Driver("Neta", jerusalem);

        driverRepository.saveAll(Lists.newArrayList(mary, patricia, jennifer, james, john, robert, david, daniel, noa, ofri, nata));
    }

    @Test
    public void testBasics(){
        List<City> l = (List<City>) cityRepository.findAll();
        assertEquals(((List<City>) cityRepository.findAll()).size(),4);
        assertEquals((driverRepository.findAllDriversByCity(cityRepository.findByName("Beer-Sheva")).size()), 2);
    }

    @Test
    public void testSanity() throws WaltException {
        City tlv = cityRepository.findByName("Tel-Aviv");
        Customer beethoven = customerRepository.findByName("Beethoven");
        Date deliveryTime = new Date();
        Restaurant veganRestaurant = restaurantRepository.findByName("vegan");

        Delivery delivery = waltService.createOrderAndAssignDriver(beethoven, veganRestaurant, deliveryTime);
        assertEquals(delivery.getCustomer().getId(), beethoven.getId());
        assertEquals(delivery.getRestaurant().getId(), veganRestaurant.getId());
        assertEquals(delivery.getDeliveryTime(), deliveryTime);
        assertEquals(delivery.getDriver().getCity().getId(), tlv.getId());
    }

    @Test
    public void testCustomerHasDifferentCityThanRestaurant()  {
        Customer beethoven = customerRepository.findByName("Beethoven");
        Date deliveryTime = new Date();
        Restaurant meatRestaurant = restaurantRepository.findByName("meat");

        try {
            waltService.createOrderAndAssignDriver(beethoven, meatRestaurant, deliveryTime);
        } catch (WaltException e) {
            assertEquals(e.getMessage(), "Customer's city and Restaurant's city must be the same");
            return;
        }

        assertTrue("Delivery was assigned even though the customer's and restaurant's cities are different", false);
    }

    @Test
    public void testNoAvailableDriver() {
        Customer c1 = customerRepository.findByName("Mozart");
        Date d1 = new Date();
        Restaurant r1 = restaurantRepository.findByName("meat");

        for (int i = 0; i < 4; i++) {
            try {
                waltService.createOrderAndAssignDriver(c1, r1, d1);
            } catch (WaltException e) {
                assertEquals(e.getMessage(), "No available drivers were found");
                return;
            }
        }
        assertTrue("There is an available driver", false);
    }

}
