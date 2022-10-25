package gitlet;

import java.io.*;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  @author Ryan Hayame
 */
public class Repository {
    /*

      List all instance variables of the Repository class here with a useful
      comment above them describing what that variable represents and how that
      variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /** The .gitlet/objects directory. */
    public static final File GITLET_OBJECTS_DIR = join(GITLET_DIR, "objects");
    /** The .gitlet/refs directory. */
    public static final File GITLET_REFS_DIR = join(GITLET_DIR, "refs");
    /** The .gitlet/refs/heads directory. */
    public static final File GITLET_HEADS_DIR = join(GITLET_REFS_DIR, "heads");
    /** The .gitlet/refs/paths file.?? */
    public static final File GITLET_REMOTE_PATHS_FILE = join(GITLET_REFS_DIR, "paths");
    /** The .gitlet/index file. */
    public static final File GITLET_INDEX_FILE = join(GITLET_DIR, "index");
    /** The .gitlet/HEAD file. */
    public static final File GITLET_HEAD_FILE = join(GITLET_DIR, "HEAD");
    /** The .gitlet/refs/heads/master file. */
    public static final File master_file = join(GITLET_HEADS_DIR, "master");


    /** Prepares the directories and folders for the init command */
    public static void initPrep() throws IOException {
        if (GITLET_DIR.exists()) {
            message("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        } else {
            GITLET_DIR.mkdirs();
            GITLET_OBJECTS_DIR.mkdirs();
            GITLET_REFS_DIR.mkdirs();
            GITLET_HEADS_DIR.mkdirs();

            GITLET_REMOTE_PATHS_FILE.createNewFile();
            GITLET_INDEX_FILE.createNewFile();
            GITLET_HEAD_FILE.createNewFile();
            master_file.createNewFile();
        }
    }

    /** Main part of the init command */
    public static void initMain() throws IOException {
        initPrep();
        Commit initialCommit = new Commit();
        String hashedInitialCommitFile = writeObjectIntoObjectsDirectory(initialCommit);
        // Simple: points master pointer to initial commit
        // Complex: writes SHA1 ID of initial commit file into .gitlet/refs/heads/master file
        writeContents(master_file, hashedInitialCommitFile);
        updateHead("master");
    }

    public static String writeObjectIntoObjectsDirectory(Serializable object) {
        String SHA1ID = sha1(serialize(object));
        String twoCharacters = firstTwo(SHA1ID);
        File twoCharacters_DIR = join(GITLET_OBJECTS_DIR, twoCharacters);
        if (!twoCharacters_DIR.isDirectory()) {
            twoCharacters_DIR.mkdirs();
        }
        File SHA1HashFile = join(twoCharacters_DIR, SHA1ID);
        writeObject(SHA1HashFile, object);
        return SHA1ID;
    }

    public static String writeBlobIntoObjectsDirectory(byte[] blob) {
        String SHA1ID = sha1(blob);
        String twoCharacters = firstTwo(SHA1ID);
        File twoCharacters_DIR = join(GITLET_OBJECTS_DIR, twoCharacters);
        if (!twoCharacters_DIR.isDirectory()) {
            twoCharacters_DIR.mkdirs();
        }
        File SHA1HashFile = join(twoCharacters_DIR, SHA1ID);
        writeContents(SHA1HashFile, blob);
        return SHA1ID;
    }

    // gets the first two characters of a string
    public static String firstTwo(String str) {
        return str.substring(0, 2);
    }

    // gets the first seven characters of a string
    public static String firstSeven(String str) {
        return  str.substring(0, 7);
    }

    /** Simple: points HEAD* pointer to active branch/initial commit
    Complex: writes active branch name into .gitlet/HEAD file */
    public static void updateHead(String branchName) {
        writeContents(GITLET_HEAD_FILE, branchName);
    }

    // gets SHA1 ID string of head
    public static String getHeadCommitID() {
        return readContentsAsString(getActiveBranch());
    }

    // Gets Commit from objects directory using SHA1ID of the commit
    public static Commit getCommitFromID(String SHA1ID) {
        File SHA1HashFile = getFileFromObjectsFolder(SHA1ID);
        return readObject(SHA1HashFile, Commit.class);
    }

    public static File getFileFromObjectsFolder(String SHA1ID) {
        File f = join(GITLET_OBJECTS_DIR, firstTwo(SHA1ID));
        if (!f.isDirectory()) {
            message("No commit with that id exists.");
            System.exit(0);
        }
        File f2 = join(f, SHA1ID);
        if (!f2.isFile()) {
            message("No commit with that id exists.");
            System.exit(0);
        }
        return f2.getAbsoluteFile();
    }


    public static void add(String fileName) throws IOException {
        // file -> bytes (blob)
        File absolutePath = checkIfFileExists(fileName, CWD);
        byte[] blob = readContents(absolutePath);
        // If the current working version of the file is identical to the version in the current commit,
        // do not stage it to be added, and remove it from the staging area if it is already there
        Commit oldCommit = getCommitFromID(getHeadCommitID());
        String treeSHA1ID = oldCommit.tree;
        // bytes(blob) -> SHA1 ID string; blob also added to objects directory
        String BlobSHA1ID = writeBlobIntoObjectsDirectory(blob);

        // see if file is identical to previous commit
        boolean differentFiles = compareToCurrentCommit(treeSHA1ID, absolutePath, blob);

        // if something is staged already
        if (GITLET_INDEX_FILE.length() != 0) {
            Stage alreadyStaged = readFromIndex();
            // if the file was already staged for removal, it will no longer we staged for removal
            if (removeFromRemovalStage(absolutePath)) {
                return;
            }
            // if the file was already staged for addition
            if (alreadyStaged.additionStageTree.containsKey(absolutePath)) {
                // update only if update is not identical to previous commit
                // if identical to previous commit, do not stage and remove from staging area
                if (differentFiles) {
                    alreadyStaged.additionStageTree.replace(absolutePath, BlobSHA1ID);
                } else {
                    // removes a file from staging area if it was staged for addition
                    if (removeFromAdditionStage(absolutePath)) {
                        return;
                    }
                }
            }
            // if the file was not already staged for either addition or removal
            // adds to addition stage only if different from version in current commit
            if (differentFiles) {
                alreadyStaged.additionStageTree.put(absolutePath, BlobSHA1ID);
                writeToIndex(alreadyStaged);
            }
        } else {
            // if nothing is staged
            // adds to addition stage only if different from version in current commit
            if (differentFiles) {
                // stage object written to index
                Stage stagedToAdd = new Stage(absolutePath, BlobSHA1ID, "add");
                writeToIndex(stagedToAdd);
            }
        }
    }

    // compares a file + blob to the file + blob in current commit
    // returns false if the files are identical and true if they are different
    public static boolean compareToCurrentCommit(String treeSHA1ID, File absolutePath, byte[] blob) {
        // if there is one real commit already
        if (!treeSHA1ID.isEmpty()) {
            TreeMap<File, String> oldCommitTree = getTreeMapFromObjectsFolder(treeSHA1ID);
            // if the file to add is NOT a completely new file
            if (oldCommitTree.containsKey(absolutePath)) {
                // checks if file contents have NOT been changed since last commit
                if (oldCommitTree.get(absolutePath).equals(sha1(blob))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static File checkIfFileExists(String fileName, File directory) {
        boolean check = new File(directory, fileName).exists();
        // makes sure the file exists in working directory
        if (!check) {
            message("File does not exist.");
            System.exit(0);
        }
        File f = new File(fileName);
        return f.getAbsoluteFile();
    }

    public static void writeToIndex (Stage stage) {
        writeObject(GITLET_INDEX_FILE, stage);
    }

    public static Stage readFromIndex () {
        return readObject(GITLET_INDEX_FILE, Stage.class);
    }

    public static void clearStagingArea() throws IOException {
        if (GITLET_INDEX_FILE.delete()) {
            GITLET_INDEX_FILE.createNewFile();
        }
    }

    public static void commit(String message) throws IOException {
        // check staging area; if no files staged, abort
        if (GITLET_INDEX_FILE.length() == 0) {
            message("No changes added to the commit.");
            System.exit(0);
        }
        // check for blank message
        if (message.isEmpty()) {
            message("Please enter a commit message.");
            System.exit(0);
        }
        // creates new commit with same stuff as old one
        Commit oldCommit = getCommitFromID(getHeadCommitID());
        String treeSHA1ID = oldCommit.tree;
        Commit newCommit = new Commit(message, getHeadCommitID(), treeSHA1ID);
        // gets stage object from index file
        Stage stagedObject = readFromIndex();
        // if this is NOT the first real commit
        if (!treeSHA1ID.isEmpty()) {
            // get old commit tree
            File oldCommitTreeFile = getFileFromObjectsFolder(treeSHA1ID);
            Stage oldCommitStage = readObject(oldCommitTreeFile, Stage.class);
            TreeMap<File, String> oldCommitTree = oldCommitStage.commitTree;
            // iterates through addition stage tree
            Set<Map.Entry<File, String>> addSet = stagedObject.additionStageTree.entrySet();
            for (Map.Entry<File, String> entry : addSet) {
                File key = entry.getKey();
                String value = entry.getValue();
                // if old commit tree contains same file as addition stage, update the value
                if (oldCommitTree.containsKey(key)) {
                    oldCommitTree.replace(key, value);
                } else {
                    // if old commit tree does not contain this file yet, add it to tree
                    oldCommitTree.put(key, value);
                }
            }
            // iterates through remove stage tree and removes files + blobs from being tracked in tree
            Set<Map.Entry<File, String>> removeSet = stagedObject.removalStageTree.entrySet();
            for (Map.Entry<File, String> entry : removeSet) {
                File key = entry.getKey();
                oldCommitTree.remove(key);
            }
            // creates a new stage object with an updated version of the old commit tree
            Stage newCommitStage = new Stage(oldCommitTree);
            // newCommitStage is stored in objects directory and referenced by the new commit
            newCommit.tree = writeObjectIntoObjectsDirectory(newCommitStage);
        } else {
            // if this is the first real commit (must be adding)
            // creates a new stage object with a commit tree equal to the files + blobs staged for addition
            Stage newCommitStage = new Stage(stagedObject.additionStageTree);
            // newCommitStage is stored in objects directory and referenced by the new commit
            newCommit.tree = writeObjectIntoObjectsDirectory(newCommitStage);
        }
        // adds new commit to objects directory
        String newCommitSHA1ID = writeObjectIntoObjectsDirectory(newCommit);
        updateActiveBranch(newCommitSHA1ID);
        updateHead(getActiveBranch().getName());
        clearStagingArea();
    }

    public static File getActiveBranch() {
        String activeBranch = readContentsAsString(GITLET_HEAD_FILE);
        File activeBranchFile = join(GITLET_HEADS_DIR, activeBranch);
        return activeBranchFile;
    }

    public static void updateActiveBranch(String newCommitSHA1ID) {
        writeContents(getActiveBranch(), newCommitSHA1ID);
    }

    public static void rm(String fileName) throws IOException {
        Commit oldCommit = getCommitFromID(getHeadCommitID());
        String treeSHA1ID = oldCommit.tree;
        File file = join(CWD, fileName);
        if (!file.isFile()) {
            file.createNewFile();
        }
        File absolutePath = file.getAbsoluteFile();
        // removes a file from staging area (index) if it was staged for addition
        if (removeFromAdditionStage(absolutePath)) {
            return;
        }
        // if trying to rm a file with only initial commit
        if (treeSHA1ID.isEmpty()) {
            message("No reason to remove the file.");
            System.exit(0);
        }
        // only works if there is at least one commit
        TreeMap<File, String> oldCommitTree = getTreeMapFromObjectsFolder(treeSHA1ID);
        // if trying to rm a file that does not exist in old commit tree
        if (!oldCommitTree.containsKey(absolutePath)) {
            message("No reason to remove the file.");
            System.exit(0);
        } else {
            // if trying to rm a file that exists in old commit tree
            // stages file + blob for removal in index
            writeToIndexForRemoval(oldCommitTree, absolutePath);
            // removes file from working directory
            absolutePath.delete();
        }
    }

    // only works if there is at least one real commit
    // gets commitTree from objects folder using tree SHA1 ID string
    public static TreeMap<File, String> getTreeMapFromObjectsFolder(String treeSHA1ID) {
        File treeFile = getFileFromObjectsFolder(treeSHA1ID);
        Stage stageObject = readObject(treeFile, Stage.class);
        return stageObject.getCommitTree();
    }

    // gets file and blob from most recent commit and adds it to "staged for removal" tree map of stage object
    // writes stage object into index file for removal
    public static void writeToIndexForRemoval(TreeMap<File, String> oldCommitTree, File absolutePath) {
        String blobSHA1ID = oldCommitTree.get(absolutePath);
        Stage stagedObject;
        // if there is nothing staged, make a new staged object
        if (GITLET_INDEX_FILE.length() == 0) {
            stagedObject = new Stage(absolutePath, blobSHA1ID, "remove");
        } else {
            // if there is already something staged, updated staged object
            stagedObject = readFromIndex();
            stagedObject.removalStageTree.put(absolutePath, blobSHA1ID);
            GITLET_INDEX_FILE.delete();
        }
        writeToIndex(stagedObject);
    }

    // removes a file from staging area if it is there for addition
    // does not check to see if contents of file are the same
    // returns true if the file is removed
    public static boolean removeFromAdditionStage(File f) {
        if (GITLET_INDEX_FILE.length() == 0) {
            return false;
        }
        Stage stagedObject = readFromIndex();
        Set<Map.Entry<File, String>> addSet = stagedObject.additionStageTree.entrySet();
        for (Map.Entry<File, String> entry : addSet) {
            File key = entry.getKey();
            String value = entry.getValue();
            if (key.equals(f)) {
                stagedObject.additionStageTree.remove(key);
                writeToIndex(stagedObject);
                // deletes the file's blob from objects folder
                getFileFromObjectsFolder(value).delete();
                return true;
            }
        }
        return false;
    }

    // removes a file from staging area if it is there for removal
    // does not check to see if contents of file are the same
    // returns true if the file is removed
    public static boolean removeFromRemovalStage(File f) {
        if (GITLET_INDEX_FILE.length() == 0) {
            return false;
        }
        Stage stagedObject = readFromIndex();
        Set<Map.Entry<File, String>> removeSet = stagedObject.removalStageTree.entrySet();
        for (Map.Entry<File, String> entry : removeSet) {
            File key = entry.getKey();
            if (key.equals(f)) {
                stagedObject.removalStageTree.remove(key);
                writeToIndex(stagedObject);
                return true;
            }
        }
        return false;
    }

    public static byte[] getBlobFromObjectsFolder(String BlobSHA1ID) {
        File BlobFile = getFileFromObjectsFolder(BlobSHA1ID);
        return readContents(BlobFile);
    }

    // Same thing as checkoutTwo except from head commit
    public static void checkoutOne(String fileName) throws IOException {
        checkoutTwo(getHeadCommitID(), fileName);
    }

    public static byte [] getFileContentsFromCommitId(String commitID, String fileName) throws IOException {
        File f = join(GITLET_OBJECTS_DIR, firstTwo(commitID));
        File f2 = join(f, commitID);
        Commit commit = getCommitFromID(commitID);
        // if commit with that commitID exists in objects folder
        if (f.isDirectory() && f2.isFile() && !commit.tree.isEmpty()) {
            TreeMap<File, String> commitTreeMap = getTreeMapFromObjectsFolder(commit.tree);
            boolean check = new File(CWD, fileName).exists();
            String blob = "";
            File absolutePath;
            // if file is in CWD
            if (check) {
                absolutePath = join(CWD, fileName).getAbsoluteFile();
            } else {
                // if file is not in CWD
                File newFile = join(CWD, fileName);
                newFile.createNewFile();
                absolutePath = newFile.getAbsoluteFile();
            }
            blob = commitTreeMap.get(absolutePath);
            if (blob == null) {
                message("File does not exist in that commit.");
                System.exit(0);
            }
            return getBlobFromObjectsFolder(blob);
        } else if (commit.tree.isEmpty()) {
            // if the chosen commit is the initial commit
            message("File does not exist in that commit.");
            System.exit(0);
            return null;
        } else {
            // if the commit does not exist
            return null;
        }
    }

    /** Takes the version of the file as it exists in the commit with the given id, and puts
    it in the working directory, overwriting the version of the file that’s already there
    if there is one. The new version of the file is not staged.
    If no commit with the given id exists, print No commit with that id exists. Otherwise, if
    the file does not exist in the given commit, print the same message as for failure case 1.
    Do not change the CWD. */

    // takes version of file as it exists in given commitID and puts in CWD, overwriting version if it's already there
    public static void checkoutTwo(String commitID, String fileName) throws IOException {
        if (commitID.length() < 40) {
            commitID = getFullID(commitID);
        }
        byte [] blob = getFileContentsFromCommitId(commitID, fileName);
        if (blob != null) {
            overwriteFileInCWD(fileName, blob);
        } else {
            message("No commit with that id exists.");
            System.exit(0);
        }
    }

    public static void overwriteFileInCWD(String fileName, byte [] blob) throws IOException {
        File file = join(CWD, fileName);
        file.delete();
        file.createNewFile();
        writeContents(file, blob);
    }

    // Gets full SHA1ID of from objects folder using shortened ID
    public static String getFullID(String shortenedID) {
        File folder = join(GITLET_OBJECTS_DIR, firstTwo(shortenedID));
        if (!folder.isDirectory()) {
            message("No commit with that id exists.");
            System.exit(0);
        }
        List<String> fileNames = new ArrayList<>();
        for (File fileEntry : folder.listFiles()) {
            fileNames.add(fileEntry.getName());
        }
        TreeMap<String, String> matches = new TreeMap<>();
        for (String element : fileNames) {
            if (element.startsWith(shortenedID)) {
                matches.put(element, shortenedID);
            }
        }
        if (matches.size() > 1) {
            message("Found multiple commits with that ID. Try to be more specific.");
            System.exit(0);
        } else if (matches.size() == 0) {
            message("No commit with that id exists.");
            System.exit(0);
        }
        return matches.firstKey();
    }

    public static void log() {
        Commit commit = getCommitFromID(getHeadCommitID());
        while (commit != null) {
            System.out.println(commit.convertToString());
            if (!commit.parent.isEmpty()) {
                commit = getCommitFromID(commit.parent);
            } else {
                commit = null;
            }
        }
    }

    public static void global_log() {
        // create hashset
        HashSet<String> hashSet = new HashSet<>();
        // get all branch pointers
        List<String> headsList = plainFilenamesIn(GITLET_HEADS_DIR);
        // get all unique commits in linear time
        for (String branch : headsList) {
            File branchFile = join(GITLET_HEADS_DIR, branch);
            String branchHeadCommitID = readContentsAsString(branchFile);
            recursivelyTraverseCommits(branchHeadCommitID, hashSet);
        }
        // get all "lost" commits from resetting
        if (GITLET_REMOTE_PATHS_FILE.length() != 0) {
            ArrayList<String> lostCommits = getLostCommitObject().lostCommitsArrayList;
            for (String commitSHA1ID : lostCommits) {
                recursivelyTraverseCommits(commitSHA1ID, hashSet);
            }
        }
    }

    // recursively traverses through commits and adds them to a hash set while also printing unique commits
    public static void recursivelyTraverseCommits(String commitID, HashSet<String> hashSet) {
        if (hashSet.contains(commitID)) {
            return;
        } else {
            // if the hash set does not contain the commit ID
            hashSet.add(commitID);
            Commit commit = getCommitFromID(commitID);
            String parent = commit.parent;
            String parentTwo = commit.parentTwo;
            if (!parent.isEmpty()) {
                recursivelyTraverseCommits(parent, hashSet);
            }
            if (!parentTwo.isEmpty()) {
                recursivelyTraverseCommits(parentTwo, hashSet);
            }
            System.out.println(commit.convertToString());
        }
    }

    public static int matches;
    public static void find(String message) {
        // create hashset
        HashSet<String> hashSet = new HashSet<>();
        // get all branch heads
        List<String> headsList = plainFilenamesIn(GITLET_HEADS_DIR);
        // tracks number of matches
        matches = 0;
        // get all unique commits in linear time
        for (String branch : headsList) {
            File branchFile = join(GITLET_HEADS_DIR, branch);
            String branchHeadCommitID = readContentsAsString(branchFile);
            recursivelyTraverseCommits2(branchHeadCommitID, hashSet, message);
        }
        // get all "lost" commits from resetting
        if (GITLET_REMOTE_PATHS_FILE.length() != 0) {
            ArrayList<String> lostCommits = getLostCommitObject().lostCommitsArrayList;
            for (String commitSHA1ID : lostCommits) {
                recursivelyTraverseCommits2(commitSHA1ID, hashSet, message);
            }
        }
        if (matches == 0) {
            message("Found no commit with that message.");
        }
    }

    // recursively traverses through commits and adds them to a hash set while also printing commit IDs if message is found
    public static void recursivelyTraverseCommits2(String commitID, HashSet<String> hashSet, String message) {
        if (hashSet.contains(commitID)) {
            return;
        } else {
            hashSet.add(commitID);
            Commit commit = getCommitFromID(commitID);
            String parent = commit.parent;
            String parentTwo = commit.parentTwo;
            if (!parent.isEmpty()) {
                recursivelyTraverseCommits2(parent, hashSet, message);
            }
            if (!parentTwo.isEmpty()) {
                recursivelyTraverseCommits2(parentTwo, hashSet, message);
            }
            if (commit.message.equals(message)) {
                System.out.println(commitID);
                matches++;
            }
        }
    }

    // creates a new branch with the given name and points it at the current head commit
    public static void branch(String branchName) throws IOException {
        // if a branch with the given name already exists, print error
        boolean check = new File(GITLET_HEADS_DIR, branchName).exists();
        if (check) {
            message("A branch with that name already exists.");
            System.exit(0);
        }
        File newBranchFile = join(GITLET_HEADS_DIR, branchName);
        newBranchFile.createNewFile();
        writeContents(newBranchFile, getHeadCommitID());
    }

    public static void checkoutThree(String branchName) throws IOException {
        // If no branch with that name exists, print error
        boolean check = new File(GITLET_HEADS_DIR, branchName).exists();
        if (!check) {
            message("No such branch exists.");
            System.exit(0);
        }
        // If that branch is the current branch, print error
        if (branchName.equals(getActiveBranch().getName())) {
            message("No need to checkout the current branch.");
            System.exit(0);
        }
        // Takes all files in the commit at the head of the given branch, and puts them in the
        // working directory, overwriting the versions of the files that are already there if they exist
        File givenBranchFile = join(GITLET_HEADS_DIR, branchName);
        // stuff for head/current branch + commit
        String headCommitSHA1ID = getHeadCommitID();
        Commit headCommit = getCommitFromID(headCommitSHA1ID);
        // stuff for given/future branch + commit
        String newCommitSHA1ID = readContentsAsString(givenBranchFile);
        // fixes error that occurs when head branch is initial commit
        if (headCommit.tree.isEmpty()) {
            // if checking out from initial commit and there is a file in CWD
            List<String> CWDFiles = plainFilenamesIn(CWD);
            for (String file : CWDFiles) {
                if (file.endsWith(".txt")) {
                    message("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
            // if checking out from initial commit and there is no file in CWD
            clearStagingArea();
        } else {
            updateCWDFiles(newCommitSHA1ID, headCommitSHA1ID);
        }
        updateHead(branchName);
    }

    // takes all files given/new commit and puts them in CWD, overwriting if alr exist
    // any files in current commit but not new commit are deleted from the CWD
    // staging area is also cleared
    public static void updateCWDFiles(String newCommitSHA1ID, String currentCommitSHA1ID) throws IOException {
        Commit currentCommit = getCommitFromID(currentCommitSHA1ID);
        TreeMap<File, String> currentCommitTreeMap = getTreeMapFromObjectsFolder(currentCommit.tree);
        Commit newCommit = getCommitFromID(newCommitSHA1ID);
        List<String> CWDFiles = plainFilenamesIn(CWD);
        // if checking out initial commit
        if (newCommit.tree.isEmpty()) {
            // deletes all extra files in CWD
            for (String file : CWDFiles) {
                File absolutePath = checkIfFileExists(file, CWD);
                if (file.endsWith(".txt") && currentCommitTreeMap.containsKey(absolutePath)) {
                    restrictedDelete(absolutePath);
                }
            }
        } else {
            // gets tree map of files+blobs from commit being checked out
            TreeMap<File, String> newCommitTreeMap = getTreeMapFromObjectsFolder(newCommit.tree);
            Set<Map.Entry<File, String>> set = newCommitTreeMap.entrySet();
            // puts all files from new commit/branch into CWD
            for (Map.Entry<File, String> entry : set) {
                File key = entry.getKey();
                checkoutTwo(newCommitSHA1ID, key.getName());
            }
            // If a 1. working file is 2. untracked in the current branch and 3. would be overwritten by the checkout, print error
            for (String file : CWDFiles) {
                File absolutePath = checkIfFileExists(file, CWD);
                if (file.endsWith(".txt") && !currentCommitTreeMap.containsKey(absolutePath) &&
                        newCommitTreeMap.containsKey(absolutePath)) {
                    message("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
            // Any files that are tracked in the current branch but are not present in the checked-out branch are deleted.
            Set<Map.Entry<File, String>> set2 = currentCommitTreeMap.entrySet();
            for (Map.Entry<File, String> entry2 : set2) {
                File key = entry2.getKey();
                if (!newCommitTreeMap.containsKey(key)) {
                    restrictedDelete(key);
                }
            }
        }
        clearStagingArea();
    }

    public static void rm_branch(String branchName) {
        // get all branch heads
        List<String> headsList = plainFilenamesIn(GITLET_HEADS_DIR);
        // If a branch with the given name does not exist, print error
        if (!headsList.contains(branchName)) {
            message("A branch with that name does not exist.");
            System.exit(0);
        } else {
            File branch = join(GITLET_HEADS_DIR, branchName);
            // if you try to remove the branch you’re currently on, prints error
            if (getActiveBranch().equals(branch)) {
                message("Cannot remove the current branch.");
                System.exit(0);
            }
            branch.delete();
        }
    }

    public static void status() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("=== Branches ===\n");
        List<String> headsList = plainFilenamesIn(GITLET_HEADS_DIR);
        for (String branch : headsList) {
            if (readContentsAsString(GITLET_HEAD_FILE).equals(branch)) {
                buffer.append("*" + branch + "\n");
            } else {
                buffer.append(branch + "\n");
            }
        }
        buffer.append("\n=== Staged Files ===\n");
        if (GITLET_INDEX_FILE.length() != 0) {
            Stage stagedObject = readFromIndex();
            if (stagedObject.additionStageTree.size() != 0) {
                Set<File> keySet = stagedObject.additionStageTree.keySet();
                for (File file : keySet) {
                    String fileName = file.getName();
                    buffer.append(fileName + "\n");
                }
            }
        }
        buffer.append("\n=== Removed Files ===\n");
        if (GITLET_INDEX_FILE.length() != 0) {
            Stage stagedObject = readFromIndex();
            if (stagedObject.removalStageTree.size() != 0) {
                Set<File> keySet = stagedObject.removalStageTree.keySet();
                for (File file : keySet) {
                    String fileName = file.getName();
                    buffer.append(fileName + "\n");
                }
            }
        }
        buffer.append("\n=== Modifications Not Staged For Commit ===\n");
        buffer.append("\n=== Untracked Files ===\n");
        System.out.println(buffer.toString());
    }

    // pretty much same as checkout three except you can choose any commit and head pointer moves
    public static void reset(String commitID) throws IOException {
        if (commitID.length() < 40) {
            commitID = getFullID(commitID);
        }
        String currentCommitSHA1ID = getHeadCommitID();
        // checks out all files tracked by the given commit
        // removes tracked files (files in current commit) that are not present in given commit
        updateCWDFiles(commitID, currentCommitSHA1ID);
        // paths file keeps track of all "lost" commits in an array list
        // store new object there if this is the first lost commit
        if (GITLET_REMOTE_PATHS_FILE.length() == 0) {
            LostCommits newLostCommitsObject = new LostCommits(currentCommitSHA1ID);
            writeObject(GITLET_REMOTE_PATHS_FILE, newLostCommitsObject);
        } else {
            // update array list within object if this is not the first lost commit
            LostCommits lostCommitsObject = getLostCommitObject();
            lostCommitsObject.lostCommitsArrayList.add(currentCommitSHA1ID);
            GITLET_REMOTE_PATHS_FILE.delete();
            GITLET_REMOTE_PATHS_FILE.createNewFile();
            writeObject(GITLET_REMOTE_PATHS_FILE, lostCommitsObject);
        }
        // moves the current branch's head to that commit
        moveCurrentBranchHead(commitID);
        clearStagingArea();
    }

    // moves the current branch's head to that commit
    public static void moveCurrentBranchHead(String newBranchHeadCommit) throws IOException {
        File activeBranchFile = getActiveBranch();
        activeBranchFile.delete();
        activeBranchFile.createNewFile();
        writeContents(activeBranchFile, newBranchHeadCommit);
    }

    // gets "lost" commit objects
    /** "lost" commits are commits that cannot normally be reached by starting at the head of the branch
     * and working backwards to the initial commit by following the path of parents. they get lost by moving
     * the head of the branch.
    */
    public static LostCommits getLostCommitObject() {
        return readObject(GITLET_REMOTE_PATHS_FILE, LostCommits.class);
    }

    public static void merge(String branchName) throws IOException {
        // If there are staged additions or removals present, print error
        if (GITLET_INDEX_FILE.length() != 0) {
            message("You have uncommitted changes.");
            System.exit(0);
        }
        // if a branch with the given name does not exist, print error
        if (!join(GITLET_HEADS_DIR, branchName).exists()) {
            message("A branch with that name does not exist.");
            System.exit(0);
        }
        // if attempting to merge a branch with itself, print error
        if (branchName.equals(getActiveBranch().getName())) {
            message("Cannot merge a branch with itself.");
            System.exit(0);
        }

        // Setup for given branch
        File givenBranchFile = join(GITLET_HEADS_DIR, branchName);
        String givenBranchCommitID = readContentsAsString(givenBranchFile);
        Commit givenBranchCommit = getCommitFromID(givenBranchCommitID);

        // Setup for current/HEAD branch
        File currentBranchFile = getActiveBranch();
        String currentCommitID = readContentsAsString(currentBranchFile);
        Commit currentCommit = getCommitFromID(currentCommitID);

        // gets split point
        String splitPointID = traverseCommitsForSplitPoint(currentCommitID, givenBranchCommitID);
        Commit splitCommit = getCommitFromID(splitPointID);

        // if split point commit = branchName's commit, do nothing
        // ends with message "Given branch is an ancestor of the current branch."
        if (splitPointID.equals(givenBranchCommitID)) {
            message("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        // if split point commit = current branch commit, checkoutThree(branchName)
        // print message "Current branch fast-forwarded."
        if (splitPointID.equals(currentCommitID)) {
            checkoutThree(branchName);
            message("Current branch fast-forwarded.");
            System.exit(0);
        }

        // get all 3 commit trees
        TreeMap<File, String> currentCommitTreeMap = getTreeMapFromObjectsFolder(currentCommit.tree);
        TreeMap<File, String> givenCommitTreeMap = getTreeMapFromObjectsFolder(givenBranchCommit.tree);
        TreeMap<File, String> splitCommitTreeMap = new TreeMap<>();
        if (!splitCommit.tree.isEmpty()) {
            splitCommitTreeMap = getTreeMapFromObjectsFolder(splitCommit.tree);
        }

        //if an untracked file in current commit would be overwritten or deleted by merge, print error
        List<String> CWDFiles = plainFilenamesIn(CWD);
        for (String file : CWDFiles) {
            File absolutePath = checkIfFileExists(file, CWD);
            byte [] blob = readContents(absolutePath);
            String blobID = sha1(blob);
            // if a file is in CWD and current commit, but with different contents, print error
            if (file.endsWith(".txt") && currentCommitTreeMap.containsKey(absolutePath)
                    && !blobID.equals(currentCommitTreeMap.get(absolutePath))) {
                message("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
            // if a file is in CWD and not in current commit, print error
            if (file.endsWith(".txt") && !currentCommitTreeMap.containsKey(absolutePath)) {
                message("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        // get all unique files from all 3 commit trees
        HashSet<File> allFiles = new HashSet<>();
        getFilesFromTree(currentCommitTreeMap, allFiles);
        getFilesFromTree(givenCommitTreeMap, allFiles);
        getFilesFromTree(splitCommitTreeMap, allFiles);

        //do main stuff 1-8
        TreeMap<File, String> mergedTreeMap = new TreeMap<>();
        for (File f: allFiles) {
            String result = mergePrep(f, currentCommitTreeMap, givenCommitTreeMap, splitCommitTreeMap);
            if (result.equals("conflict")) {
                String conflictedFileContents = getConflictedFileContents2(currentCommitTreeMap, givenCommitTreeMap, f);
                // replace contents of the conflicted file and add into merged commit
                writeContents(f, conflictedFileContents);
                byte[] blob = readContents(f);
                String blobID = writeBlobIntoObjectsDirectory(blob);
                mergedTreeMap.put(f, blobID);
                message("Encountered a merge conflict.");
            } else if (!result.isEmpty()) {
                mergedTreeMap.put(f, result);
            }
        }
        String message = "Merged " + branchName + " into " + getActiveBranch().getName() + ".";
        // creates a new stage object with an updated version of the old commit tree
        Stage newCommitStage = new Stage(mergedTreeMap);
        // newCommitStage is stored in objects directory and referenced by the new commit
        String mergedCommitTree = writeObjectIntoObjectsDirectory(newCommitStage);
        // if merge would generate an error because the merged commit has no changes in it, print normal commit error message
        if (mergedCommitTree.equals(currentCommit.tree)) {
            message("No changes added to the commit.");
            System.exit(0);
        }
        // remakes CWD with merged changes
        remakeCWD(mergedTreeMap);
        Commit mergedCommit = new Commit(message, currentCommitID, givenBranchCommitID, mergedCommitTree);
        String newCommitSHA1ID = writeObjectIntoObjectsDirectory(mergedCommit);
        updateActiveBranch(newCommitSHA1ID);
        updateHead(getActiveBranch().getName());
    }

    // remakes CWD after merge with files only in merged commit tree
    public static void remakeCWD(TreeMap<File, String> mergedTreeMap) throws IOException {
        // deletes all text files in CWD
        List<String> CWDFiles = plainFilenamesIn(CWD);
        for (String file : CWDFiles) {
            File absolutePath = checkIfFileExists(file, CWD);
            if (file.endsWith(".txt")) {
                absolutePath.delete();
            }
        }
        // remakes file if it is in merged commit tree
        Set<Map.Entry<File, String>> set = mergedTreeMap.entrySet();
        for (Map.Entry<File, String> entry : set) {
            File key = entry.getKey();
            String value = entry.getValue();
            key.createNewFile();
            byte[] blob = getBlobFromObjectsFolder(value);
            writeContents(key, blob);
        }
    }

    public static String getConflictedFileContents2(TreeMap<File, String> currentCommitTreeMap, TreeMap<File, String> givenCommitTreeMap, File f) {
        String currentBranchContent = "";
        String givenBranchContent = "";
        if (currentCommitTreeMap.containsKey(f)) {
            String blob = currentCommitTreeMap.get(f);
            currentBranchContent = readContentsAsString(getFileFromObjectsFolder(blob));
        }
        if (givenCommitTreeMap.containsKey(f)) {
            String blob = givenCommitTreeMap.get(f);
            givenBranchContent = readContentsAsString(getFileFromObjectsFolder(blob));
        }
        String finalString = "<<<<<<< HEAD\n" + currentBranchContent + "=======\n" + givenBranchContent + ">>>>>>>";
        //System.out.println(finalString);
        return finalString;
    }

    /** public static String getConflictedFileContents (TreeMap<File, String> currentCommitTreeMap, TreeMap<File, String> givenCommitTreeMap, File f) {
        StringBuilder sb = new StringBuilder();
        String currentBranchContent = "";
        String givenBranchContent = "";
        if (currentCommitTreeMap.containsKey(f)) {
            String blob = currentCommitTreeMap.get(f);
            currentBranchContent = readContentsAsString(getFileFromObjectsFolder(blob));
        }
        if (givenCommitTreeMap.containsKey(f)) {
            String blob = givenCommitTreeMap.get(f);
            givenBranchContent = readContentsAsString(getFileFromObjectsFolder(blob));
        }
        sb.append("<<<<<<< HEAD\n");
        sb.append(currentBranchContent);
        sb.append("\n=======\n");
        sb.append(givenBranchContent);
        sb.append("\n>>>>>>>");
        return sb.toString();
    }
     */

    // other/master = given
    // head = current
    public static String mergePrep(File f, TreeMap<File, String> currentCommitTreeMap, TreeMap<File, String> givenCommitTreeMap, TreeMap<File, String> splitCommitTreeMap) {
        //System.out.println(f.getName() + " /// " + f);
        // if split contains the file
        if (splitCommitTreeMap.containsKey(f)) {
            // 3: in split, modified in other and head in same way (both removed), f stays removed
            if (splitCommitTreeMap.containsKey(f) && !currentCommitTreeMap.containsKey(f) && !givenCommitTreeMap.containsKey(f)) {
                //message("3");
                return "";
            }
            if (!givenCommitTreeMap.containsKey(f) && currentCommitTreeMap.containsKey(f)) {
                // 6: in split, unmodified in head, and absent in other, then remove
                if (splitCommitTreeMap.get(f).equals(currentCommitTreeMap.get(f)) && !givenCommitTreeMap.containsKey(f)) {
                    //message("6");
                    return "";
                }
            }
            if (!currentCommitTreeMap.containsKey(f) && givenCommitTreeMap.containsKey(f)) {
                // 7: in split, unmodified in other, and absent in head, then remain absent
                if (splitCommitTreeMap.get(f).equals(givenCommitTreeMap.get(f)) && !currentCommitTreeMap.containsKey(f)) {
                    //message("7");
                    return "";
                }
            }
            if (currentCommitTreeMap.containsKey(f) && givenCommitTreeMap.containsKey(f)) {
                // 1: in split, modified in other but not head, return other
                if (currentCommitTreeMap.get(f).equals(splitCommitTreeMap.get(f)) && !givenCommitTreeMap.get(f).equals(splitCommitTreeMap.get(f))) {
                    //message("1");
                    return givenCommitTreeMap.get(f);
                }
                // 2: in split, modified in head but not other, return head
                if (!currentCommitTreeMap.get(f).equals(splitCommitTreeMap.get(f)) && givenCommitTreeMap.get(f).equals(splitCommitTreeMap.get(f))) {
                    //message("2");
                    return currentCommitTreeMap.get(f);
                }
                // 3: in split, modified in other and head in same way (both same change), then use other or head
                if (splitCommitTreeMap.containsKey(f) && currentCommitTreeMap.get(f).equals(givenCommitTreeMap.get(f))) {
                    //message("3.1");
                    return currentCommitTreeMap.get(f);
                }
            }
            // 8: modified in other, and modified differently in head, then conflict (does not need to be in split)
            // modified can mean removed
            if (!givenCommitTreeMap.containsKey(f) && currentCommitTreeMap.containsKey(f)) {
                //message("8");
                return "conflict";
            }
            if (givenCommitTreeMap.containsKey(f) && !currentCommitTreeMap.containsKey(f)) {
                //message("8.1");
                return "conflict";
            }
            if (!givenCommitTreeMap.get(f).equals(currentCommitTreeMap.get(f))) {
                //message("8.2");
                return "conflict";
            } else {
                message("Encountered a merge conflict.");
                System.exit(0);
            }
        }
        // if split does not contain the file or is initial commit
        else {
            // 4: if not in split nor other, but in head, then return head
            if (!givenCommitTreeMap.containsKey(f) && currentCommitTreeMap.containsKey(f)) {
                //message("4");
                return currentCommitTreeMap.get(f);
            }
            // 5: if not in split nor in head, but in other, then return other
            if (!currentCommitTreeMap.containsKey(f) && givenCommitTreeMap.containsKey(f)) {
                //message("5");
                return givenCommitTreeMap.get(f);
            }
            // 8: modified in other, and modified differently in head, then conflict (does not need to be in split)
            // modified can mean removed
            if (!givenCommitTreeMap.containsKey(f) && currentCommitTreeMap.containsKey(f)) {
                //message("8.3");
                return "conflict";
            }
            if (givenCommitTreeMap.containsKey(f) && !currentCommitTreeMap.containsKey(f)) {
                //message("8.4");
                return "conflict";
            }
            if (!givenCommitTreeMap.get(f).equals(currentCommitTreeMap.get(f))) {
                //message("8.5");
                return "conflict";
            } else {
                message("Encountered a merge conflict.");
                System.exit(0);
            }
        }
        return "";
    }


    // gets all files from a tree map and adds them to a hash set (no duplicate files added)
    public static void getFilesFromTree(TreeMap<File, String> treeMap, HashSet<File> allFiles) {
        Set<Map.Entry<File, String>> Set = treeMap.entrySet();
        for (Map.Entry<File, String> entry : Set) {
            File key = entry.getKey();
            allFiles.add(key);
        }
    }

    // gets the latest common ancestor of two commits, even when there is branching and merging involving 2 parents
    public static String traverseCommitsForSplitPoint(String currentCommitID, String givenBranchCommitID) {
        HashSet<String> IDHashSet = new HashSet<>();
        String splitPointID = "";
        ArrayList<String> extraIDList = new ArrayList<>();
        while (!currentCommitID.isEmpty() || !givenBranchCommitID.isEmpty()) {
            if (!currentCommitID.isEmpty()) {
                if (!IDHashSet.add(currentCommitID)) {
                    splitPointID = currentCommitID;
                    break;
                }
                Commit currentCommit = getCommitFromID(currentCommitID);
                if (!currentCommit.parentTwo.isEmpty()) {
                    extraIDList.add(currentCommit.parentTwo);
                }
                currentCommitID = currentCommit.parent;
            }
            if (!givenBranchCommitID.isEmpty()) {
                if (!IDHashSet.add(givenBranchCommitID)) {
                    splitPointID = givenBranchCommitID;
                    break;
                }
                Commit givenBranchCommit = getCommitFromID(givenBranchCommitID);
                if (!givenBranchCommit.parentTwo.isEmpty()) {
                    extraIDList.add(givenBranchCommit.parentTwo);
                }
                givenBranchCommitID = givenBranchCommit.parent;
            }
            int extraPaths = extraIDList.size();
            int i = 0;
            if (extraPaths > 0) {
                while (i < extraPaths && !extraIDList.get(i).isEmpty()) {
                    if (!IDHashSet.add(extraIDList.get(i))) {
                        splitPointID = extraIDList.get(i);
                        break;
                    }
                    Commit extraIDCommit = getCommitFromID(extraIDList.get(i));
                    if (!extraIDCommit.parentTwo.isEmpty()) {
                        extraIDList.add(extraIDCommit.parentTwo);
                    }
                    extraIDList.set(i, extraIDCommit.parent);
                    i++;
                }
            }
        }
        return splitPointID;
    }
}
