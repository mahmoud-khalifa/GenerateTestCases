package com.intellij.generatetestcases.model;


import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.intellij.generatetestcases.testframework.SupportedFrameworks;
import com.intellij.generatetestcases.testframework.TestFrameworkStrategy;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.generatetestcases.model.TestMethodRowStatus.NO_TEST_SUBJECT;
import static com.intellij.generatetestcases.model.TestMethodRowStatus.SECTION_Header;
import static com.intellij.generatetestcases.testframework.TestFrameworkStrategy.TEST_CLASS_SUFFIX;


/**
 * User: mahmoudkhalifa Date: 8/5/15
 */
public class TestMethodRows {

	// #################################################################################################################
	//  Instance Variables
	// #################################################################################################################

    private PsiClass containingClass;
    private Project project;
	private List<TestMethodRow>	testMethodRows;

	// #################################################################################################################
	//  Constructors
	// #################################################################################################################

	private TestMethodRows(	final PsiFile file,
							final Project project) {

		testMethodRows = getTestMethodRows(file, project);
        this.project = project;
	}

	private TestMethodRows() {

		testMethodRows = new ArrayList<TestMethodRow>();
	}

	// #################################################################################################################
	//  Factory Methods
	// #################################################################################################################

	public static TestMethodRows forFile(final PsiFile file, final Project project) {

        return new TestMethodRows(file, project);
	}

	public static TestMethodRows empty() {

		return new TestMethodRows();
	}

	// #################################################################################################################
	//  Public APIs
	// #################################################################################################################

	public int size() {

		return testMethodRows.size();
	}

	public TestMethodRow get(int index) {

		return testMethodRows.get(index);
	}

	public void itemSelectedAtIndex(final int index) {


        // ====  save containingClass  =====
        TestFrameworkStrategy frameworkStrategy = SupportedFrameworks.getStrategyForFramework(project, "JUNIT4");
        saveTestClass(containingClass, frameworkStrategy);

        // ====  perform the action  =====
        TestMethodRow testMethodRow = get(index);
        testMethodRow.performRequiredAction();
	}

	// #################################################################################################################
	//  Helper Methods
	// #################################################################################################################

	private List<TestMethodRow> getTestMethodRows(final PsiFile file, final Project project) {

        TestFrameworkStrategy frameworkStrategy = SupportedFrameworks.getStrategyForFramework(project, "JUNIT4");
        containingClass = findContainingClass(file);
        PsiMethod selectedSutMethod = null;

        if (containingClass.getName()
                .endsWith(TEST_CLASS_SUFFIX)) {

            PsiClass sutPsiClass = frameworkStrategy.findSutPsiClass(containingClass);
            if (sutPsiClass != null){
                PsiMethod selectedTestMethod = getSelectedMethodIfExists(file, project);
                containingClass = sutPsiClass;

                selectedSutMethod = getCorrespondingSutMethod(selectedTestMethod, containingClass, frameworkStrategy);
            }

        } else {
            selectedSutMethod = getSelectedMethodIfExists(file, project);
        }

        // ====  save TestClass if exists  =====
        saveTestClass(containingClass, frameworkStrategy);

        if (selectedSutMethod != null) {
            return getFilteredTestMethodRows(containingClass, frameworkStrategy, selectedSutMethod);
        } else {
            return getAllTestMethodRows(containingClass, frameworkStrategy);
        }
	}

    private void saveTestClass(final PsiClass psiClass, final TestFrameworkStrategy frameworkStrategy) {
        PsiClass testClass = frameworkStrategy.findBackingPsiClass(psiClass);

        if (testClass != null){
            VirtualFile virtualFile = testClass.getContainingFile().getVirtualFile();
            FileDocumentManager fileDocManager = FileDocumentManager.getInstance();
            Document document = fileDocManager.getDocument(virtualFile);
            fileDocManager.saveDocument(document);
        }

    }

    private PsiMethod getCorrespondingSutMethod(final PsiMethod psiTestMethod, final PsiClass sutClass, final TestFrameworkStrategy frameworkStrategy) {
        TestClass testClass = BDDCore.createTestClass(sutClass, frameworkStrategy);
        for (TestMethod testMethod : testClass.getAllMethods()) {
            PsiMethod backingElement = testMethod.getBackingElement();
            if (backingElement!=null && backingElement.equals(psiTestMethod))
                return testMethod.getSutMethod();
        }
        return null;
    }

    private List<TestMethodRow> getFilteredTestMethodRows(final PsiClass containingClass, final TestFrameworkStrategy frameworkStrategy, final PsiMethod selectedMethod) {
        final List<TestMethodRow> testMethodRows = new ArrayList<TestMethodRow>();

        String selectedMethodDescription = TestMethodRow.getMethodDescription(selectedMethod);
        TestMethodRow sectionTestMethodRow = TestMethodRow.createSectionWithMethod(selectedMethod);
        testMethodRows.add(sectionTestMethodRow);

        final List<TestMethodRow> allTestMethodRows = getAllTestMethodRows(containingClass, frameworkStrategy);
        for (TestMethodRow testMethodRow : allTestMethodRows){
            if (testMethodRow.getStatus() == SECTION_Header) {
                continue;
            } else if (testMethodRow.getStatus() == NO_TEST_SUBJECT) {
                String testMethodDescription = TestMethodRow.getTestMethodDescription(testMethodRow.getPsiTestMethod().getName());
                if (selectedMethodDescription.equals(testMethodDescription))
                    testMethodRows.add(testMethodRow);
            } else if (testMethodRow.getTestMethod().getSutMethod().equals(selectedMethod)) {
                    testMethodRows.add(testMethodRow);
            }
        }

        return testMethodRows;
    }

