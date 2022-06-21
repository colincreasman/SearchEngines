package cecs429.indexes;

import cecs429.text.DiskIndexReader;
import cecs429.text.DiskIndexWriter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DiskPositionalIndex implements Index {
    private List<String> mVocabulary;
    private Index mIndex;
    private IndexDAO mIndexDAO;
    private String mPath;
    private HashMap<String, Integer> mTermFrequencies; // maps each term to its tf(t,d) value
    private  HashMap<Integer, List<Double>> mDocWeights;
    private List<Double> mByteLocations;

    /**
     * basic constructor for a new index with no current on-disk data
     * constructor uses the provided Index object  will call the necessary methods to build and write a new one to disk
     */
    public DiskPositionalIndex(Index posIndex, DiskIndexDAO dao) {
        mIndex = posIndex;
        mVocabulary = mIndex.getVocabulary();
        mIndexDAO = dao;
        mDocWeights = new HashMap<>();
        System.out.println("Writing index postings to disk...");

        mByteLocations = dao.writeIndex(posIndex, dao.getIndexPath());
        //TODO: add some method that calculates weights while indexing

//        // if the on-disk index doesn't currently exist, it must be initialized  and written before loading
//        if (!dao.checkIndex() || posIndex == null) {
//            //TODO: Implement these methods and uncomment
//            initialize();
//        }
//        load();
    }

    /**
     * Multi-arg constructor for indexes that are already written to disk
     * Takes in 2 Path args specifying the existing "postings.bin" and "docWeights.bin" files to read from
     * Optimizes index construction by reading existing data from the .bin files without having to reconstruct and write a new PositionalInvertedIndex
     */
//    public DiskPositionalIndex(Path postings, Path docWeights) {
//        mVocabulary = new ArrayList<String>();
//        mIndex = new HashMap<>();
//        mDocWeights = new HashMap<>();
//
//        //TODO: Implement these methods and uncomment
//        load(postings, docWeights);
//    }

//    /**
//     * starts by initializing a PositionalInvertedIndex and writing the index to disk,
//     * then loads field data by reading the values from the newly written index files
//     */
//    public void initialize() {
//        // start by creating the necessary index files
//        mIndexDAO.createIndex();
//
//        // iterate through all the terms in the vocab and write their postings to disk  open up the postings.bin file
//        for (String term : mVocabulary) {
//            List<Posting> currPostings =  mIndex.getPostings(term);
//            mIndexDAO.writePostings(mIndex, term, currPostings);
//
//        }
//
//        DiskIndexReader reader = new DiskIndexReader();
//    }
//
//    /**
//     * loads all index data to the current instance's fields by reading them from the existing .bin files
//     */
//    public void load() {
//    }

    /**
     * Retrieves a list of Postings of documents that contain the given term.
     *
     * @param term
     */
    @Override
    public List<Posting> getPostings(String term) {
        return null;
    }

    // queries the persistent RDB to retrieve only the docId's from given term's postings as well as each doc's corresponding tf(t,d) value
    // returns results in a HashMap linking each docId to its tf(t,d)
    //TODO: set up RDB reading here
    @Override
    public HashMap<Integer, List<Integer>> getPostingsWithoutPositions(String term) {
        HashMap<Integer, List<Integer>> results = new HashMap<>();

        // get the list of docIds for the term
        List<Integer> docs = mIndexDAO.readDocIds(mIndex, term);

        // get the list of tf(t,d) values for each doc
        for (Integer d : docs) {
            List<Integer> frequencies = mIndexDAO.readTermDocFrequencies(mIndex, term, d);
            // add components into final hashmap
            results.put(d, frequencies);
        }
        // for a ranked retrieval, we only need the tf(t,d) values for all the documents containing the term
        return results;
    }

    public List<Posting> getPostings(Index index, String term) {
        return null;
    }

    /**
     * A (sorted) list of all terms in the index vocabulary.
     */
    @Override
    public List<String> getVocabulary() {
        return null;
    }

    @Override
    public void addTerm() {

    }

    @Override
    public void addTerm(String term, int id, int position) {

    }

    @Override
    public void addTerm(String term, int id) {

    }

    @Override
    public String viewTermPostings(String term) {
        return null;
    }
}
