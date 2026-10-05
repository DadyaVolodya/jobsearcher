package ru.fsp.jobsearcher.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.stereotype.Component;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.api.common.ErrorCode;
import ru.fsp.jobsearcher.infrastructure.config.AppProperties;

@Component
public class LocalStorageAdapter implements StoragePort {

    private final Path root;

    public LocalStorageAdapter(AppProperties props) throws IOException {
        this.root = Path.of(props.storage().localRoot()).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    @Override
    public StoredObject store(String key, InputStream data, long size, String contentType) {
        try {
            Path target = root.resolve(key).normalize();
            if (!target.startsWith(root)) {
                throw new ApiException(ErrorCode.VALIDATION, org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid key");
            }
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredObject(key, Files.size(target));
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file");
        }
    }
}
