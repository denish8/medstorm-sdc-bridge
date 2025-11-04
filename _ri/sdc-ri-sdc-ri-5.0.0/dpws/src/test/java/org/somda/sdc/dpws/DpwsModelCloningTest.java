package org.somda.sdc.dpws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.model.LocalizedStringType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DpwsModelCloningTest extends DpwsTest {

    @BeforeEach
    void beforeEach() throws Exception {
        setUp();
    }

    @Test
    void deepCopy() {
        var manufacturer = new LocalizedStringType();
        manufacturer.setValue("mock manufacturer");
        var thisModelType = new ThisModelBuilder()
                .setManufacturer(List.of(manufacturer))
                .get();

        var thisModelTypeCopy = thisModelType.createCopy();

        assertEquals(thisModelType, thisModelTypeCopy);

        // check change one object and expect copy doesn't change, meaning its deep not shallow copy
        thisModelType.getManufacturer().get(0).setValue("updated manufacturer");

        assertNotEquals(thisModelType, thisModelTypeCopy);
        assertEquals(thisModelType.getManufacturer().get(0).getValue(), "updated manufacturer");
        assertEquals(thisModelTypeCopy.getManufacturer().get(0).getValue(), "mock manufacturer");
    }
}