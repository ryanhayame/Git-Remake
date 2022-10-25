package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;

// only one stage object is written to index file (staging area) at a time
public class Stage implements Serializable{

    // additionStageTree = treeMap of files + blobs staged for addition
    public TreeMap<File, String> additionStageTree;

    // removalStageTree = treeMap of files + blobs staged for removal
    public TreeMap<File, String> removalStageTree;

    // commitTree = treeMap of files + blobs for commits
    public TreeMap<File, String> commitTree;

    public Stage(TreeMap<File, String> tree) {
        this.additionStageTree = null;
        this.removalStageTree = null;
        this.commitTree = tree;
    }
    public Stage(File f, String hashedBlob, String addOrRemove) {
        this.additionStageTree = new TreeMap<>();
        this.removalStageTree = new TreeMap<>();
        this.commitTree = null;
        if (addOrRemove.equals("add")) {
            this.additionStageTree.put(f, hashedBlob);
        } else if (addOrRemove.equals("remove")) {
            this.removalStageTree.put(f, hashedBlob);
        }
    }

    public TreeMap<File, String> getCommitTree() {
        return commitTree;
    }
}
