package controllers;

import controllers.storage.StorageFileNotFoundException;
import controllers.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.stream.Collectors;

import converter.EnquiryDataConverter;
import csv.mapping.EnquiryMapping;
import csv.model.EnquiryData;
import de.bytefish.elasticutils.elasticsearch5.client.ElasticSearchClient;
import de.bytefish.elasticutils.elasticsearch5.client.bulk.configuration.BulkProcessorConfiguration;
import de.bytefish.elasticutils.elasticsearch5.client.bulk.options.BulkProcessingOptions;
import de.bytefish.jtinycsvparser.CsvParser;
import de.bytefish.jtinycsvparser.CsvParserOptions;
import de.bytefish.jtinycsvparser.CsvReaderOptions;
import de.bytefish.jtinycsvparser.mapping.CsvMappingResult;
import de.bytefish.jtinycsvparser.tokenizer.rfc4180.Options;
import de.bytefish.jtinycsvparser.tokenizer.rfc4180.RFC4180Tokenizer;
import elastic.mapping.EnquiryDataMapper;
import elastic.model.Enquiry;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;

import java.io.*;
import java.net.InetAddress;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class FileUploadController {

    private final StorageService storageService;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Environment env;

    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException { return "uploadForm"; }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file2,
                                   RedirectAttributes redirectAttributes) {

        String lineSeparator = System.getProperty("line.separator");

        Options options = new Options('"', '\\', ',');

        RFC4180Tokenizer tokenizer = new RFC4180Tokenizer(options);

        CsvParserOptions options3 = new CsvParserOptions(false, tokenizer);

        EnquiryMapping mapping3 = new EnquiryMapping(() -> new EnquiryData());

        CsvParser<EnquiryData> parser3 = new CsvParser<>(options3, mapping3);

        List<String> list = new ArrayList<String>();

        try {
            Preloader.loadCountries();
            Preloader.loadMunicipalities();
        } catch(FileNotFoundException e) {
            log.error(e.getMessage());
        }

        String indexName = env.getProperty("es.index");

        EnquiryDataMapper mapping4 = new EnquiryDataMapper(env.getProperty("es.type"));

        BulkProcessorConfiguration bulkConfiguration = new BulkProcessorConfiguration(BulkProcessingOptions.builder()
                .setBulkActions(1000)
                .build());

        boolean enableSsl = Boolean.parseBoolean(System.getProperty("ssl", env.getProperty("es.ssl")));
        String cluster = env.getProperty("es.cluster");
        String user = env.getProperty("es.user");

        Settings settings = Settings.builder()
                .put("client.transport.nodes_sampler_interval", "5s")
                .put("client.transport.sniff", false)
                .put("transport.tcp.compress", true)
                .put("cluster.name", cluster)
                .put("xpack.security.transport.ssl.enabled", enableSsl)
                .put("request.headers.X-Found-Cluster", cluster)
                .put("xpack.security.user", user)
                .build();

        try (TransportClient transportClient = new PreBuiltXPackTransportClient(settings)) {

            String endpoint = env.getProperty("es.endpoint");
            int port = Integer.parseInt(env.getProperty("es.port"));

            try {
                transportClient
                        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(endpoint), port));
            } catch (Exception e) {
                log.error("could not resolve es endpoint: " + endpoint + ":" + port);
            }

            // createIndex(transportClient, indexName);
            // createMapping(transportClient, indexName, mapping4);

            ElasticSearchClient<Enquiry> client = new ElasticSearchClient<>(transportClient, indexName, mapping4, bulkConfiguration);

            InputStream fis = null;
            try {
                fis = file2.getInputStream();
            } catch (IOException e) {
                log.error("failing to read from file");
            }
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(fis, "UTF8"));
            } catch (UnsupportedEncodingException e) {
                log.error(e.getMessage());
            }
            String line = null;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                log.error("failing to read line");
            }
            int lines = 1;
            int errors = 0;

            while(line != null) {
                List<CsvMappingResult<EnquiryData>> result3 = parser3.readFromString(line, new CsvReaderOptions(lineSeparator)).collect(Collectors.toList());

                try {
                    log.info(String.valueOf(result3.get(0)));

                    EnquiryData result = result3.get(0).getResult();

                    // il set comprende delle richieste o delle prenotazioni la cui data di arrivo è prima della data in cui viene effettuata la richiesta oppure la prenotazione;
                    if(result.getArrival().isBefore(result.getSubmittedOn().toLocalDate())) {
                        throw new Exception("arrival '" + result.getArrival() + "' is before submittedOn '" + result.getSubmittedOn() + "'");
                    }
                    // il set comprende delle richieste o delle prenotazioni per periodi che riguardino periodi antecedenti il primo gennaio 2015;
                    LocalDate dt = LocalDate.of(2015, 1, 1);
                    if(result.getArrival().isBefore(dt)) {
                        throw new Exception("arrival '" + result.getArrival() + "' is before 2015");
                    }
                    if(result.getDeparture().isBefore(dt)) {
                        throw new Exception("departure '" + result.getDeparture() + "' is before 2015");
                    }
                    // il set comprende richieste o delle prenotazioni per periodi della durata superiore all’anno;
                    long months = ChronoUnit.MONTHS.between(result.getArrival(), result.getDeparture());
                    if(months > 12) {
                        throw new Exception("length of stay of " + months + " months (since '" + result.getArrival() + "' until '" + result.getDeparture() + "') is greater than 12 months");
                    }
                    // il set comprende delle richieste o delle prenotazioni che vengono effettuate con più di 18 mesi di anticipo rispetto alla data di arrivo.
                    long months2 = ChronoUnit.MONTHS.between(result.getSubmittedOn().toLocalDate(), result.getArrival()); // https://www.leveluplunch.com/java/examples/number-of-months-between-two-dates/
                    if(months2 > 18) {
                        throw new Exception("the difference between submittedOn '" + result.getSubmittedOn() + "' and arrival '" + result.getArrival() + "' (" + months2 + " months) is greater than 18 months");
                    }
                } catch(Exception e) {
                    String message = "line " + lines + ": " + e.getMessage();

                    list.add(message);
                    log.warn(message);

                    errors++;
                }

                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    log.error("failing to read line");
                }
                lines++;
            }

            if(errors == 0) {
                String message = "your data has been stored";

                list.add(message);
                log.info(message);

                try {
                    InputStream fis2 = file2.getInputStream();
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(fis2, "UTF8"));
                    line = reader2.readLine();

                    while(line != null) {
                        List<CsvMappingResult<EnquiryData>> result3 = parser3.readFromString(line, new CsvReaderOptions(lineSeparator)).collect(Collectors.toList());

                        client.index(EnquiryDataConverter.convert(result3.get(0).getResult()));

                        line = reader2.readLine();
                    }
                } catch(Exception e) {
                    log.error(e.getMessage());
                }

                try {
                    client.awaitClose(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.error("could not wait for specified timeout");
                }
            }

            try {
                reader.close();
            } catch (IOException e) {
                log.error("failing to close buffered reader");
            }
            try {
                fis.close();
            } catch (IOException e) {
                log.error("failing to close input stream");
            }
        }

        redirectAttributes.addFlashAttribute("message", list.toString());

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}
