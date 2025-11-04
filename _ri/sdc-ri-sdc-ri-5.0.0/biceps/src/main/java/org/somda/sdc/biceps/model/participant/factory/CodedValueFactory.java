package org.somda.sdc.biceps.model.participant.factory;

import org.somda.sdc.biceps.model.participant.CodedValue;

import javax.annotation.Nullable;

/**
 * Convenience factory to create coded values.
 */
public class CodedValueFactory {
    /**
     * Creates a coded value from the IEEE nomenclature.
     * <p>
     * The given code is not verified against compliance with IEEE nomenclature.
     *
     * @param code the code to use.
     * @return a new instance.
     */
    public static CodedValue createIeeeCodedValue(String code) {
        return createCodedValue(null, null, code, null);
    }

    /**
     * Creates a coded value from the IEEE nomenclature.
     * <p>
     * The given code is not verified against compliance with IEEE nomenclature.
     *
     * @param code             the code to use.
     * @param symbolicCodeName an optional symbolic code name (null if unknown).
     * @return a new instance.
     */
    public static CodedValue createIeeeCodedValue(String code, @Nullable String symbolicCodeName) {
        return createCodedValue(null, null, code, symbolicCodeName);
    }

    /**
     * Creates a coded value.
     *
     * @param codingSystem        the coding system to use or null if based on IEEE nomenclature.
     * @param codingSystemVersion an optional coding system version.
     * @param code                the code.
     * @return a new instance.
     */
    public static CodedValue createCodedValue(@Nullable String codingSystem,
                                       @Nullable String codingSystemVersion,
                                       @Nullable String code) {
        return createCodedValue(codingSystem, codingSystemVersion, code, null);
    }

    /**
     * Creates a coded value.
     *
     * @param codingSystem        the coding system to use or null if based on IEEE nomenclature.
     * @param codingSystemVersion an optional coding system version (null if unknown).
     * @param code                the code.
     * @param symbolicCodeName    an optional symbolic code name (null if unknown).
     * @return a new instance.
     */
    public static CodedValue createCodedValue(@Nullable String codingSystem,
                                              @Nullable String codingSystemVersion,
                                              @Nullable String code,
                                              @Nullable String symbolicCodeName) {
        final CodedValue codedValue = new CodedValue();
        codedValue.setCodingSystem(codingSystem);
        codedValue.setCodingSystemVersion(codingSystemVersion);
        codedValue.setCode(code);
        codedValue.setSymbolicCodeName(symbolicCodeName);
        return codedValue;
    }
}
