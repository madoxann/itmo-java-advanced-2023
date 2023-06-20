package info.kgeorgiy.ja.fedorenko.walk;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class RecursiveWalk {
    private static final int HASH_SIZE = 64;

    private static BigInteger getHashBInt(Path inFile) {
        BigInteger hash;

        try (FileInputStream in = new FileInputStream(inFile.toFile())) {
            int readCh;
            byte[] buffer = new byte[1024];
            MessageDigest sha = MessageDigest.getInstance("SHA-256");

            while ((readCh = in.read(buffer)) != -1) {
                sha.update(buffer, 0, readCh);
            }
            hash = new BigInteger(1, sha.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Digest: " + e.getMessage());
        } catch (IOException | SecurityException | FileSystemNotFoundException | UnsupportedOperationException e) {
            hash = BigInteger.ZERO;
        }

        return hash;
    }

    private static Map<String, BigInteger> walk(String fileName) {
        Map<String, BigInteger> fileMap = new HashMap<>();

        try {
            Path file = Paths.get(fileName);

            if (Files.isDirectory(file)) {
                try (Stream<Path> entries = Files.list(file)) {
                    entries.forEach(path -> fileMap.putAll(walk(path.toString())));

                    return fileMap;
                }
            } else return Map.of(fileName, getHashBInt(file));
        } catch (IOException | InvalidPathException | SecurityException e) {
            return Map.of(fileName, BigInteger.ZERO);
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Incorrect argument count!");
            return;
        } else if (args[0] == null || args[1] == null) {
            System.err.println("Incorrect arguments: files cannot be null!");
            return;
        }

        try {
            Path outPath = Paths.get(args[1]);
            if (outPath.getParent() != null) {
                Files.createDirectories(outPath.getParent());
            }

            try (BufferedReader in = new BufferedReader(new FileReader(args[0], StandardCharsets.UTF_8));
                 BufferedWriter out = new BufferedWriter(new FileWriter(args[1], StandardCharsets.UTF_8))) {
                String inFile;

                while ((inFile = in.readLine()) != null) {
                    for (Map.Entry<String, BigInteger> entry : walk(inFile).entrySet()) {
                        out.write(String.format("%0" + HASH_SIZE + "x %s", entry.getValue(), entry.getKey()));
                        out.newLine();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("IOException thrown: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Security violation occurred: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Invalid path: " + e.getMessage());
        }
    }
}