package org.somda.sdc.glue.common.uri;

import jregex.Matcher;
import jregex.Pattern;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.CodedValue;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.helper.UrlUtf8;

/**
 * Utility class to map from complex device component coded value to URI and back to coded value.
 * <p>
 * This class implements the grammar defined in IEEE 11073-20701 section 9.2.
 */
public class ComplexDeviceComponentMapper {
    private static final String SCHEME = "sdc.cdc.type";

    private static final Pattern PATTERN = new Pattern(
            "^(" +
                    "(?i:sdc.cdc.type):/" +
                    // Warning: this intentionally differs from
                    // the standard to distinguish between authority and path
                    // Do not change the first of the following groups to "SEGMENT_REGEX" instead
                    // of "SEGMENT_NZ_REGEX|"
                    "(({codingSystem}" + GlueConstants.SEGMENT_NZ_REGEX + ")|)/" +
                    "({codingSystemVersion}(?(codingSystem)" +
                    GlueConstants.SEGMENT_REGEX + "|" +
                    GlueConstants.AUTHORITY + "))/" +
                    "({code}" + GlueConstants.SEGMENT_NZ_REGEX + ")" +
                    ")$"
    );

    /**
     * Maps an abstract complex component descriptor to URI representation.
     *
     * @param descriptor the device component where to access the type.
     * @return the mapped URI.
     * @throws UriMapperGenerationArgumentException in case no valid URI could be generated from the input.
     */
    public static String fromComplexDeviceComponent(AbstractComplexDeviceComponentDescriptor descriptor)
            throws UriMapperGenerationArgumentException {
        final CodedValue codedValue = descriptor.getType();
        if (codedValue == null) {
            throw new UriMapperGenerationArgumentException("No CodedValue was provided");
        }

        return fromCodedValue(codedValue);
    }

    /**
     * Given a coded value that belongs to an abstract complex component descriptor,
     * this function creates the URI representation.
     *
     * @param codedValue a complex device component's type.
     * @return the mapped URI.
     * @throws UriMapperGenerationArgumentException in case no valid URI could be generated from the input.
     */
    public static String fromCodedValue(CodedValue codedValue) throws UriMapperGenerationArgumentException {
        String codingSystem = codedValue.getCodingSystem();
        if ("urn:oid:1.2.840.10004.1.1.1.0.0.1".equals(codingSystem)) {
            codingSystem = null;
        }

        String uri = SCHEME + ":" +
                "/" + UrlUtf8.encodePChars(codingSystem) +
                "/" + UrlUtf8.encodePChars(codedValue.getCodingSystemVersion()) +
                "/" + UrlUtf8.encodePChars(codedValue.getCode());

        try {
            fromUri(uri);
        } catch (UriMapperParsingException e) {
            throw new UriMapperGenerationArgumentException(
                    "No valid URI could be generated from the given CodedValue with " +
                            "Code:" + codedValue.getCode() + ", " +
                            "CodingSystem:" + codedValue.getCodingSystem() + ", " +
                            "CodingSystemVersion:" + codedValue.getCodingSystemVersion());
        }

        return uri;
    }

    /**
     * Maps a complex device component type URI string to a coded value.
     *
     * @param complexDeviceComponentTypeUri the URI to parse.
     * @return a coded value if pattern of URI matches.
     * @throws UriMapperParsingException in case no valid URI was given.
     */
    public static CodedValue fromUri(String complexDeviceComponentTypeUri) throws UriMapperParsingException {

        Matcher matcher = PATTERN.matcher(complexDeviceComponentTypeUri);
        if (matcher.matches()) {

            final String codingSystem = UrlUtf8.decodePChars(matcher.group("codingSystem"));
            final String codingSystemVersion = UrlUtf8.decodePChars(matcher.group("codingSystemVersion"));
            final String code = UrlUtf8.decodePChars(matcher.group("code"));

            CodedValue codedValue = new CodedValue();
            codedValue.setCodingSystem(codingSystem.isEmpty() ? null : codingSystem);
            codedValue.setCodingSystemVersion(codingSystemVersion.isEmpty() ? null : codingSystemVersion);
            codedValue.setCode(code.isEmpty() ? null : code);
            return codedValue;
        }

        throw new UriMapperParsingException(
                "Invalid URI for the mapper " + ComplexDeviceComponentMapper.class.toString());
    }
}
