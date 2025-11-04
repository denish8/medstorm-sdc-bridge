package org.somda.sdc.glue.common.uri;

import jregex.Matcher;
import jregex.Pattern;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.MeansContextState;
import org.somda.sdc.biceps.model.participant.OperatorContextState;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.model.participant.WorkflowContextState;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.common.helper.UrlUtf8;

/**
 * Utility class to map between context-based URIs and instance identifiers.
 * <p>
 * This class implements the grammar defined in IEEE 11073-20701 section 9.4.
 */
public class ContextIdentificationMapper {
    private static final String NULL_FLAVOR_ROOT = "biceps.uri.unk";
    private static final String SCHEME_PREFIX = "sdc.ctxt.";

    private static final Pattern URI_PATTERN = new Pattern(GlueConstants.URI_REGEX);

    private static final Pattern CONTEXT_SOURCE_VALIDATOR = new Pattern(
            "^(?i:sdc.ctxt.(loc|pat|ens|wfl|opr|mns))$");

    private static final Pattern INSTANCE_IDENTIFIER_PATTERN = new Pattern(
            "/({root}" + GlueConstants.SEGMENT_NZ_REGEX + ")/" +
                    "({extension}" + GlueConstants.SEGMENT_REGEX + ")$"
    );

    private static final Pattern SCHEME_VALIDATOR = new Pattern("^" + GlueConstants.SCHEME_SEGMENT + "$");

    /**
     * Converts from an instance identifier to an URI.
     *
     * @param instanceIdentifier the instance identifier to convert.
     * @param contextSource      the type of context to create a scheme for.
     * @return an URI that reflects the instance identifier.
     * @throws UriMapperGenerationArgumentException in case no valid URI could be generated from the input.
     */
    public static String fromInstanceIdentifier(InstanceIdentifier instanceIdentifier,
                                                ContextSource contextSource)
            throws UriMapperGenerationArgumentException {
        final String root = instanceIdentifier.getRootName() == null ?
                NULL_FLAVOR_ROOT : UrlUtf8.encodePChars(instanceIdentifier.getRootName());
        final String extension = UrlUtf8.encodePChars(instanceIdentifier.getExtensionName());

        final String resultingUri = contextSource.getSourceString() + ":/" + root + "/" + extension;

        try {
            fromUri(resultingUri, contextSource);
        } catch (UriMapperParsingException e) {
            throw new UriMapperGenerationArgumentException(
                    "No valid URI could be generated from the given instance identifier: " +
                            instanceIdentifier.toString() + " and context source: " + contextSource.toString());
        }

        return resultingUri;
    }

    /**
     * Converts from an URI string to an instance identifier.
     *
     * @param contextIdentificationUri the URI to parse.
     * @param expectedContextSource    the expected context source.
     * @return the converted instance identifier.
     * @throws UriMapperParsingException in case no valid URI was given.
     */
    public static InstanceIdentifier fromUri(String contextIdentificationUri,
                                             ContextSource expectedContextSource)
            throws UriMapperParsingException {

        // In case the input will be changed in the future
        Matcher expectedSourceMatcher = SCHEME_VALIDATOR.matcher(expectedContextSource.getSourceString());
        if (!expectedSourceMatcher.matches()) {
            throw new UriMapperParsingException("The expected context source: '" +
                    expectedContextSource.getSourceString() + " is not valid");
        }

        Matcher uriMatcher = URI_PATTERN.matcher(contextIdentificationUri);

        if (uriMatcher.matches()) {

            final String contextSource = uriMatcher.group("scheme");

            if (!expectedContextSource.getSourceString().equals(contextSource)) {
                throw new UriMapperParsingException(
                        "The expected context source: '" + expectedContextSource.getSourceString() +
                                "' does not match with the actual context source: '" + contextSource + "'" +
                                ContextIdentificationMapper.class.toString());
            }

            Matcher instanceIdentifierMatcher = INSTANCE_IDENTIFIER_PATTERN.matcher(uriMatcher.group("path"));

            if (instanceIdentifierMatcher.matches()) {
                final String root = instanceIdentifierMatcher.group("root");
                final String extension = instanceIdentifierMatcher.group("extension");

                final InstanceIdentifier instanceIdentifier = new InstanceIdentifier();
                final String decodedRoot = UrlUtf8.decodePChars(root);
                instanceIdentifier.setRootName(decodedRoot.equals(NULL_FLAVOR_ROOT) ? null : decodedRoot);
                final String decodedExtension = UrlUtf8.decodePChars(extension);
                instanceIdentifier.setExtensionName(decodedExtension.isEmpty() ? null : decodedExtension);

                return instanceIdentifier;
            } else {
                throw new UriMapperParsingException(
                        "Invalid encoding of InstanceIdentifier in the URI for the mapper " +
                                ContextIdentificationMapper.class.toString());
            }
        } else {
            throw new UriMapperParsingException("Invalid URI for the mapper " +
                    ContextIdentificationMapper.class.toString());
        }
    }

    /**
     * Defines the context instance identifier URI type.
     * <p>
     * This type reflects the scheme of the URI, which is according to the IEEE 11073-20701
     * <ul>
     * <li>sdc.ctxt.loc for {@link #Location}
     * <li>sdc.ctxt.pat for {@link #Patient}
     * <li>sdc.ctxt.ens for {@link #Ensemble}
     * <li>sdc.ctxt.wfl for {@link #Workflow}
     * <li>sdc.ctxt.opr for {@link #Operator}
     * <li>sdc.ctxt.mns for {@link #Means}
     * </ul>
     */
    public enum ContextSource {
        Location(SCHEME_PREFIX + "loc", LocationContextState.class),
        Patient(SCHEME_PREFIX + "pat", PatientContextState.class),
        Ensemble(SCHEME_PREFIX + "ens", EnsembleContextState.class),
        Workflow(SCHEME_PREFIX + "wfl", WorkflowContextState.class),
        Operator(SCHEME_PREFIX + "opr", OperatorContextState.class),
        Means(SCHEME_PREFIX + "mns", MeansContextState.class);

        private final String sourceString;
        private final Class<? extends AbstractContextState> sourceClass;

        ContextSource(String sourceString, Class<? extends AbstractContextState> sourceClass) {
            this.sourceString = sourceString;
            this.sourceClass = sourceClass;
        }

        public String getSourceString() {
            return sourceString;
        }

        public Class<? extends AbstractContextState> getSourceClass() {
            return sourceClass;
        }
    }
}
