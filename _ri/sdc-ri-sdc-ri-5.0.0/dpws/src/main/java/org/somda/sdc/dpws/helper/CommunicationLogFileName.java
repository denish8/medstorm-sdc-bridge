package org.somda.sdc.dpws.helper;

import org.somda.sdc.dpws.soap.CommunicationContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Helper class to build up communication log file names.
 */
public class CommunicationLogFileName {
    private static final String SEPARATOR = "_";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH-mm-ss-SSS");

    /**
     * Creates a communication log file base name.
     *
     * @param direction            the communication direction.
     * @param communicationContext the communication context to be used to find address and port info.
     * @return a string of the format {@code <NANO-TIME>_<HH-mm-ss-SSS>_<DIRECTION>_<ADDR>_<PORT>}.
     */
    public static String create(String direction, CommunicationContext communicationContext) {
        var destAddr = communicationContext.getTransportInfo().getRemoteAddress().orElse("[unk-addr]");
        var destPort = communicationContext.getTransportInfo().getRemotePort().orElse(-1);
        var date = LocalTime.now();
        return System.nanoTime() + SEPARATOR + date.format(DATE_TIME_FORMATTER) + SEPARATOR + direction +
                SEPARATOR + destAddr + SEPARATOR + destPort;
    }

    /**
     * Appends an identifier to a base name.
     *
     * @param prefix   the base name prefix.
     * @param appendix the portion to append.
     * @return a string of the format {@code <PREFIX>_<APPENDIX>}.
     */
    public static String append(String prefix, String appendix) {
        return prefix + SEPARATOR + appendix;
    }

    /**
     * Appends the SOAP file extension to the given name.
     *
     * @param name the file name.
     * @return a string of the format {@code <NAME>.xml}.
     */
    public static String appendSoapSuffix(String name) {
        return name + ".xml";
    }

    /**
     * Appends the HTTP header file extension to the given name.
     *
     * @param name the file name.
     * @return a string of the format {@code <NAME>.txt}.
     */
    public static String appendHttpHeaderSuffix(String name) {
        return name + ".txt";
    }
}
