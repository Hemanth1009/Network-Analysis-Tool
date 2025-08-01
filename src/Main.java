import javax.swing.*;

public class Main
{
    public static void main(String[] args) {
        DBConnector.connect();
        DBConnector.createTable();
//        DBConnector.insertSignal("WiFi Router",-68);
        DBConnector.displaySignal();
        SwingUtilities.invokeLater(DBConnector.SignalLoggerUI::new);
    }
}