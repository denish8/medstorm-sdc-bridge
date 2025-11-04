package org.somda.sdc.glue.common.uri;


/**
 * Shall be thrown in case the arguments for generating an URI in one of the mappers are not compatible with the
 * URI generation rules.
 */
public class UriMapperGenerationArgumentException extends Exception {

    UriMapperGenerationArgumentException(String message) {
        super(message);
    }
}
