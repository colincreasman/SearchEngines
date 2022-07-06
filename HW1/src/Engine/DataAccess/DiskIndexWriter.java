package Engine.DataAccess;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import Engine.Indexes.PositionalInvertedIndex;
import Engine.Weights.*;
import Engine.Indexes.Index;
import Engine.Indexes.Posting;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import javax.swing.text.Position;

import static App.Driver.ActiveConfiguration.activeCorpus;

public class DiskIndexWriter {
    private static File mIndexDir;
    private static BinFileDao mBinDao;
    private static DbFileDao mDbDao;
    private static List<Long> mByteLocations;

    public DiskIndexWriter() {
        String indexPath = activeCorpus.getPath() + "/index";
        mIndexDir = new File(indexPath);
        mBinDao = new BinFileDao(mIndexDir);
        mDbDao = new DbFileDao(mIndexDir);
        mByteLocations = new ArrayList<>();
    }

    public List<Long> writeIndex(Index index, String corpusPath) {
        mIndexDir = new File(corpusPath);
        mBinDao = new BinFileDao(mIndexDir);
        mDbDao = new DbFileDao(mIndexDir);

        mDbDao.open("termLocations");
        mBinDao = new BinFileDao(mIndexDir);

        mBinDao.open("postings");

        System.out.println("Writing the index to disk...");
        // start timer
        long start = System.currentTimeMillis();

        // start a byte address counter at 0 for the beginning of the file
        long byteAddress = 0;

        for (String term : index.getVocabulary()) {
            // use the byteCount of each new term to add to the list of byteLocations and write to the db file
            mByteLocations.add(byteAddress);
            mDbDao.writeTermLocation(term, byteAddress);

            // write the current term's postings to the binary file at the current byteAddress, then increment it by the amount bytes returned
            List<Posting> termPostings = index.getPostings(term);

            try {
                long postingsBytes = mBinDao.writePostings(byteAddress, termPostings);
                byteAddress += postingsBytes;
            } catch (IOException ex) {
                System.out.println("Error: Failed to write postings to disk for the current term: " + term);
                ex.printStackTrace();
            }
            long stop = System.currentTimeMillis();
            long elapsedSeconds = (long) ((stop - start) / 1000.0);
            System.out.println("Finished writing index to disk in approximately " + elapsedSeconds + " seconds.");
        }
        mBinDao.close("postings");
        return mByteLocations;
    }

    public void writeDocWeights(List<DocWeight> docWeights) {
        mBinDao.open("docWeights");
        try {
            mBinDao.writeDocWeights(docWeights); // will be written at byte location: docWeightsOut. length() - 4
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        mBinDao.open("docWeights");
    }

    public List<String> readVocabulary() {
        mDbDao.open("termLocations");
        return mDbDao.readVocabulary();
    }

    public boolean hasExistingIndex() {
        return mIndexDir.exists();
    }
}
