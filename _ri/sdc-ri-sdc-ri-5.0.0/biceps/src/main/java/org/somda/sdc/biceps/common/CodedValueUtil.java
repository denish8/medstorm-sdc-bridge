package org.somda.sdc.biceps.common;

import org.somda.sdc.biceps.model.participant.CodedValue;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Utilities for {@link CodedValue}s.
 */
public class CodedValueUtil {

    public static final String DEFAULT_CODING_SYSTEM = "urn:oid:1.2.840.10004.1.1.1.0.0.1";

    /**
     * Checks for equality of two coded values according to BICEPS.
     *
     * @param value1 a coded value
     * @param value2 another coded value
     * @return true if equal according to BICEPS, false otherwise
     */
    public static boolean isEqual(@Nullable CodedValue value1, @Nullable CodedValue value2) {
        // null is never equal
        if (value1 == null || value2 == null) {
            return false;
        }

        // check if codes are equal
        if (codesAreEqual(value1, value2)) {
            return true;
        }

        // check if value1 is equal to any translation of value2
        for (CodedValue.Translation translation : value1.getTranslation()) {
            if (codesAreEqual(translation, value2)) {
                return true;
            }
        }

        // check if value2 is equal to any translation of value1
        for (CodedValue.Translation translation : value2.getTranslation()) {
            if (codesAreEqual(translation, value1)) {
                return true;
            }
        }

        return false;
    }

    private static boolean codesAreEqual(CodedValue.Translation value1, CodedValue value2) {
        return value1.getCode().equals(value2.getCode()) &&
                getCodingSystem(value1).equals(getCodingSystem(value2)) &&
                codingSystemVersionsEqual(value1, value2);
    }

    private static boolean codesAreEqual(CodedValue value1, CodedValue value2) {
        return value1.getCode().equals(value2.getCode()) &&
                getCodingSystem(value1).equals(getCodingSystem(value2)) &&
                codingSystemVersionsEqual(value1, value2);
    }

    private static boolean codingSystemVersionsEqual(CodedValue value1, CodedValue value2) {
        if (value1.getCodingSystemVersion() == null) {
            return value2.getCodingSystemVersion() == null;
        }
        return value1.getCodingSystemVersion().equals(value2.getCodingSystemVersion());
    }

    private static boolean codingSystemVersionsEqual(CodedValue.Translation value1, CodedValue value2) {
        if (value1.getCodingSystemVersion() == null) {
            return value2.getCodingSystemVersion() == null;
        }
        return value1.getCodingSystemVersion().equals(value2.getCodingSystemVersion());
    }

    private static String getCodingSystem(CodedValue.Translation value) {
        return Optional.ofNullable(value.getCodingSystem()).orElse(DEFAULT_CODING_SYSTEM);
    }

    private static String getCodingSystem(CodedValue value) {
        return Optional.ofNullable(value.getCodingSystem()).orElse(DEFAULT_CODING_SYSTEM);
    }
}
