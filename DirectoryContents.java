import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * This class provides functionality to generate a text representation of a directory's contents,
 * including file structure and file contents. It can be configured using a properties file to
 * exclude certain directories, file extensions, or specific files.
 */
public class DirectoryContents {
    /** The default name of the properties file to be used for configuration. */
    private static final String DEFAULT_PROPERTIES_FILE = "config.properties";
    
    /** The maximum file size (in bytes) that will be included in the output. Default is 1 MB. */
    private static long MAX_FILE_SIZE = 1024 * 1024; // 1 MB default
    
    /** Set of directory names to be excluded from processing. */
    private static Set<String> EXCLUDED_DIRS = new HashSet<>();
    
    /** Set of file extensions to be excluded from processing. */
    private static Set<String> EXCLUDED_EXTENSIONS = new HashSet<>();
    
    /** Set of specific file names to be excluded from processing. */
    private static Set<String> EXCLUDED_FILES = new HashSet<>();

    /**
     * The main entry point of the program.
     * 
     * @param args Command line arguments. If provided, the first argument is used as the target directory,
     *             and the second argument (if present) is used as the properties file name.
     */
    public static void main(String[] args) {
        String targetDirectory = System.getProperty("user.dir");
        String propertiesFile = DEFAULT_PROPERTIES_FILE;

        // Parse command-line arguments
        if (args.length > 0) {
            targetDirectory = args[0];
            if (args.length > 1) {                
                propertiesFile = args[1];
            }
        }

        // Check if the target directory exists and is a directory
        Path targetPath = Paths.get(targetDirectory);
        if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
            System.out.println("Error: The specified target directory does not exist or is not a directory.");
            System.exit(1);
        }

        try {
            loadProperties(propertiesFile);
            String contents = getDirectoryContents(targetDirectory);
            String outputFilename = targetPath.getFileName() + "_contents.txt";
            Path outputPath = targetPath.resolve(outputFilename);
            Files.write(outputPath, contents.getBytes(StandardCharsets.UTF_8));
            System.out.println("Directory contents saved to '" + outputPath + "'.");
        } catch (IOException e) {
            System.out.println("An IO error occurred: " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println("A security error occurred: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Loads configuration properties from the specified file.
     * 
     * @param propertiesFile The name of the properties file to load.
     * @throws IOException If an error occurs while reading the properties file.
     */
    private static void loadProperties(String propertiesFile) throws IOException {
        Properties props = new Properties();
        Path propPath = Paths.get(propertiesFile);
        if (Files.exists(propPath)) {
            try (InputStream input = Files.newInputStream(propPath)) {
                props.load(input);
                MAX_FILE_SIZE = Long.parseLong(props.getProperty("max.file.size", String.valueOf(MAX_FILE_SIZE)).trim());
                addNonEmptyValues(EXCLUDED_DIRS, props.getProperty("excluded.dirs", ""));
                addNonEmptyValues(EXCLUDED_EXTENSIONS, props.getProperty("excluded.extensions", ""));
                addNonEmptyValues(EXCLUDED_FILES, props.getProperty("excluded.files", ""));
            }
        } else {
            System.out.println("Properties file not found. Using default values.");
        }
    }

    /**
     * Adds non-empty, trimmed values from a comma-separated string to a Set.
     * 
     * @param set The Set to add values to.
     * @param values A comma-separated string of values.
     */
    private static void addNonEmptyValues(Set<String> set, String values) {
        for (String value : values.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
    }

    /**
     * Generates a string representation of the contents of the specified directory.
     * 
     * @param directory The path of the directory to process.
     * @return A string containing the directory structure and file contents.
     * @throws IOException If an error occurs while reading the directory or its contents.
     */
    private static String getDirectoryContents(String directory) throws IOException {
        if (directory == null) {
            throw new IllegalArgumentException("Directory cannot be null");
        }
        Path directoryPath = Paths.get(directory);
        String directoryName = directoryPath.getFileName().toString();

        StringBuilder sb = new StringBuilder();
        sb.append(getHeader());
        sb.append(getInstructions(directoryName));
        
        System.out.println("\nFetching directory structure for: " + directoryName);
        sb.append("Directory Structure: ").append(directoryName).append("\n");
        sb.append(traverseDirectoryIteratively(directoryPath));
        
        System.out.println("\nFetching file contents for: " + directoryName);
        sb.append("\n\n").append(getFileContentsIteratively(directoryPath));

        return sb.toString();
    }

    /**
     * Traverses the directory structure iteratively and generates a string representation.
     * 
     * @param directory The root directory to traverse.
     * @return A string representation of the directory structure.
     * @throws IOException If an error occurs while reading the directory contents.
     */
    private static String traverseDirectoryIteratively(Path directory) throws IOException {
        StringBuilder structure = new StringBuilder();
        Deque<Pair<Path, Path>> dirsToVisit = new ArrayDeque<>();
        dirsToVisit.push(new Pair<>(Paths.get(""), directory));

        while (!dirsToVisit.isEmpty()) {
            Pair<Path, Path> pair = dirsToVisit.pop();
            Path path = pair.getFirst();
            Path currentDir = pair.getSecond();

            if (EXCLUDED_DIRS.contains(currentDir.getFileName().toString())) {
                continue;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir)) {
                List<Path> items = new ArrayList<>();
                for (Path entry : stream) {
                    items.add(entry);
                }
                items.sort(Comparator.comparing(Path::toString));

                for (Path item : items) {
                    Path relPath = path.resolve(item.getFileName());
                    if (Files.isDirectory(item)) {
                        structure.append(relPath).append("/\n");
                        dirsToVisit.push(new Pair<>(relPath, item));
                    } else if (!shouldExcludeFile(item)) {
                        structure.append(relPath).append("\n");
                    }
                }
            } catch (AccessDeniedException e) {
                structure.append(path).append(": Permission denied\n");
            }
        }
        return structure.toString();
    }

    /**
     * Determines whether a file should be excluded based on its name or extension.
     * 
     * @param file The file to check.
     * @return true if the file should be excluded, false otherwise.
     */
    private static boolean shouldExcludeFile(Path file) {
        String fileName = file.getFileName().toString();
        if (EXCLUDED_FILES.contains(fileName)) {
            return true;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            String extension = fileName.substring(dotIndex + 1);
            return EXCLUDED_EXTENSIONS.contains(extension);
        }
        return false;
    }

    /**
     * Checks if a file is likely to be a binary file by examining its contents.
     * 
     * @param filePath The path of the file to check.
     * @return true if the file is likely to be binary, false otherwise.
     * @throws IOException If an error occurs while reading the file.
     */
    private static boolean isBinaryFile(Path filePath) throws IOException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(filePath))) {
            byte[] buff = new byte[64];
            int bytesRead = in.read(buff);
            for (int i = 0; i < bytesRead; i++) {
                if (buff[i] == 0) return true;
            }
            return false;
        }
    }

