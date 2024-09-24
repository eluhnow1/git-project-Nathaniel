import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Blob {
    //Global variable to toggle compression on or off
    private static boolean compressionEnabled = true;

    //Generates a unique filename using SHA1 hash of file data
    private static String generateSha1(String filePath) throws IOException, NoSuchAlgorithmException {
        byte[] fileBytes = readFileContent(filePath);
        //Creates SHA1 hash from the file content
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = sha1.digest(fileBytes);
        //Converts hash bytes to string
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    //Reads file content and compresses it if enabled
    private static byte[] readFileContent(String filePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
        if (compressionEnabled) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(); //output stream that writes the input data into a byte array
            try (ZipOutputStream zipper = new ZipOutputStream(byteStream)){
                zipper.putNextEntry(new ZipEntry(Paths.get(filePath).getFileName().toString()));//Creates a new zip file to be written
                zipper.write(fileBytes); //Writes the original file bytes to the zip entry
                zipper.closeEntry(); 
            }
            catch (IOException e){
                e.printStackTrace();
            }
            return byteStream.toByteArray(); //Returns the compressed bytes
        }
        return fileBytes; //This only happens if compression is disabled
    }

    //Creates a new blob in the objects directory
    public static void createBlob(String filePath) throws IOException, NoSuchAlgorithmException, ObjectsDirectoryNotFoundException {
        String sha1Filename = generateSha1(filePath);
        //Checks if the objects directory exists
        if (!Files.exists(Paths.get("git/objects"))) {
            throw new ObjectsDirectoryNotFoundException("Objects directory does not exist. Please initialize the repository");
        }
        //Reads the file content and writes it into the blob
        byte[] fileBytes = readFileContent(filePath);
        Files.write(Paths.get("git/objects", sha1Filename), fileBytes);
        
        //Writes a new line into the index
        String fileName = Paths.get(filePath).getFileName().toString();
        String indexEntry = sha1Filename + " " + fileName + "\n";
        Files.write(Paths.get("git/index"), indexEntry.getBytes(StandardCharsets.UTF_8));
    }

    //Tests the Blob creation, works with both compression and not
    public static void main(String[] args) {
        try {
            Git.initGitRepo();
            resetTestFiles();
            //Creates string with 10,000 a's
            StringBuilder sb = new StringBuilder("");
            for (int i=0; i<10000; i++) {
                sb.append("a");
            }
            System.out.println("Compression Enabled: " + compressionEnabled);
            Files.write(Paths.get("example.txt"), sb.toString().getBytes(StandardCharsets.UTF_8));
            createBlob("example.txt");
            System.out.println("Blob created successfully");

            //Tests if the hash and file contents are correct
            String officialSha1Hash = generateSha1("example.txt");
            boolean hashCreatedSuccessfully = Files.exists(Paths.get("git/objects", officialSha1Hash));
            boolean fileContentsCorrect = false;
            boolean indexContentCorrect = false;
            //Tests if the contents of the blob were copied correctly
            if (hashCreatedSuccessfully) {
                byte[] blobContentBytes = Files.readAllBytes(Paths.get("git/objects", officialSha1Hash));
                byte[] originalFileBytes = readFileContent("example.txt");
                fileContentsCorrect = java.util.Arrays.equals(blobContentBytes, originalFileBytes);
            }
            //Tests if the line in the index file is correctly formatted
            for (String line : Files.readAllLines(Paths.get("git/index"))) {
                if (line.equals(officialSha1Hash+" example.txt")) {
                    indexContentCorrect=true;
                    break;
                }
            }
            //Tests if compression works
            if (compressionEnabled) {
                byte[] blobContentBytes = Files.readAllBytes(Paths.get("git/objects", officialSha1Hash));
                byte[] uncompressedBytes = Files.readAllBytes(Paths.get("example.txt"));
                System.out.println("Original Size: " + uncompressedBytes.length);
                System.out.println("Compressed Size: " + blobContentBytes.length);
            }

            System.out.println("Hash created successfully: " + hashCreatedSuccessfully);
            System.out.println("File contents correct: " + fileContentsCorrect);
            System.out.println("Index line Correct: " + indexContentCorrect);

            resetTestFiles();
        } catch (IOException | NoSuchAlgorithmException | ObjectsDirectoryNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Removes test files: example.txt, the corresponding blob, and the index entry
    private static void resetTestFiles() throws IOException, NoSuchAlgorithmException {
        //Creates string with 10,000 a's
        StringBuilder sb = new StringBuilder("");
        for (int i=0; i<10000; i++) {
            sb.append("a");
        }
        //Deletes the blob file
        try {
            Files.write(Paths.get("example.txt"), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String sha1Hash = generateSha1("example.txt");
        Path blobFile = Paths.get("git/objects", sha1Hash);
        Files.deleteIfExists(blobFile);
        System.out.println("Deleted blob file: " + blobFile.toString());

        //Removes the line in the index file
        Path indexFile = Paths.get("git/index");
        if (Files.exists(indexFile)) {
            String blobEntry = sha1Hash + " example.txt";
            String indexContent = new String(Files.readAllBytes(indexFile), StandardCharsets.UTF_8);
            String updatedIndexContent = indexContent.replace(blobEntry + "\n", "");
            Files.write(indexFile, updatedIndexContent.getBytes(StandardCharsets.UTF_8));
            System.out.println("Removed the entry from the index file");
        }

        //Deletes the example.txt file
        Path exampleFile = Paths.get("example.txt");
        Files.deleteIfExists(exampleFile);
        System.out.println("Deleted example.txt");
    }
}

//Creates custom error message
class ObjectsDirectoryNotFoundException extends Exception {
    public ObjectsDirectoryNotFoundException(String message) {
        super(message);
    }
}
