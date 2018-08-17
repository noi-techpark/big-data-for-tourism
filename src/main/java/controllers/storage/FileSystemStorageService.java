package controllers.storage;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

@Service
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation;

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public void store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file " + file.getOriginalFilename());
            }
            Path target = this.rootLocation.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), target);
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public void store(MultipartFile file, String location) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file " + file.getOriginalFilename());
            }
            String directories = this.rootLocation.toString() + location;
            Path pathLocation = Paths.get(directories);
            if (!Files.exists(pathLocation)) {
                boolean created = new File(directories).mkdirs();
                if (!created) {
                    throw new StorageException("Failed to create directory " + directories);
                }
            }
            Path target = pathLocation.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), target);
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public void move(String filename, String location) {
        File file = new File(this.rootLocation.toString() + filename);
        try {
            if (!file.exists()) {
                throw new StorageException(file.getName() + " does not exist");
            }
            String directories = this.rootLocation.toString() + location;
            Path pathLocation = Paths.get(directories);
            if (!Files.exists(pathLocation)) {
                boolean created = new File(directories).mkdirs();
                if (!created) {
                    throw new StorageException("Failed to create directory " + directories);
                }
            }
            Path target = pathLocation.resolve(file.getName());
            Files.move(Paths.get(this.rootLocation.toString() + filename), target);
        } catch (IOException e) {
            throw new StorageException("Failed to move file " + file.getName(), e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(path -> this.rootLocation.relativize(path));
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }

    }

    @Override
    public List<Map<String, String>> loadAll(String location) {
        List<Map<String, String>> list  = new ArrayList<Map<String, String>>();
        String cvsSplitBy = ",";
        try {
            String directories = this.rootLocation.toString() + location;
            Path pathLocation = Paths.get(directories);
            Files.walk(pathLocation, 1)
                    .filter(path -> !path.equals(pathLocation)).forEach(path -> {
                Map<String, String> file = new HashMap<String, String>();
                file.put("filename", path.getFileName().toString());

                file.put("firstDate", "");
                try {
                    BufferedReader br = new BufferedReader(new FileReader(path.toFile()));
                    String text = br.readLine();
                    String[] line = text.split(cvsSplitBy);
                    file.put("firstDate", line[0]);
                } catch (Exception e) { }

                file.put("lastDate", "");
                try {
                    ReversedLinesFileReader fr = new ReversedLinesFileReader(path.toFile());
                    String text = fr.readLine();
                    String[] line = text.split(cvsSplitBy);
                    file.put("lastDate", line[0]);
                } catch (Exception e) { }

                BasicFileAttributes attr;
                try {
                    attr = Files.readAttributes(path, BasicFileAttributes.class);
                } catch (IOException e) {
                    throw new StorageException("Failed to read attributes", e);
                }
                file.put("uploadedDate", new SimpleDateFormat("yyyy-MM-dd").format(attr.creationTime().toMillis()));

                list.add(file);
            });
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
        return list;

    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if(resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException("Could not read file: " + filename);

            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    @Override
    public void init() {
        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectory(rootLocation);
            }
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }
}
