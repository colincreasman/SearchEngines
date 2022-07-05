package Engine.DataAccess;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import Engine.Weights.*;
import Engine.Indexes.Index;
import Engine.Indexes.Posting;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

public class DiskIndexWriter {
    private static File mIndexDir;
    private static Index mIndex;
    private static BinFileDao mBinDao;
    private static DbFileDao mDbDao;
    private static List<Long> mByteLocations;

    public DiskIndexWriter() {
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
                    System.out.println("Error: Failed to write postings to disk for the term: " + term);
                    ex.printStackTrace();
                }


            long stop = System.currentTimeMillis();
            long elapsedSeconds = (long) ((stop - start) / 1000.0);
            System.out.println("Finished writing index to disk in approximately " + elapsedSeconds + " seconds.");
        }

        catch (IOException ex) {
            ex.printStackTrace();
        }

        return mByteLocations;
    }

    @Override
    public void writeTermLocation(String term, long bytePosition) {
        // try to initialize the db as a child file of the overall index directory
        try {
            // if possible, create a B+ tree that maps all the on-disk terms to their byte locations
            DB mTermsDb = DBMaker.fileDB(mDbPath).make();
            BTreeMap<String, Long> termsMap = mTermsDb.treeMap("map").keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .createOrOpen();

            // now we can write the term and its location to disk by simply calling put() on the map
            termsMap.put(term, bytePosition);
            mTermsDb.close();
        } catch (Exception ex) {
            System.out.println("Failed to write the term '" + term + "' with a byte location of " + bytePosition + ". \n");
            ex.printStackTrace();
        }
    }

    public void writeDocWeights(List<DocWeight> docWeights) {
        // try to initialize the docWeights file as a child file of the overall index directory
        try {
            // make sure there is no other docWeights.bin file already in index the directory before trying to write a new one
            if (mDocWeightsBin.exists()) {
                mDocWeightsBin.delete();
            }

            File mDocWeightsBin = new File(mDocWeightsPath);
            FileOutputStream docWeightsStream = new FileOutputStream(mDocWeightsBin);
            DataOutputStream docWeightsOut = new DataOutputStream(docWeightsStream);

            int avgDocLength = 0;
            for (DocWeight w : docWeights) {
                w.calculate();
                double docLd = w.getValue();
                int docLength = w.getDocLength();
                avgDocLength += docLength;
                int byteSize = w.getByteSize();
                int avgFrequency = w.getAvgTermFrequency();
                // write the per-doc weight data of each docWeight in the order of: docWeight, docLength, byteSize, avgTfTd,
                docWeightsOut.writeDouble(docLd); // byte 0
                docWeightsOut.writeInt(docLength); // byte 8
                docWeightsOut.writeInt(byteSize); // byte 12
                docWeightsOut.writeInt(avgFrequency); // byte 16
                // next DocWeight starts writing @ byte 20
            }

            docWeightsOut.writeInt(avgDocLength); // will be written at byte location: docWeightsOut.length() - 4
            avgDocLength = avgDocLength / docWeights.size();

            docWeightsOut.close();
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public double readDocWeight(int docId) {
        //HashMap<Integer, Double> results = new HashMap<>();
        double weight = 0.0;

        try (RandomAccessFile reader = new RandomAccessFile(mDocWeightsPath, "r")) {
            int byteLocation = (docId * 12) + 4; //
            // start reading from the beginning of the file, the first byte should hold the first doc id
            reader.seek(byteLocation);
            // read the first Id as-is
            // int previousId = 0;
            // for (int i = 0; i < activeCorpus.getCorpusSize(); i++) {
//                if (i > 0) {
//                    int gapId = reader.readInt();
//                    currId = previousId + gapId;
//                    previousId = currId;
//                }
            //   int currId = reader.readInt();

            weight = reader.readDouble();
            //    results.put(currId, currWeight);
            //}
            reader.close();
        }
        catch (Exception ex) {
            System.out.println("Failed to read the doc weights from disk. '");
            ex.printStackTrace();
        }
        return weight;
    }

    @Override
    public long readByteLocation(String term) {
        // only proceed to read from disk if they havent been loaded yet
        long location = 0;
        // try to initialize the db as a child file of the overall index directory
        try {
            // if possible, create a B+ tree that maps all the on-disk terms to their byte locations
            DB termsDb = DBMaker.fileDB(mDbPath).make();
            BTreeMap<String, Long> termsMap = termsDb.treeMap("map").keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .open();

            location = termsMap.get(term);

            termsDb.close();
        }
        catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        return location;
    }

    @Override
    public List<String> readVocabulary() {
        // initialize necessary structs
        List<String> results = new ArrayList<>();
        // check if the term locations have already been loaded into the class field before reading them from the disk
        if (mTermLocations != null && mTermLocations.size() > 0) {
            results.addAll(mTermLocations.keySet());
            return results;
        }

        // only proceed to read vocab from disk if it hasn't already been loaded
        DB termsDb = null;
        BTreeMap<String, Long> termsMap = null;

        // try to initialize the db as a child file of the overall index directory
        try {
            // if possible open the existing B+ tree that maps all the currently written on-disk terms to their byte locations
            termsDb = DBMaker.fileDB(mDbPath).make();
            termsMap = termsDb.treeMap("map").keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG).open(); // since this is a read-only function, we need to open the map, not openOrClose

            // extract the list of all terms from the map B+ tree as an in-memory List iterator
            Iterator<String> termsOnDisk = termsMap.keyIterator();
            // now we can add all the on-disk terms to an in-memory return value by extracting terms from the iterator until the stream ends
            while (termsOnDisk.hasNext()) {
                results.add(termsOnDisk.next());
            }
            termsMap.close();
            termsDb.close();
        }
        catch (Exception ex) {
            System.out.println("Error reading from on disk index: No on-disk B+ tree was found in the provided index directory");
            return null;
        }
        return results;
    }

    public void createIndexDirectory() {
        // make sure there is no current index folder in the given path before indexing
        if (mIndexDir.exists()) {
            // if there's already a folder, delete it and its contents
            mIndexDir.delete();
        }
        mIndexDir = new File(mIndexPath);
        // now that we know there's no current index dir in this path, make the directory and set up the individual file paths within it
        mIndexDir.mkdir();


        // make sure there is no other postings.bin file already in index the directory before trying to write a new one
        if (mPostingsBin.exists()) {
            mPostingsBin.delete();
        }
        mPostingsBin = new File(mPostingsPath);

        // make sure there is no other docWeights.bin file already in index the directory before trying to write a new one
        if (mDocWeightsBin.exists()) {
            mDocWeightsBin.delete();
        }
        mDocWeightsBin = new File(mDocWeightsPath);

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
