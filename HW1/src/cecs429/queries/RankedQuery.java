package cecs429.queries;

import cecs429.documents.Document;
import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.TokenProcessor;
import org.checkerframework.checker.units.qual.A;

import static edu.csulb.Driver.ActiveConfiguration.*;

import java.util.*;

public class RankedQuery implements QueryComponent {
    private final int K_TERMS = 10;
    private List<String> mTerms;


    private List<String> mProcessedTerms;
    private HashMap<Integer, Double> mDocWeights;
    private PriorityQueue<Posting> mRankedPostings;
    private HashMap<Integer, Posting> mRankMap;

    private int mCorpusSize = activeCorpus.getCorpusSize();

    public RankedQuery(List<String> terms) {
        mTerms = terms;
        mProcessedTerms = new ArrayList<>();
        // mRankedPostings = new HashMap<>();
    }

    /**
     * Retrieves a list of postings for the query component, using an Index as the source.
     *
     * @param processor
     * @param index
     */
    @Override
    public List<Posting> getPostingsWithoutPositions(TokenProcessor processor, Index index) {

      //  Comparator<Posting> compareByAccumulator = Comparator.comparingDouble(Posting::getAccumulator);
        //  Comparator<Posting> comparByDocId = Comparator.comparingDouble(Posting::getAccumulator);

        // initialize the queue to sort by accumulators
        mRankedPostings = new PriorityQueue<>();
        mRankMap = new HashMap<>();

        // process query terms with the passed in processor before ranking
        for (String term : mTerms) {
            mProcessedTerms.addAll(processor.processToken(term));
        }

        // loop through each processed term in the query (mTerms)
        for (String term : mProcessedTerms) {

            List<Posting> postings = index.getPostingsWithoutPositions(term);

            for (int i = 0; i < postings.size(); i++) {
                Posting currPosting = postings.get(i);

                // retrieve the necessary variables from the current posting
                int docFrequency = postings.size(); // dft
                double fraction = (double) mCorpusSize / docFrequency;
                double queryTermWeight = Math.log(1 + fraction); // w(q,t)
                currPosting.setQueryTermWeight(queryTermWeight);
                double docTermWeight = currPosting.getDocTermWeight(); // w(d,t)

                double increment = queryTermWeight * docTermWeight;
                int currDoc = currPosting.getDocumentId();

                if (mRankMap.isEmpty()) {
                    currPosting.increaseAccumulator(increment);
                    mRankMap.put(currDoc, currPosting);
                    //       mRankedPostings.add(currPosting);
                }
                // check if the current head (should have the max docId at this time)
                else {
                    // try to update the current acc in the map
                    try {
                        Posting existing = mRankMap.get(currDoc);
                        existing.increaseAccumulator(increment);
                        // if the current docId has already been added, we remove it from the accumulators queue and replace it with the updated value
                        // increment the posting's accumulator before adding it back it
                        //    mRankedPostings.remove();
//                        currPosting.increaseAccumulator(increment);
                        //  mRankedPostings.add(currPosting);
                    } catch (NullPointerException ex) {
                        // if the key is null in the rank map, there has yet to be added an acc for this docId
                        currPosting.increaseAccumulator(increment);
                        mRankMap.put(currDoc, currPosting);
                        //mRankedPostings.add(currPosting);
                    }
                }
            }
        }


        // now go through the final map of accumulators and divide each one by the Ld value for its doc
        for (int docId : mRankMap.keySet()) {
            // defaults acc to 0
            // retrieve all doc weights written to disk
            double docWeight = indexDao.readDocWeight(docId);

            Posting currRank = mRankMap.get(docId);
           // double docWeight = mDocWeights.get(docId); // Ld

            if (mRankMap.get(docId).getAccumulator() != 0) {
                double finalAcc = currRank.getAccumulator() / docWeight;
                currRank.setAccumulator(finalAcc);
                currRank.setDocWeight(docWeight);
            }

            mRankedPostings.add(currRank);
//
//            // continually remove the smallest element for every added term after the K_TERMS limit is reached
            if (mRankedPostings.size() > K_TERMS) {
                mRankedPostings.poll();
            }
        }
        List<Posting> results = new ArrayList<>();

        // add them to the results list in the correct order
        while (!mRankedPostings.isEmpty()) {
            results.add(mRankedPostings.poll());
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
}




