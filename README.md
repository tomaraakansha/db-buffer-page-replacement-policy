# db-buffer-page-replacement-policy
This project implements two page replacement policies: FIFO and Random and buffer hit ratios for these 
policies of the buffer manager layer and of the database management system 
[MINIBASE](https://research.cs.wisc.edu/coral/mini_doc/minibase.html). These hit ratios indicate the effectiveness of buffer replacement policies.

**DO NOT add Bin folder or class files to the gitignore file**
**DO NOT delete bin folder, as this contains the compiled classes needed to run the code**

# How to run this project:

## prerequisite:
* Have a JDK 17 installed

This project consists of makefile for the commands to compile and run the code.

## Running project on Linux

1. Download make command:
```
sudo apt update
sudo apt install make
```

2. Go inside the FIFO-policy folder/Random-policy folder, run the following command to compile the code:
```
make bufmgr
```
This command will compile the code in src folder

3. run this command to run the code:
```
make bhrtest
make bmtest
```

## Running project on Windows

For running this project on windows, you need to make changes in the makefile to point it to the right location
(change '/' to '\'). 