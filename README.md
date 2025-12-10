# Git Commit Tracker Plugin for IntelliJ IDEA

A powerful IntelliJ IDEA plugin to track and filter commits across multiple Git repositories in your workspace.

## Features

- **Auto-detect Git Repositories**: Automatically discovers all Git repositories in your open IntelliJ projects
- **User-Specific Commits**: Shows only commits by the current user (based on Git config)
- **Advanced Filtering**:
  - Filter by date range (from/to dates)
  - Filter by specific repository
  - Search commits by message, hash, or author
- **Clean UI**: Tool window panel with sortable table
- **Performance Optimized**: Built-in caching for handling many repositories efficiently

## Requirements

- IntelliJ IDEA 2024.3 or later
- Java 21 or later
- Git repositories in your project

## Installation

### From Source

1. Clone this repository
2. Open the project in IntelliJ IDEA
3. Run the Gradle task: `./gradlew buildPlugin`
4. Install the plugin from `build/distributions/git-commit-tracker-1.0.0.zip`

### Development

To run the plugin in a sandbox IDE:

```bash
./gradlew runIde
```

## Usage

1. Open a project with Git repositories in IntelliJ IDEA
2. Find the "Git Commits" tool window (usually at the bottom panel)
3. View all your commits across all repositories
4. Use filters to narrow down results:
   - **Search**: Type text to search in commit messages, hashes, or authors
   - **Date Range**: Enter dates in format `yyyy-MM-dd` (e.g., `2024-01-01`)
   - **Repository**: Select a specific repository or view all
5. Click column headers to sort the table
6. Click "Refresh" to reload commits from repositories

## Development

### Project Structure

```
git-commit-tracker/
├── src/main/kotlin/com/yourcompany/committracker/
│   ├── toolwindow/          # UI components (tool window, panel, table)
│   ├── services/            # Core services (commit query, repo detection, caching)
│   ├── models/              # Data models
│   ├── ui/                  # UI components (filters, renderers)
│   ├── listeners/           # Event listeners
│   └── utils/               # Utility classes
└── src/main/resources/META-INF/
    └── plugin.xml           # Plugin configuration
```

### Building

```bash
# Build the plugin
./gradlew buildPlugin

# Run tests
./gradlew test

# Run in sandbox IDE
./gradlew runIde
```

## Technical Details

- **Language**: Kotlin
- **Build System**: Gradle with IntelliJ Platform Plugin 2.x
- **Min Version**: IntelliJ IDEA 2024.3 (requires Java 21)
- **Git Integration**: Uses git4idea plugin APIs
- **Async Processing**: Kotlin Coroutines
- **Caching**: In-memory with 5-minute expiration

## License

[Add your license here]

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
