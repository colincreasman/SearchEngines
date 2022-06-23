package cecs429.queries;

import cecs429.documents.Document;
import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.TokenProcessor;
import static edu.csulb.Driver.ActiveConfiguration.*;

import java.util.*;

public class RankedQuery implements QueryComponent {
    private final int K_TERMS = 10;
    private List<String> mTerms;
    private List<String> mProcessedTerms;
    private HashMap<Integer, Double> mDocWeights;
    private PriorityQueue<Posting> mRankedPostings;
    private HashMap<Integer, Double> mRankMap;

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
    public List<Posting> getPostings(TokenProcessor processor, Index index) {

        Comparator<Posting> compareByAccumulator = Comparator.comparingInt(Posting::getDocumentId);
        //  Comparator<Posting> comparByDocId = Comparator.comparingDouble(Posting::getAccumulator);

        // initialize the queue to sort by accumulators
        mRankedPostings = new PriorityQueue<>(compareByAccumulator);
        mRankMap = new HashMap<>();

        // process query terms with the passed in processor before ranking
        for (String term : mTerms) {
            mProcessedTerms.addAll(processor.processToken(term));
        }

        // loop through each processed term in the query (mTerms)
        for (String term : mProcessedTerms) {

            List<Posting> postings = index.getPostingsWithoutPositions(term);
            Posting currPosting = postings.get(0);

            for (int i = 0; i < postings.size(); i++) {
                // retrieve the necessary variables from the current posting
                int docFrequency = postings.size(); // dft
                double fraction = (double) mCorpusSize / docFrequency;
                double queryTermWeight = calculateTermWeight(docFrequency); // w(q,t)
                double docTermWeight = currPosting.getTermWeight();
                double increment = queryTermWeight * docTermWeight;
                double currAcc = 0;
                int currDoc = currPosting.getDocumentId();

                if (mRankMap.isEmpty()) {
                    currPosting.increaseAccumulator(increment);
                    mRankMap.put(currDoc, currPosting.getAccumulator());
                    //       mRankedPostings.add(currPosting);
                }

                // check if the current head (should have the max docId at this time)
                else {
                    // try to update the current acc in the map
                    try {
                        double newAcc = mRankMap.get(currDoc) + increment;
                        mRankMap.replace(currDoc, newAcc);
                        // if the current docId has already been added, we remove it from the accumulators queue and replace it with the updated value
                        // increment the posting's accumulator before adding it back it
                        //    mRankedPostings.remove();
                        currPosting.increaseAccumulator(increment);
                        //  mRankedPostings.add(currPosting);
                    } catch (NullPointerException ex) {
                        // if the key is null in the rank map, there has yet to be added an acc for this docId
                        currPosting.increaseAccumulator(increment);
                        mRankMap.put(currDoc, currPosting.getAccumulator());
                        //mRankedPostings.add(currPosting);
                    }
                }
            }
        }

        // retrieve all doc weights written to disk
        mDocWeights = indexDao.readDocWeights();
        // now go through the final map of accumulators and divide each one by the Ld value for its doc
        for (int docId : mRankMap.keySet()) {
            // defaults acc to 0
            Posting postingRank = new Posting(docId);
            double weight = mDocWeights.get(docId);

            if (mRankMap.get(docId) != 0) {
                double finalRank = mRankMap.get(docId) / weight;
                postingRank.increaseAccumulator(finalRank);
            }

            mRankedPostings.add(postingRank);

            // continually remove the smallest element for every added term after the K_TERMS limit is reached
            if (mRankedPostings.size() > K_TERMS) {
                mRankedPostings.poll();
            }
        }
        List<Posting> results = mRankedPostings.stream().toList();
        return results;
    }




