package gitlet;


import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gitlet.Utils.serialize;
import static gitlet.Utils.sha1;

/** Represents a gitlet commit object.
 *  @author Ryan Hayame
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    public String message;

    /** The timestamp of this Commit. */
    private Date timestamp;

    /** The file name string of the parent of this Commit. */
    public String parent;

    /** The second parent ID string of this Commit if merged */
    public String parentTwo;

    /** The string of the tree where the files : blobs being tracked by the commit are stored */
    public String tree;

    /** Constructors for Commits */
    // Makes initial commit
    public Commit() {
        this.message = "initial commit";
        this.timestamp = new Date(0);
        this.parent = "";
        this.parentTwo = "";
        this.tree = "";
    }
    // Makes commit with one parent
    public Commit(String message, String parent, String tree) {
        this.message = message;
        this.timestamp = new Date();
        this.parent = parent;
        this.parentTwo = "";
        this.tree = tree;
    }

    // Makes commit with two parents
    public Commit(String message, String parent, String parentTwo, String tree) {
        this.message = message;
        this.timestamp = new Date();
        this.parent = parent;
        this.parentTwo = parentTwo;
        this.tree = tree;
    }

    // used in log command
    public String convertToString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("===\n");
        buffer.append("commit " + sha1(serialize(this)) + "\n");
        if (!parentTwo.isEmpty()) {
            buffer.append("Merge: " + Repository.firstSeven(parent) + " " + Repository.firstSeven(parentTwo) + "\n");
        }
        String date = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z").
                format(timestamp);
        buffer.append("Date: " + date + "\n");
        buffer.append(message + "\n");
        return buffer.toString();
    }
}
