package csv.mapping;

import csv.model.CountryData;
import de.bytefish.jtinycsvparser.mapping.CsvMapping;
import de.bytefish.jtinycsvparser.builder.IObjectCreator;

public class CountryMapping extends CsvMapping<CountryData> {

    public CountryMapping(IObjectCreator creator) {
        super(creator);

        mapProperty(0, String.class, CountryData::setName);
        mapProperty(1, String.class, CountryData::setLatitude);
        mapProperty(2, String.class, CountryData::setLongitude);
    }
}

