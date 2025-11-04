package org.somda.sdc.glue.common.uri;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to map between Participant Key Purpose URIs and OIDs.
 * <p>
 * This class implements the grammar defined in IEEE 11073-20701 section 9.3.
 */
public class ParticipantKeyPurposeMapper {
    private static final String SCHEME = "sdc.mds.pkp";
    private static final String NO_LEADING_ZERO_EXCEPT_FOR_SINGLE_DIGIT = "(0|([1-9][0-9]*))";
    private static final String OID_SEGMENT =
            "(" +
                    "(" +
                    "(" + NO_LEADING_ZERO_EXCEPT_FOR_SINGLE_DIGIT + ".)*" +
                    "(" + NO_LEADING_ZERO_EXCEPT_FOR_SINGLE_DIGIT + "." +
                    NO_LEADING_ZERO_EXCEPT_FOR_SINGLE_DIGIT + ")" +
                    "(." + NO_LEADING_ZERO_EXCEPT_FOR_SINGLE_DIGIT + ")*" +
                    ")|" +
                    NO_LEADING_ZERO_EXCEPT_FOR_SINGLE_DIGIT + ")";
    private static final Pattern PATTERN = Pattern.compile("^((?i:" + SCHEME + "):(?<oid>" + OID_SEGMENT + "))$");

    /**
     * Creates a Participant Key Purpose URI out of an OID.
     *
     * @param oid the OID to convert.
     * @return the converted URI.
     * @throws UriMapperGenerationArgumentException in case no valid URI could be generated from the input.
     */
    public static String fromOid(Oid oid) throws UriMapperGenerationArgumentException {
        final String uri = SCHEME + ":" + oid;

        try {
            fromUri(uri);
        } catch (UriMapperParsingException e) {
            throw new UriMapperGenerationArgumentException("No valid URI could be generated from the given OID: " +
                                                                   oid);
        }

        return uri;
    }

    /**
     * Creates an OID given a Participant Key Purpose encoded URI.
     *
     * @param uri the URI to convert.
     * @return the converted OID.
     * @throws UriMapperParsingException in case no valid URI was given.
     */
    public static Oid fromUri(String uri) throws UriMapperParsingException {

        Matcher matcher = PATTERN.matcher(uri);

        if (matcher.matches()) {
            try {
                return new Oid(matcher.group("oid"));
            } catch (GSSException e) {
                throw new UriMapperParsingException(
                        "Invalid URI for the mapper " + ParticipantKeyPurposeMapper.class +
                                " due to GSSException " + e);
            }
        }

        throw new UriMapperParsingException(
                "Invalid URI for the mapper " + ParticipantKeyPurposeMapper.class);
    }
}
