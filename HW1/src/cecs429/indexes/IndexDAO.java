package cecs429.indexes;

import org.mapdb.BTreeMap;

import java.util.HashMap;
import java.util.List;

/**
 * Data Access interface allows communication persistent indexes for common CRUD operations
 * Can be extended to multiple different types of datastore (files, relationalDB, etc)
 */
public interface IndexDAO {

    // checks for any currently existing instances of the datastore being implemented
    boolean hasExistingIndex();

    /**
     * writes data from the provided index to a persistent datastore indicated by the string arg
     * the actual instantiation of the datastore is handled per implementation depending on how each interprets
     * returns a list of 8-byte integer values showing the locations of each term within the index
     *
     * @return
     */
    List<Long> writeIndex(Index index, String datastore);

    void writeTermLocation(String term, long bytePosition);

    void writeDocWeight(int docId, double weight);

    HashMap<Integer, Double> readDocWeights();

    List<String> readVocabulary();



    HashMap<String, Long> readTermLocations();

    // reads select postings data from the disk to return a list of postings,
    // each constructed with vals for its docId and docWeights
    // all other postings data will be handled in the getPostings() method calling this one
    // initialize necessary vars for results and structs to read from the termsDB
    List<Posting> readPostings(long byteLocation);
}
