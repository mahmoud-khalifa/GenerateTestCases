package com.intellij.generatetestcases.model;

import org.jetbrains.annotations.NotNull;

import com.intellij.generatetestcases.ui.swing.TestMethodRowsWindowBuilder;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;

/**
 * User: mahmoudkhalifa
 * Date: 8/5/15
 */
public class GenerateTestCasesProjectComponent implements ProjectComponent {
    private Project project;

    public GenerateTestCasesProjectComponent(Project project) {
        this.project = project;
    }

    /*    */
    @Override
    public void projectOpened() {
        // ====  Add tool window  =====
        TestMethodRowsWindowBuilder classOutlineWindowBuilder = new TestMethodRowsWindowBuilder(this.project);
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(this.project);
        ToolWindow tw = toolWindowManager.registerToolWindow("Tests", classOutlineWindowBuilder.panel(), ToolWindowAnchor.RIGHT);
        tw.setIcon(IconLoader.getIcon("/images/junitopenmrs.gif"));
        tw.setToHideOnEmptyContent(true);
    }

    @Override
    public void projectClosed() {

    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(this.project);
        toolWindowManager.unregisterToolWindow("Tests");
    }

    @NotNull
    @Override
    public String getComponentName() {
        return GenerateTestCasesProjectComponent.class.getSimpleName();
    }
}
