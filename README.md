# PUP Lagoon Food Helper

A modern, efficient Android application designed to help students and visitors navigate the diverse food landscape of the PUP Lagoon area. Built with Jetpack Compose and powered by custom-optimized data structures for a seamless experience.

## Features

- **Smart Search & Filtering**: Find your favorite meals instantly using real-time search, filtered by category and price range.
- **Interactive Lagoon Map**: A zoomable, interactive map showing all stall locations and campus landmarks.
- **Intelligent Pathfinding**: Get precise walking directions from any campus gate to your chosen food stall, following the physical "Lagoon Loop."
- **Stall Deep-Dives**: View stall menus, photos, and exact locations in an intuitive bottom-sheet interface.
- **Favorites & Pinning**: "Keep" your go-to stalls for quick access and highlighting on the map.
- **Seamless Onboarding**: Interactive tutorials and onboarding screens to help you get the most out of the app.

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Data Persistence**: CSV-backed repository with custom B-Tree indexing.
- **Concurrency**: Kotlin Coroutines & StateFlow for reactive UI updates.
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)

## Technical Highlights

### High-Performance Data Retrieval
To ensure instantaneous search results without the overhead of a traditional database, the app implements a custom **Multi-Value B-Tree**. This allows for $O(\log n)$ search and range queries, even as the food catalog grows.

### The "Lagoon Loop" Pathfinding
Unlike generic A* algorithms, our pathfinding is optimized for the circular geometry of the PUP Lagoon. It uses a sequence-based approach that mimics natural walking paths, ensuring directions are physically accurate and easy to follow.

### Data Merging & Deduplication
The search engine automatically groups variations of the same item (e.g., different sizes or quantities) into single, clean search results, providing a clutter-free experience while still allowing price-range transparency.

## Project Structure

```text
app/src/main/java/com/example/pup_lagoon_app/
├── data/           # Data models, B-Tree implementation, and Repository
├── ui/             # UI layer
│   ├── components/ # Reusable Compose widgets (ZoomableBox, FilterDialog, etc.)
│   ├── theme/      # Material 3 design system & styling
│   └── utils/      # UI helper functions
├── viewmodel/      # Business logic & state management (MainViewModel)
└── MainActivity.kt # Main entry point & Navigation
```

## Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/puplagoonapp.git
   ```
2. **Open in Android Studio**:
   Import the project and wait for Gradle sync to complete.
3. **Run the App**:
   Click the **Run** button in Android Studio or use `Shift + F10` to deploy to an emulator or physical device.

## Documentation

For a deeper dive into the architecture and algorithms, see:
- [GUIDE.md](GUIDE.md) - Technical Architecture & Algorithm Guide

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
