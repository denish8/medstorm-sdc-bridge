package org.somda.sdc.glue;

import org.ietf.jgss.Oid;
import org.somda.sdc.glue.common.uri.ParticipantKeyPurposeMapper;

import java.util.Collections;

/**
 * Any constants relevant to SDC Glue.
 */
public class GlueConstants {
    /**
     * Resource path to BICEPS XML Schemas.
     */
    public static final String SCHEMA_PATH = "ExtensionPoint.xsd:BICEPS_ParticipantModel.xsd:BICEPS_MessageModel.xsd";

    /**
     * Key purpose dot-notated OID that expresses compliance
     * with all mandatory requirements for an SDC service provider.
     */
    public static final String OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER = "1.2.840.10004.20701.1.1";

    /**
     * Key purpose dot-notated OID that expresses compliance
     * with all mandatory requirements for an SDC service consumer.
     */
    public static final String OID_KEY_PURPOSE_SDC_SERVICE_CONSUMER = "1.2.840.10004.20701.1.2";

    static {
        try {
            // This assignment should never throw unless OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER is modified
            // to something malformed. A separate unit test covers this (GlueConstantsTest::staticInitialization())
            SCOPE_SDC_PROVIDER = ParticipantKeyPurposeMapper.fromOid(new Oid(OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Definition of the SDC participant discovery scope.
     * <p>
     * This scope, encoded in accordance with SDC Glue clause 9.3,
     * claims conformance with IEEE 11073-20701, published 2018.
     */
    public static final String SCOPE_SDC_PROVIDER;

    /**
     * SegmentNz and Segment regex definitions.
     */
    public static final String SCHEME_SEGMENT = "(?i:[a-z][a-z0-9+-.]*)";
    private static final String ALLOWED_CHARS = "[a-zA-Z0-9\\-._~!$&'()*+,;=:@]";
    private static final String P_CHAR = "(?:(?:%[a-fA-F0-9]{2})+|(?:" + ALLOWED_CHARS + ")+)";
    public static final String SEGMENT_NZ_REGEX = P_CHAR + "+";
    public static final String SEGMENT_REGEX = P_CHAR + "*";
    private static final String DEC_OCTET = "((1[0-9][0-9])|(2[0-4][0-9])|(25[0-5])|([0-9])|([1-9][0-9]))";
    public static final String IPV4_ADDRESS = String.join("\\.", Collections.nCopies(4, DEC_OCTET));
    private static final String REG_NAME = "(?:(?:%[a-fA-F0-9]{2})+|(?:" + "[a-zA-Z0-9\\-._~!$&'()*+,;=]" + ")+)*";
    private static final String USER_INFO = "(?:(?:%[a-fA-F0-9]{2})+|(?:" + "[a-zA-Z0-9\\-._~!$&'()*+,;=:]" + ")+)*";
    private static final String HEXDIG = "[a-fA-F0-9]";
    private static final String IPV_FUTURE = "v" + HEXDIG + "." + "[a-zA-Z0-9\\-._~!$&'()*+,;=:]";
    // The following might require an (?a) flag in other implementations than java.util.regex.Pattern
    private static final String IPV6_ADDRESS = "\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|" +
            "(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|" +
            "((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|" +
            "(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|" +
            ":((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|" +
            "(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|" +
            "((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
            "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|" +
            ":))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|" +
            "((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
            "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|" +
            "(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|" +
            "((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
            "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|" +
            "(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|" +
            "((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
            "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|" +
            "(:(((:[0-9A-Fa-f]{1,4}){1,7})|" +
            "((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
            "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))" +
            "(%[a-fA-F0-9]{2})?\\s*";
    private static final String IP_LITERAL = "\\[" + "((" + IPV6_ADDRESS + ")|(" + IPV_FUTURE + "))" + "\\]";
    private static final String HOST = "({host}(" + REG_NAME + "|" + IPV4_ADDRESS + "|" + IP_LITERAL + "))";
    public static final String AUTHORITY = "(({userInfo}" + USER_INFO + ")@)?" + HOST + "(:({port}[0-9]*))?";
    private static final String PATH_EMPTY = "";
    private static final String PATH_ROOTLESS = SEGMENT_NZ_REGEX + "(/" + SEGMENT_REGEX + ")*";
    private static final String PATH_NOSCHEME = "[a-zA-Z0-9\\-._~!$&'()*+,;=@]+" + "(/" + SEGMENT_REGEX + ")*";
    private static final String PATH_ABSOLUTE = "/(" + SEGMENT_NZ_REGEX + "(/" + SEGMENT_REGEX + ")*" + ")*";
    private static final String PATH_ABEMPTY = "(/" + SEGMENT_REGEX + ")*";
    private static final String PATH = "(" +
            PATH_ABEMPTY + "|" +
            PATH_ABSOLUTE + "|" +
            PATH_NOSCHEME + "|" +
            PATH_ROOTLESS + "|" +
            PATH_EMPTY + "|" +
            ")";
    private static final String QUERY = "(" + P_CHAR + "|/|\\?)*";
    private static final String FRAGMENT = QUERY;

    // Added negative lookahead for "//" to prevent authority from being interpreted as path
    public static final String URI_REGEX = "^(" +
            "({scheme}" + SCHEME_SEGMENT + ")" +
            ":" +
            "(//({authority}" + AUTHORITY + "))?" +
            "({path}" +
            "(?(authority)" +
            "(((?=/)" + PATH + ")|)|((?!//)" + PATH + ")" +
            ")" +
            ")" +
            "(\\?({query}" + QUERY + "))?" +
            "(#({fragment}" + FRAGMENT + "))?" +
            ")$";

    // The "&" character had to be excluded from the "pchar" definition as it already is used a delimiter.
    // This is a bug in the GLUE standard. The "&" should not be allowed, except as a delimiter.
    private static final String QUERY_ITEM_SEGMENT = "(?:(?:%[a-fA-F0-9]{2})+|(?:[a-zA-Z0-9\\-._~!$'()*+,;=:@])+)";
    private static final String QUERY_ITEM = "((fac=" + QUERY_ITEM_SEGMENT + "+)|" +
            "(bldng=" + QUERY_ITEM_SEGMENT + "+)|" +
            "(poc=" + QUERY_ITEM_SEGMENT + "+)|" +
            "(flr=" + QUERY_ITEM_SEGMENT + "+)|" +
            "(rm=" + QUERY_ITEM_SEGMENT + "+)|" +
            "(bed=" + QUERY_ITEM_SEGMENT + "+))";
    public static final String LOC_CTXT_QUERY = "^(" + QUERY_ITEM + "(&" + QUERY_ITEM + ")*)?$";
}
