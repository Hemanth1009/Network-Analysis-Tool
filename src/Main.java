import javax.swing.*;

public class Main
{
    public static void main(String[] args) {
        DBConnector.connect();
        DBConnector.createTable();
        DBConnector.displaySignal();
        SwingUtilities.invokeLater(DBConnector.SignalLoggerUI::new);
    }
}
