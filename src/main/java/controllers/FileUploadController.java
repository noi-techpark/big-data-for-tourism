package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.domainmodel.UserRepository;
import controllers.storage.StorageFileNotFoundException;
import controllers.storage.StorageService;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.RandomStringUtils;
import org.elasticsearch.action.bulk.byscroll.BulkByScrollResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

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

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin")
    @Secured("ROLE_ADMIN")
    public String adminArea(Model model) throws IOException {
        String indexName = env.getProperty("es.index");

        BulkProcessorConfiguration bulkConfiguration = new BulkProcessorConfiguration(BulkProcessingOptions.builder()
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

        int maxYear = 0;
        ArrayList<String> users = new ArrayList<String>();
        Map<String, ArrayList<String>> aggs = new HashMap<String, ArrayList<String>>();

        try (TransportClient transportClient = new PreBuiltXPackTransportClient(settings)) {

            String endpoint = env.getProperty("es.endpoint");
            int port = Integer.parseInt(env.getProperty("es.port"));

            try {
                transportClient
                        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(endpoint), port));
            } catch (Exception e) {
                log.error("could not resolve es endpoint: " + endpoint + ":" + port);
            }

            List<controllers.domainmodel.User> usersList = userRepository.findAll();
            for (controllers.domainmodel.User user1 : usersList) {
                users.add(user1.getUsername());
            }

            AggregationBuilder aggregation =
                    AggregationBuilders
                            .terms("aggs")
                            .field("user");

            AggregationBuilder aggregation2 =
                    AggregationBuilders
                            .dateHistogram("aggs2")
                            .field("arrival")
                            .dateHistogramInterval(DateHistogramInterval.MONTH)
                            .format("yyyy-MM");

            SearchResponse response2 = transportClient.prepareSearch(indexName)
                    .setSource(new SearchSourceBuilder().size(0))
                    .addAggregation(aggregation.subAggregation(aggregation2))
                    .get();

            Terms agg = response2.getAggregations().get("aggs");

            for (Terms.Bucket entry : agg.getBuckets()) {
                String username = entry.getKey().toString();

                ArrayList<String> keys = new ArrayList<String>();

                Histogram agg2 = entry.getAggregations().get("aggs2");
                for (Histogram.Bucket entry2 : agg2.getBuckets()) {
                    String key = entry2.getKey().toString();

                    String[] parts = key.split("-");
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    String formattedKey = year + "-" + month;

                    keys.add(formattedKey);

                    if (year > maxYear) {
                        maxYear = year;
                    }
                }

                aggs.put(username, keys);
            }

        }

        ArrayList<String> months = new ArrayList<String>();
        for (int i = 0; i <= 11; i++) {
            months.add(new DateFormatSymbols(new Locale("en", "GB")).getShortMonths()[i]);
        }

        model.addAttribute("users", users);
        model.addAttribute("aggs", aggs);
        model.addAttribute("maxYear", maxYear);
        model.addAttribute("shortMonths", months);

        return "adminArea";
    }

    @RequestMapping(value = "/addUsers", method = RequestMethod.POST)
    @Secured("ROLE_ADMIN")
    public String addUsers(RedirectAttributes redirectAttributes,
                                 @RequestParam("email[]") String[] emails) {

        for (String email :
                emails) {
            String username = UUID.randomUUID().toString().replace("-", "");
            String password = RandomStringUtils.randomAlphabetic(20);
            byte[] hash = stringToMD5(password);
            String encryptedPassword = new String(Hex.encodeHex(hash));

            userRepository.create(username, encryptedPassword, email, "ROLE_USER", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

            log.info("new user: " + username);

            try {
                Email simpleMail = new Email();
                simpleMail.addRecipient("", email, Message.RecipientType.TO);
                simpleMail.setSubject("Tourism Data Collector: Your username and password");
                simpleMail.setText("Hi there!\n\nYour username: " + username + "\n\nYour password: " + password + "\n\nCheers");
                new Mailer().sendMail(simpleMail);
            } catch(Exception e) {
                log.error(e.getMessage());
            }
        }

        redirectAttributes.addFlashAttribute("message", "<small>Congratulations! The user was successfully created</small><br/><br/>");

        return "redirect:/admin/";
    }

    @GetMapping("/admin/deleteUser/{username}")
    @Secured("ROLE_ADMIN")
    public String deleteUser(RedirectAttributes redirectAttributes,
                                 @PathVariable String username) {

        userRepository.delete(username);

        log.info("user deleted: " + username);

        redirectAttributes.addFlashAttribute("message", "<small>Congratulations! The user was successfully deleted</small><br/><br/>");

        return "redirect:/admin/";
    }

    @GetMapping("/admin/setNewPasswordForUser/{username}")
    @Secured("ROLE_ADMIN")
    public String setNewPasswordForUser(RedirectAttributes redirectAttributes,
                             @PathVariable String username) {

        try {
            controllers.domainmodel.User user = userRepository.findByUsername(username);

            String email = user.getEmail();
            String password = RandomStringUtils.randomAlphabetic(20);
            byte[] hash = stringToMD5(password);
            String encryptedPassword = new String(Hex.encodeHex(hash));

            userRepository.updatePasswordByUsername(username, encryptedPassword);

            Email simpleMail = new Email();
            simpleMail.addRecipient("", email, Message.RecipientType.TO);
            simpleMail.setSubject("Tourism Data Collector: Your new password");
            simpleMail.setText("Hi there!\n\nYour username: " + username + "\n\nYour password: " + password + "\n\nCheers");
            new Mailer().sendMail(simpleMail);

            log.info("set a new password for: " + username);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        redirectAttributes.addFlashAttribute("message", "<small>Yay! A new password has been sent to " + username + "</small><br/><br/>");

        return "redirect:/admin/";
    }

    @GetMapping("/admin/notifyUser/{username}/{year}/{month}")
    @Secured("ROLE_ADMIN")
    public String notifyUser(RedirectAttributes redirectAttributes,
                             @PathVariable String username,
                             @PathVariable int year,
                             @PathVariable int month) {

        controllers.domainmodel.User user = userRepository.findByUsername(username);

        String email = user.getEmail();
        String shortMonth = new DateFormatSymbols(new Locale("en", "GB")).getShortMonths()[month - 1];

        Email simpleMail = new Email();
        simpleMail.addRecipient("", email, Message.RecipientType.TO);
        simpleMail.setSubject("Tourism Data Collector: Missing data for " + year + "/" + shortMonth);
        simpleMail.setText("Hi!\n\nPlease upload your data for " + year + "/" + shortMonth + "!\n\nCheers");
        new Mailer().sendMail(simpleMail);

        log.info("notify " + username + " for missing data");

        redirectAttributes.addFlashAttribute("message", "<small>The user " + username + " has been informed about missing data</small><br/><br/>");

        return "redirect:/admin/";
    }

    private byte[] stringToMD5(String value) {
        byte[] hash = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            InputStream stream = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
            DigestInputStream inputStream = new DigestInputStream(stream, md5);
            while (inputStream.read() != -1);
            hash = md5.digest();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return hash;
    }

    @GetMapping("/delete/{uniqueKey:.+}")
    public String deleteUploadedFiles(@PathVariable String uniqueKey, RedirectAttributes redirectAttributes) throws IOException {
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

        redirectAttributes.addFlashAttribute("message", "<small>Dataset was successfully deleted (" + deleted + " rows).</small><br/><br/>");

        return "redirect:/";
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username = ((User) principal).getUsername();

        String indexName = env.getProperty("es.index");

        BulkProcessorConfiguration bulkConfiguration = new BulkProcessorConfiguration(BulkProcessingOptions.builder()
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

        Map<String, String> uniqueKeys = new HashMap<>();

        try (TransportClient transportClient = new PreBuiltXPackTransportClient(settings)) {

            String endpoint = env.getProperty("es.endpoint");
            int port = Integer.parseInt(env.getProperty("es.port"));

            try {
                transportClient
                        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(endpoint), port));
            } catch (Exception e) {
                log.error("could not resolve es endpoint: " + endpoint + ":" + port);
            }

            AggregationBuilder aggregation =
                    AggregationBuilders
                            .filter("aggs", QueryBuilders.termQuery("user", username));

            AggregationBuilder aggregation2 =
                    AggregationBuilders
                            .terms("aggs2")
                            .field("unique_key.keyword")
                            .order(Terms.Order.aggregation("aggs3", false));

            AggregationBuilder aggregation3 =
                    AggregationBuilders
                            .max("aggs3")
                            .field("uploaded_on");

            SearchResponse response2 = transportClient.prepareSearch(indexName)
                    .setSource(new SearchSourceBuilder().size(0))
                    .addAggregation(aggregation.subAggregation(aggregation2.subAggregation(aggregation3)))
                    .get();

            Filter agg = response2.getAggregations().get("aggs");
            Terms agg2 = agg.getAggregations().get("aggs2");
            for (Terms.Bucket entry2 : agg2.getBuckets()) {
                String uniqueKey = entry2.getKey().toString();
                Long uploadedOn = new Long(0);
                try {
                    uploadedOn = new BigDecimal(entry2.getAggregations().get("aggs3").getProperty("value").toString()).longValue();
                } catch(NumberFormatException e) {
                    log.error(e.getMessage());
                }
                LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(uploadedOn), ZoneId.systemDefault());
                String formattedDt = dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

                uniqueKeys.put(uniqueKey, formattedDt);
            }

            log.info("sets: " + uniqueKeys.toString());
        }

        model.addAttribute("uniqueKeys", uniqueKeys);

        return "uploadForm";
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public String handleFileUpload(@RequestParam("file") MultipartFile file2,
                                   RedirectAttributes redirectAttributes,
                                   @RequestParam("email") String recipientAddress,
                                   HttpServletRequest request) {

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username = ((User) principal).getUsername();

        String lineSeparator = System.getProperty("line.separator");

        Options options = new Options('"', '\\', ',');

        RFC4180Tokenizer tokenizer = new RFC4180Tokenizer(options);

        CsvParserOptions options3 = new CsvParserOptions(false, tokenizer);

        EnquiryMapping mapping3 = new EnquiryMapping(() -> new EnquiryData());

        CsvParser<EnquiryData> parser3 = new CsvParser<>(options3, mapping3);

        List<String> list = new ArrayList<String>();

        Set<ByteBuffer> hashCodes = new HashSet<ByteBuffer>();

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
                    byte[] hashCodeBytes = hashCode.getBytes("UTF-8");
                    if(hashCodes.contains(ByteBuffer.wrap(hashCodeBytes))) {
                        throw new Exception("hashCode '" + hashCode + "' does already exist");
                    }

                    hashCodes.add(ByteBuffer.wrap(hashCodeBytes));
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
                LocalDateTime uploadedOn = LocalDateTime.now().withNano(0);

                String message = "your data has been stored";

                list.add(message);
                log.info(message);

                try {
                    InputStream fis2 = file2.getInputStream();
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(fis2, "UTF8"));
                    line = reader2.readLine();

                    while(line != null) {
                        List<CsvMappingResult<EnquiryData>> result3 = parser3.readFromString(line, new CsvReaderOptions(lineSeparator)).collect(Collectors.toList());

                        client.index(EnquiryDataConverter.convert(result3.get(0).getResult(), uniqueKey, username, uploadedOn));

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

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("message", result);

        return objectNode.toString();
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/lost", method = RequestMethod.GET)
    public String lostForm() throws IOException {
        return "lost";
    }

    @RequestMapping(value = "/lost", method = RequestMethod.POST)
    public String resetPassword(RedirectAttributes redirectAttributes,
                           @RequestParam("email") String email) {
        try {
            controllers.domainmodel.User user = userRepository.findByEmail(email);

            String username = user.getUsername();
            String password = RandomStringUtils.randomAlphabetic(20);
            byte[] hash = stringToMD5(password);
            String encryptedPassword = new String(Hex.encodeHex(hash));

            userRepository.updatePasswordByUsername(username, encryptedPassword);

            Email simpleMail = new Email();
            simpleMail.addRecipient("", email, Message.RecipientType.TO);
            simpleMail.setSubject("Tourism Data Collector: Your new password");
            simpleMail.setText("Hi there!\n\nYour username: " + username + "\n\nYour password: " + password + "\n\nCheers");
            new Mailer().sendMail(simpleMail);

            log.info("set a new password for: " + username);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        redirectAttributes.addFlashAttribute("message", "<small>Yay! A new password has been sent to you</small><br/><br/>");

        return "redirect:/lost";
    }

}
