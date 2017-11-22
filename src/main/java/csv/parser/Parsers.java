package csv.parser;

import csv.mapping.EnquiryMapping;
import csv.model.EnquiryData;
import de.bytefish.jtinycsvparser.CsvParser;
import de.bytefish.jtinycsvparser.CsvParserOptions;
import de.bytefish.jtinycsvparser.tokenizer.StringSplitTokenizer;

public class Parsers {

    public static CsvParser<EnquiryData> EnquiryDataParser()
    {
        return new CsvParser<>(new CsvParserOptions(true, new StringSplitTokenizer(",", true)), new EnquiryMapping(() -> new EnquiryData()));
    }

}

