package org.somda.sdc.glue.common.uri;


/**
 * Shall be thrown in case the URI to parse is not valid for the given mapper or is no valid URI in general.
 */
public class UriMapperParsingException extends Exception {

    UriMapperParsingException(String message) {
        super(message);
    }
}
