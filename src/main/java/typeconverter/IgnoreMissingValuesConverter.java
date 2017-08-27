package typeconverter;

import de.bytefish.jtinycsvparser.typeconverter.ITypeConverter;
import de.bytefish.jtinycsvparser.utils.StringUtils;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class IgnoreMissingValuesConverter implements ITypeConverter<Integer> {

    private List<String> missingValueRepresentation;

    public IgnoreMissingValuesConverter(String... missingValueRepresentation) {
        this(Arrays.asList(missingValueRepresentation));
    }

    public IgnoreMissingValuesConverter(List<String> missingValueRepresentation) {
        this.missingValueRepresentation = missingValueRepresentation;
    }

    @Override
    public Integer convert(final String s) {

        if(StringUtils.isNullOrWhiteSpace(s)) {
            return 0;
        }

        boolean isMissingValue = missingValueRepresentation
                .stream()
                .anyMatch(x -> x.equals(s));

        if(isMissingValue) {
            return 0;
        }

        return Integer.parseInt(s);
    }

    @Override
    public Type getTargetType() {
        return Integer.class;
    }
}
