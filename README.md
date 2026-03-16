# The Guide

**The Guide** is a cross‑platform desktop application built with Kotlin and Jetpack Compose, designed to help believers (The Bride) access, read, and present spiritual content easily. It brings together key resources such as songs, Bible reading, structured tables, read-along letters, and presentation features in a seamless desktop experience.

⚠️ *Media playback is powered by the reusable [`AuraPlayer`](https://github.com/M0ssi-P/AuraPlayer) media library.*

---

## 🚀 Features

### 📖 Bible & Scripture
- Full scripture navigation with chapter and verse selection  
- Quick search for passages  
- Easy reading layout optimized for screen and projector use  

### 🎵 Song Library
- Manage and browse worship songs  
- View lyrics and song details  
- Queue songs for presentation or reading  

### 📊 Structured Tables
- Build and display tables of content (e.g., service order, song lists)  
- Organize resources for live events or study sessions  
- Export or print tables for planning  

### 📝 Read-Along BT Letters
- Weekly letters released every **Sunday at midnight**  
- Read-along mode to follow along easily  
- Clean and **modern UI** for smooth reading experience  
- Automatically updates new letters when released  

### 📽️ Presentation Mode
- Fullscreen display designed for **church services and group gatherings**  
- Switch between Bible, songs, tables, and letters with smooth UI  
- Customisable theme and layout settings  

### 🎧 Media Playback
- Audio and video playback integrated using the **AuraPlayer** library  
- Supports local media files with controls for play, pause, seek, and timeline  
- Integrated seamlessly with read-along content and presentations  

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| UI | Kotlin + Jetpack Compose Desktop |
| Media Engine | MPV (via JNI binding, powered by AuraPlayer) |
| Build System | Gradle |
| Languages | Kotlin |

---

## 📦 Installation

1. Clone the repository  
   ```bash
   git clone https://github.com/M0ssi-P/guide.git
