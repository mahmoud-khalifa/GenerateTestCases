package com.intellij.generatetestcases.ui.swing;


import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import java.awt.*;


public class TestMethodRowsWindowBuilder {
    private final TestMethodRowsTableModel model;
    private JBTable table;
    private boolean automaticNavigation;

	public TestMethodRowsWindowBuilder(final Project project) {

        this.model = new TestMethodRowsTableModel(project);
    }

    public JPanel panel() {

		return createPanel(this.model);
	}

	private JPanel createPanel(final TestMethodRowsTableModel model) {

		table = new JBTable(model) {

			public TableCellRenderer getCellRenderer(int i, int i1) {

				return new TestMethodRowTableCellRenderer();
			}
		};
		table.setTableHeader(null);
		table.setShowGrid(false);
		table.setSelectionMode(0);
		table.getSelectionModel()
				.addListSelectionListener(new ListSelectionListener() {

                    public void valueChanged(ListSelectionEvent event) {

                        if ((!event.getValueIsAdjusting()) && (table.getSelectedRow() != -1) && !isAutomaticNavigation())
                            model.performAction(table.getSelectedRow());
                    }
                });

        JScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.getVerticalScrollBar().setUnitIncrement(30);

		JPanel jPanel = new JPanel(new BorderLayout());
		jPanel.add(scrollPane, BorderLayout.CENTER);
		return jPanel;
	}

    private boolean isAutomaticNavigation() {
        if (automaticNavigation){
            setAutomaticNavigation(false);
            return true;
        } else {
            return false;
        }
    }

    private void setAutomaticNavigation(boolean automaticNavigation) {
        this.automaticNavigation = automaticNavigation;
    }

}

