package gitlet;

import java.io.File;
import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */
public class Main {

    // saves the path to the Repository class file
    static File CWD = new File(System.getProperty("user.dir"));
    static File hiddenGitletFolder = Utils.join(CWD, ".gitlet/");
    static File RepositoryFile = Utils.join(hiddenGitletFolder,"repositoryClass");

    public static Repository ourRepo;

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {

        if (args.length == 0) {

            System.out.println("Please enter a command.");

        } else if (args[0].equals("init")) {

            // create gitlet repository!
            init();

        } else if (hiddenGitletFolder.exists()) {

            if (args[0].equals("add")) {

                String fileToBeAdded = args[1];

                // first read in our Repo class
                ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                // now run the addition method
                ourRepo.stageFileForAddition(fileToBeAdded);

                // rewrite the Repo file
                byte[] ourRepoBytes = Utils.serialize(ourRepo);
                Utils.writeContents(RepositoryFile, ourRepoBytes);

            } else if (args[0].equals("commit")) {

                String commitMessage = args[1];

                // first read in our Repo class
                ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                // now run the commit method
                ourRepo.makeCommit(commitMessage);

                // rewrite the Repo file
                byte[] ourRepoBytes = Utils.serialize(ourRepo);
                Utils.writeContents(RepositoryFile, ourRepoBytes);

            } else if (args[0].equals("log")) {

                // first read in our Repo class
                ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                // run the log method
                ourRepo.printLog();

                // rewrite the Repo file
                byte[] ourRepoBytes = Utils.serialize(ourRepo);
                Utils.writeContents(RepositoryFile, ourRepoBytes);

            } else if (args[0].equals("branch")) {

                // save new branch's name
                String newBranchName = args[1];

                // first read in our Repo class
                ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                // run the make branch method
                ourRepo.makeBranch(newBranchName, ourRepo.HEAD);

                // rewrite the Repo file
                byte[] ourRepoBytes = Utils.serialize(ourRepo);
                Utils.writeContents(RepositoryFile, ourRepoBytes);

            } else if (args[0].equals("rm-branch")) {

                // save the branch to delete's name
                String branchToDeleteName = args[1];

                // first read in our Repo class
                ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                // run the delete branch method
                ourRepo.deleteBranch(branchToDeleteName);

                // rewrite the Repo file
                byte[] ourRepoBytes = Utils.serialize(ourRepo);
                Utils.writeContents(RepositoryFile, ourRepoBytes);

            } else if (args[0].equals("checkout")) {

                if (args[1].equals("--")) {

                    // first read in our Repo class
                    ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                    // run the appropriate method

                    String fileNameToCheckout = args[2];
                    ourRepo.checkoutFileFromHEAD(fileNameToCheckout);

                    // rewrite the Repo file
                    byte[] ourRepoBytes = Utils.serialize(ourRepo);
                    Utils.writeContents(RepositoryFile, ourRepoBytes);

                } else if (args.length == 4 && args[2].equals("--")) {

                    String fileNameToCheckout = args[3];
                    String commitToCheckout = args[1];

                    // first read in our Repo class
                    ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                    // run that mawf method

                    ourRepo.checkoutFileFromSomeCommit(fileNameToCheckout, commitToCheckout);

                    // rewrite the Repo file
                    byte[] ourRepoBytes = Utils.serialize(ourRepo);
                    Utils.writeContents(RepositoryFile, ourRepoBytes);

                } else if (args.length == 2) {

                    String branchName = args[1];

                    // first read in our Repo class
                    ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                    ourRepo.checkoutBranch(branchName);

                    byte[] ourRepoBytes = Utils.serialize(ourRepo);
                    Utils.writeContents(RepositoryFile, ourRepoBytes);

                } else {

                    System.out.println("Incorrect operands.");

                }

            } else if (args[0].equals("rm")) {

                // first read in our Repo class
                ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                // run method
                String fileToRemoveName = args[1];
                ourRepo.removeFile(fileToRemoveName);

                // rewrite the Repo file
                byte[] ourRepoBytes = Utils.serialize(ourRepo);
                Utils.writeContents(RepositoryFile, ourRepoBytes);

            } else if (args[0].equals("global-log")) {

                // first read in our Repo class
                ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                // run method
                ourRepo.globalLog();

                // rewrite the Repo file
                byte[] ourRepoBytes = Utils.serialize(ourRepo);
                Utils.writeContents(RepositoryFile, ourRepoBytes);

            } else if (args[0].equals("find")) {

                String commitMessage = args[1];

                // first read in our Repo class
                ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                // run method
                ourRepo.find(commitMessage);

                // rewrite the Repo file
                byte[] ourRepoBytes = Utils.serialize(ourRepo);
                Utils.writeContents(RepositoryFile, ourRepoBytes);

            } else if (args[0].equals("reset")) {

                String commitSHA1ToCheckout = args[1];

                // first read in our Repo class
                ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                ourRepo.reset(commitSHA1ToCheckout);

                // rewrite the Repo file
                byte[] ourRepoBytes = Utils.serialize(ourRepo);
                Utils.writeContents(RepositoryFile, ourRepoBytes);

            } else if (args[0].equals("status")) {

                // first read in our Repo class
                ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                ourRepo.status();

                // rewrite the Repo file
                byte[] ourRepoBytes = Utils.serialize(ourRepo);
                Utils.writeContents(RepositoryFile, ourRepoBytes);

            } else if (args[0].equals("merge")) {

                // first read in our Repo class
                ourRepo = Utils.readObject(RepositoryFile, Repository.class);

                String branchToMerge = args[1];

                ourRepo.merge(branchToMerge);

                byte[] ourRepoBytes = Utils.serialize(ourRepo);
                Utils.writeContents(RepositoryFile, ourRepoBytes);

            } else {

                System.out.println("No command with that name exists.");
                return;

            }

        } else {

            System.out.println("Not in an initialized Gitlet directory.");

        }

    }

    // initializes and saves the repository class
    public static void init() {

        // failure case
        if (RepositoryFile.exists()) {

            System.out.println("A Gitlet version-control system already " +
                    "exists in the current directory.");

            return;

        }

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
