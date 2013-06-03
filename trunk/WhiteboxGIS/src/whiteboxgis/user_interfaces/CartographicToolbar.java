/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whiteboxgis.user_interfaces;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import whiteboxgis.WhiteboxGui;

/**
 *
 * @author johnlindsay
 */
public class CartographicToolbar extends JToolBar {

    private JButton alignAndDistribute = new JButton();
    private JButton alignRight = new JButton();
    private JButton alignLeft = new JButton();
    private JButton alignTop = new JButton();
    private JButton alignBottom = new JButton();
    private JButton centerVerticalBtn = new JButton();
    private JButton centerHorizontalBtn = new JButton();
    private JButton distributeVertically = new JButton();
    private JButton distributeHorizontally = new JButton();
    private JButton group = new JButton();
    private JButton ungroup = new JButton();
//    private JSeparator separator1 = new JSeparator();
//    private JSeparator separator2 = new JSeparator();
    private WhiteboxGui host;
    private static String pathSep = File.separator;
    private boolean buttonVisibility = false;
//    private int separatorOrientation = SwingConstants.HORIZONTAL;
    
    // constructors
    public CartographicToolbar() {
        // no-arg constructor
        init();
    }

    public CartographicToolbar(WhiteboxGui host, boolean buttonVisibility) {
        this.host = host;
        this.buttonVisibility = buttonVisibility;
        init();
    }

    // properties
    public WhiteboxGui getHost() {
        return host;
    }

    public void setHost(WhiteboxGui host) {
        this.host = host;
    }

    public boolean isButtonVisibility() {
        return buttonVisibility;
    }

    public void setButtonVisibility(boolean buttonVisibility) {
        this.buttonVisibility = buttonVisibility;
    }

    // methods
    private void init() {
        if (host == null) {
            return;
        }
        this.setOrientation(SwingConstants.VERTICAL);
        
        String imgLocation = host.getResourcesDirectory() + "Images" + pathSep + "AlignAndDistribute.png";
        ImageIcon image = new ImageIcon(imgLocation, "");

        //Create and initialize the button.
        alignAndDistribute.setToolTipText("Align And Distribute");
        alignAndDistribute.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (buttonVisibility) {
                    buttonVisibility = false;
                } else {
                    buttonVisibility = true;
                }
                
                createToolbar();
            }
        });
        alignAndDistribute.setOpaque(false);
        alignAndDistribute.setBorderPainted(false);

        try {
            alignAndDistribute.setIcon(image);
        } catch (Exception e) {
            alignAndDistribute.setText("alignAndDistribute");
            host.showFeedback(e.getMessage());
        }
        
        alignRight = makeToolBarButton("AlignRight.png", "alignRight", "Align Right", "alignRight");
        
        centerVerticalBtn = makeToolBarButton("CenterVertical.png",
                "centerVertical", "Center Vertical", "centerVertical");
        
        alignLeft = makeToolBarButton("AlignLeft.png", "alignLeft",
                "Align Left", "alignLeft");
        
        alignTop = makeToolBarButton("AlignTop.png", "alignTop",
                "Align Top", "alignTop");
        
        centerHorizontalBtn = makeToolBarButton("CenterHorizontal.png",
                "centerHorizontal", "Center Horizontal", "centerHorizontal");
        
        alignBottom = makeToolBarButton("AlignBottom.png", "alignBottom",
                "Align Bottom", "alignBottom");
        
        distributeVertically = makeToolBarButton("DistributeVertically.png",
                "distributeVertically", "Distribute Vertically", "distributeVertically");
        
        distributeHorizontally = makeToolBarButton("DistributeHorizontally.png",
                "distributeHorizontally", "Distribute Horizontally", "distributeHorizontally");
        
        group = makeToolBarButton("GroupElements.png",
                "groupElements", "Group elements", "groupElements");
        
        ungroup = makeToolBarButton("UngroupElements.png",
                "ungroupElements", "Unroup elements", "ungroupElements");
        
        
        createToolbar();
    }
    
    private void createToolbar() {
        this.removeAll();
        
        alignAndDistribute.setBorderPainted(buttonVisibility);
        add(alignAndDistribute);

        if (buttonVisibility) {
            addSeparator();
            add(alignRight);
            add(centerVerticalBtn);
            add(alignLeft);
            add(alignTop);
            add(centerHorizontalBtn);
            add(alignBottom);
            addSeparator();
            add(distributeVertically);
            add(distributeHorizontally);
            addSeparator();
            add(group);
            add(ungroup);
        }
    }

    private JButton makeToolBarButton(String imageName, String actionCommand, String toolTipText, String altText) {
        //Look for the image.
        String imgLocation = host.getResourcesDirectory() + "Images" + pathSep + imageName;
        ImageIcon image = new ImageIcon(imgLocation, "");

        //Create and initialize the button.
        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(host);
        button.setOpaque(false);
        button.setBorderPainted(false);
        try {
            button.setIcon(image);
        } catch (Exception e) {
            button.setText(altText);
            host.showFeedback(e.getMessage());
        }

        return button;
    }
}