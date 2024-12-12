package app;

import java.sql.*;
import java.util.Scanner;
import java.util.regex.Pattern;

public class main {

    private static final String HOST = "localhost";
    private static final String USER = "new_username4";  // Use environment variable for security
    private static final String PASSWORD = "new_password4";  // Use environment variable for security
    private static final String DATABASE = "new_database4";
    private static final String URL = "jdbc:mysql://" + HOST + "/" + DATABASE;

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            // Ensure database and table are created if not exist
            createDatabaseIfNotExists(conn);
            createTableIfNotExists(conn);

            Scanner scanner = new Scanner(System.in);
            boolean keepRunning = true;

            // Main menu loop
            while (keepRunning) {
                displayMenu();
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        createRecord(conn, scanner);
                        break;
                    case "2":
                        readRecords(conn, scanner);
                        break;
                    case "3":
                        updateRecord(conn, scanner);
                        break;
                    case "4":
                        deleteRecord(conn, scanner);
                        break;
                    case "5":
                        keepRunning = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                        break;
                }
            }

            scanner.close();

        } catch (SQLException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    // Ensure database exists, if not create it
    private static void createDatabaseIfNotExists(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            String checkDatabaseQuery = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + DATABASE + "'";
            ResultSet rs = stmt.executeQuery(checkDatabaseQuery);

            if (!rs.next()) {
                // Database doesn't exist, create it
                stmt.executeUpdate("CREATE DATABASE " + DATABASE);
                System.out.println("Database created: " + DATABASE);
            }
        } catch (SQLException e) {
            System.err.println("Error checking/creating database: " + e.getMessage());
        }
    }

    // Ensure table exists, if not create it
    private static void createTableIfNotExists(Connection conn) {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(255) NOT NULL," +
                "email VARCHAR(255) NOT NULL," +
                "phone VARCHAR(20)," +
                "address TEXT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableSQL);
            System.out.println("Table 'users' is ready.");
        } catch (SQLException e) {
            System.err.println("Error checking/creating table: " + e.getMessage());
        }
    }

    // Display main menu
    private static void displayMenu() {
        System.out.println("\nChoose an operation: ");
        System.out.println("1. Create a record");
        System.out.println("2. Read records");
        System.out.println("3. Update a record");
        System.out.println("4. Delete a record");
        System.out.println("5. Exit");
        System.out.print("Enter your choice: ");
    }

    // 1. Create a new record with user input and validation
    private static void createRecord(Connection conn, Scanner scanner) {
        String name = getValidInput(scanner, "Enter name: ", CRUD::validateName);
        String email = getValidInput(scanner, "Enter email: ", CRUD::validateEmail);
        String phone = getValidInput(scanner, "Enter phone (digits only, at least 10): ", CRUD::validatePhone);
        String address = getValidInput(scanner, "Enter address: ", CRUD::validateAddress);

        String sql = "INSERT INTO users (name, email, phone, address, created_at) VALUES (?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.setString(4, address);
            stmt.executeUpdate();
            System.out.println("Record created successfully!");
        } catch (SQLException e) {
            System.err.println("Error creating record: " + e.getMessage());
        }
    }

    // 2. Read records from the database
    private static void readRecords(Connection conn, Scanner scanner) {
        String sql = "SELECT * FROM users";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id"));
                System.out.println("Name: " + rs.getString("name"));
                System.out.println("Email: " + rs.getString("email"));
                System.out.println("Phone: " + rs.getString("phone"));
                System.out.println("Address: " + rs.getString("address"));
                System.out.println("Created At: " + rs.getTimestamp("created_at"));
                System.out.println("-----------------------------------");
            }
        } catch (SQLException e) {
            System.err.println("Error reading records: " + e.getMessage());
        }
    }

    // 3. Update an existing record
    private static void updateRecord(Connection conn, Scanner scanner) {
        int id = Integer.parseInt(getValidInput(scanner, "Enter the ID of the record to update: ", CRUD::validateId));

        String name = getOptionalValidInput(scanner, "Enter new name (or press enter to skip): ", CRUD::validateName);
        String email = getOptionalValidInput(scanner, "Enter new email (or press enter to skip): ", CRUD::validateEmail);
        String phone = getOptionalValidInput(scanner, "Enter new phone (or press enter to skip): ", CRUD::validatePhone);
        String address = getOptionalValidInput(scanner, "Enter new address (or press enter to skip): ", CRUD::validateAddress);

        String sql = "UPDATE users SET name = ?, email = ?, phone = ?, address = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.isEmpty() ? null : name);
            stmt.setString(2, email.isEmpty() ? null : email);
            stmt.setString(3, phone.isEmpty() ? null : phone);
            stmt.setString(4, address.isEmpty() ? null : address);
            stmt.setInt(5, id);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Record updated successfully!");
            } else {
                System.out.println("No record found with that ID.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating record: " + e.getMessage());
        }
    }

    // 4. Delete a record
    private static void deleteRecord(Connection conn, Scanner scanner) {
        int id = Integer.parseInt(getValidInput(scanner, "Enter the ID of the record to delete: ", CRUD::validateId));
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Record deleted successfully!");
            } else {
                System.out.println("No record found with that ID.");
            }
        } catch (SQLException e) {
            System.err.println("Error deleting record: " + e.getMessage());
        }
    }

    // Validate methods
    private static String getValidInput(Scanner scanner, String prompt, InputValidator validator) {
        String input;
        while (true) {
            System.out.print(prompt);
            input = scanner.nextLine();
            if (validator.validate(input)) {
                break;
            } else {
                System.out.println("Invalid input. Please try again.");
            }
        }
        return input;
    }

    private static String getOptionalValidInput(Scanner scanner, String prompt, InputValidator validator) {
        String input;
        System.out.print(prompt);
        input = scanner.nextLine();
        if (input.isEmpty()) return "";
        while (!validator.validate(input)) {
            System.out.println("Invalid input. Please try again.");
            System.out.print(prompt);
            input = scanner.nextLine();
        }
        return input;
    }

    // InputValidator interface
    interface InputValidator {
        boolean validate(String input);
    }

    // Dummy CRUD class for validation
    static class CRUD {
        public static boolean validateName(String name) {
            return !name.isEmpty();
        }

        public static boolean validateEmail(String email) {
            String emailRegex = "^[\\w-\\.]+@[\\w-]+\\.[a-z]{2,4}$";
            return Pattern.matches(emailRegex, email);
        }

        public static boolean validatePhone(String phone) {
            return phone.matches("\\d{10,15}");
        }

        public static boolean validateAddress(String address) {
            return !address.isEmpty();
        }

        public static boolean validateId(String id) {
            try {
                Integer.parseInt(id);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}



