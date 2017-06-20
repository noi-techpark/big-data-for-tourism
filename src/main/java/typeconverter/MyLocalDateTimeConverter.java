package typeconverter;

import de.bytefish.jtinycsvparser.typeconverter.ITypeConverter;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class MyLocalDateTimeConverter implements ITypeConverter<LocalDateTime> {

    private DateTimeFormatter dateTimeFormatter;

    public MyLocalDateTimeConverter() {
        this(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public MyLocalDateTimeConverter(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
    }

    @Override
    public LocalDateTime convert(String value) {
        LocalDateTime dt = null;
        try {
            dt = LocalDateTime.parse(value, dateTimeFormatter); // 2011-12-03T10:15:30
        } catch(DateTimeParseException e) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // 2011-12-03 10:15:30
            this.dateTimeFormatter = formatter;
            dt = LocalDateTime.parse(value, dateTimeFormatter);
        }
        return dt;
    }

    @Override
    public Type getTargetType() {
        return LocalDateTime.class;
    }

    @Override
    public String toString() {
        return "LocalDateTimeConverter{" +
                "dateTimeFormatter=" + dateTimeFormatter +
                '}';
    }
}
