package Anesk;

import javax.swing.*;
import javax.swing.Timer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.Image; 

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;


public final class LoadingScreen {
  private LoadingScreen() {}

  // ---------- Skin ----------
  public static class Skin {
    public final Color[] palette;
    public Skin(Color... p) { this.palette = p; }
  }
  public interface ProgressSink { void accept(int percent); }

  public static void run(
      String title, int width, int height,
      List<Skin> skins,
      java.util.function.Consumer<ProgressSink> loader,
      Supplier<JComponent> gameFactory) {

    JFrame f = new JFrame(title);
    f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    f.setSize(width, height);
    f.setLocationRelativeTo(null);
    f.setIconImage(loadAppIcon());

    LoaderPanel panel = new LoaderPanel(skins);
    f.setContentPane(panel);
    f.setVisible(true);

    SwingWorker<Void,Integer> worker = new SwingWorker<>() {
      @Override protected Void doInBackground() {
        AtomicInteger last = new AtomicInteger(0);
        loader.accept(p -> {
          int clamped = Math.max(0, Math.min(100, p));
          if (clamped != last.get()) {
            last.set(clamped);
            publish(clamped);
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
          }
        });
        return null;
      }
      @Override protected void process(List<Integer> chunks) {
        if (!chunks.isEmpty()) panel.setProgress(chunks.get(chunks.size()-1));
      }
      @Override protected void done() {
        panel.setProgress(100);
        Timer t = new Timer(250, e -> {
          JComponent game = gameFactory.get();
          f.setContentPane(game);
          f.revalidate();
          game.requestFocusInWindow();
          ((Timer)e.getSource()).stop();
        });
        t.setRepeats(false);
        t.start();
      }
    };
    worker.execute();
  }

  public static List<Skin> defaultSkinsWWW() {
    Skin brand = new Skin(hex("#8C00FF"), hex("#9B25FF"), hex("#AA4DFF"), hex("#B973FF"));
    Skin solar = new Skin(hex("#FFC400"), hex("#FFDA4D"), hex("#FFE27A"), hex("#FFF0B8"));
    Skin neon  = new Skin(hex("#00FFC8"), hex("#4DFFD8"), hex("#7AFFE3"), hex("#B8FFF0"));
    Skin magma = new Skin(hex("#FF3B30"), hex("#FF6B61"), hex("#FF8C85"), hex("#FFB3AE"));
    return List.of(brand, solar, neon, magma);
  }

  private static final class LoaderPanel extends JPanel implements ActionListener {
    private static final int MARGIN = 28;
    private static final int SEGMENT_SPACING = 6;
    private static final int SEGMENT_SIZE = 10;
    private static final int FPS = 60;

    private final List<Point2D> perimeter = new ArrayList<>();
    private final List<Skin> skins;
    private int progress = 0;
    private float shimmerPhase = 0;
    private final Timer anim;  // <-- name not 'timer' to avoid confusion

    LoaderPanel(List<Skin> skins) {
      this.skins = (skins == null || skins.isEmpty()) ? defaultSkinsWWW() : skins;
      setBackground(new Color(14,14,18));
      setForeground(Color.WHITE);
      setDoubleBuffered(true);
      setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
      anim = new Timer(1000 / FPS, this);
      anim.start();

      addComponentListener(new ComponentAdapter() {
        @Override public void componentResized(ComponentEvent e) { rebuildPerimeter(); }
      });
    }

    void setProgress(int p) { this.progress = Math.max(0, Math.min(100, p)); repaint(); }
    @Override public void addNotify() { super.addNotify(); rebuildPerimeter(); }
    private void rebuildPerimeter() {
      perimeter.clear();
      int w = getWidth(), h = getHeight();
      int x0 = MARGIN, y0 = MARGIN, x1 = w - MARGIN, y1 = h - MARGIN;
      for (int x = x0; x <= x1; x += SEGMENT_SPACING) perimeter.add(new Point2D.Double(x, y0));
      for (int y = y0; y <= y1; y += SEGMENT_SPACING) perimeter.add(new Point2D.Double(x1, y));
      for (int x = x1; x >= x0; x -= SEGMENT_SPACING) perimeter.add(new Point2D.Double(x, y1));
      for (int y = y1; y >= y0; y -= SEGMENT_SPACING) perimeter.add(new Point2D.Double(x0, y));
      repaint();
    }
    @Override public void actionPerformed(ActionEvent e) { shimmerPhase += 0.015f; repaint(); }

    @Override protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      String msg = "Preparing assets… " + progress + "%";
      g2.setColor(new Color(28,28,36));
      g2.fillRoundRect(12, 12, getWidth()-24, 48, 16, 16);
      g2.setColor(new Color(230,230,235));
      g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
      g2.drawString(msg, 24, 42);

      Stroke old = g2.getStroke();
      g2.setStroke(new BasicStroke(2f));
      g2.setColor(new Color(255,255,255,24));
      g2.draw(new RoundRectangle2D.Double(MARGIN-8, MARGIN-8,
              getWidth()-2*(MARGIN-8), getHeight()-2*(MARGIN-8), 16, 16));

      if (!perimeter.isEmpty() && progress > 0) {
        int fillCount = (int)Math.floor(perimeter.size() * (progress / 100.0));
        int bandSize = 12;
        for (int i = 0; i < fillCount; i++) {
          Point2D p = perimeter.get(i);
          Skin skin = skins.get((i / bandSize) % skins.size());
          Color base = skin.palette[i % skin.palette.length];
          float pulse = 0.5f + 0.5f*(float)Math.sin(shimmerPhase*2.0 + i*0.15);
          int a = (int)(180 + 60 * pulse);
          Color c = new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
          boolean head = (i >= fillCount - 4);
          int size = head ? SEGMENT_SIZE + 2 : SEGMENT_SIZE;

          g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 40));
          g2.fill(new Ellipse2D.Double(p.getX()- (size+8)/2.0, p.getY()- (size+8)/2.0, size+8, size+8));
          g2.setColor(c);
          g2.fill(new Ellipse2D.Double(p.getX()- size/2.0, p.getY()- size/2.0, size, size));
        }
      }

      String hint = "Tip: Press ESC in games to return to Hub";
      Font fSmall = getFont().deriveFont(Font.PLAIN, 12f);
      FontMetrics fm = g2.getFontMetrics(fSmall);
      g2.setFont(fSmall);
      g2.setColor(new Color(200,200,210,160));
      g2.drawString(hint, getWidth()-fm.stringWidth(hint)-16, getHeight()-16);

      g2.setStroke(old);
      g2.dispose();
    }
  }

  private static Color hex(String s) { return Color.decode(s.trim()); }

  private static Image loadAppIcon() {
    BufferedImage img = new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setColor(new Color(140,0,255));
    g.fillRoundRect(0,0,32,32,8,8);
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(3f));
    g.drawLine(6,22,26,10);
    g.dispose();
    return img;
  }
}
