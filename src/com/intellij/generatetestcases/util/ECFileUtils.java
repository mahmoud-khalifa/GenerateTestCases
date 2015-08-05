package com.intellij.generatetestcases.util;


import java.io.File;

/**
 * This class provides utility methods for file.
 *
 * @author Rolf
 * @since 29-11-12
 */
public class ECFileUtils {


    /**
     * Deletes file from specific location
     *
     * @param filePath
     */
    public static void deleteFile(String filePath) {

        deleteFile(new File(filePath));
    }

    /**
     * Deletes file given the file reference
     *
     * @param file
     */
    public static void deleteFile(File file) {

        file.delete();

    }

    /**
     * Tests whether a file exists.
     *
     * @param pathToFile a string of file pathname
     * @return true if and only if the file exists; false otherwise
     */
    public static boolean fileExists(String pathToFile) {

        final File file;
        file = new File(pathToFile);

        boolean exists;
        exists = file.exists();

        if (exists && !file.isFile())
            throw new IllegalStateException("The path '" + pathToFile + "' exists but is not a file.");


        return exists;
    }

}
