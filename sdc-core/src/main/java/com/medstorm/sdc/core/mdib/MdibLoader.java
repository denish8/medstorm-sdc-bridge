package com.medstorm.sdc.core.mdib;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.access.WriteDescriptionResult;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Populates a {@link LocalMdibAccess} from an MDIB XML document.
 *
 * <p>Without this step a provider starts up, is discoverable and answers
 * {@code GetMdib} &mdash; with an <em>empty</em> MDIB. Consumers connect
 * successfully and then find no metrics, which is a confusing failure to
 * diagnose from the consumer side.
 *
 * <p>The expected document is a {@code GetMdibResponse} (the form SDC-ri's
 * {@link MdibXmlIo} reads), i.e. the same shape a consumer would receive on the
 * wire. That makes a captured {@code GetMdibResponse} directly reusable as a
 * provider's initial MDIB.
 *
 * <p>Not thread-safe with respect to a single {@code LocalMdibAccess}: load once,
 * before the device is started.
 */
public final class MdibLoader {
    private static final Logger LOG = LoggerFactory.getLogger(MdibLoader.class);

    private final MdibXmlIo mdibXmlIo;
    private final ModificationsBuilderFactory modificationsBuilderFactory;

    @Inject
    public MdibLoader(MdibXmlIo mdibXmlIo, ModificationsBuilderFactory modificationsBuilderFactory) {
        this.mdibXmlIo = Objects.requireNonNull(mdibXmlIo, "mdibXmlIo");
        this.modificationsBuilderFactory =
                Objects.requireNonNull(modificationsBuilderFactory, "modificationsBuilderFactory");
    }

    /**
     * Convenience for callers that hold the Guice injector rather than using
     * injection themselves.
     */
    public static MdibLoader from(Injector injector) {
        Objects.requireNonNull(injector, "injector");
        return new MdibLoader(
                injector.getInstance(MdibXmlIo.class),
                injector.getInstance(ModificationsBuilderFactory.class));
    }

    /**
     * Reads {@code mdibXml} and commits its descriptors and states to {@code target}.
     *
     * <p>Single states missing from the document are generated with BICEPS-required
     * defaults, so a document carrying only {@code MdDescription} is accepted.
     *
     * @param target   the provider-side MDIB to populate.
     * @param mdibXml  a {@code GetMdibResponse} document. Closed by the caller.
     * @return the write result, describing which entities were inserted.
     * @throws MdibLoadException if the document cannot be parsed or committed.
     */
    public WriteDescriptionResult load(LocalMdibAccess target, InputStream mdibXml) throws MdibLoadException {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(mdibXml, "mdibXml");

        final Mdib mdib;
        try {
            mdib = mdibXmlIo.readMdib(mdibXml);
        } catch (Exception e) {
            // readMdib declares JAXBException and throws ClassCastException when the
            // document is well-formed XML but not a GetMdibResponse.
            throw new MdibLoadException("Could not parse MDIB XML: " + e, e);
        }

        final MdibDescriptionModifications modifications;
        try {
            modifications = modificationsBuilderFactory
                    .createModificationsBuilder(mdib, /* createSingleStateIfMissing */ true)
                    .get();
        } catch (RuntimeException e) {
            throw new MdibLoadException("Could not derive MDIB modifications from the parsed document: " + e, e);
        }

        try {
            WriteDescriptionResult result = target.writeDescription(modifications);
            LOG.info("MDIB loaded: {} entities inserted", result.getInsertedEntities().size());
            return result;
        } catch (Exception e) {
            throw new MdibLoadException("Could not commit MDIB to LocalMdibAccess: " + e, e);
        }
    }

    /**
     * Reads the MDIB from a file on disk.
     */
    public WriteDescriptionResult load(LocalMdibAccess target, Path mdibXmlFile) throws MdibLoadException {
        Objects.requireNonNull(mdibXmlFile, "mdibXmlFile");
        if (!Files.isReadable(mdibXmlFile)) {
            throw new MdibLoadException("MDIB file not found or not readable: " + mdibXmlFile.toAbsolutePath());
        }
        try (InputStream in = Files.newInputStream(mdibXmlFile)) {
            LOG.info("Loading MDIB from {}", mdibXmlFile.toAbsolutePath());
            return load(target, in);
        } catch (IOException e) {
            throw new MdibLoadException("Could not read MDIB file " + mdibXmlFile.toAbsolutePath() + ": " + e, e);
        }
    }

    /**
     * Reads the MDIB from a classpath resource, e.g. {@code "mdib/my-device.xml"}.
     */
    public WriteDescriptionResult loadFromClasspath(LocalMdibAccess target, String resourcePath)
            throws MdibLoadException {
        Objects.requireNonNull(resourcePath, "resourcePath");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = MdibLoader.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new MdibLoadException("MDIB resource not found on classpath: " + resourcePath);
            }
            LOG.info("Loading MDIB from classpath resource {}", resourcePath);
            return load(target, in);
        } catch (IOException e) {
            throw new MdibLoadException("Could not read MDIB resource " + resourcePath + ": " + e, e);
        }
    }
}
