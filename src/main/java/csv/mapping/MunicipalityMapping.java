package csv.mapping;

import csv.model.MunicipalityData;
import de.bytefish.jtinycsvparser.mapping.CsvMapping;
import de.bytefish.jtinycsvparser.builder.IObjectCreator;

public class MunicipalityMapping extends CsvMapping<MunicipalityData> {

    public MunicipalityMapping(IObjectCreator creator) {
        super(creator);

        mapProperty(0, String.class, MunicipalityData::setName);
        mapProperty(1, String.class, MunicipalityData::setCode);
        mapProperty(2, String.class, MunicipalityData::setLatitude);
        mapProperty(3, String.class, MunicipalityData::setLongitude);
    }
}

