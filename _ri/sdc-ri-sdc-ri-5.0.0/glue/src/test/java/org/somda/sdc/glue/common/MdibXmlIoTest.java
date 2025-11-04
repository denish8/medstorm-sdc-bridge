package org.somda.sdc.glue.common;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.factory.MdibMapperFactory;
import test.org.somda.common.LoggingTestWatcher;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(LoggingTestWatcher.class)
class MdibXmlIoTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    @Test
    void ioRoundTrip() throws PreprocessingException, JAXBException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Injector injector = UT.getInjector();
        final MdibXmlIo mdibXmlIo = injector.getInstance(MdibXmlIo.class);
        final BaseTreeModificationsSet baseTreeModificationsSet = new BaseTreeModificationsSet(
                new MockEntryFactory(injector.getInstance(MdibTypeValidator.class)));

        final LocalMdibAccessFactory localMdibAccessFactory = injector.getInstance(LocalMdibAccessFactory.class);
        final LocalMdibAccess localMdibAccess = localMdibAccessFactory.createLocalMdibAccess();
        final MdibDescriptionModifications baseTree = baseTreeModificationsSet.createFullyPopulatedTree();
        localMdibAccess.writeDescription(baseTree);

        final MdibMapperFactory mapperFactory = injector.getInstance(MdibMapperFactory.class);
        final MdibMapper mdibMapper = mapperFactory.createMdibMapper(localMdibAccess);
        final Mdib expectedMdib = mdibMapper.mapMdib();
        mdibXmlIo.writeMdib(expectedMdib, outputStream);
        final Mdib actualMdib = mdibXmlIo.readMdib(new ByteArrayInputStream(outputStream.toByteArray()));

        // some plausibility checks
        assertEquals(expectedMdib.getMdDescription().getMds().get(0).getHandle(),
                actualMdib.getMdDescription().getMds().get(0).getHandle());
        assertEquals(expectedMdib.getMdDescription().getMds().get(0).getExtension().getAny().size(),
                actualMdib.getMdDescription().getMds().get(0).getExtension().getAny().size());
        assertEquals(expectedMdib.getMdDescription().getMds().get(0).getClock().getResolution(),
                actualMdib.getMdDescription().getMds().get(0).getClock().getResolution());

        final ArrayListMultimap<String, AbstractState> states = ArrayListMultimap.create();
        actualMdib.getMdState().getState().forEach(state -> states.put(state.getDescriptorHandle(), state));
        expectedMdib.getMdState().getState().forEach(expectedState ->
                assertFalse(states.get(expectedState.getDescriptorHandle()).isEmpty()));
    }

}