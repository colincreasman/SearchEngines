package cecs429.indexes;

import java.util.List;
import java.util.Objects;

/**
 * Data Access interface allows communication persistent indexes for common CRUD operations
 * Can be extended to multiple different types of datastore (files, relationalDB, etc)
 */
public interface IndexDAO {

    // checks for any currently existing instances of the datastore being implemented
    public boolean hasExistingIndex();

    // creates a new instance of the datastore being implemented
    public void createIndex();
    /**
     *  writes data from the provided index to a persistent datastore indicated by the string arg
     *  the actual instantiation of the datastore is handled per implementation depending on how each interprets
     *  returns a list of 8-byte integer values showing the locations of each term within the index
     */
    public List<Double> writeIndex(Index index, String datastore);

    /**
     * Reads the raw Postings data from the datastore for a given term
     * then converts the raw data to a list of Postings objects and returns them
     * The conversion of Postings data is handled in each implementation depending on how it stores data
     * @param term
     * @return
     */
    public List<Posting> readPostings(Index index, String term);

    /**
     * Reads the raw tf(t,d) data from the persistent data store for a given term & docId
     * @param term
     * @param docId
     * @return the converted tf(t,d) data as a list of doubles
     */
    public List<Integer> readTermDocFrequencies(Index index, String term, int docId);

    /**
     * reads only the set of all docId's in the given term's postings
     * @param term
     * @return
     */
    public List<Integer> readDocIds(Index index, String term);
    /**
     * reads the raw Ld data from docWeights.bin (or other implemented datastore) for a given docId
     * @param docId
     * @return raw Ld data converted to Double
     */
    public Double readDocWeight(Index index, int docId);


}
