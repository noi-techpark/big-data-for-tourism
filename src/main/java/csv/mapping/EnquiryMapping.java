package csv.mapping;

import csv.model.EnquiryData;
import de.bytefish.jtinycsvparser.mapping.CsvMapping;
import de.bytefish.jtinycsvparser.builder.IObjectCreator;
import de.bytefish.jtinycsvparser.typeconverter.LocalDateConverter;
import typeconverter.MyLocalDateTimeConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EnquiryMapping extends CsvMapping<EnquiryData> {

    public EnquiryMapping(IObjectCreator creator) {
        super(creator);

        mapProperty(0, LocalDate.class, EnquiryData::setArrival, new LocalDateConverter(DateTimeFormatter.ISO_LOCAL_DATE));
        mapProperty(1, LocalDate.class, EnquiryData::setDeparture, new LocalDateConverter(DateTimeFormatter.ISO_LOCAL_DATE));
        mapProperty(2, String.class, EnquiryData::setCountry);
        mapProperty(3, Integer.class, EnquiryData::setAdults);
        mapProperty(4, Integer.class, EnquiryData::setChildren);
        mapProperty(5, Integer.class, EnquiryData::setDestination);
        mapProperty(6, Integer.class, EnquiryData::setCategory);
        mapProperty(7, Integer.class, EnquiryData::setBooking);
        mapProperty(8, Integer.class, EnquiryData::setCancellation);
        mapProperty(9, LocalDateTime.class, EnquiryData::setSubmittedOn, new MyLocalDateTimeConverter(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}

