package com.intellij.generatetestcases.testframework;

import com.intellij.generatetestcases.util.BddUtil;
import com.intellij.generatetestcases.util.TextFile;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocTag;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: Jaime Hablutzel
 */
public abstract class JUnitStrategyBase extends AbstractTestFrameworkStrategy {

    private static final String SUBJECT_CLASS = "SUBJECT_CLASS";

    protected JUnitStrategyBase(Project project) {
        super(project);
        this.project = project;
    }

    private Project project;

    @Override
    public String getSuggestedTestMethodName(@NotNull String originMethodName, @NotNull PsiParameter[] parameters, @NotNull String description) {
        return generateGenericTestMethodName(originMethodName, parameters, description);
    }

    /**
     * This method completes the test method structure returned by {@link AbstractTestFrameworkStrategy#createBackingTestMethod(com.intellij.psi.PsiClass, com.intellij.psi.PsiMethod, java.lang.String)} in the way JUNIT 3 and 4 expect.
     *
     * @param testClass
     * @param sutMethod
     * @param testDescription @return
     * @return
     * @should manage appropiately existence of multiple junit Assert's imports across junit versions
     * @should manage appropiately any condition of the backing test class (imports, existing methods, modifiers, etc)
     * @should add Assert.fail("Not yet implemented") statement to method body
     */
    @NotNull
    @Override
    public PsiMethod createBackingTestMethod(PsiClass testClass, PsiMethod sutMethod, String testDescription) {


        PsiMethod realTestMethod = super.createBackingTestMethod(testClass, sutMethod, testDescription);

        //  add org.junit.Assert.fail("Not yet implemented");,
        PsiJavaFile javaFile = (PsiJavaFile) testClass.getContainingFile();

        boolean assertImportExists = javaFile.getImportList().findSingleImportStatement("Assert") == null ? false : true;
        boolean makeFullQualified = false;

        //  if Assert exists and is different to both of previous, place fully qualified statement
        if (assertImportExists) {

            //  verify if junit.framework.Assert exists, if it does do not import org.junit.Assert
            //  verify import for Assert before actually importing


            //  replace it by ((PsiJavaFile) testClass.getContainingFile()).getImportList()
            PsiImportStatement bei = javaFile.getImportList().findSingleClassImportStatement("org.junit.Assert");
//            List<PsiImportStatementBase> basicExpectedImport = BddUtil.findImportsInClass(testClass, );

            PsiImportStatement oei = javaFile.getImportList().findSingleClassImportStatement("junit.framework.Assert");
//            List<PsiImportStatementBase> otherExpectedImport = BddUtil.findImportsInClass(testClass, "");

            if (bei == null && oei == null) {
                // then it is a weird class
                makeFullQualified = true;
            }


        } else {
            //  create basic import
            BddUtil.addImportToClass(sutMethod.getProject(), testClass, getFrameworkBasePackage() + ".Assert");
        }


        // org.junit.Assert
        PsiElementFactory elementFactory2 = JavaPsiFacade.getElementFactory(sutMethod.getProject());

        PsiStatement statement;
        if (makeFullQualified) {
            statement = elementFactory2.createStatementFromText(getFrameworkBasePackage() + ".Assert.fail(\"Not yet implemented\");", null);
        } else {
            statement = elementFactory2.createStatementFromText("Assert.fail(\"Not yet implemented\");", null);
        }

        realTestMethod.getBody().addAfter(statement, realTestMethod.getBody().getLastBodyElement());

        return realTestMethod;
    }

    @Override
    public int injectBackingTestMethod(@NotNull PsiClass testClass, @NotNull PsiMethod sutMethod, @NotNull PsiDocTag tag, @NotNull String description) {

        if (StringUtils.isBlank(description)) {
            throw new IllegalArgumentException("javadoc annotation should not have empty description");
        }

            // ====  Get the file path of the class  =====
        final String javaFilePath;
        javaFilePath = testClass.getContainingFile().getVirtualFile().getCanonicalPath();

        // ====  Load the class as text file  =====
        TextFile textFile;
        textFile = TextFile.fromPath(javaFilePath);

        // ====  get parameters of getSUTInstance method  =====
        final PsiParameter[] sutParameters;
        sutParameters = getSUTInstanceParameters(testClass);


        // ====  Generate the test method  =====
        final String testMethod = generateTestMethod(sutMethod.getName(), sutMethod.getReturnTypeElement(), sutMethod.getParameterList().getParameters(), tag, description, sutParameters);

        // ====  Get the index to insert the method at it  =====
        final int insertionIndex = getInsertionIndex(textFile);

        // ====  Inject the method into the text file  =====
        textFile.addLine(insertionIndex, testMethod);

        // ====  Insert import statement for thrown Exception =====
        if (BddUtil.isValidThrowsTag(tag)){
            final String importStatement;
            importStatement = generateImportForTag(tag);

            textFile.addLine(2, importStatement);
        }

        // ====  Save the text file  =====
        textFile.write(javaFilePath);


        return insertionIndex - countLines(testMethod);

    }


