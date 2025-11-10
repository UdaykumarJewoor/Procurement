package com.pradeepit.pit_client_service.util;

import com.pradeepit.pit_client_service.model.TrashFile;
import com.pradeepit.pit_client_service.repository.TrashFileRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FileStorageService {

    // Base directory for file uploads
    public static final String BASE_DIR = System.getProperty("user.dir") + "/src/main/resources/Media/";
    @Autowired
    private TrashFileRepository trashFileRepository;
    @Autowired
    private IdGenerationService idGenerationService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    @Value("${trash.timeout}")
    private int trashTimeout;

    @Value("${trash.age-in-days}")
    private int trashAgeInDays;

    @Value("${trash.cleanup-interval}")
    private int cleanupInterval;


    @PostConstruct
    public void initializeServices() {
        prepareDirectories(); // Ensure directories are created
        startTrashWatcher(); // Start trash folder monitoring
        scheduleTrashCleanup(); // Schedule periodic cleanup
    }

    /**
     * Saves the file in the specified directory.
     *
     * @param file      The file to be saved
     * @param directory The directory where the file should be stored
     * @return The unique file name generated for the saved file
     * @throws IOException If any error occurs while saving the file
     */
    public String saveFile(MultipartFile file, String directory, String identifier) throws IOException {
        // Generate a unique filename by appending the identifier and the original file name
        String uniqueFileName = identifier + "_" + file.getOriginalFilename();

        // Construct the full directory path
        Path directoryPath = Paths.get(BASE_DIR + directory);

        // Ensure the directory exists
        Files.createDirectories(directoryPath);

        // Create the destination path for the file
        Path destinationPath = directoryPath.resolve(uniqueFileName);

        // Save the file to the specified directory
        file.transferTo(destinationPath.toFile());

        log.info("File successfully saved at: {}", destinationPath);

        // Return the generated file name to store it in the database
        return uniqueFileName;
    }


    /**
     * Prepares the directories for file uploads.
     * This ensures that required directories are created if they don't exist.
     */
    public void prepareDirectories() {
        // Define the subdirectories inside "Media"
        String[] directories = {
                "clients/files",     // For clients documents (GST, TAN)
                "clients/logos",     // For clients logos
                "customer/images",
                "candidates/images",
                "candidates/resumes",
                "candidates/files",
                "job/files",
                "trash"
        };

        // Loop through each directory and create it if it doesn't exist
        for (String dir : directories) {
            Path dirPath = Paths.get(BASE_DIR + dir);
            if (Files.notExists(dirPath)) {
                try {
                    Files.createDirectories(dirPath);
                    log.info("{} directory created: {}", dir, dirPath);
                } catch (IOException e) {
                    log.error("Failed to create {} directory: {}", dir, dirPath, e);
                }
            }
        }
    }

    /**
     * Utility method to determine the storage path based on the type of file.
     *
     * @param fileType The type of the file
     * @return The appropriate directory to store the file
     */
    public String getDirectoryForFileType(String fileType) {
        return switch (fileType) {
            case "client-logo" -> "clients/logos"; // Store clients logos
            case "client-gst", "client-tan" -> "clients/files"; // Store clients documents (GST/TAN)
            case "customer-image" -> "customer/images"; // Customer images
            case "candidate-image" -> "candidates/images"; // Store candidates images
            case "candidate-resume" -> "candidates/resumes"; // Store candidates resumes
            case "candidate-files" -> "candidates/files"; // Store candidates files (AADHAAR, PAN)
            case "job-files" -> "job/files"; // Store job files (offer letter, notice, etc.)
            case "trash" -> "trash";
            default -> "documents";
        };
    }


    /**
     * Moves the specified file to the trash.
     * The source path is dynamically determined based on the file type.
     *
     * @param fileName The name of the file to move to trash
     * @param fileType The type of the file (e.g., "job-file", "client-logo", etc.)
     * @throws IOException If any error occurs while moving the file
     */
    public void moveToTrash(String fileName, String fileType) throws IOException {
        if (fileName != null && !fileName.isEmpty()) {
            String directory = getDirectoryForFileType(fileType);
            Path sourcePath = Paths.get(BASE_DIR, directory, fileName);

            if (Files.exists(sourcePath)) {
                Path trashPath = Paths.get(BASE_DIR, "trash", fileName);
                Files.createDirectories(trashPath.getParent());
                Files.move(sourcePath, trashPath);

                log.info("Moved old file to trash: {}", trashPath);

                // Save file details to the database
                TrashFile trashFile = new TrashFile();
                trashFile.setId(idGenerationService.generateTrashFileId());
                trashFile.setFileName(fileName);
                trashFile.setFileType(fileType);
                trashFile.setOriginalPath(sourcePath.toString());
                trashFile.setTrashPath(trashPath.toString());
                trashFile.setMovedToTrashAt(LocalDateTime.now());
                trashFileRepository.save(trashFile);
            } else {
                log.warn("File not found, unable to move to trash: {}", sourcePath);
            }
        } else {
            log.warn("Invalid file name provided, unable to move to trash.");
        }
    }

    /**
     * Deletes the specified file.
     *
     * @param filePath Path of the file to delete
     */
    private void deleteFile(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                // Delete the file from the filesystem
                Files.delete(filePath);
                log.info("Deleted file from trash: {}", filePath);

                // Remove the file entry from the database
                String trashPathString = filePath.toString();
                trashFileRepository.deleteByTrashPath(trashPathString);
                log.info("Deleted file entry from the database: {}", trashPathString);
            } else {
                log.warn("File already deleted or not found: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
        }
    }

    /**
     * Periodically checks for files in the trash that are older than the configured lifetime
     * and deletes them from both the filesystem and the database.
     */
    private void scheduleTrashCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
//                log.info("Starting periodic trash cleanup...");
                cleanupTrash();
            } catch (Exception e) {
                log.error("Error during periodic trash cleanup", e);
            }
        }, 0, cleanupInterval, TimeUnit.SECONDS); // Run every 10 seconds
    }

    /**
     * Cleans up files in the trash folder whose `movedToTrashAt` timestamp
     * in the database is older than the configured threshold.
     */
    public void cleanupTrash() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(trashAgeInDays);
        List<TrashFile> oldTrashFiles = trashFileRepository.findAllByMovedToTrashAtBefore(cutoffTime);

        for (TrashFile trashFile : oldTrashFiles) {
            Path trashPath = Paths.get(trashFile.getTrashPath());
            try {
                if (Files.exists(trashPath)) {
                    Files.delete(trashPath);
                    log.info("Deleted file from trash: {}", trashPath);
                } else {
                    log.warn("File not found in trash: {}", trashPath);
                }

                trashFileRepository.delete(trashFile);
                log.info("Deleted file entry from database: {}", trashFile);
            } catch (IOException e) {
                log.error("Failed to delete file: {}", trashPath, e);
            }
        }
    }

    /**
     * Start monitoring the trash directory for changes.
     */
    public void startTrashWatcher() {
        Path trashDirectory = Paths.get(BASE_DIR, "trash");

        if (!Files.exists(trashDirectory)) {
            log.warn("Trash directory does not exist: {}", trashDirectory);
            return;
        }

        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            trashDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            log.info("Started watching trash directory: {}", trashDirectory);

            Executors.newSingleThreadExecutor().execute(() -> {
                while (true) {
                    try {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                Path newFilePath = trashDirectory.resolve((Path) event.context());
                                log.info("New file detected in trash: {}", newFilePath);

                                scheduler.schedule(() -> deleteFile(newFilePath), trashTimeout, TimeUnit.SECONDS);
                            }
                        }
                        key.reset();
                    } catch (InterruptedException e) {
                        log.info("Trash watcher interrupted.", e);
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("Error while watching trash directory.", e);
                    }
                }
            });
        } catch (IOException e) {
            log.error("Failed to start trash watcher.", e);
        }
    }
}
