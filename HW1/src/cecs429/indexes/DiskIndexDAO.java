package cecs429.indexes;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static edu.csulb.Driver.ActiveConfiguration.*;

import cecs429.documents.Document;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import javax.print.Doc;

public class DiskIndexDAO implements IndexDAO {
  //  private PositionalInvertedIndex mPosIndex;
    private static String mIndexPath;
    private static String mPostingsPath;
    private static String mDocWeightsPath;
    private static String mDbPath;
    private static List<Long> mByteLocations;
    private HashMap<String, Integer> mTermLocations; // maps each term to its byte location in postings.bin

    public DiskIndexDAO(String corpusPath) {
        mIndexPath = corpusPath + "/index";
        mDbPath = mIndexPath + "/termsMap.db";
        mPostingsPath = mIndexPath + "/postings.bin";
        mDocWeightsPath = mIndexPath + "/docWeights.bin";
        mByteLocations = new ArrayList<>();

        // create the index dir if it doesn't already exist
        File indexDir = new File(mIndexPath);
        if (!indexDir.exists()) {
            indexDir.mkdir();
        }
    }

    @Override
    public boolean hasExistingIndex() {
        Path dir = Paths.get(mIndexPath).toAbsolutePath();
        Path postings = Paths.get(mPostingsPath).toAbsolutePath();
        Path weights = Paths.get(mDocWeightsPath).toAbsolutePath();

        // only return true if all of the required files already exist
        if (Files.exists(dir) && Files.exists(postings)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<Long> writeIndex(Index index, String corpusPath) {
        Collections.sort(index.getVocabulary());

        File indexDir = new File(mIndexPath);
        // make sure there is no current index folder in the given path before indexing
        if (indexDir.exists()) {
            // if there's already a folder, delete it and its contents
            indexDir.delete();
        }
        // now that we know there's no current index dir in this path, make the directory and set up the individual file paths within it
        indexDir.mkdir();

        // try to make a new .bin file using the mPostingsPath set at construction
        File postingsBin = new File(mPostingsPath);
        // make sure there is no other postings.bin file already in index the directory before trying to write a new one
        if (postingsBin.exists()) {
            postingsBin.delete();
        }

        File docWeightsBin = new File(mDocWeightsPath);
        // make sure there is no other docWeights.bin file already in index the directory before trying to write a new one
        if (docWeightsBin.exists()) {
            docWeightsBin.delete();
        }

        // initialize necessary structs for the results list and the DB writers
        List<Long> results = new ArrayList<>();

        // try to initialize the db as a child file of the overall index directory
        try {
            // set up a  B+ tree that maps all the on-disk terms to their byte locations
            DB termsDb = DBMaker.fileDB(mDbPath).make();
            BTreeMap<String, Long> termsMap = termsDb.treeMap("map").keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .createOrOpen();

            // setup output streams for postings.bin and docWeights.bin
            FileOutputStream postingsStream = new FileOutputStream(postingsBin);DataOutputStream postingsOut = new DataOutputStream(postingsStream);

            // start a byte counter at 0 that will increment by 4 bytes everytime something new is written to the file
            // alternatively, we can get the byte location of each term by calling .size() on the output file for every new term - "returns the current number of bytes written to the output stream so far"
            // initialize both approaches to 0 before counting any terms
            long bytesByCount = 0;

            for (String term : index.getVocabulary()) {
                List<Posting> currPostings = index.getPostings(term);
                int docFrequency = currPostings.size();

                // we also need to write the current term and byte location to the RDB B+ tree by adding the vals to the map using .put()
                termsMap.put(term, bytesByCount);
                postingsOut.writeInt(docFrequency);


                mByteLocations.add(bytesByCount);
                bytesByCount += 4; // whenever a 4-byte int is written to the file, increment the byteCounter by 4 to account for the 4 bytes used to write the integer

                // for the first of the current term's postings, we can take its docId as-is without using gaps
                int docId = currPostings.get(0).getDocumentId();

                // now go through each posting for the given term
                for (int i = 0; i < currPostings.size(); i++) {
                    // setup vars for the current posting's tf(t,d) value and its list of term locations
                    int termFrequency = currPostings.get(i).getTermPositions().size(); // tf(t,d)
                    List<Integer> positions = currPostings.get(i).getTermPositions();

                    // if this is not the first posting for a given term, the docId must be re-assigned using the gap between itself and the previous docId
                    if (i > 0) {
                        int oldId = currPostings.get(i - 1).getDocumentId();
                        int currId = currPostings.get(i).getDocumentId();
                        docId += (currId - oldId);
                    }

                    // write the current docId and tf(t,d) values to the file
                    postingsOut.writeInt(docId);
                    bytesByCount += 4;

                    // TODO: get the w(d,t) value of the current term and doc by calling calculateTermWeight(termFrequency), then write it to disk right here (after writing the docId but before the tf(t,d)
                    double termWeight = calculateTermDocWeight(termFrequency);
                    postingsOut.writeDouble(termWeight);
                    bytesByCount += 8; // increment byte position by 8 to account for the 8 bytes used for termWeight

                    postingsOut.writeInt(termFrequency);
                    bytesByCount += 4;

                    // setup the currPosition var that will be written to disk on for each term position
                    // just like the docIds above, initialize currPosition BEFORE iterating through the list by assigning it to the first term position as-is
                    int termPosition = positions.get(0);

                    // now handle the remaining term positions by finding the gaps between each subsequent position
                    for (int j = 0; j < positions.size(); j++) {
                        // for every position after the first, re-assign it to the difference between itself and the previous position
                        if (j > 0) {
                            int oldPosition = positions.get(j - 1);
                            int currPosition = positions.get(i);
                            termPosition += (currPosition - oldPosition);
                        }
                        // write the updated position to the file and increment the byte counter
                        postingsOut.writeInt(termPosition);
                        bytesByCount += 4;
                    }
                }
            }
            termsDb.close();
            postingsOut.close();
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
            DB termsDb = DBMaker.fileDB(mDbPath).make();
            BTreeMap<String, Long> termsMap = termsDb.treeMap("map").keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .createOrOpen();

            // now we can write the term and its location to disk by simply calling put() on the map
            termsMap.put(term, bytePosition);
            termsDb.close();
        } catch (Exception ex) {
            System.out.println("Failed to write the term '" + term + "' with a byte location of " + bytePosition + ". \n");
            ex.printStackTrace();
        }
    }

    @Override
    public void writeDocWeights(HashMap<Integer,Double> weightsMap) {
        // try to initialize the docWeights file as a child file of the overall index directory
        try {
            File docWeightsBin = new File(mDocWeightsPath);
            // make sure there is no other docWeights.bin file already in index the directory before trying to write a new one
            if (docWeightsBin.exists()) {
                docWeightsBin.delete();
            }

            FileOutputStream docWeightsStream = new FileOutputStream(docWeightsBin);
            DataOutputStream docWeightsOut = new DataOutputStream(docWeightsStream);

            int gapId = 0;
            for (int currId : weightsMap.keySet()
            ) {
                //gapId = gapId + (currId - gapId);
                // only write AFTER updating the gap
                docWeightsOut.writeInt(currId);
                docWeightsOut.writeDouble(weightsMap.get(currId));
            }
            docWeightsOut.close();
        }
//                // for the first term only, write the gapId as-is without updating it with the current doc
//                if (gapId == 0) {
//                    docWeightsOut.writeInt(gapId);
//                    gapId += currId; // ONLY DO THIS FOR THE FIRST TERM because its zero
//                }
//                // use the gapId to write the remaining docIds
//                else {
//                    // increment gapId by the difference between itself and the next docId
//                    gapId = gapId + (currId - gapId);
//                    // only write AFTER updating the gap
//                    docWeightsOut.writeInt(currId)
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public HashMap<Integer, Double> readDocWeights() {
        HashMap<Integer, Double> results = new HashMap<>();
        double weight = 0.0;

        try (RandomAccessFile reader = new RandomAccessFile(mDocWeightsPath, "r")) {
            // start reading from the beginning of the file, the first byte should hold the first doc id
            reader.seek(0);
            for (int i = 0; i < activeCorpus.getCorpusSize(); i++) {
                int currId = reader.readInt();
                double currWeight = reader.readDouble();
                results.put(currId, currWeight);
            }
//            while (gapId < reader.length()) {
//                // start the reader at the first byte location in the file
//                reader.seek(gapId
//
//                // get the first docId from the first byte and its docweight from the next 8 bytes after it
//                int currId = reader.readInt();
//                double currWeight = reader.readDouble();
//            }
        }
        catch (Exception ex) {
            System.out.println("Failed to read the doc weights from disk. '");
            ex.printStackTrace();
        }
        return results;
    }

    public HashMap<String, Long> readTermLocations() {
        HashMap<String, Long> results = new HashMap<>();
        // try to initialize the db as a child file of the overall index directory
        try {
            // if possible, create a B+ tree that maps all the on-disk terms to their byte locations
            DB termsDb = DBMaker.fileDB(mDbPath).make();
            BTreeMap<String, Long> termsMap = termsDb.treeMap("map").keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .open();


            // extract the list of all terms from the map B+ tree as an in-memory List iterator
            Iterator<String> termsOnDisk = termsMap.keyIterator();
            // now we can add all the on-disk terms to an in-memory return value by extracting terms from the iterator until the stream ends
            while (termsOnDisk.hasNext()) {
                String term = termsOnDisk.next();
                long location = termsMap.get(term);
                results.put(term, location);
//            }
//            // now we can write the term and its location to disk by simply calling put() on the map
//            for (String term : termsMap.keySet()) {
//                results.put(term, termsMap.get(term));
            }
            termsDb.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return results;
    }

    @Override
    public List<String> readVocabulary() {
        // initialize necessary structs
        List<String> results = new ArrayList<>();
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
            termsDb.close();
        }
        catch (Exception ex) {
            System.out.println("Error reading from on disk index: No on-disk B+ tree was found in the provided index directory");
            return null;
        }
        return results;
    }

    // reads select postings data from the disk to return a list of postings,
    // each constructed with vals for its docId and docWeights
    // all other postings data will be handled in the getPostings() method calling this one
    // initialize necessary vars for results and structs to read from the termsDB
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

                // next jump ahead by the 4 * (number of terms) to get to the next docId
                reader.skipBytes(4 * currTermFrequency);

                Posting currPosting = new Posting(currentDocId, currentWeight, currTermFrequency);
                results.add(currPosting);
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return results;
    }

    // calculates the weight of a term in a given doc using the formula w(d,t) = 1 + ln(tf,td)
    private double calculateTermDocWeight(int termDocFrequency) {
        double weight = 0.0;

        switch (weightingScheme) {
            // the default scheme uses the formula w(d,t) = 1 + ln(tf,td)
            case Default: {
                double termFrequency = (double) termDocFrequency;
                weight = 1 + Math.log(termFrequency);
                break;
            }

            // TODO: implement the rest of these!
            case TF_IDF: {
                break;
            }
            case Okapi: {

            }
            case Wacky: {
                break;
            }
            default: {
                System.out.println("Error: Selected weighting scheme has not been implemented yet");
                break;
            }
        }
        return weight;
    }
}
