// Importing classes needed for creating a simple HTTP server
import com.sun.net.httpserver.HttpServer;   // For running HTTP server
import com.sun.net.httpserver.HttpHandler;  // For handling HTTP requests
import com.sun.net.httpserver.HttpExchange; // Represents HTTP request/response exchange

import java.io.*;              // For file reading/writing (BufferedReader, FileReader)
import java.net.InetSocketAddress; // For binding server to a port
import java.util.*;            // For List, HashMap, etc.

public class SignLanguageServerPlain {

    // ---------- Trie Data Structure ----------
    // A Trie node represents one character in a word.
    static class Node {
        Node[] children = new Node[26]; // 26 letters (a-z)
        boolean eow = false;            // End Of Word flag (true if word ends here)
    }

    // Root node of the Trie (starting point)
    static Node root = new Node();

    // Map to store mapping of characters or digits → image file paths
    static HashMap<String, String> signMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
       
        // Load alphabet and number sign images into signMap
        loadSignImages();

        // Load frequently used words from file into Trie for prefix suggestions
        loadWordsFromFile("words.txt");

        // Create an HTTP server running on port 4567
        HttpServer server = HttpServer.create(new InetSocketAddress(4567), 0);

        // Register endpoints (like API routes)
        server.createContext("/suggestions", new SuggestHandler()); // for word suggestions
        server.createContext("/images", new ImageHandler());         // for letter images

        server.setExecutor(null); // Use default executor (thread management)
        System.out.println("Server running at http://localhost:4567");
        server.start(); // Start the server
    }

    // ---------- Load words into Trie from file ----------
    static void loadWordsFromFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            // Read each line (each word) from file
            while ((line = br.readLine()) != null) {
                line = line.trim().toLowerCase();
                // Skip blank lines
                if (!line.isEmpty()) insertWord(line);
            }
            System.out.println("Words loaded from " + filename);
        } catch (Exception e) {
            System.out.println("Error loading words: " + e.getMessage());
        }
    }

    // ---------- Load Sign Language Images ----------
    static void loadSignImages() {
        // Map A–Z to image paths (e.g., images/A.jpeg)
        for (char c = 'A'; c <= 'Z'; c++)
            signMap.put("" + c, "images/" + c + ".jpeg");

        // Map 0–10 to image paths (e.g., images/0.jpeg)
        for (int n = 0; n <= 10; n++)
            signMap.put("" + n, "images/" + n + ".jpeg");
    }

    // ---------- Insert a word into Trie ----------
    static void insertWord(String word) {
        Node ptr = root; // Start from root
        for (char ch : word.toLowerCase().toCharArray()) {
            int index = ch - 'a'; // Find index (0–25)
            // If child node doesn’t exist, create it
            if (ptr.children[index] == null)
                ptr.children[index] = new Node();
            ptr = ptr.children[index]; // Move down
        }
        ptr.eow = true; // Mark end of word
    }

    // ---------- Find suggestions for given prefix ----------
    static List<String> getSuggestions(String prefix) {
        Node ptr = root;
        // Traverse Trie based on prefix
        for (char ch : prefix.toLowerCase().toCharArray()) {
            int index = ch - 'a';
            // If path not found, no words start with prefix
            if (ptr.children[index] == null) return new ArrayList<>();
            ptr = ptr.children[index];
        }
        // Collect all words starting from this node
        List<String> result = new ArrayList<>();
        collectWords(ptr, prefix.toLowerCase(), result);
        return result;
    }

    // ---------- Recursive helper to collect words from Trie ----------
    static void collectWords(Node ptr, String word, List<String> result) {
        if (ptr.eow) result.add(word); // If end of word, add to result
        // Explore all possible next characters (a-z)
        for (int i = 0; i < 26; i++)
            if (ptr.children[i] != null)
                collectWords(ptr.children[i], word + (char) (i + 'a'), result);
    }

    // ---------- HTTP Handler for /suggestions ----------
    static class SuggestHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            // Example URL: /suggestions?prefix=ap
            String query = t.getRequestURI().getQuery();
            String prefix = "";
            if (query != null && query.startsWith("prefix=")) {
                prefix = query.substring(7); // Extract prefix value
            }

            // Get word suggestions from Trie
            List<String> suggestions = getSuggestions(prefix);
            String json = toJsonArray(suggestions); // Convert to JSON format

            // ✅ Add CORS headers (allow browser access)
            t.getResponseHeaders().add("Content-Type", "application/json");
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            // Send response back to browser
            t.sendResponseHeaders(200, json.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(json.getBytes());
            os.close();
        }
    }

    // ---------- HTTP Handler for /images ----------
    static class ImageHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            // Example URL: /images?text=APPLE
            String query = t.getRequestURI().getQuery();
            String text = "";
            if (query != null && query.startsWith("text="))
                text = query.substring(5); // Extract the text to translate

            List<String> paths = new ArrayList<>();
            // Go through each character in the word
            for (int i = 0; i < text.length(); i++) {
                String ch = "" + text.charAt(i);
                // Special case: if "10" appears, treat it as single sign
                if (i < text.length() - 1 && text.substring(i, i + 2).equals("10")) {
                    ch = "10"; i++;
                }
                // Add the image path if it exists
                if (signMap.containsKey(ch))
                    paths.add(signMap.get(ch));
            }

            String json = toJsonArray(paths); // Convert list of paths to JSON

            // ✅ Add CORS headers (for frontend to access)
            t.getResponseHeaders().add("Content-Type", "application/json");
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            // Send JSON response
            t.sendResponseHeaders(200, json.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(json.getBytes());
            os.close();
        }
    }

    // ---------- Convert a list to JSON array string ----------
    static String toJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i)).append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
