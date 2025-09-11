package Anesk;

import java.awt.Color;

final class Toast {
  final String text;
  final Color accent;
  final long t0 = System.nanoTime();
  final int ms;

  Toast(String text, Color accent, int ms) {
    this.text = text;
    this.accent = accent;
    this.ms = ms;
  }

  int ageMs() {
    return (int)((System.nanoTime() - t0) / 1_000_000L);
  }
}