    /**
     * Iteratively reads the contents of all files in the given directory and its subdirectories.
     * 
     * @param directory The root directory to process.
     * @return A string containing the contents of all processed files.
     * @throws IOException If an error occurs while reading the directory or file contents.
     */
    private static String getFileContentsIteratively(Path directory) throws IOException {
        StringBuilder fileContents = new StringBuilder();
        Deque<Pair<Path, Path>> dirsToVisit = new ArrayDeque<>();
        dirsToVisit.push(new Pair<>(Paths.get(""), directory));

        while (!dirsToVisit.isEmpty()) {
            Pair<Path, Path> pair = dirsToVisit.pop();
            Path path = pair.getFirst();
            Path currentDir = pair.getSecond();

            if (EXCLUDED_DIRS.contains(currentDir.getFileName().toString())) {
                continue;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir)) {
                List<Path> items = new ArrayList<>();
                for (Path entry : stream) {
                    items.add(entry);
                }
                items.sort(Comparator.comparing(Path::toString));

                for (Path item : items) {
                    Path relPath = path.resolve(item.getFileName());
                    if (Files.isDirectory(item)) {
                        dirsToVisit.push(new Pair<>(relPath, item));
                    } else if (!shouldExcludeFile(item)) {
                        fileContents.append("================\nFile: ").append(relPath).append("\n================\n");
                        long fileSize = Files.size(item);
                        if (fileSize > MAX_FILE_SIZE) {
                            fileContents.append(String.format("Content: Skipped (file size: %.2f MB, max allowed: %.2f MB)\n\n", 
                                                              fileSize / (1024.0 * 1024.0), MAX_FILE_SIZE / (1024.0 * 1024.0)));
                        } else if (isBinaryFile(item)) {
                            fileContents.append("Content: Skipped binary file\n\n");
                        } else {
                            try {
                                String content = new String(Files.readAllBytes(item), StandardCharsets.UTF_8);
                                fileContents.append("Content:\n").append(content).append("\n\n");
                            } catch (IOException e) {
                                fileContents.append("Content: Skipped due to error: ").append(e.getMessage()).append("\n\n");
                            }
                        }
                    }
                }
            } catch (AccessDeniedException e) {
                fileContents.append(path).append(": Permission denied\n\n");
            }
        }
        return fileContents.toString();
    }

    /**
     * Generates the header section of the output file.
     * 
     * @return A string containing the header information.
     */
    private static String getHeader() {
        return "================================================================\n" +
               "DIRECTORYCONTENTS OUTPUT FILE\n" +
               "================================================================\n\n" +
               "This file was generated by DIRECTORYCONTENTS on: " + ZonedDateTime.now().toInstant() + "\n\n" +
               "Purpose:\n" +
               "--------\n" +
               "This file contains a packed representation of the entire repository's contents.\n" +
               "It is designed to be easily consumable by AI systems for analysis, code review,\n" +
               "or other automated processes.\n\n" +
               "File Format:\n" +
               "------------\n" +
               "The content is organized as follows:\n" +
               "1. This header section\n" +
               "2. Multiple file entries, each consisting of:\n" +
               "   a. A separator line (================)\n" +
               "   b. The file path (File: path/to/file)\n" +
               "   c. Another separator line\n" +
               "   d. The full contents of the file\n" +
               "   e. A blank line\n\n" +
               "Usage Guidelines:\n" +
               "-----------------\n" +
               "1. This file should be treated as read-only. Any changes should be made to the\n" +
               "   original repository files, not this packed version.\n" +
               "2. When processing this file, use the separators and \"File:\" markers to\n" +
               "   distinguish between different files in the repository.\n" +
               "3. Be aware that this file may contain sensitive information. Handle it with\n" +
               "   the same level of security as you would the original repository.\n\n" +
               "Notes:\n" +
               "------\n" +
               "- Some files may have been excluded based on .gitignore rules and DIRECTORYCONTENTS's\n" +
               "  configuration.\n" +
               "- Binary files are not included in this packed representation.\n\n\n" +
               "================================================================\n" +
               "Repository Files\n" +
               "================================================================\n\n";
    }

    /**
     * Generates instructions for analyzing the directory contents.
     * 
     * @param directoryName The name of the directory being analyzed.
     * @return A string containing analysis instructions.
     */
    private static String getInstructions(String directoryName) {
        return "Prompt: Analyze the " + directoryName + " directory to understand its structure, purpose, and functionality. Follow these steps to study the contents:\n\n" +
               "1. Read the README file to gain an overview of the project, its goals, and any setup instructions.\n\n" +
               "2. Examine the directory structure to understand how the files and directories are organized.\n\n" +
               "3. Identify the main entry point of the application (e.g., main.py, app.py, index.js) and start analyzing the code flow from there.\n\n" +
               "4. Study the dependencies and libraries used in the project to understand the external tools and frameworks being utilized.\n\n" +
               "5. Analyze the core functionality of the project by examining the key modules, classes, and functions.\n\n" +
               "6. Look for any configuration files (e.g., config.py, .env) to understand how the project is configured and what settings are available.\n\n" +
               "7. Investigate any tests or test directories to see how the project ensures code quality and handles different scenarios.\n\n" +
               "8. Review any documentation or inline comments to gather insights into the codebase and its intended behavior.\n\n" +
               "9. Identify any potential areas for improvement, optimization, or further exploration based on your analysis.\n\n" +
               "10. Provide a summary of your findings, including the project's purpose, key features, and any notable observations or recommendations.\n\n" +
               "Use the files and contents provided below to complete this analysis:\n\n";
    }

    /**
     * A simple pair class to hold two related objects.
     * 
     * @param <T> The type of the first object in the pair.
     * @param <U> The type of the second object in the pair.
     */
    private static class Pair<T, U> {
        private final T first;
        private final U second;

        /**
         * Constructs a new Pair with the given values.
         * 
         * @param first The first object in the pair.
         * @param second The second object in the pair.
         */
        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }

        /**
         * Gets the first object in the pair.
         * 
         * @return The first object.
         */
        public T getFirst() { return first; }

        /**
         * Gets the second object in the pair.
         * 
         * @return The second object.
         */
        public U getSecond() { return second; }
    }
}