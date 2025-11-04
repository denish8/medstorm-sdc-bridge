package org.somda.sdc.glue.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import test.org.somda.common.LoggingTestWatcher;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class ModificationsBuilderTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    @Test
    void createSingleStateIfNotExists() {
        var builderFactory = UT.getInjector().getInstance(ModificationsBuilderFactory.class);

        var mdib = new Mdib();
        var mdDescription = new MdDescription();
        mdib.setMdDescription(mdDescription);

        var mds = MockModelFactory.createDescriptor(Handles.MDS_0, MdsDescriptor.class);
        mdDescription.getMds().add(mds);

        var vmd = MockModelFactory.createDescriptor(Handles.VMD_0, VmdDescriptor.class);
        mds.getVmd().add(vmd);

        assertThrows(RuntimeException.class, () ->
                builderFactory.createModificationsBuilder((Mdib) mdib.clone(), false));

        assertDoesNotThrow(() -> builderFactory.createModificationsBuilder((Mdib) mdib.clone(), true));

        var modifications = builderFactory.createModificationsBuilder((Mdib) mdib.clone(), true).get();
        assertEquals(2, modifications.getModifications().size());
        assertEquals(1, modifications.getModifications().get(0).getStates().size());
        assertEquals(1, modifications.getModifications().get(1).getStates().size());
    }

    @Test
    void applyDefaultStates() {
        var builderFactory = UT.getInjector().getInstance(ModificationsBuilderFactory.class);

        var mdib = new Mdib();
        var mdDescription = new MdDescription();
        mdib.setMdDescription(mdDescription);
        var mds = MockModelFactory.createDescriptor(Handles.MDS_0, MdsDescriptor.class);
        mdDescription.getMds().add(mds);
        var clock = MockModelFactory.createDescriptor(Handles.CLOCK_0, ClockDescriptor.class);
        mds.setClock(clock);

        ModificationsBuilder modificationsBuilder = builderFactory.createModificationsBuilder(mdib, true);
        MdibDescriptionModifications modifications = modificationsBuilder.get();
        assertEquals(2, modifications.getModifications().size());
        assertEquals(1,  modifications.getModifications().get(1).getStates().size());
        var state = modifications.getModifications().get(1).getStates().get(0);
        assertTrue(state instanceof ClockState);
        assertFalse(((ClockState)state).isRemoteSync());
    }
}