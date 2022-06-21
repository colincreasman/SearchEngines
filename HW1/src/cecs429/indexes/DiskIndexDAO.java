package cecs429.indexes;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.mapdb.*;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;


public class DiskIndexDAO implements IndexDAO {
    private PositionalInvertedIndex mPosIndex;
    private String mIndexPath;
    private String mPostingsPath;
    private String mDocWeightsPath;
    private String mDbPath;
    private List<Long> mByteLocations;
    private static DB indexDB;
    private static BTreeMap<String, Long> termMap;
    private HashMap<String, Integer> mTermFrequencies; // maps each term to its tf(t,d) value

    public DiskIndexDAO(String indexDir) {
        mIndexPath = indexDir + "/index";
        mByteLocations = new ArrayList<>();
        mDbPath = mIndexPath + "\\termMap.db";
        mPostingsPath = mIndexPath + "/postings.bin";
        mDocWeightsPath = mIndexPath + "/docWeights.bin";

        // setup needed structures
        indexDB = null;
        termMap = null;

        // create the index dir if it doesn't already exist
        File dir = new File(mIndexPath);
        if (!dir.exists()) {
            dir.mkdir();
        }

        // try to initialize the db as a child file of the greater corpus path
        try {
            //create B+ tree for terms and addresses

            //if (termMap == null || termMap.isEmpty()) {
            File dbFile = new File(mDbPath);
            if (!dbFile.exists()) {
                indexDB = DBMaker.fileDB(mDbPath).make();
                termMap = indexDB.treeMap("map")
                        .keySerializer(Serializer.STRING)
                        .valueSerializer(Serializer.LONG)
                        .counterEnable()
                        .createOrOpen();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
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
        }
        else {
            return false;
        }
    }

    @Override
    public List<Long> writeIndex(Index index, String path) {

        System.out.println("Writing index to disk...");

        try {
            // create the index dir
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdir();
            }

            // create the postings file
            String postingsPath = path + "/postings.bin";

            File postingsBin = new File(postingsPath);
            FileOutputStream fileStream = new FileOutputStream(postingsBin);
            DataOutputStream outStream = new DataOutputStream(fileStream);


            int byteCounter = 0; // keep a counter that increments by 4 bytes everytime the file is written to
            for (String term : index.getVocabulary()) {
                // for every new term, add its byte position by converting the current value of byteCounter to an 8-byte double
                long currLocation = (long) byteCounter;
                mByteLocations.add(currLocation);

                // we also need to write each new term to the RDB map, along with its byte location
                writeTermAndLocation(term, currLocation);

                List<Posting> currPostings = index.getPostings(term);
                int docFrequency = currPostings.size(); // set up df(t) and assign it to the number of postings for the current term
                int docId = currPostings.get(0).getDocumentId(); // for the first doc only, write the docId as-is
                outStream.writeInt(docFrequency); // write df(t) to disk as-is

                // whenever something is written to the file, increment the byteCounter by 4 to account for the 4 bytes used to write the integer
                byteCounter += 4;

                for (int i = 0; i < currPostings.size(); i++) {
                    int termFrequency = currPostings.get(i).getTermPositions().size();// tf(t,d)
                    List<Integer> positions = currPostings.get(i).getTermPositions();

                    // for every posting after the first, re-assign the docId to the difference between itself and the previous docId
                    if (i > 0) {
                        docId -= currPostings.get(i - 1).getDocumentId();
                    }

                    // write the docId and tf(t,d) to the file
                    outStream.writeInt(docId);
                    outStream.writeInt(termFrequency);

                    // for the first positions only, write as-is
                    int currPosition = positions.get(0);

                    // now write the term positions by iterating through them and finding the gaps
                    for (int j = 0; j < positions.size(); j++) {
                        // for every position after the first, re-assign it to the difference between itself and the previous position
                        if (j > 0) {
                            currPosition -= positions.get(j - 1);
                        }
                        // write the updated position to the file
                        outStream.writeInt(currPosition);
                    }
                }
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return mByteLocations;
    }

    @Override
    public void writeTermAndLocation(String term, long bytePosition) {
        try {
            termMap.put(term, bytePosition);
            termMap.close();
        }
        catch (Exception ex) {
            System.out.println("Failed to write the term '" + term + "' with a byte location of " + bytePosition + ". \n");
            ex.printStackTrace();
        }
    }

    // retrieves a single byte location matching the provided term from the RDB on disk
    public long readLocationFromTerm(String term) {
        long location = 0;
        try {
            location = termMap.get(term);
            termMap.close();
        }
        catch (Exception ex) {
            System.out.println("Failed to read the byte location of the term '" + term + "' \n");
            ex.printStackTrace();
        }
        return location;
    }
    @Override
    public String readTermFromLocation(long byteLocation) {
        String term = "";
        try {
            if (termMap.containsValue(byteLocation)) {
                for (Map.Entry<String, Long> entry : termMap.entrySet()) {
                    if (Objects.equals(entry.getValue(), byteLocation)) {
                        term = entry.getKey();
                    }
                }
            }
        }
        catch (Exception ex) {
            System.out.println("Failed to read the term at the byte location '" + byteLocation + "' \n");
            ex.printStackTrace();
        }
        //termMap.close();
        return term;
    }

    @Override
    public List<Posting> readPostings(String term) {
        return null;
    }

    @Override
    public int readTermDocFrequency(String term, int docId) {
        int termDocFrequency = 0;

        try (RandomAccessFile reader = new RandomAccessFile(mPostingsPath, "r")) {
            long termLocation = readLocationFromTerm(term);
            reader.seek((termLocation));
            int totalDocs = reader.readInt(); // get tf(d) the amount of docs the term occurs in
            // use tf(d) to iterate through all the documents the term appears in

            int currentDoc = 0;
            for (int i = 0; i < totalDocs; i++) {
                // read the next docId and use it to increment the previous docId
                currentDoc += reader.readInt();
                // read the number of term positions in this doc - tf(t,d)
                int docFrequency = reader.readInt();

                // check each doc to find a match for the given docId
                if(currentDoc == docId) {
                    // if a match is found, use its docFrequency for the return and break
                    termDocFrequency = docFrequency;
                    break;
                }
            }
        }

        catch (IOException ex) {
            ex.printStackTrace();
        }
        return termDocFrequency;
    }

    /**
     * reads only the set of all docId's in the given term's postings
     *
     * @param index
     * @param term
     * @return
     */
    @Override
    public List<Integer> readDocIds(Index index, String term) {
        return null;
    }

    @Override
    public long readDocWeight(int docId) {
        return 0;
    }

}
