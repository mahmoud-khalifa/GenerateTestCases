package com.intellij.generatetestcases.ui.swing;


import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorMouseAdapter;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;


public class TestMethodRowsWindowBuilder implements FileEditorManagerListener {
    private final TestMethodRowsTableModel model;
    private JBTable table;
    private boolean automaticNavigation;

	public TestMethodRowsWindowBuilder(final Project project) {

        this.model = new TestMethodRowsTableModel(project);

        // ====  listen to file editor manager events  =====
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        fileEditorManager.addFileEditorManagerListener(this);

        // ====  Listen for mouse events  =====
        listenForMouseEvents();
    }

    public JPanel panel() {

		return createPanel(this.model);
	}

    // #################################################################################################################
    //  Helper Methods
    // #################################################################################################################

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
        table.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) > 0 && table.isShowing()) {
                    model.refreshModel();
                }
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

    private void updateModel (VirtualFile virtualFile){
        if (table.isDisplayable())
            model.updateModel(virtualFile);
        else
            model.selectedFile(virtualFile);
    }
    private void listenForMouseEvents() {
        EditorFactory factory = EditorFactory.getInstance();
        factory.addEditorFactoryListener(
                new EditorFactoryAdapter() {
                    @Override
                    public void editorCreated(@NotNull EditorFactoryEvent event) {
                        Editor editor = event.getEditor();
                        editor.addEditorMouseListener(
                                new EditorMouseAdapter() {
                                    @Override
                                    public void mouseClicked(EditorMouseEvent e) {
                                        Document doc = e.getEditor().getDocument();
                                        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(doc);
                                        if (virtualFile != null) {
//                                            System.out.println("Mouse clicked inside: " + virtualFile.getName());
                                            updateModel(virtualFile);
                                        }

                                    }
                                }
                        );
                    }
                }
        );
    }
    // #################################################################################################################
    //  Concrete implementation for FileEditorManagerListener
    // #################################################################################################################

    @Override
    public void fileOpened(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile virtualFile) {
//        System.out.println("fileOpened: " + virtualFile.getName());
        updateModel(virtualFile);
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile virtualFile) {

    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent fileEditorManagerEvent) {
        VirtualFile virtualFile = fileEditorManagerEvent.getNewFile();
//        System.out.println("selectionChanged in file: " + virtualFile.getName());
        updateModel(virtualFile);
    }

}

