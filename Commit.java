package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

// holds all information of a commit
public class Commit implements Serializable {

    // commit message
    public String message;
    // stores SHA-1 of parent commit
    public String parent;
    // list-like of SHA-1s to Blobs. is a copy of the
    // staging area for addition at the time of commit
    public HashMap blobs;
    // timestamp of commit
    Date timeOfCommit;
    // merged in parent
    public String mergedInParent;



    // makes the initial commit
    public Commit() {

        this.message = "initial commit";
        this.parent = null;
        // set HashMap of SHA-1s to null
        this.blobs = new HashMap();
        // set time object to the 1970s time
        this.timeOfCommit = new Date();

    }

    // makes a default commit. only assigns the message and the timestamp,
    // everything else is assigned in the Repo class
    public Commit(String message) {

        this.message = message;
        this.parent = null;
        this.blobs = new HashMap();
        // set time object to the current time
        this.timeOfCommit = new Date();

    }

}
