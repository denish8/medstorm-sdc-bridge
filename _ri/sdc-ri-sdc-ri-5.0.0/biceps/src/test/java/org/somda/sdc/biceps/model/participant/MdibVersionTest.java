package org.somda.sdc.biceps.model.participant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigInteger;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class MdibVersionTest {

    @Test
    void accessFunctions() {
        var mdibVersion = MdibVersion.create();
        assertEquals(BigInteger.ZERO, mdibVersion.getInstanceId());
        assertEquals(BigInteger.ZERO, mdibVersion.getVersion());
        assertNotNull(mdibVersion.getSequenceId());
        assertFalse(mdibVersion.getSequenceId().toString().isEmpty());
    }

    @Test
    void equality() {
        {
            var mdibVersion = MdibVersion.create();
            assertNotEquals(null, mdibVersion);
        }
        {
            var lhs = new MdibVersion("equal");
            var rhs = new MdibVersion("equal");
            assertEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion("equal");
            var rhs = new MdibVersion("unequal");
            assertNotEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion("equal", BigInteger.ONE);
            var rhs = new MdibVersion("equal", BigInteger.ONE);
            assertEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion("equal", BigInteger.ONE);
            var rhs = new MdibVersion("equal", BigInteger.ZERO);
            assertNotEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ONE);
            var rhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ONE);
            assertEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ZERO);
            var rhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ONE);
            assertNotEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion("equal", BigInteger.ZERO, BigInteger.ONE);
            var rhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ONE);
            assertNotEquals(lhs, rhs);
        }
        {
            var lhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ONE);
            var rhs = new MdibVersion("unequal", BigInteger.ONE, BigInteger.ONE);
            assertNotEquals(lhs, rhs);
        }
    }

    @Test
    void comparison() {
        {
            var mdibVersion = MdibVersion.create();
            assertTrue(mdibVersion.compareToMdibVersion(null).isEmpty());
        }
        {
            var lhs = new MdibVersion("equal");
            var rhs = new MdibVersion("equal");
            assertTrue(lhs.compareToMdibVersion(rhs).isPresent());
            assertEquals(Integer.valueOf(0), lhs.compareToMdibVersion(rhs).get());
        }
        {
            var lhs = new MdibVersion("equal");
            var rhs = new MdibVersion("unequal");
            assertTrue(lhs.compareToMdibVersion(rhs).isEmpty());
        }
        {
            var lhs = new MdibVersion("equal", BigInteger.ONE);
            var rhs = new MdibVersion("equal", BigInteger.ONE);
            assertTrue(lhs.compareToMdibVersion(rhs).isPresent());
            assertEquals(Integer.valueOf(0), lhs.compareToMdibVersion(rhs).get());
        }
        {
            var lhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ONE);
            var rhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ONE);
            assertTrue(lhs.compareToMdibVersion(rhs).isPresent());
            assertEquals(Integer.valueOf(0), lhs.compareToMdibVersion(rhs).get());
        }

        {
            var lhs = new MdibVersion("equal", BigInteger.ZERO, BigInteger.ONE);
            var rhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ONE);
            assertTrue(lhs.compareToMdibVersion(rhs).isPresent());
            assertEquals(Integer.valueOf(-1), lhs.compareToMdibVersion(rhs).get());
        }
        {
            var lhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ONE);
            var rhs = new MdibVersion("equal", BigInteger.ZERO, BigInteger.ONE);
            assertTrue(lhs.compareToMdibVersion(rhs).isPresent());
            assertEquals(Integer.valueOf(1), lhs.compareToMdibVersion(rhs).get());
        }

        {
            var lhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ZERO);
            var rhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ONE);
            assertTrue(lhs.compareToMdibVersion(rhs).isPresent());
            assertEquals(Integer.valueOf(-1), lhs.compareToMdibVersion(rhs).get());
        }
        {
            var lhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ONE);
            var rhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ZERO);
            assertTrue(lhs.compareToMdibVersion(rhs).isPresent());
            assertEquals(Integer.valueOf(1), lhs.compareToMdibVersion(rhs).get());
        }

        {
            var lhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ZERO);
            var rhs = new MdibVersion("equal", BigInteger.ZERO, BigInteger.ONE);
            assertTrue(lhs.compareToMdibVersion(rhs).isPresent());
            assertEquals(Integer.valueOf(-1), lhs.compareToMdibVersion(rhs).get());
        }
        {
            var lhs = new MdibVersion("equal", BigInteger.ZERO, BigInteger.ONE);
            var rhs = new MdibVersion("equal", BigInteger.ONE, BigInteger.ZERO);
            assertTrue(lhs.compareToMdibVersion(rhs).isPresent());
            assertEquals(Integer.valueOf(1), lhs.compareToMdibVersion(rhs).get());
        }
    }
}