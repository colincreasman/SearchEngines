package Engine.DataAccess;

import Engine.Indexes.Posting;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static App.Driver.ActiveConfiguration.indexWriter;

public class DbFileDao extends FileDao {
    private static DB mActiveDb;
    private static BTreeMap<String, Long> mActiveMap;

    public DbFileDao() {
        super();
    }

    public DbFileDao(File sourceDir) {
        super(sourceDir);
    }

    @Override
    public void create(String name) {
        String dbPath = mSourceDir + "/" + name + ".db";
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            dbFile.delete();
        }

        try {
            mActiveDb = DBMaker.fileDB(dbFile).make();
            mActiveMap = mActiveDb.treeMap(name).keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .create();

            // get the File instance for the newly created db and add it to the static list of files
//            mFiles.add(mActiveDb.get(name));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void open(String name) {
        String dbPath = mSourceDir + "/" + name + ".bin"; // construct the entire file path using the static mSourceDir and the ".bin" extension for this implementation
        File dbFile = new File(dbPath);

        // create the file if it isn't already in the source directory before continuing
        if (!dbFile.exists()) {
            create(name);
        }

        try {
            // check if currently closed before opening
            if (!mActiveDb.exists(name) || mActiveDb.isClosed()) {
                mActiveDb = DBMaker.fileDB(dbFile).make();
            }
            if (mActiveMap.isClosed())
                mActiveDb.treeMap(name).keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG).open();
        } catch (Exception ex) {
            System.out.println("Error : Could not connect to the DB file because the file and/or Db could not be opened");
            ex.printStackTrace();
        }
    }

    @Override
    public void close(String name) {
        if (!mActiveMap.isClosed()) {
            mActiveMap.close();
        }
        if (!mActiveDb.isClosed()) {
            mActiveDb.close();
        }
    }

    public void writeTermLocation(String term, long byteLocation) {
        mActiveMap.put(term, byteLocation);
        mActiveMap.getKeys();
    }

    public long readTermLocation(String term) {
        return mActiveMap.get(term);
    }

    public List<String> readVocabulary() {
        Iterator<String> keyIter = mActiveMap.keyIterator();
        List<String> terms = new ArrayList<>();
        while (keyIter.hasNext()) {
            terms.add(keyIter.next());
        }
        return terms;
    }

    
}


