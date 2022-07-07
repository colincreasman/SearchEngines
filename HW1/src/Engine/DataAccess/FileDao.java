package Engine.DataAccess;

import org.mapdb.DB;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static App.Driver.ActiveConfiguration.activeCorpus;

public class FileDao {
    // TODO: maybe make these static to allow different implementations to coexist in different sourceDir's?
    public String mFileExt;
    public File mActiveFile;

    public static File mSourceDir;
    public static List<File> mOpenFiles;


    // no-arg constructor defaults to using the activeCorpus' path and appending it to create the sourceDir
    public FileDao() {
        String dirPath = activeCorpus.getPath() + "/index";
        mSourceDir = new File(dirPath);
        // only create a new source directory if mSourceDir is null or does not already exist
        if (!mSourceDir.exists()) {
            if (mSourceDir.mkdir()) {
                mOpenFiles = Arrays.stream(mSourceDir.listFiles()).toList();
            }
            else {
                mOpenFiles = new ArrayList<>();
            }
        }
        mFileExt = "";
    }

    public FileDao(File sourceDir) {
        // check if the newly passed in directory already exists
            // if not, use it to create a new dir and reassign the static mSourceDir property
        mSourceDir = sourceDir;
            if (sourceDir.mkdir()) {
                mOpenFiles = Arrays.stream(mSourceDir.listFiles()).toList();
            }
            else {
                mOpenFiles = new ArrayList<>();
            }
            mFileExt = "";
    }


    public void create(String name) {
        String filePath = mSourceDir + "/" + name + mFileExt; // construct the entire file path using the static mSourceDir and the ".bin" extension for this implementation
        File newFile = new File(filePath);
//
//        // make sure there is no current index folder in the given path before indexing
//        if (newFile.exists()) {
//            // if there's already a folder, delete it and its contents
//            newFile.delete();
//        }
        try {
            // if creating the new file is successful, add it to the list of binFiles
            newFile.createNewFile();
        }
        catch (IOException ex) {
            System.out.println("Error: File could not be created in the current source directory. ");
        }
    }

    public void open(String name) {
        String filePath = mSourceDir + "/" + name + mFileExt; // construct the entire file path using the static mSourceDir and the ".bin" extension for this implementation
        File openFile = new File(filePath);
        // create the file if it isn't already existing
        if (!openFile.exists()) {
            create(name);
        }
        // make sure the requested file isn't already in the static list of open files
//        if (!mOpenFiles.contains(openFile)) {
//            try {
//                FileOutputStream fileStream = new FileOutputStream(openFile);
//                mActiveWriter = new DataOutputStream(fileStream);
//                mActiveReader = new RandomAccessFile(openFile, "r");
//                mActiveFile = openFile;
//                mOpenFiles.add(openFile);
//            }
//            catch (FileNotFoundException ex) {
//                System.out.println("Error: The active reader/writer could not be opened because the file does not exist ");
//            }
//        }
    }

    public void close(String name) {
        String filePath = mSourceDir + "/" + name + mFileExt;
        File closeFile = new File(filePath);

        // make sure the file exists before continuing
        if (!closeFile.exists()) {
            System.out.println("Error: The active reader/writer could not be closed because it does not exist. ");
            return;
        }

        // if it does exist, check if the file is in the list of  open files before attempting to close it
        if (mOpenFiles.contains(closeFile)) {
            mOpenFiles.remove(closeFile);
//            mActiveReader = null;
//            mActiveWriter = null;
            mActiveFile = null;
        }
        else {
            System.out.println("Error: The active reader/writer could not be closed because it was never opened. ");
        }
    }

    public String readSingleLine(int lineNum) {
        String qPath = mActiveFile.getPath();
        String queryLine = "";
        try {
            queryLine = Files.readAllLines(Paths.get(qPath)).get(lineNum);
        } catch (IOException e) {
            System.out.println("Error: The query could not be read because the requested line number is past the end of the file.");
            e.printStackTrace();
        }
        return queryLine;
    }

    public List<String> readAllLines() {
        String qPath = mActiveFile.getPath();
        List<String> qLines = new ArrayList<>();
        try {
            qLines = Files.readAllLines(Paths.get(qPath));
        } catch (IOException e) {
            System.out.println("Error: The query could not be read because the requested line number is past the end of the file.");
            e.printStackTrace();
        }
        return qLines;
    }
}