    private static int countLines(String str) {
        String[] lines = str.split("\r\n|\r|\n");
        return lines.length;
    }


    @Override
    public void removeTestMethod(@NotNull PsiClass testClass, @NotNull PsiMethod sutMethod) {
        sutMethod.navigate(true);
    }

    @Override
    protected void afterCreatingClass(Project project, PsiClass sutClass, PsiClass backingTestClass) {
        // ====  Get the file path of the class  =====
        final String javaFilePath;
        javaFilePath = backingTestClass.getContainingFile().getVirtualFile().getCanonicalPath();

        // ====  Load the class as text file  =====
        TextFile textFile;
        textFile = TextFile.fromPath(javaFilePath);

        // replace any occurrences of `SUBJECT_CLASS` to the sutClass class name
        for (int i = 0; i < textFile.size(); i++) {
            final String currentLine = textFile.getLine(i);

            if (currentLine.contains(SUBJECT_CLASS)) {
                final String newLine = currentLine.replace(SUBJECT_CLASS, sutClass.getName());
                textFile.removeLine(i);
                textFile.addLine(i, newLine);
            }
        }

        // ====  Write the class to desk  =====
        textFile.write(javaFilePath);
    }

    protected abstract String getFrameworkBasePackage();

    // #################################################################################################################
    //  Helper Methods
    // #################################################################################################################

    private PsiParameter[] getSUTInstanceParameters(final PsiClass testClass) {
        final PsiParameter[] sutParameters;

        final PsiMethod[] sutMethods = testClass.findMethodsByName("getSUTInstance", true);
        if (sutMethods == null || sutMethods.length == 0) {
            sutParameters = new PsiParameter[]{};
        } else {
            sutParameters = sutMethods[0].getParameterList().getParameters();
        }

        // ====  Return  =====
        return sutParameters;
    }


    // #####  Generate Test Method  #####

    /*
     * Rules for detecting the insertion index
     * - If comment that includes `teardown`, `tear-down`, `tear down`, `cleanup`, `clean-up` or `clean up` (case-insensitive) exists, insert them before the first comment (could be multiple lines)
     * - If no comment exists, insert it before the first method that has a `@After` annotation
     * - If no such method exists, insert it at the end of the class
     * @return
     */
    private int getInsertionIndex(final TextFile textFile) {

        int lastNoneCommentLineIndex = 0;

        // ====  get Tear-down section line number  =====
        for (int i = 0; i < textFile.size(); i++) {
            final String currentLine = textFile.getLine(i);

            if (!currentLine.trim().startsWith("//")){
                lastNoneCommentLineIndex = i;
            }
            if (isTearDownSection(currentLine)){
                return lastNoneCommentLineIndex;
            }
            if (currentLine.trim().startsWith("@After")){
                return i-1;
            }
        }
        return textFile.size() - 1;
    }

    /**
     *
     * @param line
     * @return true If line that includes `teardown`, `tear-down`, `tear down`, `cleanup`, `clean-up` or `clean up` (case-insensitive) exists
     */
    private boolean isTearDownSection(final String line) {
        String lowerCaseLine = line.toLowerCase();
        if (lowerCaseLine.contains("teardown"))
            return true;

        if (lowerCaseLine.contains("tear-down"))
            return true;

        if (lowerCaseLine.contains("tear down"))
            return true;

        if (lowerCaseLine.contains("cleanup"))
            return true;

        if (lowerCaseLine.contains("clean-up"))
            return true;

        if (lowerCaseLine.contains("clean up"))
            return true;

        return false;
    }

