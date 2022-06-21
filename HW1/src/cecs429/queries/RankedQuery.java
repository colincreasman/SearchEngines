package cecs429.queries;

import cecs429.documents.Document;
import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.TokenProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RankedQuery implements QueryComponent {
    private List<String> mTerms;
    private List<String> mProcessedTerms;
    private HashMap<String,List<Double>> mAccumulators;
    private int mCorpusSize;

    public RankedQuery(List<String> terms, int corpusSize) {
        mTerms = terms;
        mProcessedTerms = new ArrayList<>();
        mAccumulators = new HashMap<>();
        mCorpusSize = corpusSize;
    }

    /**
     * Retrieves a list of postings for the query component, using an Index as the source.
     *
     * @param processor
     * @param index
     */
    @Override
    public List<Posting> getPostings(TokenProcessor processor, Index index) {
        return null;
    }

    // calculates the document-term weight [W(d,t)] and query-term weights [W(q,t)] of the provided term
    // uses the product of both weights to return a value that can be used to increment the accumulator for this document
    public double getAccumulatorIncrement(Document doc, String term) {

        return 0.0;
    }
}
