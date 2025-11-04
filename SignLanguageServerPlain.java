import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

public class SignLanguageServerPlain {

    // ---------- Trie ----------
    static class Node {
        Node[] children = new Node[26];
        boolean eow = false;
    }

    static Node root = new Node();
    static HashMap<String, String> signMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        loadSignImages();
        loadSignImages(); // load alphabet/number images
        loadWordsFromFile("words.txt"); // load daily-use words

        // Example words in Trie
        // String[] words = {"apple", "apply", "ape", "bat", "ball", "banana"};
        // for (String w : words) insertWord(w);

        HttpServer server = HttpServer.create(new InetSocketAddress(4567), 0);
        server.createContext("/suggestions", new SuggestHandler());
        server.createContext("/images", new ImageHandler());
        server.setExecutor(null);
        System.out.println("Server running at http://localhost:4567");
        server.start();
    }
    // Instead of manually inserting words
static void loadWordsFromFile(String filename) {
    try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim().toLowerCase();
            if (!line.isEmpty()) insertWord(line);
        }
        System.out.println("Words loaded from " + filename);
    } catch (Exception e) {
        System.out.println("Error loading words: " + e.getMessage());
    }
}

    static void loadSignImages() {
        for (char c = 'A'; c <= 'Z'; c++)
            signMap.put("" + c, "images/" + c + ".jpeg");
        for (int n = 0; n <= 10; n++)
            signMap.put("" + n, "images/" + n + ".jpeg");
    }

    static void insertWord(String word) {
        Node ptr = root;
        for (char ch : word.toLowerCase().toCharArray()) {
            int index = ch - 'a';
            if (ptr.children[index] == null) ptr.children[index] = new Node();
            ptr = ptr.children[index];
        }
        ptr.eow = true;
    }

    static List<String> getSuggestions(String prefix) {
        Node ptr = root;
        for (char ch : prefix.toLowerCase().toCharArray()) {
            int index = ch - 'a';
            if (ptr.children[index] == null) return new ArrayList<>();
            ptr = ptr.children[index];
        }
        List<String> result = new ArrayList<>();
        collectWords(ptr, prefix.toLowerCase(), result);
        return result;
    }

    static void collectWords(Node ptr, String word, List<String> result) {
        if (ptr.eow) result.add(word);
        for (int i = 0; i < 26; i++)
            if (ptr.children[i] != null)
                collectWords(ptr.children[i], word + (char) (i + 'a'), result);
    }

    static class SuggestHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
        String query = t.getRequestURI().getQuery(); // e.g., prefix=ap
        String prefix = "";
        if (query != null && query.startsWith("prefix=")) {
            prefix = query.substring(7);
        }
        List<String> suggestions = getSuggestions(prefix);
        String json = toJsonArray(suggestions);

        // ✅ Add CORS headers
        t.getResponseHeaders().add("Content-Type", "application/json");
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        t.sendResponseHeaders(200, json.getBytes().length);
        OutputStream os = t.getResponseBody();
        os.write(json.getBytes());
        os.close();
    }
}

static class ImageHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
        String query = t.getRequestURI().getQuery(); // e.g., text=APPLE
        String text = "";
        if (query != null && query.startsWith("text=")) text = query.substring(5);
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            String ch = "" + text.charAt(i);
            if (i < text.length() - 1 && text.substring(i, i + 2).equals("10")) {
                ch = "10"; i++;
            }
            if (signMap.containsKey(ch)) paths.add(signMap.get(ch));
        }
        String json = toJsonArray(paths);

        // ✅ Add CORS headers
        t.getResponseHeaders().add("Content-Type", "application/json");
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        t.sendResponseHeaders(200, json.getBytes().length);
        OutputStream os = t.getResponseBody();
        os.write(json.getBytes());
        os.close();
    }
}


    // Manual JSON array builder
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
