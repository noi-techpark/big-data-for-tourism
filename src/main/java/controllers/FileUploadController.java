package controllers;

import controllers.storage.StorageFileNotFoundException;
import controllers.storage.StorageService;
import org.elasticsearch.action.bulk.byscroll.BulkByScrollResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.config.ServerConfig;
import org.simplejavamail.mailer.config.TransportStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.MatchResult;
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

import javax.mail.Message;
import javax.servlet.http.HttpServletRequest;

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

    @GetMapping("/delete/{uniqueKey:.+}")
    public String deleteUploadedFiles(@PathVariable String uniqueKey, Model model) throws IOException {
        long deleted = 0;

        String indexName = env.getProperty("es.index");

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

            BulkByScrollResponse response =
                    DeleteByQueryAction.INSTANCE.newRequestBuilder(transportClient)
                            .filter(QueryBuilders.matchQuery("unique_key", uniqueKey))
                            .source(indexName)
                            .get();

            deleted = response.getDeleted();

            log.info("number of deleted documents: " + deleted);
        }

        model.addAttribute("deletedRows", deleted);

        return "deleteDataset";
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException { return "uploadForm"; }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file2,
                                   RedirectAttributes redirectAttributes,
                                   @RequestParam("email") String recipientAddress,
                                   HttpServletRequest request) {

        String lineSeparator = System.getProperty("line.separator");

        Options options = new Options('"', '\\', ',');

        RFC4180Tokenizer tokenizer = new RFC4180Tokenizer(options);

        CsvParserOptions options3 = new CsvParserOptions(false, tokenizer);

        EnquiryMapping mapping3 = new EnquiryMapping(() -> new EnquiryData());

        CsvParser<EnquiryData> parser3 = new CsvParser<>(options3, mapping3);

        List<String> list = new ArrayList<String>();

        String uniqueKey = "";

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

        int errors = 0;

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
            try {
                Scanner scan = new Scanner(line);
                scan.findInLine(",");
                MatchResult match = scan.match();
                match.groupCount();
            } catch(IllegalStateException e) {
                log.info("delimiter ',' not found, trying with ';'");

                Options options2 = new Options('"', '\\', ';');

                RFC4180Tokenizer tokenizer2 = new RFC4180Tokenizer(options2);

                CsvParserOptions options32 = new CsvParserOptions(false, tokenizer2);

                parser3 = new CsvParser<>(options32, mapping3);
            }
            int lines = 1;
            int maxErrors = 30;

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
                    // il set comprende delle richieste o delle prenotazioni che vengono effettuate con più di 36 mesi di anticipo rispetto alla data di arrivo.
                    long months2 = ChronoUnit.MONTHS.between(result.getSubmittedOn().toLocalDate(), result.getArrival()); // https://www.leveluplunch.com/java/examples/number-of-months-between-two-dates/
                    if(months2 > 36) {
                        throw new Exception("the difference between submittedOn '" + result.getSubmittedOn() + "' and arrival '" + result.getArrival() + "' (" + months2 + " months) is greater than 36 months");
                    }
                    String hashCode = result.getHash();
                    SearchResponse response = transportClient.prepareSearch(indexName)
                            .setSource(new SearchSourceBuilder().size(0).query(QueryBuilders.termQuery("hash_code", hashCode)))
                            .get();
                    if(response.getHits().getTotalHits() > 0) {
                        throw new Exception("hashCode '" + hashCode + "' does already exist");
                    }
                } catch(Exception e) {
                    String msg = e.getMessage();
                    if(null == msg) {
                        msg = "invalid data";
                        LocalDateTime dt = LocalDateTime.now();
                        switch(result3.get(0).getError().getIndex()) {
                            case 0: msg = "invalid data for arrival. Allowed date format is YYYY-MM-DD (for example: " + dt.format(DateTimeFormatter.ISO_LOCAL_DATE) + ")";
                                    break;
                            case 1: msg = "invalid data for departure. Allowed date format is YYYY-MM-DD (for example: " + dt.format(DateTimeFormatter.ISO_LOCAL_DATE) + ")";
                                    break;
                            case 2: msg = "invalid data for country. Expected data type: String";
                                    break;
                            case 3: msg = "invalid data for adults. Expected data type: Integer";
                                    break;
                            case 4: msg = "invalid data for children. Expected data type: Integer";
                                    break;
                            case 5: msg = "invalid data for destination. Expected data type: Integer";
                                    break;
                            case 6: msg = "invalid data for category. Expected data type: Integer";
                                    break;
                            case 7: msg = "invalid data for booking. Expected data type: Integer";
                                    break;
                            case 8: msg = "invalid data for cancellation. Expected data type: Integer";
                                    break;
                            case 9: msg = "invalid data for submittedOn. Allowed date format is YYYY-MM-DDThh:mm:ss (for example: " + dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ")";
                                    break;
                        }
                    }
                    String message = "Line " + lines + ": " + msg;

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

                if(errors >= maxErrors) { // display only the first x errors
                    String message = "more than " + maxErrors + " errors cannot be viewed at once";

                    list.add(message);
                    log.info(message);

                    break;
                }
            }

            if(errors == 0) {
                uniqueKey = UUID.randomUUID().toString();

                String message = "your data has been stored";

                list.add(message);
                log.info(message);

                try {
                    InputStream fis2 = file2.getInputStream();
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(fis2, "UTF8"));
                    line = reader2.readLine();

                    while(line != null) {
                        List<CsvMappingResult<EnquiryData>> result3 = parser3.readFromString(line, new CsvReaderOptions(lineSeparator)).collect(Collectors.toList());

                        client.index(EnquiryDataConverter.convert(result3.get(0).getResult(), uniqueKey));

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

                if(!recipientAddress.isEmpty()) {
                    String url = request.getRequestURL().toString();
                    try {
                        Email simpleMail = new Email();
                        simpleMail.addRecipient("", recipientAddress, Message.RecipientType.TO);
                        simpleMail.setText("Hi there!\n\nThank you for uploading your data. Your dataset ID is " + uniqueKey + ".\n\nOpen the following link to delete all your records:\n\n" + url + "delete/" + uniqueKey + "\n\nCheers");
                        new Mailer().sendMail(simpleMail);
                    } catch(Exception e) {
                        log.error(e.getMessage());
                    }
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

        String result = "";
        if(errors == 0) {
            result = "<h1>Congratulations! Your data has been stored</h1>";
            result += "<div>";
            result += "Now you can sit back and enjoy a coffee. Please record your dataset ID: " + uniqueKey;
            result += "</div>";
        } else {
            result = "<h1>Oops! Your data has NOT been stored</h1>";
            result += "<div>";
            result += "One or more errors occurred while parsing your data. Please correct and reupload your file:<br/><br/>";
            result += "<ul>";
            for (String item : list) {
                result += "<li><small>" + item + "</small></li>";
            }
            result += "</ul>";
            result += "</div>";
        }

        redirectAttributes.addFlashAttribute("message", result);

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}
