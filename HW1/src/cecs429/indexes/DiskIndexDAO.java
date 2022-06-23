package cecs429.indexes;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static edu.csulb.Driver.ActiveConfiguration.*;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

public class DiskIndexDAO implements IndexDAO {
    private PositionalInvertedIndex mPosIndex;
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
        Collections.sort(mPosIndex.getVocabulary());

        File indexDir = new File(corpusPath + "/index");
        // make sure there is no current index folder in the given path before indexing
        if (indexDir.exists()) {
            // if there's already a folder, delete it and its contents
            indexDir.delete();
        }
        // now that we know there's no current index dir in this path, make the directory and set up the individual file paths within it
        indexDir.mkdir();
        mDbPath = indexDir + "/termsMap.db";
        mPostingsPath = indexDir + "/postings.bin";

        // initialize necessary structs for the results list and the DB writers
        List<Long> results = new ArrayList<>();
        DB termsDb = null;
        BTreeMap<String, Long> termsMap = null;

        // try to initialize the db as a child file of the overall index directory
        try {
            // if possible, create a B+ tree that mapes all the on-disk terms to their byte locations
            termsDb = DBMaker.fileDB(mDbPath).make();
            termsMap = termsDb.treeMap("map").keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .createOrOpen();

            // try to make a new .bin file using the mPostingsPath set at construction
            File postingsBin = new File(mPostingsPath);
            // make sure there is no other postings.bin file already in index the directory before trying to write a new one
            if (postingsBin.exists()) {
                postingsBin.delete();
            }
            // now use it to create an output stream to allowing us to write to the new file
            FileOutputStream fileStream = new FileOutputStream(postingsBin);
            DataOutputStream outStream = new DataOutputStream(fileStream);

            // start a byte counter at 0 that will increment by 4 bytes everytime something new is written to the file
            // alternatively, we can get the byte location of each term by calling .size() on the output file for every new term - "returns the current number of bytes written to the output stream so far"
            // initialize both approaches to 0 before counting any terms
            long bytesByCount = 0;

            for (String term : index.getVocabulary()) {

                List<Posting> currPostings = index.getPostings(term);
                int docFrequency = currPostings.size();
                outStream.writeInt(docFrequency);

                // we also need to write the current term and byte location to the RDB B+ tree by adding the vals to the map using .put()
                termsMap.put(term, bytesByCount);

                bytesByCount += 4; // whenever a 4-byte int is written to the file, increment the byteCounter by 4 to account for the 4 bytes used to write the integer
                mByteLocations.add(bytesByCount);

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
                        docId = docId - oldId;
                    }

                    // write the current docId and tf(t,d) values to the file
                    outStream.writeInt(docId);
                    bytesByCount += 4;

                    // TODO: get the w(d,t) value of the current term and doc by calling calculateTermWeight(termFrequency), then write it to disk right here (after writing the docId but before the tf(t,d)
                    double termWeight = calculateTermWeight(termFrequency);
                    outStream.writeDouble(termWeight);
                    // increment byte position by 8 to account for the 8 bytes used for termWeight
                    bytesByCount += 8;

                    outStream.writeInt(termFrequency);
                    bytesByCount += 4;

                    // setup the currPosition var that will be written to disk on for each term position
                    // just like the docIds above, initialize currPosition BEFORE iterating through the list by assigning it to the first term position as-is
                    int currPosition = positions.get(0);

                    // now handle the remaining term positions by finding the gaps between each subsequent position
                    for (int j = 0; j < positions.size(); j++) {
                        // for every position after the first, re-assign it to the difference between itself and the previous position
                        if (j > 0) {
                            int oldPosition = positions.get(j - 1);
                            currPosition = currPosition - oldPosition;
                        }
                        // write the updated position to the file and increment the byte counter
                        outStream.writeInt(currPosition);
                        bytesByCount += 4;
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        termsDb.close();
        return mByteLocations;
    }

    // calculates the weight of a term in a given doc using the formula w(d,t) = 1 + ln(tf,td)
    private double calculateTermWeight(int termFrequency) {
        double weight = 0.0;

        switch (weightingScheme) {
            // the default scheme uses the formula w(d,t) = 1 + ln(tf,td)
            case Default: {
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

    @Override
    public void writeTermLocations(String term, long bytePosition) {
        // initialize necessary structs
        DB termsDb = null;
        BTreeMap<String, Long> termsMap = null;

        // try to initialize the db as a child file of the overall index directory
        try {
            // if possible, create a B+ tree that mapes all the on-disk terms to their byte locations
            termsDb = DBMaker.fileDB(mDbPath).make();
            termsMap = termsDb.treeMap("map").keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .createOrOpen();

            // now we can write the term and its location to disk by simply calling put() on the map
            termsMap.put(term, bytePosition);

        } catch (Exception ex) {
            System.out.println("Failed to write the term '" + term + "' with a byte location of " + bytePosition + ". \n");
            ex.printStackTrace();
        }
        termsDb.close();
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
        } catch (Exception ex) {
            System.out.println("Error reading from on disk index: No on-disk B+ tree was found in the provided index directory");
            return null;
        }
        try {
            // extract the list of all terms from the map B+ tree as an in-memory List iterator
            Iterator<String> termsOnDisk = termsMap.keyIterator();

            // now we can add all the on-disk terms to an in-memory return value by extracting terms from the iterator until the stream ends
            while (termsOnDisk.hasNext()) {
                results.add(termsOnDisk.next());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        termsDb.close();
        return results;
    }


    /**
     * Reads the raw tf(t,d) data from the persistent data store for a given term & docId
     *
     * @param term
     * @param docId
     * @return the converted tf(t,d) data as a list of doubles
     */
    @Override
    public int readTermDocFrequency(String term, int docId) {
        return 0;
    }

    // for a given term, list of all the docIds and their termDocWeights
    @Override
    public HashMap<Integer, Double> readTermData(String term) {
        // initialize necessary vars for results and structs to read from the termsDB
        HashMap<Integer, Double> results = new HashMap<>();
        int termDocFrequency = 0;
        double termDocWeight = 0.0;
        long byteLocation = 0;
        DB termsDb = null;
        BTreeMap<String, Long> termsMap = null;

        // try to initialize the db as a child file of the overall index directory
        try {
            // if possible, create a B+ tree that maps all the on-disk terms to their byte locations
            termsDb = DBMaker.fileDB(mDbPath).make();
            termsMap = termsDb.treeMap("map").keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .createOrOpen();

            //  after successfully loading the on-disk terms map, we can quickly get the byte position of the given term by simply calling .get() on the termsMap
            byteLocation = termsMap.get(term);

            // now that we know the byte location for the term, try to open a fileReader on the postings.bin file
            try (RandomAccessFile reader = new RandomAccessFile(mPostingsPath, "r")) {
                // start by seeking to the byte location of the term - this is where all of its postings data begins
                // now we can easily find any other postings data we need by incrementing the necessary amount of bytes from that intial position
                reader.seek(byteLocation);

                // first get tf(d) (the amount of docs the term occurs in) which should be at the exact byte indicated by the termLocation
                int totalDocs = reader.readInt();

                // before iterating through all the term's docs, initialize the first docId to 0
                // this allows it to accurately represent every subsequent docId by continually incrementing with the gap size as we iterate
                int currentDocId = 0;

                // now we can use that tf(d) value find the rest of the term's data
                for (int i = 0; i < totalDocs; i++) {
                    if (i == 0) {
                        // for the first value only, read the doc ID as-is with no gaps
                        currentDocId = reader.readInt();
                    }

                    // read the next docId and use it to increment the gap betweem the previous docId
                    currentDocId += reader.readInt();

                    // read the next term weight directly from the current reader position (they are not written as gaps)
                    double currentWeight = reader.readDouble();

                    // add the current docId and weight to the results before jumping to the next posting
                    results.put(currentDocId, currentWeight);

                    // increment the counter by 8 to account for the 8bytes read for the weight
                    reader.skipBytes(4);

                    // increment the gap counter for docId by 4 to account for the 4 bytes read for the tf(t,d)
                    int totalPositions = reader.readInt();

                    // next jump ahead by the 4 * (number of terms) to get to the next docId
                    reader.skipBytes(4 * totalPositions);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        termsDb.close();
        return results;
    }

    /**
     * reads the raw Ld data from docWeights.bin (or other implemented datastore) for a given docId
     *
     * @param docId
     * @return raw Ld data converted to Double
     */
    @Override
    public double readDocWeight(int docId) {
        return 0;
    }
}
