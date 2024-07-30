# DirectoryContents

DirectoryContents is a Java application that generates a comprehensive text representation of multiple directory structures and file contents. It's designed to create an easily analyzable output for AI systems, code reviews, or other automated processes.

## Features

- Traverses multiple directory structures recursively
- Captures file contents (with configurable size limits)
- Excludes specified directories, file extensions, and files
- Generates a formatted output file with directory structures and file contents
- Configurable via properties file
- Command-line interface for specifying input and output directories

## Requirements

- Java 11 or higher
- Access to file system for reading directory contents

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

3. Run the application:
   ```
   java DirectoryContents /dir <input_directory1> [<input_directory2> ...] /output <output_directory>

   Example:
  java DirectoryContents /dir C:\Project1 C:\Project2 /output C:\Output
   ```

4. Check the output file `merged_contents.txt` in the specified output directory.

## Usage

The application uses command-line arguments to specify input and output directories:

```
java DirectoryContents /dir <input_directory1> [<input_directory2> ...] /output <output_directory>
```

- `/dir` specifies the input directories to process
- `<input_directory1> [<input_directory2> ...]` are the paths to the input directories
- `/output` specifies the output directory for the generated file
- `<output_directory>` is the path to the output directory

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

```

## Output Format

The generated output file includes:

1. A header section with metadata and usage instructions
2. A directory structure overview
3. File contents, each preceded by separators and file path information

## Limitations

- Access to some directories or files may be restricted due to permissions

## Contributing

Contributions to improve DirectoryContents are welcome. Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License.

## Disclaimer

This tool may capture sensitive information from your directory. Please review the output file before sharing or using it in any public context.

## Credits

This project was inspired by and adapted from [RepoToTextForLLMs](https://github.com/Doriandarko/RepoToTextForLLMs) by Doriandarko.