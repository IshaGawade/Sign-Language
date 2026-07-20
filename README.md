# 🖐️ Sign Language Translator

The **Sign Language Translator** is a simple yet interactive Java-based application that helps users understand the **American Sign Language (ASL)** alphabet.

When a user enters a word, the program displays corresponding images for each alphabet letter - helping bridge communication between hearing and speech-impaired individuals.

---

## Features

- **Alphabet-to-Sign Translation** - Converts typed English alphabets into their corresponding sign language images.
- **Sorted Dictionary View** - Displays all alphabets (A–Z) with their sign images in sorted order.
- **Trie-based Storage** - Uses a Trie (prefix tree) data structure to store and retrieve alphabet-sign mappings, enabling efficient letter-by-letter and sequence-based lookups.
- **Owner-only Feature** - Only developers can add or modify signs (not general users).
- Supports both `.jpeg` and `.jpg` image formats.

---

## Project Structure

```
Sign Language Translator/
├── src/
│   └── SignLanguageTranslator.java   # Main Java source file
├── images/
│   ├── A.jpeg
│   ├── B.jpeg
│   ├── C.jpeg
│   └── ... Z.jpeg
└── README.md
```

---

## ⚙️ How to Run the Project

1. **Clone the Repository**
```bash
   git clone https://github.com/IshaGawade/Sign-Language.git
   cd "Sign Language Translator"
```

2. **Compile the Program**
```bash
   javac src\SignLanguageTranslator.java
```

3. **Run the Program**
```bash
   java -cp src SignLanguageTranslator
```

---

## Future Enhancements

- Add real-time hand gesture recognition using a webcam and OpenCV.
- Integrate text-to-speech to pronounce words.
- Support for numbers and gestures beyond alphabets.
- Build a web or mobile version using React or Android Studio.
