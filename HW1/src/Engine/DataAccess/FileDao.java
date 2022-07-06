package Engine.DataAccess;

import org.mapdb.DB;

import javax.swing.*;
import java.io.*;
import java.util.*;

import static App.Driver.ActiveConfiguration.activeCorpus;

public abstract class FileDao {
    // TODO: maybe make these static to allow different implementations to coexist in different sourceDir's?
    public static File mSourceDir;
    public static List<File> mOpenFiles;

    // no-arg constructor defaults to using the activeCorpus' path and appending it to create the sourceDir
    public FileDao() {
        String dirPath = activeCorpus.getPath() + "/index";
        mSourceDir = new File(dirPath);
        // only create a new source directory if mSourceDir is null or does not already exist
        if (!mSourceDir.exists()) {
            mSourceDir.mkdir();
            mOpenFiles = Arrays.stream(mSourceDir.listFiles()).toList();
        }
    }

    public FileDao(File sourceDir) {
        // check if the newly passed in directory already exists
        if (!sourceDir.exists()) {
            // if not, use it to create a new dir and reassign the static mSourceDir property
            sourceDir.mkdir();
        }
        mSourceDir = sourceDir;
        mOpenFiles = Arrays.stream(mSourceDir.listFiles()).toList();
    }

    public abstract void create(String name);

    public abstract void open(String name);

    public abstract void close(String name);

    }
