package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.domainmodel.UserRepository;
import controllers.storage.StorageFileNotFoundException;
import controllers.storage.StorageService;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.RandomStringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.Message;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

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
    public String adminArea(Model model) {
        ArrayList<String> users = new ArrayList<>();

        List<controllers.domainmodel.User> usersList = userRepository.findAll();
        for (controllers.domainmodel.User user1 : usersList) {
            users.add(user1.getUsername());
        }

        this.usersActivity(model, users);

        return "adminArea";
    }

    private void usersActivity(Model model, ArrayList<String> users) {
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

        int maxYear = 0;
        Map<String, Map<String, String>> aggs = new HashMap<>();

        try (TransportClient transportClient = new PreBuiltXPackTransportClient(settings)) {

            String endpoint = env.getProperty("es.endpoint");
            int port = Integer.parseInt(env.getProperty("es.port"));

            try {
                transportClient
                        .addTransportAddress(new TransportAddress(InetAddress.getByName(endpoint), port));
            } catch (Exception e) {
                log.error("could not resolve es endpoint: " + endpoint + ":" + port);
            }

            AggregationBuilder aggregation =
                    AggregationBuilders
                            .terms("aggs")
                            .field("user");

            AggregationBuilder aggregation2 =
                    AggregationBuilders
                            .dateHistogram("aggs2")
                            .field("submitted_on")
                            .dateHistogramInterval(DateHistogramInterval.MONTH)
                            .format("yyyy-MM");

            SearchResponse response2 = transportClient.prepareSearch(indexName)
                    .setSource(new SearchSourceBuilder().size(0))
                    .addAggregation(aggregation.subAggregation(aggregation2))
                    .get();

            Terms agg = response2.getAggregations().get("aggs");

            for (Terms.Bucket entry : agg.getBuckets()) {
                String username = entry.getKey().toString();

                Map<String, String> keys = new HashMap<>();

                Histogram agg2 = entry.getAggregations().get("aggs2");
                for (Histogram.Bucket entry2 : agg2.getBuckets()) {
                    String key = entry2.getKey().toString();

                    String[] parts = key.split("-");
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    String formattedKey = year + "-" + month;

                    keys.put(formattedKey, Long.toString(entry2.getDocCount()));

                    if (year > maxYear) {
                        maxYear = year;
                    }
                }

                if (1 == users.size()) {
                    if (users.contains(username)) {
                        aggs.put(username, keys);
                    }
                } else {
                    aggs.put(username, keys);
                }
            }

        }

        ArrayList<String> months = new ArrayList<>();
        for (int i = 0; i <= 11; i++) {
            months.add(new DateFormatSymbols(new Locale("en", "GB")).getShortMonths()[i]);
        }

        model.addAttribute("users", users);
        model.addAttribute("aggs", aggs);
        model.addAttribute("maxYear", maxYear);
        model.addAttribute("shortMonths", months);
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

    @GetMapping("/del/{filename:.+}")
    public String deleteUploadedFile(@PathVariable String filename, RedirectAttributes redirectAttributes) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username = ((User) principal).getUsername();

        storageService.move("/processed/new/" + username + "/" + filename, "/delete/" + username);

        redirectAttributes.addFlashAttribute("message", "<small>Dataset was successfully deleted.</small><br/><br/>");

        return "redirect:/";
    }

    @GetMapping("/report/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> showReport(@PathVariable String filename) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username = ((User) principal).getUsername();

        Resource file = storageService.loadAsResource("/processed/new/" + username + "/" + filename);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
        //headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"");

        return ResponseEntity.ok().headers(headers).body(file);
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username = ((User) principal).getUsername();

        List<Map<String, String>> files = new ArrayList<>();
        try {
            files = storageService.loadAll("/processed/new/" + username);
        } catch(Exception e) {
            log.info(e.getMessage());
        }
        model.addAttribute("files", files);

        ArrayList<String> users = new ArrayList<>();
        users.add(username);

        this.usersActivity(model, users);

        return "uploadForm";
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public String handleFileUpload(@RequestParam("file") MultipartFile file) {

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username = ((User) principal).getUsername();

        int errors = 0;
        storageService.store(file, "/new/" + username);

        String result;
        result = "<h1>Congratulations! Your data has been stored</h1>";
        result += "<div>";
        result += "Now you can sit back and enjoy a coffee.";
        result += "</div>";

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("message", result);

        Map<String, String> f = storageService.loadSingleFile("/processed/new/" + username + "/" + file.getOriginalFilename());

        String append = "";
        if (!f.get("notValidRows").equals("0")) {
            append += "<span class=\"stats\">(<span>" + f.get("notValidRows") + "</span>/<span>" + f.get("totalRows") + "</span>)</span>";
            append += " <a class=\"info-link\" target=\"_blank\" href=\"/report/" + f.get("filenameReport") + "\"><i class=\"material-icons\">info</i></a>";
        }

        objectNode.put("row", "<tr>" +
                "<td width=\"25%\">" + f.get("filenameShorten") + "</td>" +
                "<td width=\"25%\" align=\"center\">" + f.get("uploadedDate") + "</td>" +
                "<td width=\"25%\" align=\"center\">" +
                    "<i class=\"material-icons " + f.get("status") + "\">" + f.get("status") + "</i> " + append +
                "</td>" +
                "<td width=\"25%\" align=\"center\"><a href=\"/del/" + f.get("filename") + "\" onclick=\"return confirm('Are you sure to delete this set?')\">Delete set</a></td>" +
                "</tr>");

        return objectNode.toString();
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/lost", method = RequestMethod.GET)
    public String lostForm() {
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
