package com.intellij.generatetestcases.ui.swing;


import com.intellij.generatetestcases.model.TestMethodRows;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import javax.swing.table.AbstractTableModel;
import java.util.Arrays;
import java.util.List;


public class TestMethodRowsTableModel
							extends
								AbstractTableModel {

    private final Project project;

    private PsiFile selectedFile;
    private TestMethodRows testMethodRows;

    public TestMethodRowsTableModel(final Project project) {
        this.project = project;
    }

	public int getRowCount() {
        if (testMethodRows != null)
		    return testMethodRows.size();
        else
            return 0;
	}

	public int getColumnCount() {

		return 1;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		return testMethodRows.get(rowIndex);
	}

	public void performAction(int index) {
        if (isSelectedFile(selectedFile.getVirtualFile())){
//            System.out.println("perform action for in file: " + selectedFile.getName());
            testMethodRows.itemSelectedAtIndex(index);
        }
    }

    public void selectedFile(VirtualFile virtualFile) {
        if (isSelectedFile(virtualFile)) {
            selectedFile = PsiManager.getInstance(project).findFile(virtualFile);
        }
    }

    public void refreshModel() {
        if (selectedFile.getVirtualFile().getPath().endsWith(".java")) {
            testMethodRows = TestMethodRows.forFile(selectedFile, project);
        } else {
            testMethodRows = TestMethodRows.empty();
        }

        // Refresh the table view
        fireTableDataChanged();
    }

    public void updateModel(VirtualFile virtualFile) {


        if (isSelectedFile(virtualFile)){
            System.out.println("Update model for virtual file: " + virtualFile.getName());
            selectedFile = PsiManager.getInstance(project).findFile(virtualFile);
            refreshModel();
        }
    }

    private boolean isSelectedFile(final VirtualFile virtualFile) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        List<VirtualFile>files = Arrays.asList(manager.getSelectedFiles());
        for (VirtualFile file : files) {
            if (virtualFile.equals(file))
                return true;
        }
        return false;
    }
}
