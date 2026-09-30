package rift;

import java.awt.event.KeyEvent;
import java.util.BitSet;

/** Events are accumulated on the AWT thread and consumed once per game tick. */
final class Input {
    private final BitSet down = new BitSet(1024), edges = new BitSet(1024);
    private boolean left, right, click;
    private double dx, dy;
    private int mx, my;
    synchronized void key(int key, boolean value) {
        if (value && !down.get(key)) edges.set(key);
        down.set(key, value);
    }
    synchronized void mouse(int button, boolean value) {
        if (button == 1) { if (value && !left) click = true; left = value; }
        if (button == 3) right = value;
    }
    synchronized void position(int x, int y) { mx = x; my = y; }
    synchronized void motion(double x, double y) { dx += x; dy += y; }
    synchronized void clear() { down.clear(); edges.clear(); left = right = click = false; dx = dy = 0; }
    synchronized Frame poll() {
        Frame f = new Frame((BitSet) down.clone(), (BitSet) edges.clone(), left, right, click, dx, dy, mx, my);
        edges.clear(); click = false; dx = dy = 0; return f;
    }
    record Frame(BitSet down, BitSet edges, boolean fire, boolean aim, boolean click,
                 double dx, double dy, int mx, int my) {
        boolean held(int key) { return down.get(key); }
        boolean pressed(int key) { return edges.get(key); }
        static Frame empty() { return new Frame(new BitSet(), new BitSet(), false, false, false, 0, 0, 0, 0); }
        static Frame keys(int... keys) { BitSet b = new BitSet(); for (int k : keys) b.set(k); return new Frame(b, b, false, false, false, 0, 0, 0, 0); }
    }
}
