import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This class provides functionality to generate a text representation of a directory's contents,
 * including file structure and file contents. It can be configured using a properties file to
 * exclude certain directories, file extensions, or specific files.
 */
public class DirectoryContents {
    private static final Logger LOGGER = Logger.getLogger(DirectoryContents.class.getName());
    private static final String DEFAULT_PROPERTIES_FILE = "config.properties";
    private static long maxFileSize = 1024 * 1024; // 1 MB default
    private static final Set<String> excludedDirs = new HashSet<>();
    private static final Set<String> excludedExtensions = new HashSet<>();
    private static final Set<String> excludedFiles = new HashSet<>();

    /**
     * The main entry point of the program.
     * 
     * @param args Command line arguments. Each argument is treated as a target directory.
     */
    public static void main(String[] args) {
        List<String> inputDirs = new ArrayList<>();
        String outputDir;

        try {
            if (args.length == 0 || "/help".equalsIgnoreCase(args[0])) {
                displayUsageInstructions();
                System.exit(0);
            }

            outputDir = parseArguments(args, inputDirs);
            
            if (inputDirs.isEmpty()) {
                System.out.println("Error: Please provide at least one input directory using the /dir switch.");
                displayUsageInstructions();
                System.exit(1);
            }

            if (outputDir == null) {
                System.out.println("Error: Please specify an output directory using the /output switch.");
                displayUsageInstructions();
                System.exit(1);
            }

            Path outputPath = Paths.get(outputDir);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            } else if (!Files.isDirectory(outputPath) || !Files.isWritable(outputPath)) {
                System.out.println("Error: The specified output path is not a writable directory: " + outputDir);
                System.exit(1);
            }

            loadProperties(DEFAULT_PROPERTIES_FILE);
            
            StringBuilder mergedContents = new StringBuilder();
            
            for (String targetDirectory : inputDirs) {
                Path targetPath = Paths.get(targetDirectory).toAbsolutePath();
                if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
                    System.out.println("Error: The specified target directory does not exist or is not a directory: " + targetDirectory);
                    continue;
                }

                try {
                    String contents = getDirectoryContents(targetDirectory);
                    mergedContents.append(contents).append("\n\n");
                    System.out.println("Processed directory: " + targetDirectory);
                } catch (IOException e) {
                    System.out.println("Error processing directory '" + targetDirectory + "': " + e.getMessage());
                }
            }

