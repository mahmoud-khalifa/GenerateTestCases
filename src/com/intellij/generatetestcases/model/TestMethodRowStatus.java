package com.intellij.generatetestcases.model;

/**
 * User: mahmoudkhalifa
 * Date: 8/6/15
 */
public enum TestMethodRowStatus {

    // #################################################################################################################
    //  Enum members
    // #################################################################################################################

    /**
     * Exists + match onf of the subjects
     */
    MATCH,
    /**
     * Not exist
     */
    MISSING,
    /**
     * test method exists but don't belong to any subject for any method
     */
    NO_TEST_SUBJECT,

    SECTION_Header
}
