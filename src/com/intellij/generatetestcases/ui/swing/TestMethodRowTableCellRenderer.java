package com.intellij.generatetestcases.ui.swing;


import com.intellij.generatetestcases.model.TestMethodRow;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class TestMethodRowTableCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer {


    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus, int row, int column) {

        JLabel label =
                (JLabel) super.getTableCellRendererComponent(table,
                        value,
                        isSelected,
                        hasFocus, row, column);

        TestMethodRow testMethodRow = (TestMethodRow) value;
        label.setText(testMethodRow.getDescription());
        switch (testMethodRow.getStatus()){
            case MATCH:
                label.setToolTipText("Test exists for this case - click to navigate");
                break;
            case MISSING:
                label.setToolTipText("No test exists for this case - click to create");
                label.setForeground(new Color(117, 117, 117));
                break;
            case NO_TEST_SUBJECT:
                label.setToolTipText("Test exists with no matching case - click to navigate, then remove manually");
                label.setForeground(new Color(255, 0, 0));
                break;
            case SECTION_Header:
                label.setToolTipText("Click to navigate to method");
                Font labelFont = new Font("default", Font.BOLD, 14);
                label.setFont(labelFont);
                break;
            default:
                break;
        }
        return (label);
    }

}
