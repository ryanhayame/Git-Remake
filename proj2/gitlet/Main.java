package gitlet;

import java.io.IOException;
import java.util.ResourceBundle;

import static gitlet.Repository.GITLET_DIR;
import static gitlet.Utils.message;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Ryan Hayame
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void checkArgs(String[] args, int numberOfOperands) {
        if (args.length != numberOfOperands) {
            message("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void checkInitialization() {
        if (!GITLET_DIR.exists()) {
            message("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            message("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                checkArgs(args, 1);
                try {
                    Repository.initMain();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "add":
                checkArgs(args, 2);
                checkInitialization();
                try {
                    Repository.add(args[1]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "commit":
                checkArgs(args, 2);
                checkInitialization();
                try {
                    Repository.commit(args[1]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "rm":
                checkArgs(args, 2);
                checkInitialization();
                try {
                    Repository.rm(args[1]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "log":
                checkArgs(args, 1);
                checkInitialization();
                Repository.log();
                break;
            case "global-log":
                checkArgs(args, 1);
                checkInitialization();
                Repository.global_log();
                break;
            case "find":
                checkArgs(args, 2);
                checkInitialization();
                Repository.find(args[1]);
                break;
            case "status":
                checkArgs(args, 1);
                checkInitialization();
                Repository.status();
                break;
            case "checkout":
                checkInitialization();
                if (args.length < 2 || args.length > 4) {
                    message("Incorrect operands.");
                    System.exit(0);
                }
                if (args[1].equals("--") && args.length == 3) {
                    try {
                        Repository.checkoutOne(args[2]);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else if (args.length == 4 && args[2].equals("--")) {
                    try {
                        Repository.checkoutTwo(args[1], args[3]);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else if (args.length == 2) {
                    try {
                        Repository.checkoutThree(args[1]);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    message("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "branch":
                checkArgs(args, 2);
                checkInitialization();
                try {
                    Repository.branch(args[1]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "rm-branch":
                checkArgs(args, 2);
                checkInitialization();;
                Repository.rm_branch(args[1]);
                break;
            case "reset":
                checkArgs(args, 2);
                checkInitialization();
                try {
                    Repository.reset(args[1]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "merge":
                checkArgs(args, 2);
                checkInitialization();
                try {
                    Repository.merge(args[1]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                message("No command with that name exists");
                System.exit(0);
        }
    }
}
