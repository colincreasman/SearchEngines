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

import static App.Driver.ActiveConfiguration.activeWeigher;

public class DiskIndexWriter {
    private static File mIndexDir;
    private static Index mIndex;
    private static BinFileDao mBinDao;
    private static DbFileDao mDbDao;
    private static List<Long> mByteLocations;

    public DiskIndexWriter() {

        mBinDao = new BinFileDao();
        mDbDao = new DbFileDao();
        mByteLocations = new ArrayList<>();
    }

    public List<Long> writeIndex(Index index, String corpusPath) {
        String indexPath = corpusPath + "/index";
        mIndexDir = new File(indexPath);
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
            }
            catch (IOException ex) {
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
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        mBinDao.open("docWeights");
    }

    // reads select postings data from the disk to return a list of postings,
    // each constructed with vals for its docId and docWeights
    // all other postings data will be handled in the getPostings() method calling this one
    // initialize necessary vars for results and structs to read from the termsDB
    @Override
    public List<Posting> readPostingsWithoutPositions(long byteLocation) {
        int termDocFrequency = 0;
        double termDocWeight = 0.0;
        List<Posting> results = new ArrayList<>();

        try (RandomAccessFile reader = new RandomAccessFile(mPostingsPath, "r")) {
            // start by seeking to the byte location of the term - this is where all of its postings data begins
            // now we can easily find any other postings data we need by incrementing the necessary amount of bytes from that initial position
            reader.seek(byteLocation);

            // first get tf(d) (the amount of docs the term occurs in) which should be at the exact byte indicated by the termLocation
            int totalDocs = reader.readInt();

            // before iterating through all the term's docs, initialize the first docId to 0
            // this allows it to accurately represent every subsequent docId by continually incrementing with the gap size as we iterate
            int currentDocId = 0;

            // now we can use that tf(d) value find the rest of the term's data
            for (int i = 0; i < totalDocs; i++) {
                // for the first value only, read the doc ID as-is with no gaps
                if (i == 0) {
                    currentDocId = reader.readInt();
                }
                // otherwise, read the next docId and use it to increment the gap between itself and the previous docId
                else {
                    currentDocId += reader.readInt();
                }

                // read the next term weight directly from the current reader position (they are not written as gaps)
                double currentWeight = reader.readDouble();

                // increment the gap counter for docId by 4 to account for the 4 bytes read for the tf(t,d)
                int currTermFrequency = reader.readInt();

                // next jump ahead by the 4 * (number of terms) to get to the next docId
                reader.skipBytes(4 * currTermFrequency);

                Posting currPosting = new Posting(currentDocId, currentWeight, currTermFrequency);
                results.add(currPosting);
            }
            reader.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return results;
    }

    @Override
    public List<Posting> readPostings(long byteLocation) {
        int termDocFrequency = 0;
        double termDocWeight = 0.0;

        List<Posting> results = new ArrayList<>();

        try (RandomAccessFile reader = new RandomAccessFile(mPostingsPath, "r")) {
            // start by seeking to the byte location of the term - this is where all of its postings data begins
            // now we can easily find any other postings data we need by incrementing the necessary amount of bytes from that initial position
            reader.seek(byteLocation);

            // first get tf(d) (the amount of docs the term occurs in) which should be at the exact byte indicated by the termLocation
            int totalDocs = reader.readInt();

            // before iterating through all the term's docs, initialize the first docId to 0
            // this allows it to accurately represent every subsequent docId by continually incrementing with the gap size as we iterate
            int currentDocId = 0;

            // now we can use that tf(d) value find the rest of the term's data
            for (int i = 0; i < totalDocs; i++) {
                // for the first value only, read the doc ID as-is with no gaps
                if (i == 0) {
                    currentDocId = reader.readInt();
                }
                // otherwise, read the next docId and use it to increment the gap between itself and the previous docId
                else {
                    currentDocId += reader.readInt();
                }

                // read the next term weight directly from the current reader position (they are not written as gaps)
                double currentWeight = reader.readDouble();

                // increment the gap counter for docId by 4 to account for the 4 bytes read for the tf(t,d)
                int currTermFrequency = reader.readInt();

                List<Integer> termPositions = new ArrayList<>();

                int currTermPosition = 0;

                for (int j = 0; j < currTermFrequency; j++) {
                    if (j == 0) {
                        currTermPosition = reader.readInt();
                    }
                    else {
                        currTermPosition += reader.readInt();
                    }
                    termPositions.add(currTermPosition);
                }

                Collections.sort(termPositions);
                Posting currPosting = new Posting(currentDocId, currentWeight, currTermFrequency, termPositions);
                results.add(currPosting);
            }
            reader.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return results;
    }

}
