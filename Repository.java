package gitlet;

import javax.swing.event.MouseInputListener;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

// outermost class, acts as the environment. holds important
// variables, such as the HEAD pointer, currBranch, and more. holds
// references to Maps of branches, blobs staged for addition,
// and blobs staged for removal.
public class Repository implements Serializable {

    // path of cwd
    File CWD = new File(System.getProperty("user.dir"));
    // path to .gitlet folder
    File hiddenGitletFolder = Utils.join(CWD, ".gitlet/");
    // path to branches folder
    File branchesFolder = Utils.join(hiddenGitletFolder, "branches/");
    // path to commits folder
    File commitsFolder = Utils.join(hiddenGitletFolder,"commits/");
    // path to blobs folder
    File blobsFolder = Utils.join(hiddenGitletFolder, "blobs/");

    // staging areas. key is file name, value is blob sha1
    HashMap stagedForAddition;

    // holds names of files to be removed
    Set<String> stagedForRemoval;

    // HEAD pointer, sha1 to HEAD commit
    String HEAD;

    // name of currBranch
    String currBranch;


    // sets up the repository environment
    public Repository() {

        // makes all the directories
        CWD.mkdir();
        hiddenGitletFolder.mkdir();
        branchesFolder.mkdir();
        commitsFolder.mkdir();
        blobsFolder.mkdir();

        // initialize the stages. keys are equal to the filename
        // while values are equal to the sha1 of the blob
        stagedForAddition = new HashMap();
        stagedForRemoval = new HashSet<>();

        // make and save the initial commit
        Commit initialCommit = new Commit();
        String initialCommitSHA1 = this.saveCommit(initialCommit);

        // set HEAD to initial commit
        HEAD = initialCommitSHA1;

        // make and save the master branch
        this.makeBranch("master", HEAD);

        // set currBranch to master branch
        this.currBranch = "master";

    }

    // saves a commit to the .gitlet/commits folder with
    // its file name equal to its sha1. returns the
    // sha1 of the commit instance
    public String saveCommit(Commit newCommit) {

        // create the sha1
        byte[] newCommitBytes = Utils.serialize(newCommit);
        String newCommitSHA1 = Utils.sha1(newCommitBytes);

        // now create a file for the new commit with the
        // file name as the new commit's sha1
        File newCommitFile = Utils.join(commitsFolder, newCommitSHA1);

        // create the file
        try {
            newCommitFile.createNewFile();
        } catch (
        IOException e) {
            e.printStackTrace();
        }

        // now save the serialization of newCommit into newCommitFile
        Utils.writeContents(newCommitFile, newCommitBytes);

        return newCommitSHA1;

    }

