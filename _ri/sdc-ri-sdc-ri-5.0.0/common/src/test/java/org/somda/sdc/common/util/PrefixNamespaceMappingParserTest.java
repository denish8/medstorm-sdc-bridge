package org.somda.sdc.common.util;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrefixNamespaceMappingParserTest {
    @Test
    void parse() {
        final Map<String, PrefixNamespaceMappingParser.PrefixNamespacePair> expectedMappings = Stream.of(new Object[][]{
                {"http://namespace1", new PrefixNamespaceMappingParser.PrefixNamespacePair("prefix1", "http://namespace1")},
                {"http://namespace2", new PrefixNamespaceMappingParser.PrefixNamespacePair("prefix2", "http://namespace2")},
                {"http://namespace3", new PrefixNamespaceMappingParser.PrefixNamespacePair("prefix3", "http://namespace3")},
                {"http://namespace4", new PrefixNamespaceMappingParser.PrefixNamespacePair("prefix4", "http://namespace4")}
        }).collect(Collectors.toMap(data -> (String) data[0], data -> (PrefixNamespaceMappingParser.PrefixNamespacePair) data[1]));

        final StringBuilder stringToParse = new StringBuilder();
        expectedMappings.forEach((key, value) -> stringToParse.append(value.toString()));

        final PrefixNamespaceMappingParser parser = new PrefixNamespaceMappingParser();
        final Map<String, PrefixNamespaceMappingParser.PrefixNamespacePair> actualMappings =
                parser.parse(stringToParse.toString());
        assertEquals(expectedMappings, actualMappings);
    }
}