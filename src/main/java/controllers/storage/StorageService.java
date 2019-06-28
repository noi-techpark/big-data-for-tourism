package controllers.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface StorageService {

    void init();

    void store(MultipartFile file, String location);

    void move(String filename, String location);

    List<Map<String, String>> loadAll(String location);

    Resource loadAsResource(String filename);

}
