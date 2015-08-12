package com.intellij.generatetestcases.ui.swing;


import com.intellij.generatetestcases.model.TestMethodRows;
import com.intellij.openapi.editor.*;
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
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.AbstractTableModel;


public class TestMethodRowsTableModel
							extends
								AbstractTableModel
        implements FileEditorManagerListener {

    private final Project project;

    private PsiFile selectedFile;
    private TestMethodRows testMethodRows;

    public TestMethodRowsTableModel(final Project project) {
        this.project = project;

        // ====  listen to file editor manager events  =====
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        fileEditorManager.addFileEditorManagerListener(this);

        // ====  Listen for mouse events  =====
        listenForMouseEvents();
    }

	public int getRowCount() {
		return testMethodRows.size();
	}

	public int getColumnCount() {

		return 1;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		return testMethodRows.get(rowIndex);
	}

	public void performAction(int index) {
        testMethodRows.itemSelectedAtIndex(index);
    }

    // #################################################################################################################
    //  Concrete implementation for FileEditorManagerListener
    // #################################################################################################################

    @Override
    public void fileOpened(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile virtualFile) {
        updateModel(virtualFile);
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile virtualFile) {

    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent fileEditorManagerEvent) {
        VirtualFile virtualFile = fileEditorManagerEvent.getNewFile();
        updateModel(virtualFile);
    }


    // #################################################################################################################
    //  Helper Methods
    // #################################################################################################################

    private void updateModel(VirtualFile virtualFile) {
        selectedFile = PsiManager.getInstance(project).findFile(virtualFile);

        if (virtualFile.getPath().endsWith(".java")) {
            testMethodRows = TestMethodRows.forFile(selectedFile, project);
        } else {
            testMethodRows = TestMethodRows.empty();
        }

        // Refresh the table view
        fireTableDataChanged();
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
                                            updateModel(virtualFile);
                                        }
                                    }
                                }
                        );
                    }
                }
        );
    }

}
