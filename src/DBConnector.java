import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.sql.Statement;
import java.sql.PreparedStatement;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Vector;

public class DBConnector {
    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:signal_log.db");
            System.out.println("Connected to SQLite successfully.");
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
        return conn;
    }

    public static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS signal_log(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "signal_source TEXT NOT NULL, " +
                "timestamp TEXT NOT NULL, " +
                "signal_strength INTEGER NOT NULL)";
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table created or already exists.");
        } catch (SQLException e) {
            System.out.println("Table creation failed: " + e.getMessage());
        }
    }

    public static void insertSignal(String source, int strength) {
        String sql = "INSERT INTO signal_log(signal_source, timestamp, signal_strength) VALUES (?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, source);
            pstmt.setString(2, java.time.LocalDateTime.now().toString());
            pstmt.setInt(3, strength);
            pstmt.executeUpdate();
            System.out.println("Signal logged successfully.");
        } catch (SQLException e) {
            System.out.println("Insert failed: " + e.getMessage());
        }
    }

    public static void displaySignal() {
        String query = "SELECT * FROM signal_log";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            System.out.println("Logged Signals:");
            while (rs.next()) {
                String name = rs.getString("signal_source");
                int strength = rs.getInt("signal_strength");
                String time = rs.getString("timestamp");

                System.out.printf("Device: %s | Strength: %d | Time: %s%n", name, strength, time);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Vector<Vector<String>> getAllLogs() {
        Vector<Vector<String>> logs = new Vector<>();
        String sql = "SELECT id, signal_source, signal_strength, timestamp FROM signal_log";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Vector<String> row = new Vector<>();
                row.add(String.valueOf(rs.getInt("id")));
                row.add(rs.getString("signal_source"));
                row.add(String.valueOf(rs.getInt("signal_strength")));
                row.add(rs.getString("timestamp"));
                logs.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public static void exportToCSV(String fileName) throws Exception {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM signal_log");
             FileWriter fw = new FileWriter(fileName)) {
            fw.write("id,source,strength,timestamp\n");
            while (rs.next()) {
                fw.write(rs.getInt("id") + "," + rs.getString("signal_source") + "," + rs.getInt("signal_strength") + "," + rs.getString("timestamp") + "\n");
            }
        }
    }

    // Fix method name typo: deleteLogById (not deleteLogBuyId)
    public static void deleteLogById(int id) {
        String sql = "DELETE FROM signal_log WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Log with id " + id + " deleted successfully.");
                resetAutoincrementifempty();
            } else {
                System.out.println("No log found with id " + id + ".");
            }
        } catch (SQLException e) {
            System.out.println("Delete failed: " + e.getMessage());
        }
    }

    public static class SignalLoggerUI extends JFrame {
        private JTextField sourceField, strengthField;
        private JButton addButton, refreshButton, exportButton, deleteButton;
        private JTable table;
        private DefaultTableModel tableModel;

        public SignalLoggerUI() {
            setTitle("Signal Strength Logger");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLayout(new BorderLayout());

            // Data Entry Panel
            JPanel inputPanel = new JPanel(new FlowLayout());
            sourceField = new JTextField(12);
            strengthField = new JTextField(5);
            addButton = new JButton("Add Log");
            inputPanel.add(new JLabel("Source:"));
            inputPanel.add(sourceField);
            inputPanel.add(new JLabel("Strength:"));
            inputPanel.add(strengthField);
            inputPanel.add(addButton);

            // Data Display Table
            tableModel = new DefaultTableModel(new String[]{"ID", "Source", "Strength", "TimeStamp"}, 0);
            table = new JTable(tableModel);
            JScrollPane tableScroll = new JScrollPane(table);

            // Buttons Panel
            JPanel buttonPanel = new JPanel();
            refreshButton = new JButton("Refresh Logs");
            exportButton = new JButton("Export as CSV");
            deleteButton = new JButton("Delete Log");

            buttonPanel.add(refreshButton);
            buttonPanel.add(exportButton);
            buttonPanel.add(deleteButton);

            add(inputPanel, BorderLayout.NORTH);
            add(tableScroll, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);

            pack();
            setLocationRelativeTo(null);
            setVisible(true);

            // Add Log Action
            addButton.addActionListener(e -> {
                String src = sourceField.getText().trim();
                String val = strengthField.getText().trim();
                if (src.isEmpty() || val.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Enter both fields.");
                    return;
                }
                try {
                    int strength = Integer.parseInt(val);
                    if (strength < -120 || strength > 0) throw new NumberFormatException();
                    DBConnector.insertSignal(src, strength);
                    sourceField.setText("");
                    strengthField.setText("");
                    loadTable();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Enter a valid strength (-120 to 0).");
                }
            });

            // Refresh Logs Action
            refreshButton.addActionListener(e -> loadTable());

            // Export CSV Action
            exportButton.addActionListener(e -> {
                try {
                    DBConnector.exportToCSV("signals.csv");
                    JOptionPane.showMessageDialog(this, "Exported to signals.csv");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Export failed.");
                }
            });

            // Delete Log Action
            deleteButton.addActionListener(e -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a log entry to delete.");
                    return;
                }

                int logId = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());

                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete log ID " + logId + "?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    DBConnector.deleteLogById(logId);
                    loadTable();
                }
            });

            //Reset Button
            JButton resetButton = new JButton("Reset IDs");
            buttonPanel.add(resetButton);
            resetButton.addActionListener(e ->{
                DBConnector.resetAutoincrementifempty();
                loadTable();
                JOptionPane.showMessageDialog(this,"ID counter reset (if table was empty)");
            });

            loadTable(); // Load data on startup
        }

        private void loadTable() {

            tableModel.setRowCount(0);
            int rowNum = 1;
            for (Vector<String> row : DBConnector.getAllLogs()) {
                Vector<String> displayRow = new Vector<>(row);
                displayRow.insertElementAt(String.valueOf(rowNum++),0);
                tableModel.addRow(row);
            }
        }
    }

    // Reset my auto-increment if there is no data in my table
    public static void resetAutoincrementifempty()
    {
        String countsql = "SELECT COUNT(*) AS total FROM signal_log";
        String resetsql = "DELETE FROM sqlite_sequence WHERE name = 'signal_log'";

        try(Connection conn = connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(countsql))
        {
            int total = 0;
            if(rs.next())
            {
                total = rs.getInt("total");
            }

            if(total == 0)
            {
                stmt.executeUpdate(resetsql);
                System.out.println("Auto-increment counter reset because table is empty.");

            }
            else
            {
                System.out.println("Table not empty, auto-increment counter not reset.");
            }
        }
        catch (SQLException e)
        {
            System.out.println("Failed to reset auto-increment counter: "+e.getMessage());
        }
    }
}
