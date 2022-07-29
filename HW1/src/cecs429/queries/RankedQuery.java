package cecs429.queries;

import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.TokenProcessor;
import cecs429.weights.DocTermWeight;
import cecs429.weights.DocWeight;
import cecs429.weights.QueryTermWeight;

import static edu.csulb.Driver.ActiveConfiguration.*;

import java.util.*;

public class RankedQuery implements QueryComponent {
    private int mKterms = 10;
    private List<String> mTerms;
    private List<QueryTermWeight> mQueryWeights;
    private List<String> mProcessedTerms;
    private HashMap<Integer, Double> mDocWeights;
    private PriorityQueue<DocWeight> mRankedDocs;
    private HashMap<Integer, Posting> mPostingsMap;



    public RankedQuery(List<String> terms) {
        mTerms = terms;
        mProcessedTerms = new ArrayList<>();
        mQueryWeights = new ArrayList<>();
    }

    /**
     * Retrieves a list of postings for the query component, using an Index as the source.
     *
     * @param processor
     * @param index
     */
    @Override
    public List<Posting> getPostingsWithoutPositions(TokenProcessor processor, Index index) {

        mRankedDocs = new PriorityQueue<>();
        mPostingsMap = new HashMap<>();

        // process query terms with the passed in processor before ranking
        for (String term : mTerms) {
            mProcessedTerms.addAll(processor.processToken(term));
        }

        // loop through each processed term in the query (mTerms)
        for (String term : mProcessedTerms) {

            List<Posting> postings = index.getPostingsWithoutPositions(term);
            int dFt = postings.size();

            QueryTermWeight wQt = new QueryTermWeight(term, dFt);
            mQueryWeights.add(wQt);

            for (int i = 0; i < postings.size(); i++) {
                Posting currPosting = postings.get(i);
                DocTermWeight wDt = currPosting.getDocTermWeight();
                double increment = wDt.getValue() * wQt.getValue();
                int currDocId = currPosting.getDocumentId();

                // obtain a reference to the current posting's DocWeight so we can statically update its accumulator
                DocWeight currDocWeight = currPosting.getDocWeight();

                if (mPostingsMap.isEmpty()) {
                    currDocWeight.increaseAccumulator(increment);
                    mPostingsMap.put(currDocId, currPosting);
                }

                // check if the current head (should have the max docId at this time)
                else {
                    // try to update the current acc in the map
                    try {
                        // if an existing posting is already in the map, increase the accumlator in its docWeight
                        Posting existing = mPostingsMap.get(currDocId);
                        existing.getDocWeight().increaseAccumulator(increment);

                    } catch (NullPointerException ex) {
                        // if the key is null in the rank map, there has yet to be added posting for this docId
                        currDocWeight.increaseAccumulator(increment);
                        mPostingsMap.put(currDocId, currPosting);
                    }
                }
            }
        }

        // now go through the final map of accumulators and divide each one by the Ld value for its doc
        List<Posting> results = new ArrayList<>();

        for (int docId : mPostingsMap.keySet()) {
            DocWeight currDocWeight = mPostingsMap.get(docId).getDocWeight();

            double currLd = currDocWeight.readValue(); // Ld
            double currAd = currDocWeight.getAccumulator();

            if (currAd != 0) {
                double finalAcc = currAd / currLd;
                currDocWeight.setAccumulator(finalAcc);
            }

            mRankedDocs.add(currDocWeight);

            // continually remove the smallest element for every added term after the K_TERMS limit is reached
            if (mRankedDocs.size() > mKterms) {
                mRankedDocs.poll();
            }
        }

        // now that all doc rankings have been calculated and the top K docweights have been polled, use their docId's to reference their original posting  from the PostingsMap
        while (!mRankedDocs.isEmpty()) {
            DocWeight finalDocWeight = mRankedDocs.poll();
            Posting finalPosting = mPostingsMap.get((finalDocWeight.getDocId()));
            // re-update the doc weight to ensure it preserves the final calculated values
            finalPosting.setDocWeight(finalDocWeight);
            results.add(finalPosting);
        }

        return results;
    }

    @Override
    public List<Posting> getPostings(TokenProcessor processor, Index index) {
        List<Posting> results = new ArrayList<>();
        // process query terms with the passed in processor before ranking
        for (String term : mTerms) {
            mProcessedTerms.addAll(processor.processToken(term));
        }

        // loop through each processed term in the query (mTerms)
        for (String term : mProcessedTerms) {
            List<Posting> postings = index.getPostingsWithoutPositions(term);
            results.addAll(postings);
        }
        return results;
    }

    public List<String> getProcessedTerms() {
        return mProcessedTerms;
    }

    public List<QueryTermWeight> getQueryWeights() {
        return mQueryWeights;
    }

    public void setKterms(int k) {
        mKterms = k;
    }

    public String toString() {
        String result = "";
        for (String s : mTerms) {
            result += s + " ";
        }
        return result;
    }
}
