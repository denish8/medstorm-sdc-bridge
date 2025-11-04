package org.somda.sdc.dpws;

/**
 * Constants for the Uniform Resource Identifiers (URI): Generic Syntax.
 */
public class RFC2396Patterns {
    private static final String HEX = "[a-fA-F0-9]";
    private static final String ESCAPED = "(%" + HEX + HEX + ")";

    private static final String UNRESERVED = "([a-zA-Z0-9\\-_.!~*'()])";

    private static final String RESERVED = "[;/?:@&=+$,]";
    private static final String URIC = "(" + RESERVED + "|" + UNRESERVED + "|" + ESCAPED + ")";

    private static final String FRAGMENT = "({fragment}(" + URIC + "*))";

    private static final String QUERY = "(" + URIC + "*)";

    private static final String P_CHAR = "(" + UNRESERVED + "|" + ESCAPED + "|[:@&=+$,])";
    private static final String PARAM = "(" + P_CHAR + "*)";
    private static final String SEGMENT = "(" + P_CHAR + "*(;" + PARAM + ")*)";
    private static final String PATH_SEGMENTS = "((" + SEGMENT + ")((/" + SEGMENT + ")*))";

    private static final String PORT = "([0-9]*)";

    private static final String IPV4_ADDRESS = "([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)";
    private static final String TOPLABEL = "([a-zA-Z]|([a-zA-Z]([a-zA-Z0-9]|\\-)*[a-zA-Z0-9]))";
    private static final String DOMAINLABEL = "([a-zA-Z0-9]|([a-zA-Z0-9]([a-zA-Z0-9]|\\-)*[a-zA-Z0-9]))";
    private static final String HOSTNAME = "((" + DOMAINLABEL + "\\.)*" + TOPLABEL + "(\\.)?)";
    private static final String HOST = "(" + HOSTNAME + "|" + IPV4_ADDRESS + ")";
    private static final String HOSTPORT = "(" + HOST + "(:" + PORT + ")?)";

    private static final String USER_INFO = "((" + UNRESERVED + "|" + ESCAPED + "|[;:&=+$,])*)";
    private static final String SERVER = "(((" + USER_INFO + "@)?" + HOSTPORT + ")?" + ")";

    private static final String REG_NAME = "((" + UNRESERVED + "|" + ESCAPED + "|[$,;:@&=+])+)";

    public static final String AUTHORITY = "(" + SERVER + "|" + REG_NAME + ")";
    private static final String SCHEME_SEGMENT = "({scheme}(?i:[a-z][a-z0-9+-.]*))";
    private static final String REL_SEGMENT = "(" + UNRESERVED + "|" + ESCAPED + "|[;@&=+$,])+";

    public static final String ABS_PATH = "(/" + PATH_SEGMENTS + ")";
    private static final String NET_PATH = "(//(" + AUTHORITY + ")(" + ABS_PATH + ")?)";
    private static final String REL_PATH = "(" + REL_SEGMENT + "(" + ABS_PATH + ")?)";

    private static final String URIC_NO_SLASH = "(" + UNRESERVED + "|" + ESCAPED + "|" + "[;?:@&=+$,])";

    private static final String OPAQUE_PART = "(" + URIC_NO_SLASH + "(" + URIC + ")*)";
    private static final String HIER_PART = "((" + NET_PATH + "|" + ABS_PATH + ")(\\?({absoluteUriQuery}" + QUERY
            + "))?)";

    public static final String RELATIVE_URI = "({relativeUri}(" + NET_PATH + "|" + ABS_PATH + "|" + REL_PATH
            + ")(\\?({relativeUriQuery}" + QUERY + "))?)";

    public static final String ABSOLUTE_URI = "({absoluteUri}" + SCHEME_SEGMENT + ":" + "(" + HIER_PART + "|"
            + OPAQUE_PART + "))";

    public static final String URI_REFERENCE = "(" + ABSOLUTE_URI + "|" + RELATIVE_URI + ")?(#" + FRAGMENT + ")?";

    private RFC2396Patterns() {
    }
}
