package com.intellij.generatetestcases.model;

import com.intellij.generatetestcases.util.BddUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;

import static com.intellij.generatetestcases.model.TestMethodRowStatus.*;

/**
 * User: mahmoudkhalifa
 * Date: 8/5/15
 */
public class TestMethodRow {

    // #################################################################################################################
    //  Instance Variables
    // #################################################################################################################
    private final String key;
    private final TestMethod testMethod;
    private final TestMethodRowStatus status;
    private final PsiMethod psiTestMethod;

    private final String sectionName;
    private final PsiMethod psiSutMethod;


    // #################################################################################################################
    //  Constructors
    // #################################################################################################################

    private TestMethodRow(final TestMethod testMethod){
        this.testMethod = testMethod;
        this.key = generateKey(testMethod);
        if (testMethod.reallyExists())
            this.status = MATCH;
        else
            this.status = MISSING;


        this.psiTestMethod = null;
        this.sectionName = null;
        this.psiSutMethod = null;
    }


    private TestMethodRow(final PsiMethod psiMethod) {
        this.psiTestMethod = psiMethod;
        this.key = generateKey(psiMethod);
        this.status = NO_TEST_SUBJECT;

        this.testMethod = null;
        this.sectionName = null;
        this.psiSutMethod = null;
    }


    public TestMethodRow(final String sectionName) {
        this.status = SECTION_Header;
        this.sectionName = sectionName;
        this.key = sectionName;

        this.testMethod = null;
        this.psiTestMethod = null;
        this.psiSutMethod = null;

    }
    public TestMethodRow(final PsiMethod psiMethod, final String sectionName) {
        this.status = SECTION_Header;
        this.key = sectionName;
        this.sectionName = sectionName;
        this.psiSutMethod = psiMethod;


        this.testMethod = null;
        this.psiTestMethod = null;

    }
    // #################################################################################################################
    //  Factory Methods
    // #################################################################################################################

    public static TestMethodRow forExpectedTestMethod(final TestMethod testMethod){
        return new TestMethodRow(testMethod);
    }

    public static TestMethodRow forNoTestSubjectMethod(final PsiMethod psiMethod) {
        return new TestMethodRow(psiMethod);
    }

    public static TestMethodRow createSectionWithName(final String testMethodName) {
        return new TestMethodRow(testMethodName);
    }

    public static TestMethodRow createSectionWithMethod(final PsiMethod psiMethod) {
        String selectedMethodDescription = TestMethodRow.getMethodDescription(psiMethod);
        return new TestMethodRow(psiMethod, selectedMethodDescription);

    }

    // #################################################################################################################
    //  Public APIs
    // #################################################################################################################

    public TestMethodRowStatus getStatus() {
        return status;
    }

    public TestMethod getTestMethod() {
        return testMethod;
    }

    public PsiMethod getPsiTestMethod() {
        return psiTestMethod;
    }

    public String getKey() {
        return key;
    }

    public String getDescription(){

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" ");
        if (status == SECTION_Header){
            stringBuilder.append(sectionName);

        } else if (status == NO_TEST_SUBJECT){
            stringBuilder.append("   ");
            String methodName = psiTestMethod.getName();
            if (methodName.contains("___")){
                stringBuilder.append(getMethodDescription(methodName));
            } else {
                stringBuilder.append(methodName);
            }
        } else if(this.testMethod != null){
            stringBuilder.append("   ");
            stringBuilder.append(testMethod.getDescription());
        } else {
            stringBuilder.append("   ");
            stringBuilder.append(key);
        }

        // ====  Return  =====
        return stringBuilder.toString();
    }


    public void performRequiredAction() {

        if (status == SECTION_Header){
            if (this.psiSutMethod != null)
                this.psiSutMethod.navigate(true);
        } else if (status == NO_TEST_SUBJECT) {
            psiTestMethod.navigate(true);

        } else if (this.testMethod != null) {
            if (testMethod.reallyExists()) {
                // ====  Remove method if exists  =====
                testMethod.remove();
                // ====  navigate to method to manually delete it  =====
                testMethod.getBackingElement().navigate(true);

            } else {
                // ====  Add method if not exist  =====
                int insertionLine = testMethod.create();

                // ====  reload the test class  =====
                PsiClass testClass = testMethod.getParent().getBackingElement();
                BddUtil.reloadClass(testClass);
                BddUtil.navigateToClass(testClass, insertionLine);
            }
        }
    }

    public String getSutMethodName(final PsiMethod testMethod) {
        StringBuilder stringBuilder = new StringBuilder();

        String testMethodName = testMethod.getName();
        if (testMethodName.contains("___")){
            String signature = getTestMethodDescription(testMethodName);
            stringBuilder.append(signature);
        } else {
            stringBuilder.append(testMethodName);
        }

        // ====  Return  =====
        return stringBuilder.toString();
    }


    public static String getMethodDescription(final PsiMethod psiMethod) {
        StringBuilder stringBuilder = new StringBuilder();

        // ====  Method Name  =====
        stringBuilder.append(psiMethod.getName());

        // ====  parameters  =====
        stringBuilder.append("(");

        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        for (int i = 0; i < parameters.length; i++) {
            PsiParameter parameter = parameters[i];
            if (i == 0)
                stringBuilder.append(parameter.getName());
            else
                stringBuilder.append(", ").append(parameter.getName());
        }

        stringBuilder.append(")");


        // ====  Return  =====
        return stringBuilder.toString();
    }

    public static String getTestMethodDescription(final String testMethodName) {
        StringBuilder stringBuilder = new StringBuilder();

        String signature = testMethodName.split("___")[0];
        String[] signatureComponents = signature.split("_");
        for (int i = 0; i < signatureComponents.length; i++) {
            String signatureComponent = signatureComponents[i];
            if (i == 0)
                stringBuilder.append(signatureComponent).append("(");
            else if (i == 1)
                stringBuilder.append(signatureComponent);
            else
                stringBuilder.append(", ").append(signatureComponent);
        }
        stringBuilder.append(")");

        // ====  Return  =====
        return stringBuilder.toString();
    }


    // #################################################################################################################
    //  Helper Methods
    // #################################################################################################################

    private String generateKey(final TestMethod testMethod) {
        return testMethod.getSuggestedMethodName();
    }

    private String generateKey(final PsiMethod psiMethod) {
        return psiMethod.getName();
    }


    private String getMethodDescription(final String testMethodName) {
        StringBuilder stringBuilder = new StringBuilder();

        // ====  Method Name  =====
        String description = testMethodName.split("___")[1];
        String[] descriptionComponents = description.split("_");
        for (int i=0; i<descriptionComponents.length; i++){
            String descriptionComponent = descriptionComponents[i];
            if (i==0)
                stringBuilder.append(descriptionComponent.substring(0,1).toLowerCase()).append(descriptionComponent.substring(1));
            else

                stringBuilder.append(" ").append(descriptionComponent);
        }

        // ====  Return  =====
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestMethodRow)) return false;

        TestMethodRow that = (TestMethodRow) o;

        if (!key.equals(that.key)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
