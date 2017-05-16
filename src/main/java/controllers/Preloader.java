package controllers;

import csv.mapping.CountryMapping;
import csv.mapping.MunicipalityMapping;
import csv.model.CountryData;
import csv.model.MunicipalityData;
import de.bytefish.jtinycsvparser.CsvParser;
import de.bytefish.jtinycsvparser.CsvParserOptions;
import de.bytefish.jtinycsvparser.mapping.CsvMappingResult;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Preloader {
    public static List<CsvMappingResult<CountryData>> countries;
    public static List<CsvMappingResult<MunicipalityData>> municipalities;

    public void Preloader() { }

    public static void loadCountries() throws FileNotFoundException {
        CsvParserOptions options = new CsvParserOptions(false, ",");

        CountryMapping mapping = new CountryMapping(() -> new CountryData());

        CsvParser<CountryData> parser = new CsvParser<>(options, mapping);

        File countriesFile = null;
        Path csvFile = null;
        try {
            countriesFile = ResourceUtils.getFile("classpath:files/countries-latlong.csv");
            csvFile = countriesFile.toPath();
        } catch (FileNotFoundException e) {
            try {
                countriesFile = ResourceUtils.getFile("src/main/resources/files/countries-latlong.csv");
                csvFile = countriesFile.toPath();
            } catch (FileNotFoundException e2) {
                throw new FileNotFoundException("file countries-latlong.csv not found");
            }
        }

        // List<CsvMappingResult<CountryData>> result;

        try (Stream<CsvMappingResult<CountryData>> stream = parser.readFromFile(csvFile, StandardCharsets.UTF_8)) {
            countries = stream.collect(Collectors.toList());
        }

        // CountryData country0 = countries.get(0).getResult();
        // System.out.println(country0.getName());
    }

    public static void loadMunicipalities() throws FileNotFoundException {
        CsvParserOptions options2 = new CsvParserOptions(false, ",");

        MunicipalityMapping mapping2 = new MunicipalityMapping(() -> new MunicipalityData());

        CsvParser<MunicipalityData> parser2 = new CsvParser<>(options2, mapping2);

        File municipalitiesFile = null;
        Path csvFile2 = null;
        try {
            municipalitiesFile = ResourceUtils.getFile("classpath:files/municipalities-latlong.csv");
            csvFile2 = municipalitiesFile.toPath();
        } catch (FileNotFoundException e) {
            try {
                municipalitiesFile = ResourceUtils.getFile("src/main/resources/files/municipalities-latlong.csv");
                csvFile2 = municipalitiesFile.toPath();
            } catch (FileNotFoundException e2) {
                throw new FileNotFoundException("file municipalities-latlong.csv not found");
            }
        }

        // List<CsvMappingResult<MunicipalityData>> result2;

        try (Stream<CsvMappingResult<MunicipalityData>> stream2 = parser2.readFromFile(csvFile2, StandardCharsets.UTF_8)) {
            municipalities = stream2.collect(Collectors.toList());
        }

        // MunicipalityData municipality0 = municipalities.get(0).getResult();
        // System.out.println(municipality0.getName());
    }
}
