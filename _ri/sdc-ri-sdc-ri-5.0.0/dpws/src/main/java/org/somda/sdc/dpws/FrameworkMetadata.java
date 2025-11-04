package org.somda.sdc.dpws;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Container class for framework metadata such as framework version, java version etc.
 */
@Singleton
public class FrameworkMetadata {
    private static final Logger LOG = LogManager.getLogger(FrameworkMetadata.class);
    private final Logger instanceLogger;
    private String frameworkVersion;
    private String javaVersion;
    private String javaVendor;
    private String osVersion;

    @Inject
    FrameworkMetadata(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        frameworkVersion = getClass().getPackage().getImplementationVersion();
        if (frameworkVersion == null) {
            frameworkVersion = "DEVELOPMENT VERSION" + determineGitRevision();
        }

        javaVersion = System.getProperty("java.version");
        javaVendor = System.getProperty("java.vendor");
        osVersion = System.getProperty("os.name");
    }

    public String getFrameworkVersion() {
        return frameworkVersion;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getJavaVendor() {
        return javaVendor;
    }

    public String getOsVersion() {
        return osVersion;
    }

    private String determineGitRevision() {
        // if we're in git, attach the hash
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("git", "rev-parse", "HEAD");
        try {
            // start the process
            Process process = processBuilder.start();
            StringBuilder output = readProcessOutput(process);

            // wait for git to finish
            int exitVal = process.waitFor();
            if (exitVal == 0) {
                return " " + output;
            } else {
                instanceLogger.error("Could not call git to determine revision, exit code was {}", exitVal);
            }
        } catch (IOException | InterruptedException e) {
            instanceLogger.error("Could not call git to determine revision", e);
        }
        return " unknown revision";
    }

    private StringBuilder readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();

        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            // read all ines
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            return output;
        }
    }
}
