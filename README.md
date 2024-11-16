# JBit

A Java-based desktop application for downloading and sharing files using the BitTorrent protocol. This project is designed to provide a modular, scalable, and user-friendly torrent client that adheres to the principles of the BitTorrent ecosystem.
### Features:  

  - Parse .torrent files and magnet links.  
  - Peer-to-peer file sharing with support for multiple concurrent downloads.  
  - Tracker communication for peer discovery.  
  - Upload/download bandwidth control.  
  - Resume incomplete downloads seamlessly.  
  - User-friendly desktop interface for managing torrents.  

## Project Structure

This project follows a modular architecture with the following layers and components:
## 1. Core Layer

Implements the BitTorrent protocol and handles the essential torrenting operations.

    TorrentParser: Parses .torrent files and magnet links.
    PieceManager: Manages the file pieces, download progress, and piece validation.
    PeerManager: Handles peer discovery, communication, and data exchange.
    TrackerClient: Communicates with trackers to retrieve a list of peers.
    PeerConnection: Manages BitTorrent protocol messaging between peers.
    FileManager: Manages reading and writing files to disk.

## 2. Networking Layer

Handles all networking-related tasks, abstracting socket management and bandwidth control.

    SocketHandler: Manages TCP/UDP socket communication.
    BandwidthManager: Controls download and upload speeds.

## 3. Application Layer

Coordinates the core logic with the user interface.

    TorrentSessionManager: Manages the lifecycle of torrents (start, pause, resume, stop).
    ConfigurationManager: Loads and saves user settings.
    EventNotifier: Sends updates to the UI about torrent progress, peer activity, and errors.

## 4. User Interface Layer

Provides a desktop interface for managing torrents and user settings.

    MainWindow: Displays the list of active torrents, their statuses, speeds, and progress.
    TorrentDetailsPanel: Shows detailed information about selected torrents (e.g., peers, trackers, files).
    SettingsPanel: Allows configuration of user preferences (e.g., bandwidth limits, download directory).
    NotificationSystem: Displays alerts and notifications for important events.

## 5. Utility Layer

Provides reusable utilities that support all other layers.

    HashUtils: Handles SHA-1 hashing for piece validation.
    FileUtils: Manages file operations like reading, writing, and cleaning up incomplete downloads.
    Logging: Captures logs for debugging and monitoring.

## 6. Common Constants and Enums

Holds shared constants, enums, and data structures.

    TorrentStatus: Enum representing the torrent's current status (e.g., STARTED, PAUSED, COMPLETED).
    ErrorCode: Enum for standardizing error handling codes.
    Constants: Static values like default port numbers and paths.

Technologies Used

    Java 21: Core programming language.
    JavaFX: Desktop UI framework for building the user interface.
    Maven: Build automation and dependency management.
    JUnit: For unit and integration testing.

Getting Started
Prerequisites

    Java Development Kit (JDK) 21 or later.
    Maven installed on your system.

Building the Project

  Clone the repository:

    `git clone https://github.com/yourname/torrentclient.git`

Build the project with Maven:

    `mvn clean install`

Run the application:

    `java -jar target/jbit.jar`

Contributions are welcome! Feel free to fork the repository, open issues, and submit pull requests.

This project is licensed under the MIT License.
