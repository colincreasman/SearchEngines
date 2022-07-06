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

        if (!mActiveDb.exists(tempFile.getName()) || mActiveDb == null) {
            mActiveDb = DBMaker.fileDB(tempFile).make();
            mActiveMap = mActiveDb.treeMap(name).keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .create();
        }
            // get the File instance for the newly created db and add it to the static list of files
//            mFiles.add(mActiveDb.get(name));
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }

    @Override
    public void open(String name) {
        String dbPath = mSourceDir + "/" + name + ".bin"; // construct the entire file path using the static mSourceDir and the ".bin" extension for this implementation
        File tempFile = new File(dbPath);

        if (!mActiveDb.exists(tempFile.getName()) || mActiveDb == null) {
            mActiveDb = DBMaker.fileDB(tempFile).make();
            mActiveMap = mActiveDb.treeMap(name).keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .createOrOpen();
        }
            // check if currently closed before opening
        else if (mActiveDb.isClosed() || mActiveMap.isClosed()) {
            mActiveDb = DBMaker.fileDB(tempFile).make();
            mActiveDb.treeMap(name).keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG).open();
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


