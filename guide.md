# Lagoon Food Helper - Technical Guide

## 1. Project Overview
Lagoon Food Helper is an Android application that helps students find food stalls and items in the "Lagoon" area of a university campus. It features:
- **Search**: Real-time searching by food name.
- **Filtering**: Filtering by categories and price ranges.
- **Efficient Data Handling**: Uses a custom **B-Tree** implementation for fast searching through CSV data.
- **Modern UI**: Built with **Jetpack Compose**, Google's modern toolkit for building native Android UI.

---

## 2. Tech Stack
- **Language**: [Kotlin](https://kotlinlang.org/) - A modern, concise, and safe programming language.
- **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) - Declarative UI (you describe *what* the UI looks like based on data).
- **Architecture**: **MVVM (Model-View-ViewModel)** - A standard pattern that separates the UI from the logic and data.
- **Data Source**: A local CSV file (`assets/food_records.csv`).

---

## 3. Project Structure
The code is organized into three main layers:

### 📁 `data` (The Model)
Handles data storage and retrieval.
- `FoodRecord.kt`: A **data class** representing a single food item.
- `BTree.kt`: A custom implementation of a B-Tree data structure for indexing and range searches.
- `FoodRepository.kt`: The "brain" of the data layer. It parses the CSV file and builds several B-Trees (indexes) for names, categories, and prices.

### 📁 `viewmodel` (The ViewModel)
Acts as a bridge between the Data and the UI.
- `MainViewModel.kt`: Manages the "state" of the screen (e.g., current search text, selected filters). It talks to the Repository to get search results.

### 📁 `ui` (The View)
Contains the visual components.
- `MainActivity.kt`: The entry point. It sets up the screen and the high-level layout.
- `components/`: Smaller, reusable UI pieces like `FoodItemCard.kt` and `FilterDialog.kt`.
- `theme/`: Defines colors, fonts, and the overall look (Theming).

---

## 4. Key Kotlin Concepts for Beginners

If you see these in the code and feel confused:

- **`data class`**: A special class used primarily to hold data. Kotlin automatically generates useful functions like `equals()` and `toString()`.
  ```kotlin
  data class FoodRecord(val name: String, val price: Double)
  ```
- **`by` (Delegated Properties)**: Used in Compose to automatically track changes. If a variable changes, the UI "observes" it and refreshes.
  ```kotlin
  var searchQuery by mutableStateOf("")
  ```
- **`it`**: A shorthand name for a single parameter in a lambda (anonymous function).
  ```kotlin
  // 'it' represents each category in the list
  categories.forEach { println(it) } 
  ```
- **Trailing Lambdas**: If the last argument of a function is a function itself, you can put it outside the parentheses.
  ```kotlin
  Scaffold(topBar = { /* ... */ }) { innerPadding ->
      // This block is the last argument!
  }
  ```

---

## 5. Core Component Deep Dive

### The Search Engine (`BTree.kt`)
Standard lists are slow to search as they grow. We use a **B-Tree** to keep data sorted. This allows us to find a price range (e.g., "Food between ₱50 and ₱100") very quickly without looking at every single item.

### The Repository (`FoodRepository.kt`)
When the app starts, this file:
1. Opens `assets/food_records.csv`.
2. Reads every line.
3. Cleans up the text (e.g., removing the "₱" symbol to make prices numeric).
4. Inserts the records into three different B-Trees so we can search by **Name**, **Category**, or **Price**.

### The UI Logic (`MainViewModel.kt`)
This class uses a **computed property** called `searchResults`. Every time the user types a single character in the search bar, this property recalculates:
1. It asks the Repository for an initial list from the B-Tree.
2. It further filters that list based on other criteria (like making sure it matches the selected categories).
3. The UI sees the new list and updates instantly.

---

## 6. How to Run the Project
1. Open the project in **Android Studio**.
2. Wait for **Gradle Sync** to finish (it downloads the libraries).
3. Click the green **Run** button at the top.
4. Use an Emulator or a physical Android device.

---

## 7. Developer Tips
- **Pre-views**: You can see what your UI looks like without running the app! Check `MainActivity.kt` and look for the `@Preview` functions. Click the "Split" or "Design" tab in the top right of the editor.
- **Logs**: If something goes wrong, use `Log.d("TAG", "Message")` and check the **Logcat** tab at the bottom of Android Studio.
- **State**: In Compose, if you want the screen to update, you *must* change a state variable in the ViewModel. Don't try to manipulate the UI views directly!
