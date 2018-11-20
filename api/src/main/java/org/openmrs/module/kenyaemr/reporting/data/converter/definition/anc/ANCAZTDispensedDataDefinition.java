package org.openmrs.module.kenyaemr.reporting.data.converter.definition.anc;

import org.openmrs.module.reporting.data.BaseDataDefinition;
import org.openmrs.module.reporting.data.encounter.definition.EncounterDataDefinition;
import org.openmrs.module.reporting.definition.configuration.ConfigurationPropertyCachingStrategy;
import org.openmrs.module.reporting.evaluation.caching.Caching;

/**
 * ANC AZT Dispensed Column
 */
@Caching(strategy=ConfigurationPropertyCachingStrategy.class)
public class ANCAZTDispensedDataDefinition extends BaseDataDefinition implements EncounterDataDefinition {

    public static final long serialVersionUID = 1L;

    /**
     * Default Constructor
     */
    public ANCAZTDispensedDataDefinition() {
        super();
    }

    /**
     * Constructor to populate name only
     */
    public ANCAZTDispensedDataDefinition(String name) {
        super(name);
    }

    //***** INSTANCE METHODS *****

    /**
     * @see org.openmrs.module.reporting.data.DataDefinition#getDataType()
     */
    public Class<?> getDataType() {
        return Double.class;
    }
}
