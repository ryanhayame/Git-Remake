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

## Tech Stack:
Java

## Challenges and Lessons Learned:
- Experimented with many different types of data structures to ensure optimal storage and retrieval of data
- Spent a lot of time learning the complexities of Git, as well as how it works internally
- Worked with data persistence and serialization for the first time
- Learned how to optimally traverse and search for data within various data structures

## How to Run:
-
