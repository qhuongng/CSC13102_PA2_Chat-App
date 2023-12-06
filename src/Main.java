import java.awt.Font;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;

public class Main {

    public static void setUIFont(FontUIResource f) {
        java.util.Enumeration<?> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource)
                UIManager.put(key, f);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                setUIFont(new FontUIResource((new JLabel().getFont().getName()), Font.PLAIN, 15));
            } catch (UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            new Thread(new Server()).start();
            new ClientUI("Client 1");
            new ClientUI("Client 2");
        });
    }
}