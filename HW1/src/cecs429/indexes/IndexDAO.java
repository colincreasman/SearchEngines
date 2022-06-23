package cecs429.indexes;

import org.mapdb.BTreeMap;

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


    void writeTermLocations(String term, long location);


    List<String> readVocabulary();


    List<Long> readTermLocations();

    /**
     * Reads the raw tf(t,d) data from the persistent data store for a given term & docId
     *
     * @param term
     * @param docId
     * @return the converted tf(t,d) data as a list of doubles
     */
    int readTermDocFrequency(String term, int docId);

    /**
     * reads only the set of all docId's in the given term's postings
     *
     * @param term
     * @return
     */
    List<Integer> readDocIds(Index index, String term);


    /**
     * reads the raw Ld data from docWeights.bin (or other implemented datastore) for a given docId
     *
     * @param docId
     * @return raw Ld data converted to Double
     */
    long readDocWeight(int docId);
}
