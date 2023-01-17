# Git-Remake
## Designed and implemented a local version-control software that mimics features of Git

## Features:
- A custom blob-tree-commit storage system using various data structures
  - Utilized hashsets, treemaps, and arraylists
- Persistence, serialization, and SHA1 hashing to save contents of files locally
- Supports the following commands:
  1. Adding/removing a copy of a file to a staging area (adding/removing)
  2. Saving a snapshot of tracked files in the current commit and staging area (committing)
  3. Maintaining related sequences of commits (branches)
  4. Restoring a version of one or more files, or entire commits (checking out)
  5. Viewing the history of previous commits or all commits ever made (log)
  6. Listing all staged files, removed files, and branches (status)
  7. Merging changes made in one branch into another (merging)

## Full Demo Video:
https://youtu.be/_Ge5saz1ZIs

## Tech Stack:
Java

## Challenges and Lessons Learned:
- Experimented with many different types of data structures to ensure optimal storage and retrieval of data
- Spent a lot of time learning the complexities of Git, as well as how it works internally
- Worked with data persistence and data serialization/deserialization for the first time
- Learned how to optimally traverse and search for data within various data structures

## All commands:
- java gitlet.Main init
  - Creates a new Gitlet version-control system in the current directory. This system will automatically start with one commit: a commit that contains no files and has the commit message “initial commit”. It will have a single branch: “master”.

- java gitlet.Main add [file name]
  - Adds a copy of the file as it currently exists to the staging area. For this reason, adding a file is also called staging the file for addition. Staging an already-staged file overwrites the previous entry in the staging area with the new contents. 

- java gitlet.Main commit [message]
  - Saves a snapshot of tracked files in the current commit and staging area so they can be restored at a later time, creating a new commit.

- java gitlet.Main rm [file name]
  - Unstage the file if it is currently staged for addition. If the file is tracked in the current commit, stage it for removal and remove the file from the working directory if the user has not already done so (does not remove it unless it is tracked in the current commit).

- java gitlet.Main log
  - Starting at the current head commit, display information about each commit backwards along the commit tree until the initial commit.

- java gitlet.Main global-log
  - Like log, except displays information about all commits ever made in any order.

- java gitlet.Main find [commit message]
  - Prints out the ids of all commits that have the given commit message, one per line. If there are multiple such commits, it prints the ids out on separate lines.

- java gitlet.Main status
  - Displays what branches currently exist, and marks the current branch with a *. Also displays what files have been staged for addition or removal.

- java gitlet.Main checkout -- [file name]
  - Takes the version of the file as it exists in the head commit and puts it in the working directory, overwriting the version of the file that’s already there if there is one. The new version of the file is not staged.

- java gitlet.Main checkout [commit id] -- [file name]
  - Takes the version of the file as it exists in the commit with the given id, and puts it in the working directory, overwriting the version of the file that’s already there if there is one. The new version of the file is not staged.
 
- java gitlet.Main checkout [branch name]
  - Takes all files in the commit at the head of the given branch, and puts them in the working directory, overwriting the versions of the files that are already there if they exist. Also, at the end of this command, the given branch will now be considered the current branch (HEAD). Any files that are tracked in the current branch but are not present in the checked-out branch are deleted.

- java gitlet.Main branch [branch name]
  - Creates a new branch with the given name, and points it at the current head commit.

- java gitlet.Main rm-branch [branch name]
  - Deletes the branch with the given name. This only deletes the pointer associated with the branch, not the past commits of the branch.

- java gitlet.Main reset [commit id]
  - Checks out all the files tracked by the given commit. Removes tracked files that are not present in that commit. Also moves the current branch’s head to that commit node.

- java gitlet.Main merge [branch name]
  - Merges files from the given branch into the current branch. Takes into account the file data at the two branches and the file data in the latest common ancestor of the two branches.

- rm -rf .gitlet
  - Uninitializes the repository, undoing java gitlet.Main init