    // stage a file for addition. this adds a blob's SHA1
    // to the staging area for addition
    public void stageFileForAddition(String fileName) {

        // failure case
        if (stagedForRemoval.contains(fileName)) {

            stagedForRemoval.remove(fileName);
            return;

        }

        // instantiate the file object
        File fileToBeStaged = Utils.join(CWD, fileName);

        // failure case

        if (!fileToBeStaged.exists()) {

            System.out.println("File does not exist.");

            return;

        }

        // failure case
        File HEADCommitFile = Utils.join(commitsFolder, HEAD);
        Commit HEADCommit = Utils.readObject(HEADCommitFile, Commit.class);

        // make a blob of the file's contents
        String newBlob = Utils.readContentsAsString(fileToBeStaged);
        String newBlobSHA1 = Utils.sha1(newBlob);

        if (HEADCommit.blobs.containsKey(fileName)) {

            if (HEADCommit.blobs.get(fileName).equals(newBlobSHA1)) {

                return;

            }

        }


        // save the blob
        File newBlobFile = Utils.join(blobsFolder, newBlobSHA1);

        try {
            newBlobFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // write the blob file's contents
        Utils.writeContents(newBlobFile, newBlob);

        // now add it to the stagedForAddition HashMap
        stagedForAddition.put(fileName, newBlobSHA1);

    }

    // makes and saves a new branch
    public void makeBranch(String branchName, String commitSHA1) {

        // failure case
        List<String> allBranchNames = Utils.plainFilenamesIn(branchesFolder);

        if (allBranchNames.contains(branchName)) {

            System.out.println("A branch with that name already exists.");

            return;

        }


        // make the branch file
        File branchFile = Utils.join(branchesFolder, branchName);

        try {
            branchFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // write the commit SHA1 to it
        Utils.writeContents(branchFile, commitSHA1);

    }

    // deletes a branch
    public void deleteBranch(String branchName) {

        // make the branch file
        File branchToDeleteFile = Utils.join(branchesFolder, branchName);

        // failure cases
        if (!branchToDeleteFile.exists()) {

            System.out.println("A branch with that name does not exist.");
            return;

        }

        if (branchName.equals(currBranch)) {

            System.out.println("Cannot remove the current branch.");
            return;

        }

        // delete the branch!

        if (branchToDeleteFile.exists()) {

            branchToDeleteFile.delete();

        }

    }

    // makes and saves a new commit
    public void makeCommit(String commitMessage) {

        // failure cases
        if (stagedForAddition.isEmpty() && stagedForRemoval.isEmpty()) {

            System.out.println("No changes added to the commit.");

            return;

        }

        if (commitMessage.equals("")) {

            System.out.println("Please enter a commit message.");
            return;

        }


        // create the new default commit object. we still need
        // to assign the parent commit and the blobs for which
        // this new commit will hold
        Commit newCommit = new Commit(commitMessage);

        // assign the parent of the new commit to the
        // parent commit
        newCommit.parent = HEAD;

        // now to adjust the commit's stored blobs

        // first we have to read in the parent commit and its blobs
        File parentCommitFile = Utils.join(commitsFolder, HEAD);
        Commit parentCommit = Utils.readObject(parentCommitFile, Commit.class);
        HashMap parentMap = parentCommit.blobs;

        // now we update our new HashMap with that of the staging
        // area for addition. first, let's get the set of keys
        // of our parent commit
        Set <String> parentKeys = parentMap.keySet();

        // get a copy of the staging area for addition
        HashMap stagingAreaCopy = new HashMap();
        stagingAreaCopy.putAll(this.stagedForAddition);

        // if the staging area copy doesn't have a key from parentKeys
        // we'll add that missing key. the end result is the correct
        // HashMap for the new commit
        for (String currParentKey : parentKeys) {

            if (stagingAreaCopy.containsKey(currParentKey)) {

                continue;

            // add the missing mapping
            } else {

                String currKey = currParentKey;
                String currValue = (String) parentMap.get(currKey);

                stagingAreaCopy.put(currKey, currValue);

            }

        }

        // now remove keys that are staged for removal
        for (String fileNameToRemove : this.stagedForRemoval) {

            stagingAreaCopy.remove(fileNameToRemove);

        }

        // now save a reference to this object in the new commit
        newCommit.blobs = stagingAreaCopy;

        // finally time to save the new commit!

        // first make a sha1
        byte[] newCommitBytes = Utils.serialize(newCommit);
        String newCommitSHA1 = Utils.sha1(newCommitBytes);

        // now save the new commit
        File newCommitFile = Utils.join(commitsFolder, newCommitSHA1);

        try {
            newCommitFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Utils.writeContents(newCommitFile, newCommitBytes);

        // now reassign HEAD
        this.HEAD = newCommitSHA1;

        // update branch pointer
        File currBranchFile = Utils.join(branchesFolder, currBranch);
        Utils.writeContents(currBranchFile, newCommitSHA1);

        // clear staging areas
        stagedForRemoval = new HashSet<String>();
        stagedForAddition = new HashMap();

    }

    // prints out the history of the current commit
    public void printLog() {

        // first read in the HEAD commit
        File currCommitFile = Utils.join(commitsFolder, HEAD);
        Commit currCommit = Utils.readObject(currCommitFile, Commit.class);
        // allows us to access the SHA1 of the current commit
        String nextCommitSHA1 = HEAD;

        // now iterate through the commits, ending at the initial commit
        while(true) {

            System.out.println("===");
            System.out.println("commit " + nextCommitSHA1);

            // make date look nnnice
            SimpleDateFormat niceLookingDate = new SimpleDateFormat();
            niceLookingDate.applyPattern("EEE MMM d HH:mm:ss yyyy Z");

            // hardcode initial time
            if (currCommit.parent == null) {

                System.out.println("Date: Wed Dec 31 16:00:00 1969 -0700");

            } else {

                System.out.println("Date: " + niceLookingDate.format(currCommit.timeOfCommit));

            }

            System.out.println(currCommit.message);
            System.out.println();

            if (currCommit.parent == null) {

                break;

            // go to the next commit
            } else {

                nextCommitSHA1 = currCommit.parent;
                File currCommitParentFile = Utils.join(commitsFolder, currCommit.parent);
                currCommit = Utils.readObject(currCommitParentFile, Commit.class);

            }

        }

    }

    // Takes the version of the file as it exists in the head commit,
    // the front of the current branch, and puts it in the working directory,
    // overwriting the version of the file that’s already there if there is one.
    // The new version of the file is not staged.
    public void checkoutFileFromHEAD(String fileToCheckoutName) {

        // make the file object so we can read it later
        File HEADCommitFile = Utils.join(commitsFolder, HEAD);

        // read in HEAD commit
        Commit HEADCommit = Utils.readObject(HEADCommitFile, Commit.class);

        // failure cases
        if (!HEADCommit.blobs.containsKey(fileToCheckoutName)) {

            System.out.println("File does not exist in that commit.");

            return;

        }


        // get sha1 to checkout from HEAD commit
        String fileToCheckoutBlobSHA1 = (String) HEADCommit.blobs.get(fileToCheckoutName);

        // get contents of the blob
        File fileBlob = Utils.join(blobsFolder, fileToCheckoutBlobSHA1);
        String contentsBlob = Utils.readContentsAsString(fileBlob);

        // now delete the file that's in the CWD with name fileToCheckoutName

        // first let's make this file object
        File fileToDelete = Utils.join(CWD, fileToCheckoutName);

        // delete if it exists
        Utils.restrictedDelete(fileToDelete);

        // now add the new file to the directory
        File fileToAddToCWD = Utils.join(CWD, fileToCheckoutName);

        try {
            fileToAddToCWD.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // write contents to this file
        Utils.writeContents(fileToAddToCWD, contentsBlob);

    }

    public void checkoutFileFromSomeCommit(String fileToCheckoutName, String CommitSHA1ToFind) {

        // first we have to find the commit based on the sha1 the user put in

        // let's grab all the commits from the commits' folder

        List<String> allCommitSHA1s = Utils.plainFilenamesIn(commitsFolder);

        // let's iterate through the list to see if there's a commit sha1
        // that starts with what was put in

        String commitToCheckoutSHA1 = null;

        for (String commitSHA1 : allCommitSHA1s) {

            if (commitSHA1.startsWith(CommitSHA1ToFind)) {

                commitToCheckoutSHA1 = commitSHA1;
                break;

            } else {

                continue;

            }

        }

        // failure case
        if (commitToCheckoutSHA1 == null) {

            System.out.println("No commit with that id exists.");

            return;

        }

        // read in the commit object
        File checkedOutCommitFile = Utils.join(commitsFolder, commitToCheckoutSHA1);
        Commit checkedOutCommit = Utils.readObject(checkedOutCommitFile, Commit.class);

        // failure case
        if (!checkedOutCommit.blobs.containsKey(fileToCheckoutName)) {

            System.out.println("File does not exist in that commit.");

            return;

        }

        // now grab the blob sha1 of the file we want from this commit
        String fileToCheckoutBlobSHA1 = (String) checkedOutCommit.blobs.get(fileToCheckoutName);

        // now delete the file in the CWD if it exists
        File fileToDelete = Utils.join(CWD, fileToCheckoutName);
        Utils.restrictedDelete(fileToDelete);

        // now add the new file to the CWD
        File fileToAddToCWD = Utils.join(CWD, fileToCheckoutName);

        try {
            fileToAddToCWD.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // now write the blob to this new file

        File blobToGetFile = Utils.join(blobsFolder, fileToCheckoutBlobSHA1);
        String blobContents = Utils.readContentsAsString(blobToGetFile);
        Utils.writeContents(fileToAddToCWD, blobContents);

    }


    // Takes all files in the commit at the head of the given branch,
    // and puts them in the working directory, overwriting the versions of
    // the files that are already there if they exist. Also, at the end of
    // this command, the given branch will now be considered the current branch (HEAD).
    // Any files that are tracked in the current branch but are not present in the
    // checked-out branch are deleted. The staging area is cleared, unless the
    // checked-out branch is the current branch
    public void checkoutBranch(String branchNameToCheckout) {

        File branchToCheckoutFile = Utils.join(branchesFolder, branchNameToCheckout);

        if (!branchToCheckoutFile.exists()) {

            System.out.println("No such branch exists.");

            return;

        }

        // first read in the commit from the wanted branch
        // File branchToCheckoutFile = Utils.join(branchesFolder, branchNameToCheckout);
        String commitToCheckoutSHA1 = Utils.readContentsAsString(branchToCheckoutFile);
        File commitToCheckoutFile = Utils.join(commitsFolder, commitToCheckoutSHA1);
        Commit commitToCheckout = Utils.readObject(commitToCheckoutFile, Commit.class);

        // failure cases
        if (!branchToCheckoutFile.exists()) {

            System.out.println("No such branch exists.");

            return;

        }

        if (branchNameToCheckout.equals(currBranch)) {

            System.out.println("No need to checkout the current branch.");

            return;

        }

        if (checkoutCommitFailureHelper(commitToCheckout)) {

            System.out.println("There is an untracked file in the way; " +
                    "delete it, or add and commit it first.");

            return;

        }

        // now delete all files in CWD. we'll replace them with the files of the
        // checked out commit

        List<String> filesInCWD = Utils.plainFilenamesIn(CWD);

        for (String fileNameToDelete : filesInCWD) {

            // create file object
            File fileToDelete = Utils.join(CWD, fileNameToDelete);

            // delete that mawf
            Utils.restrictedDelete(fileToDelete);

        }

        // now add all the files from our commit to our CWD

        Set<String> commitToCheckoutKeys = commitToCheckout.blobs.keySet();

        for (String fileNameToAdd : commitToCheckoutKeys) {

            // first get the blob name
            String fileBlobName = (String) commitToCheckout.blobs.get(fileNameToAdd);

            // grab the contents of the blob
            File blobFile = Utils.join(blobsFolder, fileBlobName);
            String blobContents = Utils.readContentsAsString(blobFile);

            // now add the new file to the CWD
            File fileToAdd = Utils.join(CWD, fileNameToAdd);

            try {
                fileToAdd.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Utils.writeContents(fileToAdd, blobContents);

        }

        // reassign currBranch
        currBranch = branchNameToCheckout;

        // reassign HEAD
        HEAD = commitToCheckoutSHA1;

        // clear staging area
        stagedForAddition = new HashMap();
        stagedForRemoval = new HashSet();

    }

    // true if fail, false if not fail
    public boolean checkoutCommitFailureHelper(Commit commitToCheckout) {

        List<String> filesInCWD = Utils.plainFilenamesIn(CWD);


        File HEADCommitFile = Utils.join(commitsFolder, HEAD);
        Commit HEADCommit = Utils.readObject(HEADCommitFile, Commit.class);

        Set<String> filesInHEADCommit = HEADCommit.blobs.keySet();

        HashSet<String> filesInCWDNotHEAD = new HashSet<>();

        // find files in CWD but not tracked in HEAD commit
        for (String fileInCWD : filesInCWD) {

            if (!filesInHEADCommit.contains(fileInCWD)) {

                filesInCWDNotHEAD.add(fileInCWD);

            }

        }

        // see if the files found above are tracked in the destination commit.
        // if they are, see if the blobs are the same. if the blobs are not
        // the same, throw the mawf error
        Set<String> destCommitFiles = commitToCheckout.blobs.keySet();

        for (String fileInCWDNotHEAD : filesInCWDNotHEAD) {

            if (destCommitFiles.contains(fileInCWDNotHEAD)) {

                // make blob
                File tempFile = Utils.join(CWD, fileInCWDNotHEAD);
                String tempBlob = Utils.readContentsAsString(tempFile);
                String tempBlobSHA1 = Utils.sha1(tempBlob);

                if (!commitToCheckout.blobs.get(fileInCWDNotHEAD).equals(tempBlobSHA1)) {

                    return true;

                }

            }

        }

        return false;

    }

    // stop tracking a particular file and delete it from the CWD if
    // it's there
    public void removeFile(String fileToRemoveName) {

        // first let's read in the HEAD commit
        File HEADCommitFile = Utils.join(commitsFolder, HEAD);
        Commit HEADCommit = Utils.readObject(HEADCommitFile, Commit.class);

        // failure case
        if (!stagedForAddition.containsKey(fileToRemoveName)
            && !HEADCommit.blobs.containsKey(fileToRemoveName)) {

            System.out.println("No reason to remove the file.");

            return;

        }

        // first remove the file from the staging area for addition,
        // if it's there
        stagedForAddition.remove(fileToRemoveName);

        // if the current commit is tracking the file, we'll stage the file
        // for removal and delete it from the CWD
        if (HEADCommit.blobs.containsKey(fileToRemoveName)) {

            // first stage the file for removal
            stagedForRemoval.add(fileToRemoveName);

            // now delete the file from the CWD
            File fileToDelete = Utils.join(CWD, fileToRemoveName);
            Utils.restrictedDelete(fileToDelete);

        }

    }

    // Like log, except displays information about all commits
    // ever made. The order of the commits does not matter.
    public void globalLog() {

        // iterate through the commitsFolder and print out
        // the information of each commit

        List<String> allCommits = Utils.plainFilenamesIn(commitsFolder);

        for (String commitSHA1 : allCommits) {

            // read in commit object
            File commitFile = Utils.join(commitsFolder, commitSHA1);
            Commit commit = Utils.readObject(commitFile, Commit.class);

            // now print everything out

            System.out.println("===");
            System.out.println("commit " + commitSHA1);

            // make date look nnnice
            SimpleDateFormat niceLookingDate = new SimpleDateFormat();
            niceLookingDate.applyPattern("EEE MMM d HH:mm:ss yyyy Z");

            // hardcode initial time
            if (commit.parent == null) {

                System.out.println("Date: Wed Dec 31 16:00:00 1969 -0700");

            } else {

                System.out.println("Date: " + niceLookingDate.format(commit.timeOfCommit));

            }

            System.out.println(commit.message);
            System.out.println();

        }

    }

    // Prints out the ids of all commits that have the given commit message, one per line.
    public void find(String commitMessage) {

        // first load in all commits
        File allCommitFiles = Utils.join(commitsFolder);
        List<String> allCommitSHA1s = Utils.plainFilenamesIn(allCommitFiles);

        // now search for commits with this message

        Set<String> matchingCommitSHA1s = new HashSet();

        for (String commitSHA1 : allCommitSHA1s) {

            // read in commit
            File commitFile = Utils.join(commitsFolder, commitSHA1);
            Commit commit = Utils.readObject(commitFile, Commit.class);

            if (commit.message.equals(commitMessage)) {

                matchingCommitSHA1s.add(commitSHA1);

            }

        }

        // failure cases
        if (matchingCommitSHA1s.isEmpty()) {

            System.out.println("Found no commit with that message.");

            return;

        }

        // now print out the commits' SHA1s
        for (String commitSHA1 : matchingCommitSHA1s) {

            System.out.println(commitSHA1);

        }

    }

    public void reset(String commitSHA1ToFind) {

        List<String> allCommitSHA1s = Utils.plainFilenamesIn(commitsFolder);

        // let's iterate through the list to see if there's a commit sha1
        // that starts with what was put in

        String commitToCheckoutSHA1 = null;

        for (String commitSHA1 : allCommitSHA1s) {

            if (commitSHA1.startsWith(commitSHA1ToFind)) {

                commitToCheckoutSHA1 = commitSHA1;
                break;

            } else {

                continue;

            }

        }

        // failure cases
        if (commitToCheckoutSHA1 == null) {

            System.out.println("No commit with that id exists.");
            return;

        }

        // get a list of all files from that commit
        File commitToCheckoutFile = Utils.join(commitsFolder, commitToCheckoutSHA1);
        Commit commitToCheckout = Utils.readObject(commitToCheckoutFile, Commit.class);

        // failure case
        if (checkoutCommitFailureHelper(commitToCheckout)) {

            System.out.println("There is an untracked file in the way; " +
                    "delete it, or add and commit it first.");
            return;

        }

        Set<String> commitToCheckoutFileNames = commitToCheckout.blobs.keySet();

        // remove all tracked files in current commit that aren't tracked in
        // given commit
        File currCommitFile = Utils.join(commitsFolder, HEAD);
        Commit currCommit = Utils.readObject(currCommitFile, Commit.class);
        Set<String> currTrackedFiles = currCommit.blobs.keySet();

        for (String fileName : currTrackedFiles) {

            if (!commitToCheckoutFileNames.contains(fileName)) {

                // delete
                File fileToDelete = Utils.join(CWD, fileName);
                Utils.restrictedDelete(fileToDelete);

            }

        }

        // run checkout from arbitrary commit on each file
        for (String fileName : commitToCheckoutFileNames) {

            this.checkoutFileFromSomeCommit(fileName, commitToCheckoutSHA1);

        }


        // change HEAD pointer
        // HEAD = commitToCheckoutSHA1;

        // change branch pointer
        File currBranchFile = Utils.join(branchesFolder, currBranch);
        Utils.writeContents(currBranchFile, commitToCheckoutSHA1);

        // change HEAD pointer
        HEAD = commitSHA1ToFind;


        // clear staging areas
        stagedForRemoval = new HashSet<>();
        stagedForAddition = new HashMap();

    }

    //  Displays what branches currently exist, and marks the
    //  current branch with a *. Also displays what files have
    //  been staged for addition or removal
    public void status() {

        // first get a list of all branches
        List<String> allBranchNames = Utils.plainFilenamesIn(branchesFolder);

        // now sort the list by lexicographic order
        allBranchNames.sort(Comparator.naturalOrder());

        // print branches. when we're at the current branch we'll print
        // it out with an asterisk before it
        System.out.println("=== Branches ===");

        for (String branchName : allBranchNames) {

            if (branchName.equals(currBranch)) {

                System.out.println("*" + branchName);

            } else {

                System.out.println(branchName);

            }

        }

        System.out.println();

        // now print the staged files for addition
        System.out.println("=== Staged Files ===");

        // sort file names
        Set<String> unsortedFileNamesForAdd = stagedForAddition.keySet();
        TreeSet<String> sortedFileNamesForAdd = new TreeSet(unsortedFileNamesForAdd);

        // print em out
        for (String fileName : sortedFileNamesForAdd) {

            System.out.println(fileName);

        }

        System.out.println();

        // now do the same for the files staged for removal

        System.out.println("=== Removed Files ===");

        TreeSet<String> sortedFileNamesForRem = new TreeSet(stagedForRemoval);

        // print em out
        for (String fileName : sortedFileNamesForRem) {

            System.out.println(fileName);

        }

        System.out.println();

        // placeholders for EC
        System.out.println("=== Modifications Not Staged For Commit ===");

        System.out.println();

        System.out.println("=== Untracked Files ===");

    }

    public void merge(String givenBranchName) {

        // failure cases
        if (!stagedForAddition.isEmpty() || !stagedForRemoval.isEmpty()) {

            System.out.println("You have uncommitted changes.");
            return;

        }

        List<String> allBranchNames = Utils.plainFilenamesIn(branchesFolder);
        if (!allBranchNames.contains(givenBranchName)) {

            System.out.println("A branch with that name does not exist.");
            return;

        }

        if (currBranch.equals(givenBranchName)) {

            System.out.println("Cannot merge a branch with itself.");
            return;

        }

        String splitSHA1 = splitFinder(givenBranchName);
        File splitCommitFile = Utils.join(commitsFolder, splitSHA1);
        Commit splitCommit = Utils.readObject(splitCommitFile, Commit.class);

        // get map of split commit blobs
        Map<String, String> filesInSplit = splitCommit.blobs;

        File givBranchTipFile = Utils.join(branchesFolder, givenBranchName);
        String givBranchTipSHA1 = Utils.readContentsAsString(givBranchTipFile);


        if (splitSHA1.equals(givBranchTipSHA1)) {

            System.out.println("Given branch is an ancestor of the current branch.");
            return;

        }

        if (splitSHA1.equals(HEAD)) {

            checkoutBranch(givenBranchName);
            System.out.println("Current branch fast-forwarded.");
            return;

        }

        File givBranchCommitFile = Utils.join(commitsFolder, givBranchTipSHA1);
        Commit givBranchCommit = Utils.readObject(givBranchCommitFile, Commit.class);

        // File currBranchTipFile = Utils.join(branchesFolder, currBranch);
        // String currCommitSHA1 = Utils.readContentsAsString();
        File currBranchCommitFile = Utils.join(commitsFolder, HEAD);
        Commit currCommit = Utils.readObject(currBranchCommitFile, Commit.class);


        // Get the same map from HEAD
        // Get the same map from tip of given
        Map<String, String> filesInHEAD = currCommit.blobs;
        Map<String, String> filesInGiven = givBranchCommit.blobs;

        // collect all untracked files in HEAD
        List<String> filesInCWD = Utils.plainFilenamesIn(CWD);
        Set<String> untrackedInHEAD = new HashSet<>();

        for (String fileName : filesInCWD) {

            if (!filesInHEAD.containsKey(fileName)) {

                untrackedInHEAD.add(fileName);

            }

        }


        // Make a set of all file names
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(filesInSplit.keySet());
        allFiles.addAll(filesInGiven.keySet());
        allFiles.addAll(filesInHEAD.keySet());

        // delete all of these files from the CWD
        for (String fileName : allFiles) {

            File fileToDel = Utils.join(CWD, fileName);
            Utils.restrictedDelete(fileToDel);

        }


        boolean mergeConflict = false;



        for (String fileName : allFiles) {

            boolean baiCatcher = false;

            if (untrackedInHEAD.contains(fileName)) {

                System.out.println("There is an untracked file in the way; " +
                        "delete it, or add and commit it first.");
                return;

            }

            // delete file from CWD
            File fileToDel = Utils.join(CWD, fileName);
            Utils.restrictedDelete(fileToDel);


            // all files

            if (!filesInSplit.containsKey(fileName)) {

                // files absent at the split point

                if (filesInHEAD.containsKey(fileName)
                        && !filesInGiven.containsKey(fileName)) {

                    // files not present at split and only present at HEAD

                    File newFile = Utils.join(CWD, fileName);

                    try {
                        newFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File blobFile = Utils.join(blobsFolder, filesInHEAD.get(fileName));
                    String blobContents = Utils.readContentsAsString(blobFile);
                    Utils.writeContents(newFile, blobContents);

                }

                if (filesInGiven.containsKey(fileName)
                        && filesInHEAD.containsKey(fileName)) {



                    // files absent at split point and present in
                    // both HEAD and giv

                    if (!filesInGiven.get(fileName).equals(filesInHEAD.get(fileName))) {

                        // files absent at the split point that has different contents in
                        // the given and current branches

                        mergeConflict = true;
                        File mergeFile = Utils.join(CWD, fileName);

                        try {
                            mergeFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        File currFile = Utils.join(blobsFolder, filesInHEAD.get(fileName));
                        File givFile = Utils.join(blobsFolder, filesInGiven.get(fileName));

                        String mergeConflictCont = "<<<<<<< HEAD";
                        mergeConflictCont += "\n";
                        mergeConflictCont += Utils.readContentsAsString(currFile);
                        //mergeConflictCont += "\n";
                        mergeConflictCont += "=======";
                        mergeConflictCont += "\n";
                        mergeConflictCont += Utils.readContentsAsString(givFile);
                        // mergeConflictCont += "\n";
                        mergeConflictCont += ">>>>>>>\n";

                        Utils.writeContents(mergeFile, mergeConflictCont);

                        stageFileForAddition(fileName);

                    }

                }

            }


            if (filesInHEAD.containsKey(fileName)
                    && filesInSplit.containsKey(fileName)
                    && !filesInHEAD.get(fileName).equals(filesInSplit.get(fileName))) {

                // files modified in HEAD

                if (filesInGiven.containsKey(fileName)
                    && filesInGiven.get(fileName).equals(filesInHEAD.get(fileName))) {

                    // files modified in the same way in both HEAD and giv

                    File newFile = Utils.join(CWD, fileName);

                    try {
                        newFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File blobFile = Utils.join(blobsFolder, filesInHEAD.get(fileName));
                    String blobContents = Utils.readContentsAsString(blobFile);
                    Utils.writeContents(newFile, blobContents);

                }

                if (filesInGiven.containsKey(fileName)
                        && filesInGiven.get(fileName).equals(filesInSplit.get(fileName))) {

                    // files that have been modified in the current branch
                    // but not in the given branch since the split

                    File newFile = Utils.join(CWD, fileName);

                    try {
                        newFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File blobFile = Utils.join(blobsFolder, filesInHEAD.get(fileName));
                    String blobContents = Utils.readContentsAsString(blobFile);
                    Utils.writeContents(newFile, blobContents);

                    baiCatcher = true;

                    continue;

                }

                if (!filesInGiven.containsKey(fileName)) {

                    // files modified in HEAD and deleted in Giv

                    mergeConflict = true;
                    File mergeFile = Utils.join(CWD, fileName);

                    try {
                        mergeFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File currFile = Utils.join(blobsFolder, filesInHEAD.get(fileName));

                    String mergeConflictCont = "<<<<<<< HEAD";
                    mergeConflictCont += "\n";
                    mergeConflictCont += Utils.readContentsAsString(currFile);
                    //mergeConflictCont += "\n";
                    mergeConflictCont += "=======";
                    mergeConflictCont += "\n";
                    mergeConflictCont += "";
                    // mergeConflictCont += "\n";
                    mergeConflictCont += ">>>>>>>\n";

                    Utils.writeContents(mergeFile, mergeConflictCont);

                    stageFileForAddition(fileName);


                }



            }


            if (filesInGiven.containsKey(fileName)
                    && filesInSplit.containsKey(fileName)
                    && !filesInGiven.get(fileName).equals(filesInSplit.get(fileName))) {

                // files modified in the given branch

                if (filesInHEAD.containsKey(fileName)
                        && filesInHEAD.get(fileName).equals(filesInSplit.get(fileName))) {

                    // files modified in given branch, left alone in current branch

                    File newFile = Utils.join(CWD, fileName);

                    try {
                        newFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File blobFile = Utils.join(blobsFolder, filesInGiven.get(fileName));
                    String blobContents = Utils.readContentsAsString(blobFile);
                    Utils.writeContents(newFile, blobContents);

                    continue;

                }

                if (!filesInHEAD.containsKey(fileName)) {

                    // files modified in given and deleted in HEAD

                    mergeConflict = true;
                    File mergeFile = Utils.join(CWD, fileName);

                    try {
                        mergeFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File givFile = Utils.join(blobsFolder, filesInGiven.get(fileName));

                    String mergeConflictCont = "<<<<<<< HEAD";
                    mergeConflictCont += "\n";
                    mergeConflictCont += "";
                    //mergeConflictCont += "\n";
                    mergeConflictCont += "=======";
                    mergeConflictCont += "\n";
                    mergeConflictCont += Utils.readContentsAsString(givFile);
                    // mergeConflictCont += "\n";
                    mergeConflictCont += ">>>>>>>\n";

                    Utils.writeContents(mergeFile, mergeConflictCont);

                    stageFileForAddition(fileName);

                }

                if (filesInHEAD.containsKey(fileName)
                        && !filesInHEAD.get(fileName).equals(filesInGiven.get(fileName))
                        && !baiCatcher) {

                    // files modified in Giv and Curr differently

                    // System.out.println("at mod giv and curr differently " + fileName);

                    mergeConflict = true;
                    File mergeFile = Utils.join(CWD, fileName);

                    try {
                        mergeFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // make contents of file

                    File currFile = Utils.join(blobsFolder, filesInHEAD.get(fileName));
                    File givFile = Utils.join(blobsFolder, filesInGiven.get(fileName));

                    String mergeConflictCont = "<<<<<<< HEAD";
                    mergeConflictCont += "\n";
                    mergeConflictCont += Utils.readContentsAsString(currFile);
                    //mergeConflictCont += "\n";
                    mergeConflictCont += "=======";
                    mergeConflictCont += "\n";
                    mergeConflictCont += Utils.readContentsAsString(givFile);
                    //mergeConflictCont += "\n";
                    mergeConflictCont += ">>>>>>>\n";

                    Utils.writeContents(mergeFile, mergeConflictCont);

                    stageFileForAddition(fileName);

                    continue;

                }

                if (filesInHEAD.containsKey(fileName)
                        && filesInHEAD.get(fileName).equals(filesInSplit.get(fileName))) {

                    // files mod in giv branch but not mod in current branch since
                    // the split point

                    checkoutFileFromSomeCommit(fileName, givBranchTipSHA1);
                    stageFileForAddition(fileName);

                }


            }

            if (!filesInSplit.containsKey(fileName)
                    && filesInGiven.containsKey(fileName)
                    && !filesInHEAD.containsKey(fileName)) {

                // files that were not present at the split point and are present
                // only in the given branch

                checkoutFileFromSomeCommit(fileName, givBranchTipSHA1);
                stageFileForAddition(fileName);

            }

            if (filesInSplit.containsKey(fileName)) {

                // files in split point

                if (filesInGiven.containsKey(fileName)
                     && filesInHEAD.containsKey(fileName)) {

                    // files not modified in giv or cur at all,
                    // and that exist at the split point

                    File newFile = Utils.join(CWD, fileName);

                    try {
                        newFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File blobFile = Utils.join(blobsFolder, filesInHEAD.get(fileName));
                    String blobContents = Utils.readContentsAsString(blobFile);
                    Utils.writeContents(newFile, blobContents);

                }

                if (filesInHEAD.containsKey(fileName)
                        && filesInHEAD.get(fileName).equals(filesInSplit.get(fileName))) {

                    // files present at split and unmodified at currCommit

                    if (!filesInGiven.containsKey(fileName)) {

                        // files present at split, unmodified at currCommit, and
                        // deleted in givCommit

                        removeFile(fileName);

                    }

                }

            }

        }

        // now commit!
        makeCommit("Merged " + givenBranchName + " into " + currBranch + ".");

        // now assign mergeParent
        File HEADCommitFile = Utils.join(commitsFolder, HEAD);
        Commit HEADCommit = Utils.readObject(HEADCommitFile, Commit.class);
        HEADCommit.mergedInParent = givBranchTipSHA1;

        // now rewrite the file. although the contents changed, i'm choosing
        // to ignore changing the sha1 value for now

        byte[] mergeHEADBytes = Utils.serialize(HEADCommit);
        Utils.writeContents(HEADCommitFile, mergeHEADBytes);


        if (mergeConflict) {

            System.out.println("Encountered a merge conflict.");

        }


    }

    // finds split point of two branches, returns SHA1 of that commit
    public String splitFinder(String givenBranch) {

        // load in branch pointers
        File currentBranchFile = Utils.join(branchesFolder, currBranch);
        File givenBranchFile = Utils.join(branchesFolder, givenBranch);

        String currBranchTipSHA1 = Utils.readContentsAsString(currentBranchFile);
        String givenBranchTipSHA1 = Utils.readContentsAsString(givenBranchFile);

        File currBranchTipCommitFile = Utils.join(commitsFolder, currBranchTipSHA1);
        File givenBranchTipCommitFile = Utils.join(commitsFolder, givenBranchTipSHA1);

        Commit currBranchTipCommit = Utils.readObject(currBranchTipCommitFile, Commit.class);
        Commit givenBranchTipCommit = Utils.readObject(givenBranchTipCommitFile, Commit.class);

        // now collect each commit's ancestors
        Queue<String> currBranchAncestry = new ArrayDeque<>();
        Set<String> givBranchAncestry = new HashSet<>();

        // first make the currBranch's ancestry

        String currCommitSHA1 = currBranchTipSHA1;
        Commit currCommit = currBranchTipCommit;

        while (true) {

            currBranchAncestry.add(currCommitSHA1);

            if (!(currCommit.mergedInParent == null)) {

                currBranchAncestry.add(currCommit.mergedInParent);

            }

            if (currCommit.parent == null) {

                break;

            }

            currCommitSHA1 = currCommit.parent;


            // read in parent commit
            File parentCommitFile = Utils.join(commitsFolder, currCommitSHA1);
            Commit parentCommit = Utils.readObject(parentCommitFile, Commit.class);

            currCommit = parentCommit;

        }

        // make givenBranch's ancestry

        String givCommitSHA1 = givenBranchTipSHA1;
        Commit givCommit = givenBranchTipCommit;

        while (true) {

            givBranchAncestry.add(givCommitSHA1);

            if (!(givCommit.mergedInParent == null)) {

                givBranchAncestry.add(givCommit.mergedInParent);

            }

            if (givCommit.parent == null) {

                break;

            }

            givCommitSHA1 = givCommit.parent;

            // read in parent commit
            File parentCommitFile = Utils.join(commitsFolder, givCommitSHA1);
            Commit parentCommit = Utils.readObject(parentCommitFile, Commit.class);

            givCommit = parentCommit;

        }


        // now do go through currBranch to find the split point

        String splitPointSHA1;

        for (String currBranchCommit : currBranchAncestry) {

            // now see if any commit in the curr branch's history exists in the
            // given branches history
            if (givBranchAncestry.contains(currBranchCommit)) {

                splitPointSHA1 = currBranchCommit;

                return splitPointSHA1;

            }


        }

        return "";

    }

    public static void main(String[] args) {

        // saves the path to the Repository class file
        File CWD = new File(System.getProperty("user.dir"));
        File RepositoryFile = Utils.join(CWD,"repositoryClass");

        // instantiate the repository class
        Repository gitlet = new Repository();

        // create repository file
        try {
            RepositoryFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // serialize repo file

        // write to the repository file
        byte[] gitletBytes = Utils.serialize(gitlet);
        Utils.writeContents(RepositoryFile, gitletBytes);

    }

}
