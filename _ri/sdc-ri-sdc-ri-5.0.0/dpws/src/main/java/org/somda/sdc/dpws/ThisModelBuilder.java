package org.somda.sdc.dpws;

import org.somda.sdc.dpws.model.LocalizedStringType;
import org.somda.sdc.dpws.model.ObjectFactory;
import org.somda.sdc.dpws.model.ThisModelType;

import java.util.List;

/**
 * Convenient class to build DPWS' ThisModel.
 */
public class ThisModelBuilder {
    private final ThisModelType thisModel;

    /**
     * Default constructor.
     */
    public ThisModelBuilder() {
        thisModel = (new ObjectFactory()).createThisModelType();
    }

    /**
     * Constructor with predefined manufacturer and model name.
     *
     * @param manufacturer the manufacturer name.
     * @param modelName    the model name.
     */
    public ThisModelBuilder(List<LocalizedStringType> manufacturer,
                            List<LocalizedStringType> modelName) {
        this();
        thisModel.setManufacturer(manufacturer);
        thisModel.setModelName(modelName);
    }

    /**
     * Sets the manufacturer.
     *
     * @param manufacturer to set to
     * @return this builder
     */
    public ThisModelBuilder setManufacturer(List<LocalizedStringType> manufacturer) {
        thisModel.setManufacturer(manufacturer);
        return this;
    }

    /**
     * Sets the manufacturer url.
     *
     * @param manufacturerUrl to set to
     * @return this builder
     */
    public ThisModelBuilder setManufacturerUrl(String manufacturerUrl) {
        thisModel.setManufacturerUrl(manufacturerUrl);
        return this;
    }

    /**
     * Sets the model name.
     *
     * @param modelName to set to
     * @return this builder
     */
    public ThisModelBuilder setModelName(List<LocalizedStringType> modelName) {
        thisModel.setModelName(modelName);
        return this;
    }

    /**
     * Sets the model number.
     *
     * @param modelNumber to set to
     * @return this builder
     */
    public ThisModelBuilder setModelNumber(String modelNumber) {
        thisModel.setModelNumber(modelNumber);
        return this;
    }

    /**
     * Sets the model url.
     *
     * @param modelUrl to set to
     * @return this builder
     */
    public ThisModelBuilder setModelUrl(String modelUrl) {
        thisModel.setModelUrl(modelUrl);
        return this;
    }

    /**
     * Sets the presentation url.
     *
     * @param presentationUrl to set to
     * @return this builder
     */
    public ThisModelBuilder setPresentationUrl(String presentationUrl) {
        thisModel.setPresentationUrl(presentationUrl);
        return this;
    }

    /**
     * Gets the actual model type.
     *
     * @return the internally stored model type. Caution: changes afterwards in the fluent interface will affect
     * this returned value.
     */
    public ThisModelType get() {
        return thisModel;
    }
}
