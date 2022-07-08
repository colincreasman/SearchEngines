package cecs429.indexes;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static edu.csulb.Driver.ActiveConfiguration.*;
import static edu.csulb.Driver.WeighingScheme;

import cecs429.weights.DocTermWeight;
import cecs429.weights.DocWeight;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

public class DiskIndexDAO {
    //  private PositionalInvertedIndex mPosIndex;
    private static String mIndexPath;
    private static String mPostingsPath;
    private static String mDocWeightsPath;
    private static String mDbPath;
    private static List<Long> mByteLocations;
    private HashMap<String, Long> mTermLocations; // maps each term to its byte location in postings.bin


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

    public boolean hasExistingIndex() {
        Path dir = Paths.get(mIndexPath).toAbsolutePath();
        Path postings = Paths.get(mPostingsPath).toAbsolutePath();
        Path weights = Paths.get(mDocWeightsPath).toAbsolutePath();

        // only return true if all of the required files already exist
        if (Files.exists(dir) && Files.exists(postings) && Files.exists(weights)) {
            return true;
        } else {
            return false;
        }
    }

    public List<Long> writeIndex(Index index, String corpusPath) {
        System.out.println("Writing the index to disk...");
        // start timer
        long start = System.currentTimeMillis();
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

        // initialize necessary structs for the results list and the DB writers
        List<Long> results = new ArrayList<>();
        mTermLocations = new HashMap<>();

        try {
            // set up a  B+ tree that maps all the on-disk terms to their byte locations
            DB termsDb = DBMaker.fileDB(mDbPath).make();
            BTreeMap<String, Long> termsMap = termsDb.treeMap("map").keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG)
                    .counterEnable()
                    .createOrOpen();


            // setup output streams for postings.bin and docWeights.bin
            FileOutputStream postingsStream = new FileOutputStream(postingsBin);
            DataOutputStream postingsOut = new DataOutputStream(postingsStream);

            // start a byte counter at 0 that will increment by 4 bytes everytime something new is written to the file
            // initialize both approaches to 0 before counting any terms
            long byteAddress = 0;

            for (String term : index.getVocabulary()) {
                List<Posting> currPostings = index.getPostings(term);
                //write the current term's byte location to the RDB B+ tree by adding the vals to the map using .put()
                try {
                    termsMap.put(term, byteAddress);
                    results.add(byteAddress);
                } catch (Exception ex) {
                    System.out.println("Failed to write the byte address to DB for the term: " + term);
                }
                // update byteAddress after writing
                try {
                    byteAddress = writePostings(postingsOut, currPostings, byteAddress);
                } catch (IOException ex) {
                    System.out.println("Failed to write to disk for term ' " + term + "' with the posting: " + currPostings);
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Failed to write the index to disk because the file does not exist");
        }
        return results;
    }

    /**
     * writes a list of posting for a given term
     * Each posting's data is written in the order: docId (as a gap), w(d,t) values, tf(t,d), and finally the list of term positions (as gap) {p1, p2...}
     * returns back a long value to indicate how many total bytes of the active file were used up by the list of postings
     */
    public long writePostings(DataOutputStream writer, List<Posting> termPostings, long byteAddress) throws IOException {
        // there is only a single dFt value for a list of postings, so write it to file first
        int dFt = termPostings.size();
        writer.writeInt(dFt);

        //  before iterating through each posting we can take the first docId as-is (without calculating gaps)
        int docId = termPostings.get(0).getDocumentId();

        for (int i = 0; i < dFt; i++) {
            // if this is not the first posting for a given term, the docId must be re-assigned using the gap between itself and the previous docId
            if (i > 0) {
                int oldId = termPostings.get(i - 1).getDocumentId();
                int currId = termPostings.get(i).getDocumentId();
                docId = (currId - oldId);
            }

            // write docId before any other postings data
            writer.writeInt(docId);
            byteAddress += 4;

            // use the current byteAddress to write the weights for the current posting and use the result to increment
            Posting currPosting = termPostings.get(i);
            byteAddress = writeTermWeights(writer, currPosting, byteAddress);

            // find tf(t,d) from the number of term locations and write it to file
            List<Integer> positions = currPosting.getTermPositions();
            int termFrequency = positions.size(); // tf(t,d)
            writer.writeInt(termFrequency);

            // use the current byteCount to write the current list of term positions and use the result to increment it
//            long positionIncrement = writePositions(byteAddress, positions);
//            byteAddress += positionIncrement;
            byteAddress = writePositions(writer, positions, byteAddress);
        }
        // return the final byteAddress after incrementing everything written
        return byteAddress;
    }

    /**
     * writes a list of term positions as gaps
     * returns back a long value to indicate how many total bytes of the active file were used up by the list of postings
     */
    public long writePositions(DataOutputStream writer, List<Integer> positions, long byteAddress) throws IOException {

        //  before iterating through each position we can take the first term position as-is (without calculating gaps)
        int termPosition = positions.get(0);
        for (int i = 0; i < positions.size(); i++) {
            // for every position after the first, re-assign it as a gap
            if (i > 0) {
                int oldPosition = positions.get(i - 1);
                int currPosition = positions.get(i);
                termPosition = (currPosition - oldPosition);
            }

            // write the updated position to the file and increment the byte counter
            writer.writeInt(termPosition);
            byteAddress += 4;
        }
        return byteAddress;
    }

    /**
     * writes the term weights of the provided posting to disk using each of the 4 weighing strategies calculated values
     * returns back a long value to indicate how many total bytes of the active file were used up by the list of postings
     */
    public long writeTermWeights(DataOutputStream writer, Posting p, long byteAddress) throws IOException {
        // obtain a reference to current posting's DocTermWeight
        DocTermWeight termWeight = p.getDocTermWeight();
        // now use the Weight reference to calculate values with each type of weigher
        for (WeighingScheme scheme : WeighingScheme.values()) {
            termWeight.calculate(scheme);
            writer.writeDouble(termWeight.getValue());
            byteAddress += 8; // increment the byteAddress 8 to write each weight as a double
        }
        return byteAddress;
    }

    public void writeDocWeights(List<DocWeight> docWeights) throws IOException {
        // try to make a new .bin file using the mPostingsPath set at construction
        File postingsBin = new File(mPostingsPath);
        // make sure there is no other postings.bin file already in index the directory before trying to write a new one
        if (postingsBin.exists()) {
            postingsBin.delete();
        }

        try {
            // setup output streams for postings.bin and docWeights.bin
            FileOutputStream docWeightsStream = new FileOutputStream(postingsBin);
            DataOutputStream docWeightsOut = new DataOutputStream(docWeightsStream);

            int avgDocLength = 0;

            for (DocWeight w : docWeights) {
                double docLd = w.getValue();
                long docLength = w.getDocLength();
                long byteSize = w.getByteSize();
                ;
                int avgFrequency = w.getAvgTermFrequency();

                // write the per-doc weight data of each docWeight in the order of: docWeight, docLength, byteSize, avgTfTd,
                // starts at byte 0
                docWeightsOut.writeDouble(docLd);  // now at byte 8
                docWeightsOut.writeLong(docLength); // now at byte 16
                docWeightsOut.writeLong(byteSize); // now at byte 24
                docWeightsOut.writeInt(avgFrequency); // now at byte 28
                // next DocWeight starts writing @ byte 28

                avgDocLength += docLength;
            }

            avgDocLength = avgDocLength / docWeights.size();
            // as soon as the avgDocLength is caclulated for the first time assign it to the global corpus
            activeCorpus.setAvgDocLength(avgDocLength);
            docWeightsOut.writeInt(avgDocLength);// will be written at byte location: (docWeightsOut.length() - 4)
        } catch (Exception ex) {
            System.out.println("Failed to write doc weights to disk for document:");
        }
    }

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
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        return location;
    }

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
        } catch (Exception ex) {
            System.out.println("Error reading from on disk index: No on-disk B+ tree was found in the provided index directory");
            return null;
        }
        return results;
    }

    public List<Posting> readPostingsWithoutPositions(long byteLocation) {
        int termDocFrequency = 0;
        double termDocWeight = 0.0;
        List<Posting> results = new ArrayList<>();

        try (RandomAccessFile reader = new RandomAccessFile(mPostingsPath, "r")) {

            // start by seeking to the byte location of the term - this is where all of its postings data begins
            // now we can easily find any other postings data we need by incrementing the necessary amount of bytes from that initial position
            reader.seek(byteLocation);

            // first get tf(d) (the amount of docs the term occurs in) which should be at the exact byte indicated by the termLocation
            int dFt = reader.readInt();

            // before iterating through all the term's docs, initialize the first docId to 0
            int currentDocId = 0;

            // now we can use that tf(d) value find the rest of the term's data
            for (int i = 0; i < dFt; i++) {
                // for the first value only, read the doc ID as-is with no gaps
                if (i == 0) {
                    currentDocId = reader.readInt();
                }
                // otherwise, read the next docId and use it to increment the gap between itself and the previous docId
                else {
                    currentDocId += reader.readInt();
                }
                // use the ordinal of the active weighing scheme to find out how many bytes(if any) to jump before reading the correct weight type
                int weightBytes = activeWeighingScheme.ordinal();
                reader.skipBytes(weightBytes);
                // read w(d,t)
                double currentWeight = reader.readDouble();

                // read tf(t,d)
                int currTermFrequency = reader.readInt();

                // now jump ahead by the 4 * tf(t,d) to get to the next docId without reading each term position
                reader.skipBytes(4 * currTermFrequency);

                // instantiate a generic posting and set its data with the values read from file
                Posting currPosting = new Posting(currentDocId);
                currPosting.setDocTermWeight(currentWeight);
                currPosting.setTermFrequency(currTermFrequency);
                results.add(currPosting);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return results;
    }

    public List<Posting> readPostings(long byteLocation) {
        int termDocFrequency = 0;
        double termDocWeight = 0.0;
        List<Posting> results = new ArrayList<>();

        try (RandomAccessFile reader = new RandomAccessFile(mPostingsPath, "r")) {

            // start by seeking to the byte location of the term - this is where all of its postings data begins
            // now we can easily find any other postings data we need by incrementing the necessary amount of bytes from that initial position
            reader.seek(byteLocation);

            // first get tf(d) (the amount of docs the term occurs in) which should be at the exact byte indicated by the termLocation
            int dFt = reader.readInt();

            // before iterating through all the term's docs, initialize the first docId to 0
            int currentDocId = 0;

            // now we can use that tf(d) value find the rest of the term's data
            for (int i = 0; i < dFt; i++) {
                // for the first value only, read the doc ID as-is with no gaps
                if (i == 0) {
                    currentDocId = reader.readInt();
                }
                // otherwise, read the next docId and use it to increment the gap between itself and the previous docId
                else {
                    currentDocId += reader.readInt();
                }
                // use the ordinal of the active weighing scheme to find out how many bytes(if any) to jump before reading the correct weight type
                int weightBytes = activeWeighingScheme.ordinal();
                reader.skipBytes(weightBytes);
                // read w(d,t)
                double currentWeight = reader.readDouble();

                // read tf(t,d)
                int currTermFrequency = reader.readInt();

                List<Integer> termPositions = new ArrayList<>();

                int currTermPosition = 0;

                for (int j = 0; j < currTermFrequency; j++) {
                    if (j == 0) {
                        currTermPosition = reader.readInt();
                    } else {
                        currTermPosition += reader.readInt();
                    }
                    termPositions.add(currTermPosition);
                }

                // instantiate a generic posting and set its data with the values read from file
                Posting currPosting = new Posting(currentDocId, termPositions);
                currPosting.setDocTermWeight(currentWeight);
                currPosting.setTermFrequency(currTermFrequency);
                results.add(currPosting);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return results;
    }

    public double readDocWeight(long byteLocation) {
        //HashMap<Integer, Double> results = new HashMap<>();
        double weight = 0.0;

        try (RandomAccessFile reader = new RandomAccessFile(mDocWeightsPath, "r")) {
            reader.seek(byteLocation);
            weight = reader.readDouble();
        } catch (Exception ex) {
            System.out.println("Failed to read the doc weights from disk. '");
            ex.printStackTrace();
        }
        return weight;
    }

    public double readDocTermWeight(long byteLocation) {
        double weight = 0.0;

        try (RandomAccessFile reader = new RandomAccessFile(mPostingsPath, "r")) {
            reader.seek(byteLocation);
            weight = reader.readDouble();
        } catch (Exception ex) {
            System.out.println("Failed to read the doc weights from disk. '");
            ex.printStackTrace();
        }
        return weight;
    }
}