    private String generateTestMethod(final String methodName, PsiElement returnTypeElement, PsiParameter[] parameters, final PsiDocTag tag, final String description, final PsiParameter[] sutParameters) {
        final String testMethod;

        final StringBuilder stringBuilder = new StringBuilder();

        // ====  add @Test annotation  =====
        stringBuilder.append("\t@Test\n");

        // ====  add method signature  =====
        final String methodSignature;
        methodSignature = generateMethodSignature(methodName, parameters, description);
        stringBuilder.append(methodSignature);
        stringBuilder.append("\n\n");

        // ====  Method body  =====
        final String methodBody;
        if (BddUtil.isValidShouldTag(tag)) {
            methodBody = generateReturnMethodBody(methodName, returnTypeElement, parameters, sutParameters, description);
        } else if (BddUtil.isValidThrowsTag(tag)) {
            methodBody = generateThrowMethodBody(methodName, parameters, sutParameters, description);
        } else {
            throw new IllegalStateException("Invalid tag " + tag.getName());
        }
        stringBuilder.append(methodBody);
        stringBuilder.append("\n\n");

        // ====  assign testMethod to the generated method  =====
        testMethod = stringBuilder.toString();

        // ====  Return  =====
        return testMethod;
    }


    private String generateMethodSignature(final String methodName, PsiParameter[] parameters, String description) {
        StringBuilder stringBuilder = new StringBuilder();
        // ====  return type  =====
        stringBuilder.append("\tpublic void ");


        // ====  test method name  =====
        final String generatedMethodName;
        generatedMethodName = generateGenericTestMethodName(methodName, parameters, description);

        stringBuilder.append(generatedMethodName);
        stringBuilder.append("()\n" +
                "\t\t\tthrows Exception {");

        return stringBuilder.toString();
    }


    private String generateReturnMethodBody(final String methodName, PsiElement returnTypeElement, final PsiParameter[] parameters, final PsiParameter[] sutParameters, final String description) {
        StringBuilder stringBuilder = new StringBuilder();

        // ====  Setup  =====
        // ====  Test subject  =====
        stringBuilder.append("\t\t// ====  SETUP: TEST SUBJECT  ====\n");
        final String sutStatement = generateSutStatement(sutParameters, parameters.length, description);
        stringBuilder.append(sutStatement);

        // ====  Expected result  =====
        stringBuilder.append("\t\t// ====  EXPECTED RESULT  ====\n");
        final String expectResultEqualsStatement = generateExpectResultEqualsStatement(description, returnTypeElement);
        stringBuilder.append(expectResultEqualsStatement);


        // ====  Execute  =====
        stringBuilder.append("\t\t// ====  EXECUTE  ====\n");
        stringBuilder.append("\t\t");
        if (!returnTypeElement.getText().equals("void"))
            stringBuilder.append("result = ");
        final String executeStatement = generateExecuteStatement(methodName, parameters, description);
        stringBuilder.append(executeStatement);

        // ====  Verify  =====
        stringBuilder.append("\t\t// ====  VERIFY  ====\n");
        stringBuilder.append("\t\tasserter.verify(result);\n\n");

        // ====  end of Method  =====
        stringBuilder.append("\t}");

        // ====  Return  =====
        return stringBuilder.toString();
    }


    private String generateThrowMethodBody(final String methodName, final PsiParameter[] parameters, final PsiParameter[] sutParameters, final String description) {

        StringBuilder stringBuilder = new StringBuilder();

        // ====  Setup  =====
        // ====  Test subject  =====
        stringBuilder.append("\t\t// ====  SETUP: TEST SUBJECT  ====\n");
        final String sutStatement = generateSutStatement(sutParameters, parameters.length, description);
        stringBuilder.append(sutStatement);


        // ====  Expected result  =====
        final String exception = description.split(" ")[1];
        stringBuilder.append("\t\t// ====  EXPECTED RESULT  ====\n");
        final String expectExceptionStatement = generateExpectPreapprovedException(exception, description);
        stringBuilder.append(expectExceptionStatement);


        // ====  Execute  =====
        stringBuilder.append("\t\t// ====  EXECUTE  ====\n");
        stringBuilder.append("\t\t");
        final String executeStatement = generateExecuteStatement(methodName, parameters, description);
        stringBuilder.append(executeStatement);


        // ====  end of Method  =====
        stringBuilder.append("\t}");

        // ====  Return  =====
        return stringBuilder.toString();
    }

