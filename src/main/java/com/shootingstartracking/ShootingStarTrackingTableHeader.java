package com.shootingstartracking;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class ShootingStarTrackingTableHeader extends JPanel {

    private final JLabel textLabel = new JLabel();
    private final JLabel arrowLabel = new JLabel();
    private static final ImageIcon ARROW_UP;
    private static final ImageIcon HIGHLIGHT_ARROW_UP;
    private static final ImageIcon HIGHLIGHT_ARROW_DOWN;
    private boolean ordering = false;
    static
    {
        final BufferedImage arrowDown = ImageUtil.loadImageResource(ShootingStarTrackingPlugin.class, "/arrow.png");
        final BufferedImage arrowUp = ImageUtil.rotateImage(arrowDown,Math.PI);
        ARROW_UP = new ImageIcon(arrowUp);

        final BufferedImage highlightArrowDown = ImageUtil.fillImage(arrowDown, ColorScheme.BRAND_ORANGE);
        final BufferedImage highlightArrowUp = ImageUtil.fillImage(arrowUp, ColorScheme.BRAND_ORANGE);
        HIGHLIGHT_ARROW_DOWN = new ImageIcon(highlightArrowDown);
        HIGHLIGHT_ARROW_UP = new ImageIcon(highlightArrowUp);
    }
    ShootingStarTrackingTableHeader(String title)
    {
        setLayout(new BorderLayout(5,0));
        setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0,0,0,1,ColorScheme.LIGHT_GRAY_COLOR),
                new EmptyBorder(0,5,0,2)
        ));
        setBackground(ColorScheme.SCROLL_TRACK_COLOR);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!ordering) textLabel.setForeground(ColorScheme.BRAND_ORANGE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!ordering) textLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            }
        });
        textLabel.setText(title);
        textLabel.setFont(FontManager.getRunescapeSmallFont());
        arrowLabel.setIcon(ARROW_UP);
        add(textLabel,BorderLayout.WEST);
        add(arrowLabel,BorderLayout.EAST);
    }
    void highlight(boolean on, boolean ascending)
    {
        ordering = on;
        arrowLabel.setIcon(on ? (ascending ? HIGHLIGHT_ARROW_DOWN : HIGHLIGHT_ARROW_UP) : ARROW_UP);
        textLabel.setForeground(on ? ColorScheme.BRAND_ORANGE : ColorScheme.LIGHT_GRAY_COLOR);
    }
}
