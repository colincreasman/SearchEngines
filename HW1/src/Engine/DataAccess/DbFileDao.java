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
    private DB mActiveDb;
    private BTreeMap<String, Long> mActiveMap;

    public DbFileDao() {
        super();
        mFileExt = ".db";
    }

    public DbFileDao(File sourceDir) {
        super(sourceDir);
        mFileExt = ".db";
    }

    @Override
    public void create(String name) {
        String dbPath = mSourceDir + "/" + name + mFileExt;
        File tempFile = new File(dbPath);
        try {
//        if (mActiveDb == null) {
            mActiveDb = DBMaker.fileDB(tempFile).make();
            mActiveMap = mActiveDb.treeMap(name).keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .createOrOpen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void open(String name) {
        String dbPath = mSourceDir + "/" + name + mFileExt; // construct the entire file path using the static mSourceDir
        // and the ".bin" extension for this implementation
        File tempFile = new File(dbPath);

        try {
            mActiveDb = DBMaker.fileDB(tempFile).make();
            mActiveMap =
                    mActiveDb.treeMap(name).keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG).counterEnable().createOrOpen();
        }
            // check if currently closed before opening
        catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close(String name) {
//        mActiveMap.close();
        mActiveDb.close();
    }

    public void writeTermLocation(String term, long byteLocation) {
        mActiveMap.put(term, byteLocation);
    }

    public long readTermLocation(String term) {
        long termLocation = 0;
        try {
             termLocation = mActiveMap.get(term);
        }
        catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        return termLocation;
    }

    public List<String> readVocabulary() {
        Iterator<String> keyIter = mActiveMap.keyIterator();
        List<String> terms = new ArrayList<>();
        while (keyIter.hasNext()) {
            terms.add(keyIter.next());
        }
        return terms;
    }

    public void writeVocabulary(List<String> terms, List<Long> bytes) {
        for (int i = 0; i < terms.size(); i++) {
            String term = terms.get(i);
            long currLocation = bytes.get(i);
            mActiveMap.put(term, currLocation);
        }
    }

}