            Path outputFilePath = outputPath.resolve("merged_contents.txt");
            Files.write(outputFilePath, mergedContents.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Merged directory contents saved to '" + outputFilePath + "'.");

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Error in command-line arguments: " + e.getMessage());
            displayUsageInstructions();
            System.exit(1);
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void displayUsageInstructions() {
        System.out.println("Usage: java DirectoryContents [/help] /dir <directory1> [<directory2> ...] /output <output_directory>");
        System.out.println("Options:");
        System.out.println("  /help              Display this help message");
        System.out.println("  /dir <directory>   Specify one or more input directories to process");
        System.out.println("  /output <directory> Specify the output directory for the merged contents file");
        System.out.println("\nExample:");
        System.out.println("  java DirectoryContents /dir C:\\Project1 C:\\Project2 /output C:\\Output");
    }

    private static String parseArguments(String[] args, List<String> inputDirs) {
        String outputDir = null;
        boolean expectingDir = false;
        boolean expectingOutput = false;
    
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (expectingDir) {
                if (arg.startsWith("/")) {
                    expectingDir = false;
                    // Process the new switch
                    if (arg.equalsIgnoreCase("/output")) {
                        expectingOutput = true;
                    } else {
                        throw new IllegalArgumentException("Unexpected switch while parsing directories: " + arg);
                    }
                } else {
                    inputDirs.add(arg);
                    continue;
                }
            }
            if (expectingOutput) {
                if (i == args.length - 1) {
                    throw new IllegalArgumentException("Missing output directory path after /output");
                }
                if (arg.startsWith("/")) {
                    throw new IllegalArgumentException("Expected output directory path after /output, found: " + arg);
                }
                outputDir = arg;
                expectingOutput = false;
                continue;
            }
            switch (arg.toLowerCase()) {
                case "/dir":
                    expectingDir = true;
                    break;
                case "/output":
                    if (i == args.length - 1 || args[i + 1].startsWith("/")) {
                        throw new IllegalArgumentException("Missing output directory path after /output");
                    }
                    outputDir = args[++i];
                    break;
                default:
                    if (arg.startsWith("/")) {
                        throw new IllegalArgumentException("Unknown switch: " + arg);
                    } else {
                        throw new IllegalArgumentException("Unexpected argument: " + arg);
                    }
            }
        }
    
        if (expectingDir) {
            throw new IllegalArgumentException("Missing directory path after /dir");
        }
        if (expectingOutput) {
            throw new IllegalArgumentException("Missing output directory path after /output");
        }
    
        return outputDir;
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
                maxFileSize = Long.parseLong(props.getProperty("max.file.size", String.valueOf(maxFileSize)).trim());
                addNonEmptyValues(excludedDirs, props.getProperty("excluded.dirs", ""));
                addNonEmptyValues(excludedExtensions, props.getProperty("excluded.extensions", ""));
                addNonEmptyValues(excludedFiles, props.getProperty("excluded.files", ""));
            }
        } else {
            LOGGER.warning("Properties file not found. Using default values.");
        }
    }

    /**
     * Adds non-empty, trimmed values from a comma-separated string to a Set.
     * 
     * @param set The Set to add values to.
     * @param values A comma-separated string of values.
     */
    private static void addNonEmptyValues(Set<String> set, String values) {
        set.addAll(Arrays.stream(values.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet()));
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
        Deque<DirectoryEntry> dirsToVisit = new ArrayDeque<>();
        dirsToVisit.push(new DirectoryEntry(Paths.get(""), directory));

        while (!dirsToVisit.isEmpty()) {
            DirectoryEntry entry = dirsToVisit.pop();
            Path relativePath = entry.getRelativePath();
            Path currentDir = entry.getCurrentDir();

            if (excludedDirs.contains(currentDir.getFileName().toString())) {
                continue;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir)) {
                List<Path> items = new ArrayList<>();
                stream.forEach(items::add);
                items.sort(Comparator.comparing(Path::toString));

                for (Path item : items) {
                    Path relPath = relativePath.resolve(item.getFileName());
                    if (Files.isDirectory(item)) {
                        structure.append(relPath).append("/\n");
                        dirsToVisit.push(new DirectoryEntry(relPath, item));
                    } else if (!shouldExcludeFile(item)) {
                        structure.append(relPath).append("\n");
                    }
                }
            } catch (AccessDeniedException e) {
                structure.append(relativePath).append(": Permission denied\n");
                LOGGER.log(Level.WARNING, "Access denied for directory: " + relativePath, e);
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
        if (excludedFiles.contains(fileName)) {
            return true;
        }
        return fileName.lastIndexOf('.') != -1 && 
               excludedExtensions.contains(fileName.substring(fileName.lastIndexOf('.') + 1));
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
            byte[] buffer = new byte[64];
            int bytesRead = in.read(buffer);
            if (bytesRead == -1) {
                return false;
            }
            for (int i = 0; i < bytesRead; i++) {
                if (buffer[i] == 0) {
                    return true;
                }
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
        Deque<DirectoryEntry> dirsToVisit = new ArrayDeque<>();
        dirsToVisit.push(new DirectoryEntry(Paths.get(""), directory));

        while (!dirsToVisit.isEmpty()) {
            DirectoryEntry entry = dirsToVisit.pop();
            Path relativePath = entry.getRelativePath();
            Path currentDir = entry.getCurrentDir();

            if (excludedDirs.contains(currentDir.getFileName().toString())) {
                continue;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir)) {
                List<Path> items = new ArrayList<>();
                stream.forEach(items::add);
                items.sort(Comparator.comparing(Path::toString));

                for (Path item : items) {
                    Path relPath = relativePath.resolve(item.getFileName());
                    if (Files.isDirectory(item)) {
                        dirsToVisit.push(new DirectoryEntry(relPath, item));
                    } else if (!shouldExcludeFile(item)) {
                        appendFileContent(fileContents, relPath, item);
                    }
                }
            } catch (AccessDeniedException e) {
                fileContents.append(relativePath).append(": Permission denied\n\n");
                LOGGER.log(Level.WARNING, "Access denied for directory: " + relativePath, e);
            }
        }
        return fileContents.toString();
    }

    /**
     * Appends the content of a file to the given StringBuilder.
     * 
     * @param fileContents The StringBuilder to append the file content to.
     * @param relPath The relative path of the file.
     * @param item The Path object representing the file.
     * @throws IOException If an error occurs while reading the file.
     */
    private static void appendFileContent(StringBuilder fileContents, Path relPath, Path item) throws IOException {
        fileContents.append("================\nFile: ").append(relPath).append("\n================\n");
        long fileSize = Files.size(item);
        if (fileSize > maxFileSize) {
            fileContents.append(String.format("Content: Skipped (file size: %.2f MB, max allowed: %.2f MB)\n\n", 
                                              fileSize / (1024.0 * 1024.0), maxFileSize / (1024.0 * 1024.0)));
        } else if (isBinaryFile(item)) {
            fileContents.append("Content: Skipped binary file\n\n");
        } else {
            try {
                String content = new String(Files.readAllBytes(item), StandardCharsets.UTF_8);
                fileContents.append("Content:\n").append(content).append("\n\n");
            } catch (IOException e) {
                fileContents.append("Content: Skipped due to error: ").append(e.getMessage()).append("\n\n");
                LOGGER.log(Level.WARNING, "Error reading file: " + relPath, e);
            }
        }
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
     * A simple class to hold two related objects: a relative path and a current directory.
     */
    private static class DirectoryEntry {
        private final Path relativePath;
        private final Path currentDir;

        /**
         * Constructs a new DirectoryEntry with the given values.
         * 
         * @param relativePath The relative path.
         * @param currentDir The current directory.
         */
        public DirectoryEntry(Path relativePath, Path currentDir) {
            this.relativePath = relativePath;
            this.currentDir = currentDir;
        }

        /**
         * Gets the relative path.
         * 
         * @return The relative path.
         */
        public Path getRelativePath() { return relativePath; }

        /**
         * Gets the current directory.
         * 
         * @return The current directory.
         */
        public Path getCurrentDir() { return currentDir; }
    }
}
