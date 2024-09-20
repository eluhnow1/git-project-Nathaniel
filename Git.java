import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
public class Git{
    public static void main (String [] args){
        testRepoInit();
    }

    public static void initGitRepo () {
        int pathExistsCounter=0;
        //Creates the "git" directory
        try {
            Files.createDirectory(Paths.get("git"));
        } catch (IOException e) {
            e.printStackTrace();
            pathExistsCounter++;
        }
        //Creates the "objects" directory inside in "git" directory
        try {
            Files.createDirectory(Paths.get("git/objects"));
        } catch (IOException e) {
            e.printStackTrace();
            pathExistsCounter++;
        }
        //Creates the "index" file in the "git" folder
        try {
            Files.createFile(Paths.get("git/index"));
        } catch (IOException e) {
            e.printStackTrace();
            pathExistsCounter++;
        }
        //Prints custom message if all paths already exist
        if (pathExistsCounter>=3) {
            System.out.println("Git Repository already exists");
        }
    }

    //Tests main methods
    private static void testRepoInit() {
        //Testing file creation
        initGitRepo();
        //Checks for created files
        System.out.println("git dir exists: " + doesPathExist("git"));
        System.out.println("git/objects dir exists: " + doesPathExist("git/objects"));
        System.out.println("git/index file exists: " + doesPathExist("git/index"));
        //Testing custom already created message
        initGitRepo();
    }

    //Checks if a path exists, returns the boolean answer
    private static boolean doesPathExist (String path) {
        return Files.exists(Paths.get(path));
    }

    //Deletes chosen path, returns true if path deleted, false if path did not exist
    private static boolean deletePath(String path) {
        try {
            Path targetPath = Paths.get(path);
            //Case for nonexistent path
            if (Files.notExists(targetPath)) {
                return false;
            }
            //If the path is a regular file, just deletes it
            if (Files.isRegularFile(targetPath)) {
                Files.delete(targetPath);
                return true;
            }
            //Recursively deletes elements in the file tree
            Files.walkFileTree(targetPath, new SimpleFileVisitor<Path>() {
                @Override //This is just used to help catch errors
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }//This basically says, whenever you encounter a file while walking through the tree, delete it
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }//Ensures that the now-empty directory is deleted
            });
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}