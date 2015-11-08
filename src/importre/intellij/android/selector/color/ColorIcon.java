package importre.intellij.android.selector.color;

import com.intellij.ui.JBColor;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorIcon implements Icon {

    private final String color;

    public ColorIcon(String color) {
        this.color = color.startsWith("#") ? color.substring(1) : null;
    }

    public int getIconHeight() {
        return 16;
    }

    public int getIconWidth() {
        return 16;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        JBColor color = getColor();
        if (color == null) {
            g.setColor(JBColor.WHITE);
            g.fillRect(1, 1, getIconWidth(), getIconHeight());
            g.setColor(JBColor.DARK_GRAY);

            if (g instanceof Graphics2D) {
                RenderingHints.Key key = RenderingHints.KEY_TEXT_ANTIALIASING;
                Object value = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
                ((Graphics2D) g).setRenderingHint(key, value);
            }

            String q = "?";
            FontMetrics fm = g.getFontMetrics();
            Rectangle2D r = fm.getStringBounds(q, g);
            x = (int) ((getIconWidth() - (int) r.getWidth()) * 0.7f);
            y = (getIconHeight() - (int) r.getHeight()) / 2 + fm.getAscent();
            g.drawString(q, x, y);
        } else {
            g.setColor(color);
            g.fillRect(1, 1, getIconWidth(), getIconHeight());
        }
        g.setColor(JBColor.DARK_GRAY);
        g.drawRect(1, 1, getIconWidth(), getIconHeight());
    }

    @Nullable
    private JBColor getColor() {
        String regex = "((?:[0-9a-fA-F]{2})?)" +
                "([0-9a-fA-F]{2})" +
                "([0-9a-fA-F]{2})" +
                "([0-9a-fA-F]{2})";
        Pattern p = Pattern.compile(regex);
        try {
            if (color == null) {
                return null;
            }

            Matcher m = p.matcher(color);
            if (m.find()) {
                int r = Integer.parseInt(m.group(2), 16);
                int g = Integer.parseInt(m.group(3), 16);
                int b = Integer.parseInt(m.group(4), 16);
                if (m.group(1).isEmpty()) {
                    return new JBColor(
                            new Color(r, g, b),
                            new Color(r, g, b));
                } else {
                    int a = Integer.parseInt(m.group(1), 16);
                    return new JBColor(
                            new Color(r, g, b, a),
                            new Color(r, g, b, a));
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }
}

