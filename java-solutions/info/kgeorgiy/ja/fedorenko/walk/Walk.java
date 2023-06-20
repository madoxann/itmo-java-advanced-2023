package info.kgeorgiy.ja.fedorenko.walk;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Walk {
    private static final int HASH_SIZE = 64;

    private static BigInteger getHashBInt(String inFile) {
        try (FileInputStream in = new FileInputStream(inFile)) {
            int readCh;
            byte[] buffer = new byte[1024];
            MessageDigest sha = MessageDigest.getInstance("SHA-256");

            while ((readCh = in.read(buffer)) != -1) {
                sha.update(buffer, 0, readCh);
            }

            return new BigInteger(1, sha.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e.getMessage());
        } catch (IOException | SecurityException e) {
            return BigInteger.ZERO;
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
                    out.write(String.format("%0" + HASH_SIZE + "x %s", getHashBInt(inFile), inFile));
                    out.newLine();
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