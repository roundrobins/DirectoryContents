# DirectoryContents

DirectoryContents is a Java application that generates a comprehensive text representation of a directory's structure and file contents. It's designed to create an easily analyzable output for AI systems, code reviews, or other automated processes.

## Features

- Traverses directory structures recursively
- Captures file contents (with configurable size limits)
- Excludes specified directories, file extensions, and files
- Generates a formatted output file with directory structure and file contents
- Configurable via properties file or command-line arguments


## Quickstart

To quickly get started with DirectoryContents:

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/DirectoryContents.git
   cd DirectoryContents
   ```

2. Compile the Java files:
   ```
   javac *.java
   ```

3. Run the application with default settings:
   ```
   java DirectoryContents /path/to/your/directory
   ```

4. Check the output file `directory_contents.txt` in the current directory.

For more advanced usage and configuration options, see the [Usage](#usage) and [Configuration](#configuration) sections below.

## Requirements

- Java 11 or higher
- Access to file system for reading directory contents

## Usage

1. Compile the Java files:
   ```
   javac *.java
   ```

2. Run the application:
   ```
   java DirectoryContents <directory_path> [config_file]
   ```

   If no config file is specified, it will look for `config.properties` in the current directory.

3. The application will generate an output file named `<directory_name>_contents.txt` in the target directory.

## Configuration

Create a `config.properties` file: 

```properties
# Maximum file size in bytes
max.file.size=1048576

# Directories to exclude from processing
excluded.dirs=.git,node_modules,build

# File extensions to exclude
excluded.extensions=class,jar,war

# Specific files to exclude
excluded.files=config.properties,secrets.txt

# Output format (text)
output.format=text

```

## Output Format

The generated output file includes:

1. A header section with metadata and usage instructions
2. A directory structure overview
3. File contents, each preceded by separators and file path information

## Limitations

- Binary files are not included in the output
- Files larger than the specified maximum size are skipped
- Access to some directories or files may be restricted due to permissions

## Contributing

Contributions to improve DirectoryContents are welcome. Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License.

## Disclaimer

This tool may capture sensitive information from your directory. Please review the output file before sharing or using it in any public context.

## Credits

This project was inspired by and adapted from [RepoToTextForLLMs](https://github.com/Doriandarko/RepoToTextForLLMs) by Doriandarko. Their tool for automating the analysis of GitHub repositories for LLMsserved as a valuable transcoding source for this project.