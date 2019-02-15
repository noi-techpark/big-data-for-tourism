package controllers.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface StorageService {

    void init();

    //void store(MultipartFile file);

    void store(MultipartFile file, String location);

    void move(String filename, String location);

    //Stream<Path> loadAll();

    List<Map<String, String>> loadAll(String location);

    //Path load(String filename);

    //Resource loadAsResource(String filename);

    //void deleteAll();

}
