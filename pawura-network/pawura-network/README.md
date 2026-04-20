# 🐘 Pawura Network

**Elephant Movement Tracking & Prediction System for Sri Lanka**

A Java 17 + JavaFX 21 desktop application that demonstrates core OOP principles through real-world wildlife monitoring.

---

## OOP Concepts Demonstrated

| Concept | Where |
|---|---|
| **Encapsulation** | `User`, `Location` – private fields with validated getters/setters |
| **Inheritance** | `Administrator extends User` |
| **Polymorphism** | `ElephantSighting`, `Prediction`, `NewsArticle` all implement `Displayable` |
| **Abstraction** | `AbstractPredictionModel`, `AbstractDataStore` |
| **Interfaces** | `Displayable`, `Predictable` |
| **Singleton** | `DatabaseManager` |
| **Template Method** | `AbstractPredictionModel.predict()` |

---

## Tech Stack

- **Language**: Java 17
- **UI**: JavaFX 21
- **Database**: SQLite (via `sqlite-jdbc`)
- **Build**: Maven 3.8+

---

## Quick Start

```bash
# Build
mvn clean package -q

# Run (JavaFX Maven plugin)
mvn javafx:run

# Or run the fat JAR
java -jar target/pawura-network-1.0-SNAPSHOT.jar
```

---

## Demo Login Credentials

| Username | Password | Role |
|---|---|---|
| `admin` | `password123` | Administrator |
| `ranger1` | `password123` | Ranger |
| `viewer1` | `password123` | Viewer |

---

## Project Structure

```
src/main/java/com/pawura/
├── Main.java                   Entry point
├── app/PawuraApp.java          JavaFX Application
├── model/                      Domain models (POJO)
├── contract/                   Interfaces
├── core/                       Abstract base classes
├── service/                    Business logic + DB access
├── database/DatabaseManager    SQLite Singleton
├── ui/                         JavaFX views
└── util/                       Helpers
```

---

## Features

- 🔐 Login / logout with role-based access
- 🔭 Report & view elephant sightings with location, count, behaviour
- 🗺 Schematic Sri Lanka map with sighting pins
- 📊 Movement prediction (linear extrapolation model)
- 📰 News articles about elephant conservation
- ⚙ Admin panel (admin role only)