    private List<TestMethodRow> getAllTestMethodRows(final PsiClass containingClass, TestFrameworkStrategy frameworkStrategy) {
        final List<TestMethodRow> allTestMethodRows = new ArrayList<TestMethodRow>();

        // ====  get expected test methods by scanning the scanning for @should or @throw tags  =====
        final List<TestMethodRow> expectedTestMethodRows = getExpectedTestMethodRows(containingClass, frameworkStrategy);
        allTestMethodRows.addAll(expectedTestMethodRows);

        // ====  get no subject methods, that exists in the test class and has no matching subject  =====
        final List<TestMethodRow> noSubjectTestMethodRows = getNoTestSubjectMethodRows(containingClass, frameworkStrategy, expectedTestMethodRows);
        allTestMethodRows.addAll(noSubjectTestMethodRows);

        return testMethodRowsWithSections(allTestMethodRows);
    }


    private PsiMethod getSelectedMethodIfExists(final PsiFile file, final Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        editor.getCaretModel().getOffset();

        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        int i=0;
        while (i++<50){ // to prevent infinite loop
            if (element instanceof  PsiMethod)
                return (PsiMethod)element;
            else if (element instanceof PsiClass)
                return null;
            else if (element instanceof PsiFile)
                return null;

            element = element.getParent();
        }
        return null;
    }

    private List<TestMethodRow> testMethodRowsWithSections(List<TestMethodRow> testMethodRows) {

		final List<TestMethodRow> testMethodRowsWithSections = new ArrayList<TestMethodRow>();

		Multimap<String, TestMethodRow> testMethodRowsBySection = LinkedListMultimap.create();
        Map<String, PsiMethod> sutMethodsBySection = new HashMap<String, PsiMethod>();

        for (TestMethodRow testMethodRow : testMethodRows) {
            String section;
            if (testMethodRow.getTestMethod() != null){
                PsiMethod method = testMethodRow.getTestMethod().getSutMethod();
                section = testMethodRow.getMethodDescription(method);
                sutMethodsBySection.put(section, method);

            } else {
                PsiMethod method = testMethodRow.getPsiTestMethod();
                if (method.getName().contains("___")){
                    section = testMethodRow.getSutMethodName(method);
                } else {
                    section = "No matching method";
                }
            }
            testMethodRowsBySection.put(section, testMethodRow);
        }


		for (String section : testMethodRowsBySection.keySet()) {

            PsiMethod sutMethod = sutMethodsBySection.get(section);
            if (sutMethod != null){
                TestMethodRow sectionTestMethodRow = TestMethodRow.createSectionWithMethod(sutMethod);
                testMethodRowsWithSections.add(sectionTestMethodRow);
            } else {
                TestMethodRow sectionTestMethodRow = TestMethodRow.createSectionWithName(section);
                testMethodRowsWithSections.add(sectionTestMethodRow);
            }

            for (TestMethodRow testMethodRow : testMethodRowsBySection.get(section)){
                testMethodRowsWithSections.add(testMethodRow);
            }
		}
		return testMethodRowsWithSections;
	}

	private List<TestMethodRow> getExpectedTestMethodRows(final PsiClass containingClass, final TestFrameworkStrategy frameworkStrategy) {

		final List<TestMethodRow> expectedTestMethodRows = new ArrayList<TestMethodRow>();
		if (containingClass != null) {
			TestClass testClass = BDDCore.createTestClass(containingClass, frameworkStrategy);
			for (TestMethod testMethod : testClass.getAllMethods()) {
				TestMethodRow testMethodRow = TestMethodRow.forExpectedTestMethod(testMethod);
				expectedTestMethodRows.add(testMethodRow);
			}
		}

		return expectedTestMethodRows;

	}

	private List<TestMethodRow> getNoTestSubjectMethodRows(final PsiClass containingClass, final TestFrameworkStrategy frameworkStrategy, final List<TestMethodRow> expectedTestMethodRows) {

		final List<TestMethodRow> noTestSubjectMethodRows = new ArrayList<TestMethodRow>();

		// ====  get the candidate test class  =====
		PsiClass testClass = frameworkStrategy.findBackingPsiClass(containingClass);

		if (testClass != null) {
			PsiMethod[] testClassMethods = testClass.getMethods();
			for (PsiMethod method : testClassMethods) {
				if (isTestMethod(method)) {
					TestMethodRow testMethodRow = TestMethodRow.forNoTestSubjectMethod(method);
					if (!expectedTestMethodRows.contains(testMethodRow)) {
						noTestSubjectMethodRows.add(testMethodRow);
					}
				}
			}
		}

		return noTestSubjectMethodRows;

	}

	private boolean isTestMethod(final PsiMethod method) {

		PsiAnnotation[] annotations = method.getModifierList()
											.getAnnotations();
		for (PsiAnnotation annotation : annotations) {
			if (annotation.getText()
							.equals("@Test")) {
				return true;
			}
		}
		return false;
	}

	private PsiClass findContainingClass(final PsiFile file) {

		int i = 0;
		PsiElement element = file.findElementAt(i++)
									.getParent();
		while (element != null) {
			if (element instanceof PsiClass)
				break;
			element = file.findElementAt(i++)
							.getParent();
		}
		if (element == null)
			return null;
		else
			return (PsiClass) element;
	}
}
