package com.intellij.generatetestcases.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.collect.Lists.newArrayList;

import static com.intellij.generatetestcases.util.ECTextFileUtils.loadTextFileAsList;

/**
 * User: Rolf
 * Date: 28-08-13
 */
public final class TextFile {

    // TODO: Move to final package/module
    // TODO: Make immutable, use builder

    public static String LINE_BREAK = getPlatformIndependentLineSeparator();

    private static String getPlatformIndependentLineSeparator() {

        if (LINE_BREAK == null)
            LINE_BREAK = (String) java.security.AccessController.doPrivileged(
                    new sun.security.action.GetPropertyAction("line.separator"));

        return LINE_BREAK;
    }

    // #################################################################################################################
    //  Instance variables
    // #################################################################################################################

    private final List<String> contentLines;

//    public final Encoding encoding; // TODO: Move TextFile to higher level where it has access to Encoding


    // #################################################################################################################
    //  Constructors
    // #################################################################################################################

    TextFile(final Collection<String> contentLines) {

        this.contentLines = newArrayList(contentLines);
    }

    // #################################################################################################################
    //  Factory methods
    // #################################################################################################################

    public static final TextFile create() {

        return new TextFile(new ArrayList<String>());
    }


    public static final TextFile create(Collection<String> lines) {

        return new TextFile(lines);
    }

    public static final TextFile fromPath(final String filePath) {

        // ====  Get file handle  =====
        final File fileHandle;
        fileHandle = new File(filePath);

        // ====  Return  =====
        return fromFileHandle(fileHandle);
    }

    public static final TextFile fromFileHandle(final File fileHandle) {

        final TextFile textFile;

        // ====  Load the contentLines  =====
        final Collection<String> contents;
        contents = ECTextFileUtils.loadTextFileAsList(fileHandle);

        // ====  Create instance of TextFile  =====
        textFile = new TextFile(contents);

        // ====  Return  =====
        return textFile;
    }

    public static final TextFile withContentLines(final Collection<String> contentLines) {

        return new TextFile(contentLines);
    }


    // #################################################################################################################
    //  Public API
    // #################################################################################################################


    public Collection<String> getLines() {

        return contentLines;
    }

    public Collection<String> getLines(final boolean ignoreBlankLines) {

        if (!ignoreBlankLines)

            // ====  Return  =====
            return contentLines;
        else {

            final Collection<String> contentWithoutBlankLines = newArrayList();

            // ====  Iterate contentLines and add all non-blank lines  =====
            for (String contentLine : contentLines) {

                if (!contentLine.trim().isEmpty())
                    contentWithoutBlankLines.add(contentLine);
            }

            // ====  Return  =====
            return contentWithoutBlankLines;
        }
    }

    public String getLine(final int index) {

        return contentLines.get(index);
    }

    public String getFirstLine() {

        return getLine(0);
    }

    public String getLastLine() {

        final int lastLineIndex;
        lastLineIndex = contentLines.size() - 1;

        checkElementIndex(lastLineIndex, size(), "Could not get last line because the text file is empty.");

        return getLine(lastLineIndex);
    }


    public void addLine(final String contentLine) {

        contentLines.add(contentLine);
    }

    public void addLine(final int index, final String contentLine) {

        contentLines.add(index, contentLine);
    }

    public int indexOf(final String contentLine) {

        int indexOf = -1;

        for (int i = 0; i < contentLines.size(); i++) {

            if (contentLines.get(i).equals(contentLine)) {
                indexOf = i;
                break;
            }
        }

        // ====  Return  =====
        return indexOf;
    }

    public int size() {
        return contentLines.size();
    }

    public boolean isEmpty() {

        return contentLines.isEmpty();
    }

    public Iterator<String> getLineIterator() {

        return getLines().iterator();
    }

    public Iterator<String> getLineIterator(final boolean ignoreBlankLines) {

        return getLines(ignoreBlankLines).iterator();
    }


    // #####  Methods for writing the file  #####

    public void write(final String filePath) {

        // ====  Delete existing, if needed  =====
        if (ECFileUtils.fileExists(filePath))
            ECFileUtils.deleteFile(filePath);

        // ====  Write the file  =====
        ECTextFileUtils.writeTextFile(filePath, contentLines);
    }


    public void removeLine(final int index) {

        contentLines.remove(index);
    }

    @Override
    public String toString() {

        final StringBuilder stringBuilder = new StringBuilder();

        // ====  Add each line to the String  =====
        for (String line : getLines()) {
            stringBuilder.append(line).append(LINE_BREAK);
        }

        // ====  Return  =====
        return stringBuilder.toString();
    }
}
