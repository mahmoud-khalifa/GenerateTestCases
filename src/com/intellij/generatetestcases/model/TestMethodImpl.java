package com.intellij.generatetestcases.model;

import com.intellij.generatetestcases.testframework.TestFrameworkStrategy;
import com.intellij.generatetestcases.util.BddUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.javadoc.PsiDocTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Jaime Hablutzel
 */
public class TestMethodImpl implements TestMethod {


    /**
     * Static factory method
     * Effective Java item 1
     *
     * @param tag
     * @param parent
     * @param frameworkStrategy
     * @return
     */
    static TestMethodImpl newInstance(@NotNull PsiDocTag tag, @NotNull TestClass parent, TestFrameworkStrategy frameworkStrategy) {
        return new TestMethodImpl(tag, parent, frameworkStrategy);
    }

    @Override
    public TestFrameworkStrategy getTestFrameworkStrategy() {
        return testFrameworkStrategy;
    }

    private TestFrameworkStrategy testFrameworkStrategy;

    /**
     * TODO implement
     * State class for two possible states that TestMetho c
     */
    private class TestMethodState {

        private void created(TestMethod tm) {

        }

        private void notCreated(TestMethod tm) {

        }

    }

    // TODO create a strategy for creating test methods


    private PsiMethod sutMethod;

    private PsiDocTag tag;

    private String description;

    public TestClass getParent() {
        return parent;
    }

    private TestClass parent;

    private Project project;

    private TestMethodImpl(@NotNull PsiDocTag tag, @NotNull TestClass parent, TestFrameworkStrategy frameworkStrategy) {

        // TODO instantiate an strategy
        testFrameworkStrategy = frameworkStrategy;

        this.tag = tag;
        this.project = tag.getProject();


        //  obtener el metodo a partir del docTag
        resolveSutMethod(tag);
        //  initialize the description
        this.description = BddUtil.getTagDescription(tag);

        //  bind the current test parent...
        // TODO get this using the tag, or investigate it better
        // TO get the TestClass parent from here without passing it through the constructor
        // it would be needed to implement a registry where we could look for instances for
        // some determined class to guarantee that uniqueness of parents for test methods
        //this.parent = ((PsiMethod)tag.getParent().getParent()).getContainingClass();

        // FIXME parent is being used to get the backing class, maybe delete this dependency??

        this.parent = parent;
    }


    private void resolveSutMethod(PsiDocTag tag) {
        PsiMethod method = (PsiMethod) tag.getParent().getContext();
        this.sutMethod = method;
    }


    /**
     *


     */
    public boolean reallyExists() {
        PsiMethod method1 = null;
        if (this.parent.getBackingElement() != null) {

            method1 = testFrameworkStrategy.findBackingTestMethod(this.parent.getBackingElement(), sutMethod, description);
        }
        PsiMethod method = method1;

        return (null != method) ? true : false;

    }

    @Override
    public void navigate() {
        this.getBackingElement().navigate(true);
    }

    public int create() {


        if (parent == null) {
            // TODO need to look for the parent psi test class in some other way
            // TODO create a stub for the parent or look in registry
            // TODO log it 
        } else if (parent != null && !parent.reallyExists()) {
            PsiDirectory classSourceRoot = null;

            VirtualFile[] sourceRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();
            final PsiManager manager = PsiManager.getInstance(project);

            for (VirtualFile sourceRoot : sourceRoots) {
                if (sourceRoot.isDirectory() && sourceRoot.getPath().contains(project.getName()) && sourceRoot.getPath().contains("src/test/java")) {
                    PsiDirectory directory = manager.findDirectory(sourceRoot);
                    if (directory != null) {
                        classSourceRoot = directory;
                        break;
                    }
                }
            }

            parent.create(classSourceRoot);


        }

        return testFrameworkStrategy.injectBackingTestMethod(parent.getBackingElement(), sutMethod, tag, description);

    }

    public void remove(){
        testFrameworkStrategy.removeTestMethod(parent.getBackingElement(), sutMethod);

    }

    public String getDescription() {
        return description;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PsiMethod getSutMethod() {
        return this.sutMethod;
    }


    public PsiDocTag getBackingTag() {
        return tag;
    }

    public PsiMethod getBackingElement() {
        PsiMethod method = null;
        if (this.parent.getBackingElement() != null) {
            method = testFrameworkStrategy.findBackingTestMethod(this.parent.getBackingElement(), sutMethod, description);
        }
        return method;
    }
    public String getSuggestedMethodName(){
        return testFrameworkStrategy.getSuggestedTestMethodName(sutMethod.getName(), sutMethod.getParameterList().getParameters(), description);
    }

    public Project getProject(){
        return project;
    }
}
