package importre.intellij.android.selector.color;

import javax.swing.*;
import java.awt.*;

public class ColorItemRenderer implements ListCellRenderer {
    DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

    @Override
    public Component getListCellRendererComponent(
            JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        Icon icon;
        String name;

        JLabel renderer = (JLabel) defaultRenderer
                .getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

        String values[] = (String[]) value;
        name = values[1];
        icon = new ColorIcon(values[0]);

        renderer.setIcon(icon);
        renderer.setText(name);
        return renderer;
    }
}