    private String generateExpectPreapprovedException(final String exception, final String description) {

        StringBuilder stringBuilder = new StringBuilder();

        // ====  calling expectPreapprovedException  =====
        stringBuilder.append("\t\t// TODO: Add exception message variables, if any\n");
        stringBuilder.append("\t\tasserter.expectPreapprovedException(");
        stringBuilder.append(exception);
        stringBuilder.append(".class, ApprovalMode.RECORD");

        if (isInputExists(description)) {

            final String input = extractInput(description, null);
            stringBuilder.append(", ").append(input);

        }
        // ====  end of statement  =====
        stringBuilder.append(");\n\n");

        // ====  Return  =====
        return stringBuilder.toString();
    }

    private String generateExecuteStatement(final String methodName, final PsiParameter[] parameters, final String description) {
        StringBuilder stringBuilder = new StringBuilder();

        // ====  calling method  =====
        stringBuilder.append("SUT.");
        stringBuilder.append(methodName);
        stringBuilder.append("(");

        if (isInputExists(description)) {

            final String input = extractInput(description, null);
            stringBuilder.append(input);

        } else {
            // ====  parameters  =====
            for (int i = 0; i < parameters.length; i++) {
                PsiParameter parameter = parameters[i];
                if (i == 0)
                    stringBuilder.append(parameter.getName());
                else
                    stringBuilder.append(", ").append(parameter.getName());
            }
        }


        // ====  end of statement  =====
        stringBuilder.append(");\n\n");

        // ====  Return  =====
        return stringBuilder.toString();

    }


    private String generateSutStatement(final PsiParameter[] sutParameters, final int methodParametersCount, String description) {

        final String sutStatement;

        // ====  start of statement  =====
        StringBuilder sutStatementBuilder = new StringBuilder("\t\tSUT = getSUTInstance(");

        if (isInputExists(description) && sutParameters.length == 1 && methodParametersCount == 0) {

            final String input = extractInput(description, sutParameters[0].getTypeElement());
            sutStatementBuilder.append(input);
        } else {
            for (int i = 0; i < sutParameters.length; i++) {
                if (i == 0)
                    sutStatementBuilder.append("\"input1\"");
                else
                    sutStatementBuilder.append(", ").append("\"input\"").append(i + 1);
            }
        }

        // ====  end of statement  =====
        sutStatementBuilder.append(");\n\n");

        // ====  assign sutStatement  =====
        sutStatement = sutStatementBuilder.toString();

        // ====  Return  =====
        return sutStatement;
    }

    private boolean isInputExists(final String description) {
        if (description.contains("if input is"))
            return true;

        return false;
    }

    private String extractInput(final String description, @Nullable final PsiElement parameterElement) {
        String input = description.split("if input is")[1].trim();
        if (input.equalsIgnoreCase("null"))
            return input;
        else if (parameterElement == null || parameterElement.getText().equalsIgnoreCase("string"))
            return "\"" + input + "\"";
        else
            return input;

    }

    private String generateExpectResultEqualsStatement(final String description, final PsiElement returnTypeElement) {
        // ====  start of statement  =====
        StringBuilder stringBuilder = new StringBuilder();

        // ====  parameters  =====
        if (isInputExists(description) && isOutputExists(description)) {
            stringBuilder.append("\t\tasserter.expectResultEquals(");
            final String output = extractOutput(description, returnTypeElement);
            stringBuilder.append(output);

        } else {
            stringBuilder.append("\t\tasserter.expectPreapprovedResult(ApprovalMode.RECORD, VALUE");
        }

        // ====  end of statement  =====
        stringBuilder.append(");\n\n");

        // ====  Return  =====
        return stringBuilder.toString();
    }

    private boolean isOutputExists(final String description) {
        if (description.contains("should return"))
            return true;

        return false;
    }

    private String extractOutput(final String description, PsiElement returnTypeElement) {

        String descriptionWithoutInput = description.split("if input is")[0].trim();
        String output = descriptionWithoutInput.split("should return")[1].trim();

        if (output.equalsIgnoreCase("null"))
            return output;
        else if (returnTypeElement.getText().equalsIgnoreCase("string"))
            return "\"" + output + "\"";
        else
            return output;
    }

    private String generateImportForTag(final PsiDocTag tag) {
        StringBuilder stringBuilder = new StringBuilder();

        PsiElement exceptionElement = tag.getDataElements()[0];
        String exceptionType = exceptionElement.getText();
        if (exceptionType.contains(".") && !exceptionType.startsWith("java.lang")){
            stringBuilder.append("import ");
            stringBuilder.append(exceptionType);
            stringBuilder.append(";");
        }

        return stringBuilder.toString();
    }


}
