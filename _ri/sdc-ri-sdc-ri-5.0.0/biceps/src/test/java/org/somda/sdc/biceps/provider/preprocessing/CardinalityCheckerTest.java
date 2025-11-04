package org.somda.sdc.biceps.provider.preprocessing;

import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.UnitTestUtil;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.NumericMetricDescriptor;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.org.somda.common.LoggingTestWatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingTestWatcher.class)
class CardinalityCheckerTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private CardinalityChecker cardinalityChecker;
    private MdibStorage mdibStorage;

    @BeforeEach
    void beforeEach() {
        // Given a cardinality checker
        cardinalityChecker = UT.getInjector().getInstance(CardinalityChecker.class);
        mdibStorage = mock(MdibStorage.class);
    }

    @Test
    void noInsertions() throws Exception {
        // When there are no insertions in a modification set
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                .update(MockModelFactory.createDescriptor(Handles.MDS_0, MdsDescriptor.class))
                .update(MockModelFactory.createDescriptor(Handles.VMD_0, VmdDescriptor.class))
                .delete(MockModelFactory.createDescriptor(Handles.METRIC_0, NumericMetricDescriptor.class));

        // Then expect the cardinality checker to pass
        assertDoesNotThrow(() -> apply(modifications));
    }

    @Test
    void mdsInsertion() throws Exception {
        // When there is an MDS being inserted
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                .insert(MockModelFactory.createDescriptor(Handles.MDS_0, MdsDescriptor.class));

        // Then expect the cardinality checker to pass
        assertDoesNotThrow(() -> apply(modifications));
    }

    @Test
    void sameTypeInModificationsIsOk() throws Exception {
        when(mdibStorage.getEntity(Handles.MDS_0)).thenReturn(Optional.empty());

        {
            // When there are two VMDs to be inserted below an MDS
            final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                    .insert(MockModelFactory.createDescriptor(Handles.VMD_0, VmdDescriptor.class), Handles.MDS_0)
                    .insert(MockModelFactory.createDescriptor(Handles.VMD_1, VmdDescriptor.class), Handles.MDS_0);

            // Then expect the cardinality checker to pass
            assertDoesNotThrow(() -> apply(modifications));
        }

        {
            // When there are two SCOs to be inserted below two MDSs
            final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                    .insert(MockModelFactory.createDescriptor(Handles.VMD_0, VmdDescriptor.class), Handles.MDS_0)
                    .insert(MockModelFactory.createDescriptor(Handles.VMD_1, VmdDescriptor.class), Handles.MDS_1);

            // Then expect the cardinality checker to pass
            assertDoesNotThrow(() -> apply(modifications));
        }
    }

    @Test
    void sameTypeInModificationsIsBad() throws Exception {
        when(mdibStorage.getEntity(Handles.MDS_0)).thenReturn(Optional.empty());

        ScoDescriptor descriptor = MockModelFactory.createDescriptor(Handles.SCO_0, ScoDescriptor.class);

        // When there are two SCOs to be inserted below an MDS
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                .insert(MockModelFactory.createDescriptor(Handles.SCO_0, ScoDescriptor.class), Handles.MDS_0)
                .insert(MockModelFactory.createDescriptor(Handles.SCO_1, ScoDescriptor.class), Handles.MDS_0)
                .insert(MockModelFactory.createDescriptor(Handles.SCO_2, ScoDescriptor.class), Handles.MDS_0);

        // Then expect the cardinality checker to throw an exception
        assertThrows(CardinalityException.class, () -> apply(modifications));
    }

    @Test
    void insertWithExistingChildInMdibStorageIsOk() throws Exception {
        when(mdibStorage.getEntity(Handles.MDS_0)).thenReturn(Optional.of(mock(MdibEntity.class)));

        {
            when(mdibStorage.getChildrenByType(Handles.MDS_0, VmdDescriptor.class))
                    .thenReturn(Arrays.asList(mock(MdibEntity.class)));

            // When there are two VMDs to be inserted below an MDS that exists in the MdibStorage
            // and possesses on VMD already
            final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                    .insert(MockModelFactory.createDescriptor(Handles.VMD_0, VmdDescriptor.class), Handles.MDS_0)
                    .insert(MockModelFactory.createDescriptor(Handles.VMD_1, VmdDescriptor.class), Handles.MDS_0);

            // Then expect the cardinality checker to pass
            assertDoesNotThrow(() -> apply(modifications));
        }

        {
            when(mdibStorage.getChildrenByType(Handles.MDS_0, VmdDescriptor.class))
                    .thenReturn(Collections.emptyList());

            // When there is one SCO to be inserted below an MDS that exists in the MdibStorage
            // and does not possess an SCO already
            final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                    .insert(MockModelFactory.createDescriptor(Handles.SCO_0, ScoDescriptor.class), Handles.MDS_0);

            // Then expect the cardinality checker to pass
            assertDoesNotThrow(() -> apply(modifications));
        }
    }

    @Test
    void insertWithExistingChildInMdibStorageIsBad() throws Exception {
        when(mdibStorage.getEntity(Handles.MDS_0))
                .thenReturn(Optional.of(mock(MdibEntity.class)));
        when(mdibStorage.getChildrenByType(Handles.MDS_0, ScoDescriptor.class))
                .thenReturn(Arrays.asList(mock(MdibEntity.class)));

        // When there is one SCO to be inserted below an MDS that exists in the MdibStorage
        // and possesses an SCO already
        final MdibDescriptionModifications modifications = MdibDescriptionModifications.create()
                .insert(MockModelFactory.createDescriptor(Handles.SCO_0, ScoDescriptor.class), Handles.MDS_0);

        // Then expect the cardinality checker to throw an exception
        assertThrows(CardinalityException.class, () -> apply(modifications));
    }

    private void apply(MdibDescriptionModifications modifications) throws CardinalityException {
        for (MdibDescriptionModification modification : modifications.getModifications()) {
            cardinalityChecker.process(modifications, modification, mdibStorage);
        }
    }
}