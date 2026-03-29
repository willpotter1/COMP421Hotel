import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String CONTEXT_SQL =
            "SELECT CURRENT DATE, CURRENT USER, CURRENT SCHEMA FROM SYSIBM.SYSDUMMY1";

    private static final class RestaurantOption {
        private final String restaurantName;
        private final String hotelName;

        private RestaurantOption(String restaurantName, String hotelName) {
            this.restaurantName = restaurantName;
            this.hotelName = hotelName;
        }
    }

    private static final class LoyaltyUpdate {
        private final int customerId;
        private final int bonusPoints;

        private LoyaltyUpdate(int customerId, int bonusPoints) {
            this.customerId = customerId;
            this.bonusPoints = bonusPoints;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            printConnectionBanner();
            runMainMenu(scanner);
        } catch (IllegalStateException e) {
            System.err.println("Configuration error: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    private static void printConnectionBanner() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(CONTEXT_SQL);
             ResultSet rs = stmt.executeQuery()) {
            System.out.println();
            System.out.println("Connected to DB2 successfully.");

            if (rs.next()) {
                System.out.println("Current date: " + rs.getDate(1));
                System.out.println("Current user: " + rs.getString(2));
                System.out.println("Current schema: " + rs.getString(3));
            }

            System.out.println();
        } catch (SQLException e) {
            System.err.println("Database connection failed.");
            printSqlException(e);
            System.exit(1);
        }
    }

    private static void runMainMenu(Scanner scanner) {
        boolean running = true;

        while (running) {
            printMainMenu();
            int choice = readInt(scanner, "Please enter your option: ");
            System.out.println();

            switch (choice) {
                case 1:
                    viewHotelSummary(scanner);
                    break;
                case 2:
                    findCustomerRoomReservations(scanner);
                    break;
                case 3:
                    makeRoomReservation(scanner);
                    break;
                case 4:
                    runBagStorageMenu(scanner);
                    break;
                case 5:
                    runRestaurantMenu(scanner);
                    break;
                case 6:
                    awardLoyaltyPoints(scanner);
                    break;
                case 7:
                    running = false;
                    System.out.println("Exiting program.");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }

            System.out.println();
        }
    }

    private static void printMainMenu() {
        System.out.println("Hotel Operations Main Menu");
        System.out.println("1. View Hotel Summary");
        System.out.println("2. Find Customer Room Reservations");
        System.out.println("3. Make Room Reservation");
        System.out.println("4. Bag Storage Services");
        System.out.println("5. Restaurant Services");
        System.out.println("6. Award Loyalty Points");
        System.out.println("7. Quit");
    }

    private static void runBagStorageMenu(Scanner scanner) {
        boolean inSubmenu = true;

        while (inSubmenu) {
            System.out.println("Bag Storage Services");
            System.out.println("1. Store Bag");
            System.out.println("2. Mark Bag as Collected");
            System.out.println("3. Return to Main Menu");

            int choice = readInt(scanner, "Please enter your option: ");
            System.out.println();

            switch (choice) {
                case 1:
                    storeBag(scanner);
                    break;
                case 2:
                    markBagAsCollected(scanner);
                    break;
                case 3:
                    inSubmenu = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }

            System.out.println();
        }
    }

    private static void runRestaurantMenu(Scanner scanner) {
        boolean inRestaurantList = true;

        while (inRestaurantList) {
            List<RestaurantOption> restaurants = loadRestaurantOptions();

            if (restaurants.isEmpty()) {
                System.out.println("No restaurants found.");
                return;
            }

            System.out.println("Restaurant Services");
            for (int i = 0; i < restaurants.size(); i++) {
                RestaurantOption option = restaurants.get(i);
                System.out.println((i + 1) + ". " + option.restaurantName + " - " + option.hotelName);
            }
            System.out.println((restaurants.size() + 1) + ". Return to Main Menu");

            int choice = readInt(scanner, "Please enter your option: ");
            System.out.println();

            if (choice == restaurants.size() + 1) {
                inRestaurantList = false;
            } else if (choice < 1 || choice > restaurants.size()) {
                System.out.println("Invalid option. Please try again.");
            } else {
                runRestaurantActionMenu(scanner, restaurants.get(choice - 1));
            }

            System.out.println();
        }
    }

    private static void runRestaurantActionMenu(Scanner scanner, RestaurantOption restaurant) {
        boolean inSubmenu = true;

        while (inSubmenu) {
            System.out.println("Restaurant: " + restaurant.restaurantName + " - " + restaurant.hotelName);
            System.out.println("1. View Reservations");
            System.out.println("2. Make Reservation");
            System.out.println("3. Return to Restaurant List");

            int choice = readInt(scanner, "Please enter your option: ");
            System.out.println();

            switch (choice) {
                case 1:
                    viewRestaurantReservations(restaurant.restaurantName, restaurant.hotelName);
                    break;
                case 2:
                    makeRestaurantReservation(scanner, restaurant.restaurantName, restaurant.hotelName);
                    break;
                case 3:
                    inSubmenu = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }

            System.out.println();
        }
    }

    private static List<RestaurantOption> loadRestaurantOptions() {
        List<RestaurantOption> restaurants = new ArrayList<>();
        String sql =
                "SELECT RESTAURANT_NAME, HOTEL_NAME " +
                "FROM RESTAURANT " +
                "ORDER BY HOTEL_NAME, RESTAURANT_NAME";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                restaurants.add(new RestaurantOption(
                        rs.getString("RESTAURANT_NAME"),
                        rs.getString("HOTEL_NAME")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Could not load restaurants.");
            printSqlException(e);
        }

        return restaurants;
    }

    private static void viewHotelSummary(Scanner scanner) {
        String hotelName = readLine(scanner, "Enter hotel name: ");

        String hotelSql =
                "SELECT H.NAME AS HOTEL_NAME, H.CITY, H.ADDRESS, " +
                "       COUNT(DISTINCT RM.ROOM_NUMBER) AS ROOM_COUNT, " +
                "       COUNT(DISTINCT RT.RESTAURANT_NAME) AS RESTAURANT_COUNT " +
                "FROM HOTEL H " +
                "LEFT JOIN ROOM RM ON RM.HOTEL_NAME = H.NAME " +
                "LEFT JOIN RESTAURANT RT ON RT.HOTEL_NAME = H.NAME " +
                "WHERE H.NAME = ? " +
                "GROUP BY H.NAME, H.CITY, H.ADDRESS";

        String reservationSql =
                "SELECT COUNT(*) AS ACTIVE_RESERVATIONS " +
                "FROM RESERVATION " +
                "WHERE HOTEL_NAME = ? " +
                "AND CHECK_OUT_DATE >= CURRENT DATE";

        String avgPriceSql =
                "SELECT DECIMAL(AVG(PRICE_PER_NIGHT), 10, 2) AS AVG_PRICE " +
                "FROM ROOM " +
                "WHERE HOTEL_NAME = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement hotelStmt = conn.prepareStatement(hotelSql);
             PreparedStatement reservationStmt = conn.prepareStatement(reservationSql);
             PreparedStatement avgPriceStmt = conn.prepareStatement(avgPriceSql)) {

            hotelStmt.setString(1, hotelName);
            try (ResultSet rs = hotelStmt.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("No hotel found with that name.");
                    return;
                }

                System.out.println("Hotel: " + rs.getString("HOTEL_NAME"));
                System.out.println("City: " + rs.getString("CITY"));
                System.out.println("Address: " + rs.getString("ADDRESS"));
                System.out.println("Number of rooms: " + rs.getInt("ROOM_COUNT"));
                System.out.println("Number of restaurants: " + rs.getInt("RESTAURANT_COUNT"));
            }

            reservationStmt.setString(1, hotelName);
            try (ResultSet rs = reservationStmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Current/future room reservations: " + rs.getInt("ACTIVE_RESERVATIONS"));
                }
            }

            avgPriceStmt.setString(1, hotelName);
            try (ResultSet rs = avgPriceStmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Average room price per night: " + rs.getString("AVG_PRICE"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not retrieve hotel summary.");
            printSqlException(e);
        }
    }

    private static void findCustomerRoomReservations(Scanner scanner) {
        int customerId = readInt(scanner, "Enter customer ID: ");

        String sql =
                "SELECT RESERVATION_ID, HOTEL_NAME, ROOM_NUMBER, CHECK_IN_DATE, CHECK_OUT_DATE, BOOKING_DATE " +
                "FROM RESERVATION " +
                "WHERE CID = ? " +
                "ORDER BY CHECK_IN_DATE, RESERVATION_ID";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.println("----------------------------------");
                    System.out.println("Reservation ID: " + rs.getInt("RESERVATION_ID"));
                    System.out.println("Hotel: " + rs.getString("HOTEL_NAME"));
                    System.out.println("Room number: " + rs.getInt("ROOM_NUMBER"));
                    System.out.println("Check-in: " + rs.getDate("CHECK_IN_DATE"));
                    System.out.println("Check-out: " + rs.getDate("CHECK_OUT_DATE"));
                    System.out.println("Booking date: " + rs.getDate("BOOKING_DATE"));
                }

                if (!found) {
                    System.out.println("No room reservations found for that customer.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not retrieve room reservations.");
            printSqlException(e);
        }
    }

    private static void makeRoomReservation(Scanner scanner) {
        String hotelName = readLine(scanner, "Enter hotel name: ");
        int customerId = readInt(scanner, "Enter customer id: ");
        String checkInDateText = readLine(scanner, "Enter check-in date (YYYY-MM-DD): ");
        String checkOutDateText = readLine(scanner, "Enter check-out date (YYYY-MM-DD): ");
        String conciergeIdText = readLine(scanner, "Enter concierge employee ID (press Enter to leave blank): ");

        Date checkInDate;
        Date checkOutDate;

        try {
            checkInDate = Date.valueOf(checkInDateText);
            checkOutDate = Date.valueOf(checkOutDateText);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format.");
            return;
        }

        if (!checkOutDate.after(checkInDate)) {
            System.out.println("Check-out date must be after check-in date.");
            return;
        }

        Integer conciergeId = null;
        if (!conciergeIdText.isEmpty()) {
            try {
                conciergeId = Integer.parseInt(conciergeIdText);
            } catch (NumberFormatException e) {
                System.out.println("Concierge employee ID must be an integer.");
                return;
            }
        }

        String availableRoomsSql =
                "SELECT RM.ROOM_NUMBER, RM.PRICE_PER_NIGHT, RM.NUM_BEDS " +
                "FROM ROOM RM " +
                "WHERE RM.HOTEL_NAME = ? " +
                "AND NOT EXISTS ( " +
                "    SELECT 1 " +
                "    FROM RESERVATION R " +
                "    WHERE R.HOTEL_NAME = RM.HOTEL_NAME " +
                "    AND R.ROOM_NUMBER = RM.ROOM_NUMBER " +
                "    AND ? < R.CHECK_OUT_DATE " +
                "    AND ? > R.CHECK_IN_DATE " +
                ") " +
                "ORDER BY RM.ROOM_NUMBER";

        String nextReservationIdSql =
                "SELECT COALESCE(MAX(RESERVATION_ID), 0) + 1 AS NEXT_ID FROM RESERVATION";

        String insertSql =
                "INSERT INTO RESERVATION " +
                "(RESERVATION_ID, ROOM_NUMBER, HOTEL_NAME, CHECK_IN_DATE, CHECK_OUT_DATE, BOOKING_DATE, CID, MADE_BY_EID) " +
                "VALUES (?, ?, ?, ?, ?, CURRENT DATE, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                if (!hotelExists(conn, hotelName)) {
                    System.out.println("Hotel not found.");
                    conn.rollback();
                    return;
                }

                if (!customerExists(conn, customerId)) {
                    System.out.println("Customer not found.");
                    conn.rollback();
                    return;
                }

                if (conciergeId != null && !conciergeExists(conn, conciergeId)) {
                    System.out.println("Concierge employee not found.");
                    conn.rollback();
                    return;
                }

                List<Integer> roomNumbers = new ArrayList<>();
                try (PreparedStatement availableStmt = conn.prepareStatement(availableRoomsSql)) {
                    availableStmt.setString(1, hotelName);
                    availableStmt.setDate(2, checkInDate);
                    availableStmt.setDate(3, checkOutDate);

                    try (ResultSet rs = availableStmt.executeQuery()) {
                        int index = 1;
                        System.out.println("Available rooms:");
                        while (rs.next()) {
                            roomNumbers.add(rs.getInt("ROOM_NUMBER"));
                            System.out.println(index + ". Room " + rs.getInt("ROOM_NUMBER")
                                    + " | Price: " + rs.getString("PRICE_PER_NIGHT")
                                    + " | Beds: " + rs.getInt("NUM_BEDS"));
                            index++;
                        }
                    }
                }

                if (roomNumbers.isEmpty()) {
                    System.out.println("No available rooms match those dates at that hotel.");
                    conn.rollback();
                    return;
                }

                int chosen = readInt(scanner, "Choose a room by number from the list above: ");
                if (chosen < 1 || chosen > roomNumbers.size()) {
                    System.out.println("Invalid room selection.");
                    conn.rollback();
                    return;
                }

                int roomNumber = roomNumbers.get(chosen - 1);
                int nextReservationId;

                try (PreparedStatement nextIdStmt = conn.prepareStatement(nextReservationIdSql);
                     ResultSet rs = nextIdStmt.executeQuery()) {
                    rs.next();
                    nextReservationId = rs.getInt("NEXT_ID");
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, nextReservationId);
                    insertStmt.setInt(2, roomNumber);
                    insertStmt.setString(3, hotelName);
                    insertStmt.setDate(4, checkInDate);
                    insertStmt.setDate(5, checkOutDate);
                    insertStmt.setInt(6, customerId);

                    if (conciergeId == null) {
                        insertStmt.setNull(7, Types.INTEGER);
                    } else {
                        insertStmt.setInt(7, conciergeId);
                    }

                    int rows = insertStmt.executeUpdate();
                    conn.commit();

                    System.out.println(rows + " room reservation created.");
                    System.out.println("Reservation ID: " + nextReservationId);
                    System.out.println("Room number: " + roomNumber);
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Could not make room reservation.");
            printSqlException(e);
        }
    }

    private static void storeBag(Scanner scanner) {
        int reservationId = readInt(scanner, "Enter reservation ID: ");
        int bagId = readInt(scanner, "Enter new bag ID: ");
        int conciergeId = readInt(scanner, "Enter concierge employee ID: ");

        String reservationSql =
                "SELECT CID FROM RESERVATION WHERE RESERVATION_ID = ?";

        String existingBagSql =
                "SELECT 1 FROM BAGSTORED WHERE RESERVATION_ID = ?";

        String conciergeSql =
                "SELECT 1 FROM CONCIERGE WHERE EID = ?";

        String insertSql =
                "INSERT INTO BAGSTORED (BAG_ID, TIME, COLLECTED, RESERVATION_ID, CID, CONCIERGE_EID) " +
                "VALUES (?, CURRENT TIME, 0, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement reservationStmt = conn.prepareStatement(reservationSql);
                 PreparedStatement existingBagStmt = conn.prepareStatement(existingBagSql);
                 PreparedStatement conciergeStmt = conn.prepareStatement(conciergeSql);
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                reservationStmt.setInt(1, reservationId);

                int customerId;
                try (ResultSet rs = reservationStmt.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Reservation not found.");
                        conn.rollback();
                        return;
                    }
                    customerId = rs.getInt("CID");
                }

                existingBagStmt.setInt(1, reservationId);
                try (ResultSet rs = existingBagStmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("A bag is already stored for that reservation.");
                        conn.rollback();
                        return;
                    }
                }

                conciergeStmt.setInt(1, conciergeId);
                try (ResultSet rs = conciergeStmt.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Concierge employee not found.");
                        conn.rollback();
                        return;
                    }
                }

                insertStmt.setInt(1, bagId);
                insertStmt.setInt(2, reservationId);
                insertStmt.setInt(3, customerId);
                insertStmt.setInt(4, conciergeId);

                int rows = insertStmt.executeUpdate();
                conn.commit();

                System.out.println(rows + " bag stored successfully.");
                System.out.println("Bag ID: " + bagId);
                System.out.println("Customer ID: " + customerId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Could not store bag.");
            printSqlException(e);
        }
    }

    private static void markBagAsCollected(Scanner scanner) {
        int bagId = readInt(scanner, "Enter bag ID: ");
        String sql =
                "UPDATE BAGSTORED " +
                "SET COLLECTED = 1 " +
                "WHERE BAG_ID = ? AND COLLECTED = 0";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bagId);
            int rows = stmt.executeUpdate();

            if (rows == 0) {
                System.out.println("No uncollected bag found with that ID.");
            } else {
                System.out.println("Bag marked as collected.");
            }
        } catch (SQLException e) {
            System.err.println("Could not update bag status.");
            printSqlException(e);
        }
    }

    private static void viewRestaurantReservations(String restaurantName, String hotelName) {
        String sql =
                "SELECT RESTAURANT_RESERVATION_ID, RESERVATION_DATETIME, NUM_PEOPLE, CID, MADE_BY_EID " +
                "FROM RESTAURANTRESERVATION " +
                "WHERE RESTAURANT_NAME = ? AND HOTEL_NAME = ? " +
                "ORDER BY RESERVATION_DATETIME, RESTAURANT_RESERVATION_ID";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, restaurantName);
            stmt.setString(2, hotelName);

            try (ResultSet rs = stmt.executeQuery()) {
                boolean found = false;

                System.out.println("Restaurant: " + restaurantName);
                System.out.println("Hotel: " + hotelName);

                while (rs.next()) {
                    found = true;
                    Object madeByEmployeeId = rs.getObject("MADE_BY_EID");

                    System.out.println("----------------------------------");
                    System.out.println("Reservation ID: " + rs.getInt("RESTAURANT_RESERVATION_ID"));
                    System.out.println("Reservation datetime: " + rs.getTimestamp("RESERVATION_DATETIME"));
                    System.out.println("Number of people: " + rs.getInt("NUM_PEOPLE"));
                    System.out.println("Customer ID: " + rs.getInt("CID"));
                    System.out.println("Made by employee ID: " + madeByEmployeeId);
                }

                if (!found) {
                    System.out.println("No reservations found for that restaurant.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not retrieve restaurant reservations.");
            printSqlException(e);
        }
    }

    private static void makeRestaurantReservation(Scanner scanner, String restaurantName, String hotelName) {
        int customerId = readInt(scanner, "Enter customer ID: ");
        String reservationDateTimeText = readLine(scanner, "Enter reservation datetime (YYYY-MM-DD HH:MM:SS): ");
        int numPeople = readInt(scanner, "Enter number of people: ");
        String employeeIdText = readLine(scanner, "Enter employee ID (press Enter to leave blank): ");

        if (numPeople <= 0) {
            System.out.println("Number of people must be greater than zero.");
            return;
        }

        Timestamp reservationDateTime;
        try {
            reservationDateTime = Timestamp.valueOf(reservationDateTimeText);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid datetime format.");
            return;
        }

        Integer employeeId = null;
        if (!employeeIdText.isEmpty()) {
            try {
                employeeId = Integer.parseInt(employeeIdText);
            } catch (NumberFormatException e) {
                System.out.println("Employee ID must be an integer.");
                return;
            }
        }

        String nextReservationIdSql =
                "SELECT COALESCE(MAX(RESTAURANT_RESERVATION_ID), 0) + 1 AS NEXT_ID " +
                "FROM RESTAURANTRESERVATION";

        String insertSql =
                "INSERT INTO RESTAURANTRESERVATION " +
                "(RESTAURANT_RESERVATION_ID, RESERVATION_DATETIME, NUM_PEOPLE, CID, MADE_BY_EID, RESTAURANT_NAME, HOTEL_NAME) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                if (!customerExists(conn, customerId)) {
                    System.out.println("Customer not found.");
                    conn.rollback();
                    return;
                }

                if (employeeId != null && !employeeExists(conn, employeeId)) {
                    System.out.println("Employee not found.");
                    conn.rollback();
                    return;
                }

                int nextReservationId;
                try (PreparedStatement nextIdStmt = conn.prepareStatement(nextReservationIdSql);
                     ResultSet rs = nextIdStmt.executeQuery()) {
                    rs.next();
                    nextReservationId = rs.getInt("NEXT_ID");
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, nextReservationId);
                    insertStmt.setTimestamp(2, reservationDateTime);
                    insertStmt.setInt(3, numPeople);
                    insertStmt.setInt(4, customerId);

                    if (employeeId == null) {
                        insertStmt.setNull(5, Types.INTEGER);
                    } else {
                        insertStmt.setInt(5, employeeId);
                    }

                    insertStmt.setString(6, restaurantName);
                    insertStmt.setString(7, hotelName);

                    int rows = insertStmt.executeUpdate();
                    conn.commit();

                    System.out.println(rows + " restaurant reservation created.");
                    System.out.println("Reservation ID: " + nextReservationId);
                    System.out.println("Restaurant: " + restaurantName);
                    System.out.println("Hotel: " + hotelName);
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Could not make restaurant reservation.");
            printSqlException(e);
        }
    }

    private static void awardLoyaltyPoints(Scanner scanner) {
        String startDateText = readLine(scanner, "Enter start date (YYYY-MM-DD): ");
        String endDateText = readLine(scanner, "Enter end date (YYYY-MM-DD): ");
        int pointsPerNight = readInt(scanner, "Enter points per night: ");

        Date startDate;
        Date endDate;

        try {
            startDate = Date.valueOf(startDateText);
            endDate = Date.valueOf(endDateText);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format.");
            return;
        }

        if (endDate.before(startDate)) {
            System.out.println("End date cannot be before start date.");
            return;
        }

        if (pointsPerNight <= 0) {
            System.out.println("Points per night must be greater than zero.");
            return;
        }

        String bonusPointsSql =
                "SELECT R.CID, " +
                "       SUM((DAYS(R.CHECK_OUT_DATE) - DAYS(R.CHECK_IN_DATE)) * ?) AS BONUS_POINTS " +
                "FROM RESERVATION R " +
                "WHERE R.CHECK_IN_DATE >= ? " +
                "AND R.CHECK_OUT_DATE <= ? " +
                "AND R.CHECK_OUT_DATE > R.CHECK_IN_DATE " +
                "GROUP BY R.CID " +
                "ORDER BY R.CID";

        String pointsUpdateSql =
                "UPDATE CUSTOMER " +
                "SET POINTS = COALESCE(POINTS, 0) + ? " +
                "WHERE CID = ?";

        String tierUpdateSql =
                "UPDATE CUSTOMER " +
                "SET TIER = CASE " +
                "    WHEN COALESCE(POINTS, 0) >= 10000 THEN 'Platinum' " +
                "    WHEN COALESCE(POINTS, 0) >= 5000 THEN 'Gold' " +
                "    WHEN COALESCE(POINTS, 0) >= 1000 THEN 'Silver' " +
                "    ELSE 'Bronze' " +
                "END " +
                "WHERE CID = ?";

        String customerSummarySql =
                "SELECT CID, NAME, POINTS, TIER " +
                "FROM CUSTOMER " +
                "WHERE CID = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                List<LoyaltyUpdate> updates = new ArrayList<>();

                try (PreparedStatement bonusStmt = conn.prepareStatement(bonusPointsSql)) {
                    bonusStmt.setInt(1, pointsPerNight);
                    bonusStmt.setDate(2, startDate);
                    bonusStmt.setDate(3, endDate);

                    try (ResultSet rs = bonusStmt.executeQuery()) {
                        while (rs.next()) {
                            updates.add(new LoyaltyUpdate(
                                    rs.getInt("CID"),
                                    rs.getInt("BONUS_POINTS")
                            ));
                        }
                    }
                }

                if (updates.isEmpty()) {
                    System.out.println("No customers were affected for that date range.");
                    conn.rollback();
                    return;
                }

                try (PreparedStatement pointsStmt = conn.prepareStatement(pointsUpdateSql);
                     PreparedStatement tierStmt = conn.prepareStatement(tierUpdateSql)) {
                    for (LoyaltyUpdate update : updates) {
                        pointsStmt.setInt(1, update.bonusPoints);
                        pointsStmt.setInt(2, update.customerId);
                        pointsStmt.executeUpdate();

                        tierStmt.setInt(1, update.customerId);
                        tierStmt.executeUpdate();
                    }
                }

                conn.commit();
                System.out.println("Loyalty points awarded successfully.");

                try (PreparedStatement summaryStmt = conn.prepareStatement(customerSummarySql)) {
                    System.out.println("Affected customers:");

                    for (LoyaltyUpdate update : updates) {
                        summaryStmt.setInt(1, update.customerId);

                        try (ResultSet rs = summaryStmt.executeQuery()) {
                            if (rs.next()) {
                                System.out.println("----------------------------------");
                                System.out.println("Customer ID: " + rs.getInt("CID"));
                                System.out.println("Name: " + rs.getString("NAME"));
                                System.out.println("Points: " + rs.getInt("POINTS"));
                                System.out.println("Tier: " + rs.getString("TIER"));
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Could not award loyalty points.");
            printSqlException(e);
        }
    }

    private static boolean hotelExists(Connection conn, String hotelName) throws SQLException {
        String sql = "SELECT 1 FROM HOTEL WHERE NAME = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hotelName);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean customerExists(Connection conn, int customerId) throws SQLException {
        String sql = "SELECT 1 FROM CUSTOMER WHERE CID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean conciergeExists(Connection conn, int conciergeId) throws SQLException {
        String sql = "SELECT 1 FROM CONCIERGE WHERE EID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conciergeId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean employeeExists(Connection conn, int employeeId) throws SQLException {
        String sql = "SELECT 1 FROM EMPLOYEE WHERE EID = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            String line = readLine(scanner, prompt);

            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static void printSqlException(SQLException e) {
        SQLException current = e;

        while (current != null) {
            System.err.println("SQLState: " + current.getSQLState());
            System.err.println("Error code: " + current.getErrorCode());
            System.err.println("Message: " + current.getMessage());
            current = current.getNextException();

            if (current != null) {
                System.err.println();
            }
        }
    }
}
