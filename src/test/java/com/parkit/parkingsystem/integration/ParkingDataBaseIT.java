package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown() {

    }

    @Test
    public void testParkingACar() {
        when(inputReaderUtil.readSelection()).thenReturn(1);

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        // Check that the ticket is properly saved in database
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket);
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertNotNull(ticket.getInTime());
        assertNull(ticket.getOutTime());
        assertEquals(0.0, ticket.getPrice());

        // Check that the parking spot is properly updated
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertNotNull(parkingSpot);
        assertFalse(parkingSpot.isAvailable());
    }

    @Test
    public void testParkingLotExit() throws Exception {
        Ticket ticket = new Ticket();
        Date oneHourAgo = new Date(System.currentTimeMillis() - 60 * 60 * 1000);
        ParkingSpot newSpot = new ParkingSpot(1, ParkingType.CAR, true);
        ticket.setParkingSpot(newSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setInTime(oneHourAgo);
        ticket.setOutTime(new Date());
        ticketDAO.saveTicket(ticket);

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();

        // Check that the ticket is properly updated
        Ticket exitTicket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(exitTicket);
        assertNotNull(exitTicket.getOutTime());
        assertTrue(exitTicket.getPrice() > 0.0);

        // Check that the parking spot is properly updated
        ParkingSpot parkingSpot = exitTicket.getParkingSpot();
        assertNotNull(parkingSpot);
        assertTrue(parkingSpot.isAvailable());
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        Ticket oldticket = new Ticket();
        Date oneDayAgo = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        Date oneDayAgoPlusOneHour = new Date(oneDayAgo.getTime() + 60 * 60 * 1000);
        ParkingSpot oldSpot = new ParkingSpot(2, ParkingType.CAR, true);
        oldticket.setVehicleRegNumber("ABCDEF");
        oldticket.setParkingSpot(oldSpot);
        oldticket.setInTime(oneDayAgo);
        oldticket.setOutTime(oneDayAgoPlusOneHour);
        ticketDAO.saveTicket(oldticket);

        Ticket newticket = new Ticket();
        Date oneHourAgo = new Date(System.currentTimeMillis() - 60 * 60 * 1000);
        ParkingSpot newSpot = new ParkingSpot(1, ParkingType.CAR, true);
        newticket.setVehicleRegNumber("ABCDEF");
        newticket.setParkingSpot(newSpot);
        newticket.setInTime(oneHourAgo);
        ticketDAO.saveTicket(newticket);

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();

        // Check that the ticket is properly updated
        Ticket exitTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(exitTicket);
        assertNotNull(exitTicket.getOutTime());
        assertEquals(Fare.CAR_RATE_PER_HOUR * 0.95, exitTicket.getPrice(), 0.001);
    }
}
