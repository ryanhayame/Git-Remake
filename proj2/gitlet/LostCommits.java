package gitlet;

import java.io.Serializable;
import java.util.ArrayList;

public class LostCommits implements Serializable {

    public ArrayList<String> lostCommitsArrayList;

    public LostCommits(String commitID) {
        this.lostCommitsArrayList = new ArrayList<>();
        this.lostCommitsArrayList.add(commitID);
    }

}
