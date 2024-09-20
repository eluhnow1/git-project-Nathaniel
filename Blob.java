import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Blob {
    //Generate a unique filename using SHA1 hash of file data
    private static String generateSha1(String filePath) throws IOException, NoSuchAlgorithmException {
        //Creates SHA1 hash from the file content
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = sha1.digest(Files.readAllBytes(Paths.get(filePath)));
        //Converts hash bytes to string
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    //Create a new blob in the objects directory
    public static void createBlob(String filePath) throws IOException, NoSuchAlgorithmException, ObjectsDirectoryNotFoundException {
        //Generates SHA1 hash
        String sha1Filename = generateSha1(filePath);
        // Check if the objects directory exists
        if (!Files.exists(Paths.get("git/objects"))) {
            throw new ObjectsDirectoryNotFoundException("Objects directory does not exist. Please initialize the repository");
        }
        //Copies the original file to the objects directory with the new name
        Files.copy(Paths.get(filePath), Paths.get("git/objects", sha1Filename));
        // Inserts a new line into index
        String fileName = Paths.get(filePath).getFileName().toString();
        String indexEntry = sha1Filename + " " + fileName + "\n";
        Files.write(Paths.get("git/index"), indexEntry.getBytes(StandardCharsets.UTF_8));
    }

    //Tests the Blob creation
    public static void main(String[] args) {
        try {
            Git.initGitRepo();
            resetTestFiles();
            //creates an example file for testing and tests blob creation on it
            try {
                Files.write(Paths.get("example.txt"), "this is an example file...".getBytes(StandardCharsets.UTF_8));
                createBlob("example.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Files.write(Paths.get("example2.txt"), "this is an example file...".getBytes(StandardCharsets.UTF_8));
                createBlob("example2.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Blob created sucessfully");
            //Tests if the hash and file contents are correct
            String officialSha1Hash = "b48738d9d870657c557db32bbcf5011f4691da54";
            boolean hashCreatedSuccessfully = false;
            boolean fileContentsCorrect = false;
            Path blobPath=Paths.get("git/objects/" + officialSha1Hash);
            if (Files.exists(blobPath)) {
                hashCreatedSuccessfully = true;
                byte[] blobContentBytes = Files.readAllBytes(blobPath);
                String blobContent = new String(blobContentBytes, StandardCharsets.UTF_8);
                if (blobContent.equals("this is an example file...")) {
                    fileContentsCorrect = true;
                }
            }
            System.out.println("Hash created successfully: " + hashCreatedSuccessfully);
            System.out.println("File contents correct: " + fileContentsCorrect);
        } catch (IOException | NoSuchAlgorithmException | ObjectsDirectoryNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    //Removes test files: example.txt, the corresponding blob, and the index entry
    private static void resetTestFiles() throws IOException, NoSuchAlgorithmException {
        //Deletes the blob file
        try {
            Files.write(Paths.get("example.txt"), "this is an example file...".getBytes(StandardCharsets.UTF_8));//Only making this to get the hash code if example doesn't exist
        } catch (IOException e) {
            e.printStackTrace();
        }
        String sha1Hash = generateSha1("example.txt");
        Path blobFile = Paths.get("git/objects", sha1Hash);
        Files.deleteIfExists(blobFile);
        System.out.println("Deleted blob file: " + blobFile.toString());

        //Removes the corresponding line in the index file
        Path indexFile = Paths.get("git/index");
        if (Files.exists(indexFile)) {
            //Reads all lines from the index file into string
            String blobEntry = sha1Hash + " example.txt";
            String indexContent = new String(Files.readAllBytes(indexFile), StandardCharsets.UTF_8);
            //Removes specific entry
            String updatedIndexContent = indexContent.replace(blobEntry + "\n", "");
            Files.write(indexFile, updatedIndexContent.getBytes(StandardCharsets.UTF_8));
            System.out.println("Removed the entry from the index file");
        }
        //Deletes the example.txt file
        Path exampleFile = Paths.get("example.txt");
        Files.deleteIfExists(exampleFile); // Delete if it exists
        System.out.println("Deleted example.txt");
    }
}

//Creates custom error message
class ObjectsDirectoryNotFoundException extends Exception {
    public ObjectsDirectoryNotFoundException(String message) {
        super(message);
    }
}

