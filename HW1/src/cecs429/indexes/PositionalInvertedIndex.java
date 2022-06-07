package cecs429.indexes;

import java.util.HashMap;
import java.util.List;

public class PositionalInvertedIndex implements Index {
    private final HashMap<String, List<Posting>> mIndex;


    /**
     * Retrieves a list of Postings of documents that contain the given term.
     *
     * @param term
     */
    @Override
    public List<Posting> getPostings(String term) {
        return null;
    }

    /**
     * A (sorted) list of all terms in the index vocabulary.
     */
    @Override
    public List<String> getVocabulary() {
        return null;
    }
}
