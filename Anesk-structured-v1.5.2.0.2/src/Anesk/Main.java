package Anesk;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {

      // --- Preferences must be initialized first ---
      Prefs.init();
      Maps.activate("lab-01");
      Maps.ensureActive();

      // (optional) force a default/starting skin once
      // Prefs.setSkin(Skins.Skin.SOLAR);

      JFrame f = new JFrame("Anesk · Web Weavers World");
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      Anesk t = new Anesk();
      f.setContentPane(t);
      f.setResizable(false);
      f.pack();
      f.setLocationRelativeTo(null);

      // Icons
      List<Image> icons = new ArrayList<>();
      String[] paths = {
          // prefer these first (brand icon): misc
          "/assets/icons/android-chrome-512x512.png",
          "/assets/icons/android-chrome-192x192.png",
          "/assets/icons/apple-touch-icon.png",
          "/favicon-32x32.png",
          "/assets/icons/favicon-32x32.png",
          "/assets/icons/favicon-16x16.png",
          "/anesk.ico",
          // (optional) ICOs – Swing can't read .ico by default; kept as no-op fallbacks
          "/assets/icons/favicon.ico",
          "/assets/icons/anesk.ico"
      };
      for (String p : paths) {
        var url = Anesk.class.getResource(p);
        if (url != null)
          icons.add(new ImageIcon(url).getImage());
      }
      if (!icons.isEmpty())
        f.setIconImages(icons);

      // Save progress on close
      f.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
          t.saveProgress();
        }
      });

      f.setVisible(true);
      t.requestFocusInWindow();
    });
  }
}
