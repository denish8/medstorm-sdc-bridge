package org.somda.sdc.dpws.soap.wsdiscovery;

import com.google.common.primitives.UnsignedInteger;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.somda.sdc.dpws.soap.wsdiscovery.model.AppSequenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Utility class for the WS-Discovery plugin.
 */
public class WsDiscoveryUtil {
    private final ObjectFactory wsdFactory;
    private final AtomicInteger messageIdCounter;

    @Inject
    WsDiscoveryUtil(ObjectFactory wsdFactory) {
        this.wsdFactory = wsdFactory;
        messageIdCounter = new AtomicInteger(0);
    }

    /**
     * Checks if a QName superset includes a subset.
     *
     * @param superset the superset to match against.
     * @param subset   the subset to match against.
     * @return true if subset is in superset or subset is equal to superset, otherwise false.
     */
    public boolean isTypesMatching(List<QName> superset, List<QName> subset) {
        return isMatching(superset, subset, (o1, o2) -> o1.equals(o2) ? 0 : 1);
    }

    /**
     * Checks if a scope superset includes a subset.
     *
     * @param superset the superset to match against.
     * @param subset   the subset to match against.
     * @param matchBy  the rule how to compare the URI strings.
     *                 {@link MatchBy#RFC3986} and {@link MatchBy#STRCMP0} are supported.
     * @return true if subset is in superset or subset is equal to superset given a matching algorithm in matchBy,
     * otherwise false. If the matching algorithm is not supported, this method always returns false.
     */
    public boolean isScopesMatching(List<String> superset, List<String> subset, MatchBy matchBy) {
        switch (matchBy) {
            case RFC3986:
                return isMatching(superset, subset, (o1, o2) ->
                        uriCompare(o1, o2) ? 0 : 1
                );
            case STRCMP0:
                return isMatching(superset, subset, (o1, o2) -> o1.equals(o2) ? 0 : 1);
            default:
                return false;
        }
    }

    /**
     * Creates an app sequence based on a given instance id.
     *
     * @param instanceId the instance id to create the app sequence from.
     * @return an app sequence that includes the given instance id and an unsigned integer that is incremented with
     * every method request of a specific instance of {@link WsDiscoveryUtil}.
     */
    public AppSequenceType createAppSequence(UnsignedInteger instanceId) {
        UnsignedInteger messageId = UnsignedInteger.valueOf(messageIdCounter.addAndGet(1));
        AppSequenceType appSequence = wsdFactory.createAppSequenceType();
        appSequence.setInstanceId(instanceId.longValue());
        appSequence.setMessageNumber(messageId.longValue());
        return appSequence;
    }

    private <T> boolean isMatching(List<T> superset, List<T> subset, Comparator<T> comp) {
        return superset.size() >= subset.size() && superset.stream()
            .filter(qName1 -> subset.stream()
                .anyMatch(qName2 -> comp.compare(qName1, qName2) == 0)).count() == subset.size();
    }

    private boolean uriCompare(String o1, String o2) {
        var supersetUri = URI.create(o1);
        var subsetUri = URI.create(o2);

        // paths must not have /./ or /../ segments
        var pattern = Pattern.compile("/\\./|/\\.\\./");
        var supersetPath = supersetUri.getPath();
        var subsetPath = subsetUri.getPath();
        if ((StringUtils.isNotBlank(supersetPath) && pattern.matcher(supersetPath).find()) ||
                (StringUtils.isNotBlank(subsetPath) && pattern.matcher(subsetPath).find())) {
            return false;
        }
        if (supersetUri.toString().equals(subsetUri.toString())) {
            return true;
        }
        if (!StringUtils.equalsIgnoreCase(supersetUri.getScheme(), subsetUri.getScheme())) {
            return false;
        }
        if (!StringUtils.equalsIgnoreCase(supersetUri.getAuthority(), subsetUri.getAuthority())) {
            return false;
        }
        var supersetSegments = supersetPath != null ? supersetPath.split("/") : new String[]{};
        var subsetSegments = subsetPath != null ? subsetPath.split("/") : new String[]{};

        if (subsetSegments.length > supersetSegments.length) {
            // subset is bigger than superset, its not equal even if all superset paths matches.
            return false;
        }

        for (var i = 0; i < supersetSegments.length; i++) {
            if (subsetSegments.length > i && !supersetSegments[i].equals(subsetSegments[i])) {
                return false;
            }
        }

        // we compare schemeSpecificPart only if authority is null
        if (supersetUri.getAuthority() == null && subsetUri.getAuthority() == null &&
                !StringUtils.equals(supersetUri.getSchemeSpecificPart(), subsetUri.getSchemeSpecificPart())) {
            return false;
        }

        return true;
    }
}
