package com.intellij.generatetestcases.util;


import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * User: Rolf
 * Date: 22-08-13
 */
public final class ECTextFileUtils {

    // #################################################################################################################
    //  API for creating text files
    // #################################################################################################################

    /**
     * Writes the toString() value of each item in a collection to the specified File line by line. The default VM encoding and the default line ending will be used.
     *
     * @param filePath
     * @param contentLines
     * @return
     * @throws RuntimeException In case the directory doesn't exist.
     */
    public static final File writeTextFile(final String filePath, final List<?> contentLines) {

        // ====  Get a file handle  =====
        final File file;
        file = new File(filePath);


        // ====  Write the file  =====
        try {
            PrintWriter out = new PrintWriter(file);
            for (int i=0; i<contentLines.size(); i++){
                String line  = (String) contentLines.get(i);
                out.println(line);
            }
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("An IO exception occurred while trying to write to file [" + filePath + "]. Check if the directory that should contain the (new) file exists.", e);
        }
        // ====  Return  =====
        return file;
    }

    public static final File writeTextFile(final String filePath, final Object... contentLines) {

        // ====  Convert the content lines to a collection  =====
        final Collection<?> contentLinesCollection;
        contentLinesCollection = Arrays.asList(contentLines);

        // ====  Create the text file  =====
        return writeTextFile(filePath, contentLinesCollection);
    }

    // #################################################################################################################
    //  API for reading text files
    // #################################################################################################################


    public static List<String> loadTextFileAsList(File file) {

        List<String> textFileLines = newArrayList();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String readLine = null;

            while ((readLine = bufferedReader.readLine()) != null) {
                textFileLines.add(readLine);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("The text file '" + file.getAbsolutePath() + "' could not be found.", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return textFileLines;
    }

    public static List<String> loadTextFileAsList(String fileName) {

        return loadTextFileAsList(new File(fileName));

    }


}
