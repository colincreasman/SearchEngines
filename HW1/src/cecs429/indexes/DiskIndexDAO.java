package cecs429.indexes;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DiskIndexDAO implements IndexDAO {
    PositionalInvertedIndex mPosIndex;
    String mIndexPath;
    String mPostingsPath;
    String mDocWeightsPath;
    File mPostingsBin;
    File mDocWeightsBin;
    List<Double> mByteLocations;
    HashMap<String, Integer> mTermFrequencies; // maps each term to its tf(t,d) value

    public DiskIndexDAO(String indexDir) {
        mIndexPath = indexDir + "/index";
        mByteLocations = new ArrayList<>();
        mPostingsPath = mIndexPath + "/index/postings.bin";
        mDocWeightsPath = mIndexPath + "/index/docWeights.bin";
    }

    public DiskIndexDAO(PositionalInvertedIndex index) {
        mPosIndex = index;
        //mIndexPath = indexDir;
        mByteLocations = new ArrayList<>();
    }

    @Override
    public boolean hasExistingIndex() {
        Path dir = Paths.get(mIndexPath).toAbsolutePath();
        Path postings = Paths.get(mPostingsPath).toAbsolutePath();
        Path weights = Paths.get(mDocWeightsPath).toAbsolutePath();

        // only return true if all of the required files already exist
        if (Files.exists(dir) && Files.exists(postings) && Files.exists(weights)) {
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public void createIndex() {
        mPostingsBin = new File(mPostingsPath.toString());
        mDocWeightsBin = new File(mDocWeightsPath.toString());
    }

    @Override
    public List<Double> writeIndex(Index index, String path) {
        try {
            // create the index dir
           // File dir = new File(path);
            // create the postings file
            path += "/postings.bin";

            File postingsBin = new File(path);
            FileOutputStream fileStream = new FileOutputStream(postingsBin);
            DataOutputStream outStream = new DataOutputStream(fileStream);

            int byteCounter = 0; // keep a counter that increments by 4 bytes everytime the file is written to
            for (String term : index.getVocabulary()) {
                // for every new term, add its byte position by converting the current value of byteCounter to an 8-byte double
                mByteLocations.add((double) byteCounter);

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

    /**
     * Reads the raw Postings data from the datastore for a given term
     * then converts the raw data to a list of Postings objects and returns them
     * The conversion of Postings data is handled in each implementation depending on how it stores data
     *
     * @param index
     * @param term
     * @return
     */
    @Override
    public List<Posting> readPostings(Index index, String term) {
        return null;
    }

    /**
     * Reads the raw tf(t,d) data from the persistent data store for a given term & docId
     *
     * @param index
     * @param term
     * @param docId
     * @return the converted tf(t,d) data as a list of doubles
     */
    @Override
    public List<Integer> readTermDocFrequencies(Index index, String term, int docId) {
        return null;
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

    /**
     * reads the raw Ld data from docWeights.bin (or other implemented datastore) for a given docId
     *
     * @param index
     * @param docId
     * @return raw Ld data converted to Double
     */
    @Override
    public Double readDocWeight(Index index, int docId) {
        return null;
    }

    public String getIndexPath() {
        return mIndexPath;
    }
}
