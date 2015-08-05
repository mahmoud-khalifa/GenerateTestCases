package com.intellij.generatetestcases.actions;

import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.generatetestcases.GenerateTestCasesBundle;
import com.intellij.generatetestcases.model.BDDCore;
import com.intellij.generatetestcases.model.TestClass;
import com.intellij.generatetestcases.model.TestMethod;
import com.intellij.generatetestcases.testframework.AbstractTestFrameworkStrategy;
import com.intellij.generatetestcases.testframework.SupportedFrameworks;
import com.intellij.generatetestcases.testframework.TestFrameworkStrategy;
import com.intellij.generatetestcases.ui.codeinsight.generation.PsiDocAnnotationMember;
import com.intellij.generatetestcases.util.BddUtil;
import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.DirectoryChooser;
import com.intellij.ide.util.MemberChooser;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * User: JHABLUTZEL
 * Date: 20/10/2010
 * Time: 12:27:20 PM
 */
public class GenerateTestMethods extends AnAction {

    public GenerateTestMethods() {
        super("Generate Test Methods", "Generate test methods for current file", IconLoader.getIcon("/images/junitopenmrs.gif"));
    }

    /**
     * It allows the user to create a test in the directory he chooses (test or production)
     *
     * @param e
     * @should process inmediately upper class if caret is at anonymous class
     */
    public void actionPerformed(AnActionEvent e) {

        // todo ADD support for unit testing, showing no ui, use ApplicationManager.getApplication().isUnitTestMode()
        DataContext dataContext = e.getDataContext();

        //  to get the current project
        final Project project = PlatformDataKeys.PROJECT.getData(dataContext);
        Editor editor = getEditor(dataContext);

        //  prompt to choose the strategy if it haven't been choosen before
        String testFrameworkProperty = "JUNIT4";
//        if (ApplicationManager.getApplication().isUnitTestMode()) {
//            testFrameworkProperty = "JUNIT3";
//        } else {
//
//
//            GenerateTestCasesSettings casesSettings = GenerateTestCasesSettings.getInstance(project);
//            testFrameworkProperty = casesSettings.getTestFramework();
//
//            if (StringUtils.isEmpty(testFrameworkProperty)) { //  it haven't been defined yet
//
//                ConfigurableEP[] extensions = project.getExtensions(ExtensionPointName.<ConfigurableEP>create("com.intellij.projectConfigurable"));
////                List<Configurable> list = new ArrayList<Configurable>();
//                for (ConfigurableEP component : extensions) {
//                    Configurable configurable = (Configurable) component.createConfigurable();
//                    if (configurable instanceof GenerateTestCasesConfigurable) {
//                        ShowSettingsUtil.getInstance().editConfigurable(project, configurable);
//                        break;
//                    }
//                }
//
//                //  verify if something has been selected, if not just skip
//                //  overwrite s variable
//                testFrameworkProperty = casesSettings.getTestFramework();
//                if (StringUtils.isEmpty(testFrameworkProperty)) {
//
//                    //  show dialog displaying that there is no framework selection
//                    Messages.showMessageDialog(GenerateTestCasesBundle.message("plugin.GenerateTestCases.framework.notselected.desc"), GenerateTestCasesBundle.message("plugin.GenerateTestCases.framework.notselected"),
//                            Messages.getWarningIcon());
//
//                    return;
//                }
//            }
//
//        }

        PsiClass psiClass = getSubjectClass(editor, dataContext);

        if (psiClass != null) {
            //  create test class for this psiClass

            //  get the current test framework strategy from settings


            // TODO replace it by strong typed way to determine the framework
            TestFrameworkStrategy tfs = SupportedFrameworks.getStrategyForFramework(project, testFrameworkProperty);

            final TestClass testClass = BDDCore.createTestClass(psiClass, tfs);


            if (!ApplicationManager.getApplication().isUnitTestMode()) {

                //  use tfs to find out if the required libraries by the test framework area available
                //  get current module
                Module module = ModuleUtil.findModuleForPsiElement(psiClass);


                //  test if framework is available in project
                boolean isAvailable = tfs.isTestFrameworkLibraryAvailable(module);

                //   if it isn't display dialog allowing the user to add library to the project
                if (!isAvailable) {
                    //  display alert, look for something similiar, not obstrusive :D
                    // TODO improve TestFrameworkStrategy interface to include the descriptor
                    final FixTestLibraryDialog d = new FixTestLibraryDialog(project, module, ((AbstractTestFrameworkStrategy) tfs).getTestFramework());
                    d.show();
                    if (!d.isOK()) return;
                }
            }

            //////////////////////////////


            ArrayList<ClassMember> array = new ArrayList<ClassMember>();

            List<TestMethod> allMethodsInOriginClass = testClass.getAllMethods();
            boolean createParent = false;
            //  ensure if parent exists
            PsiDirectory destinationRoot = null;
            final List<TestMethod> methodsToCreate = new ArrayList<TestMethod>();

            if (ApplicationManager.getApplication().isUnitTestMode()) {
                // in unit test mode it will create test methods for all should annotations
                methodsToCreate.addAll(allMethodsInOriginClass);
            } else {

                // TODO if methods is empty show message dialog, or disable button to generate
                //  iterar sobre los metodos de prueba
                for (TestMethod method : allMethodsInOriginClass) {

                    if (!method.reallyExists()) {
                        //  crear a psiDocAnnotation para cada metodo no existente
                        PsiDocAnnotationMember member = new PsiDocAnnotationMember(method);
                        array.add(member);
                    }
                }

                ClassMember[] classMembers = array.toArray(new ClassMember[array.size()]);
                MemberChooser<ClassMember> chooser = new MemberChooser<ClassMember>(classMembers, false, true, project);
                chooser.setTitle("Choose should annotations");
                chooser.setCopyJavadocVisible(false);
                chooser.show();
                final List<ClassMember> selectedElements = chooser.getSelectedElements();

                if (selectedElements == null || selectedElements.size() == 0) {
                    //  canceled or nothing selected
                    return;
                }


                for (ClassMember selectedElement : selectedElements) {
                    if (selectedElement instanceof PsiDocAnnotationMember) {
                        PsiDocAnnotationMember member = (PsiDocAnnotationMember) selectedElement;
                        methodsToCreate.add(member.getTestMethod());
                    }
                }

                // ensure parent exists
                if (!testClass.reallyExists()) {

                    //   otherwise allow to create in specified test sources root
                    VirtualFile[] sourceRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();

                    //  get a list of all test roots
                    final PsiManager manager = PsiManager.getInstance(project);
                    List<PsiDirectory> allTestRoots = new ArrayList<PsiDirectory>(2);
                    for (VirtualFile sourceRoot : sourceRoots) {
                        if (sourceRoot.isDirectory()) {
                            PsiDirectory directory = manager.findDirectory(sourceRoot);
                            if (directory != null) { // only source roots that really exists right now
                                allTestRoots.add(directory);
                            }
                        }
                    }


                    //  only display if more than one source root
                    if (allTestRoots.size() > 1) {
                        DirectoryChooser fileChooser = new DirectoryChooser(project);
                        fileChooser.setTitle(IdeBundle.message("title.choose.destination.directory"));
                        fileChooser.fillList(allTestRoots.toArray(new PsiDirectory[allTestRoots.size()]), null, project, "");
                        fileChooser.show();
                        destinationRoot = fileChooser.isOK() ? fileChooser.getSelectedDirectory() : null;
                    } else {
                        destinationRoot = allTestRoots.get(0);
                    }


                    if (destinationRoot != null) {
                        createParent = true;
                    } else {
                        //  just cancel
                        return;
                    }

                }
            }

            //  if backing test class exists, just create the methods in the same
            //  para cada uno de los seleccionados llamar a create
            //  create an appropiate command name
            final String commandName = GenerateTestCasesBundle.message("plugin.GenerateTestCases.creatingtestcase", testClass.getClassUnderTest().getName());
            final PsiDirectory finalDestinationRoot = destinationRoot;
            final boolean finalCreateParent = createParent;
            new WriteCommandAction(project, commandName) {

                @Override
                protected void run(Result result) throws Throwable {

                    LocalHistoryAction action = LocalHistoryAction.NULL;
                    //  wrap this with error management
                    try {

                        action = LocalHistory.getInstance().startAction(commandName);
                        if (finalCreateParent) {
                            testClass.create(finalDestinationRoot);
                        }
                        for (TestMethod testMethod : methodsToCreate) {
                            if (!testMethod.reallyExists()) {
                                testMethod.create();
                            }
                        }
                        testClass.getBackingElement().navigate(true);
                        reloadTestClass();
                    } finally {
                        action.finish();
                    }

                }

                void reloadTestClass(){
                    // ====  Get the virtual file of the class  =====
                    final VirtualFile virtualFile;
                    virtualFile = testClass.getBackingElement().getContainingFile().getVirtualFile();

                    // ====  refresh class file  =====
                    boolean asynchronous;
                    boolean recursive;
                    virtualFile.refresh(asynchronous = true, recursive = false);

                }
            }.execute();


        }


    }


    public void update(Editor editor, Presentation presentation, DataContext dataContext) {
        //  si no hay ninguna clase en el editor se deberia desactivar la accion
        presentation.setEnabled(getSubjectClass(editor, dataContext) != null);
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        DataContext dataContext = e.getDataContext();
        Editor editor = getEditor(dataContext);
        if (editor == null) {
            presentation.setEnabled(false);
        } else {
            update(editor, presentation, dataContext);
        }
    }

    protected Editor getEditor(final DataContext dataContext) {
        return PlatformDataKeys.EDITOR.getData(dataContext);
    }

    @Nullable
    private static PsiClass getSubjectClass(Editor editor, DataContext dataContext) {
        PsiFile file = LangDataKeys.PSI_FILE.getData(dataContext);
        if (file == null) return null;

        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);

        PsiClass parentPsiClass = BddUtil.getParentEligibleForTestingPsiClass(element);

        return parentPsiClass;

    }

}
