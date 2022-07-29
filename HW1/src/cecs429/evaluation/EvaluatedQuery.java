package cecs429.evaluation;


import cecs429.indexes.Posting;
import cecs429.queries.QueryComponent;
import cecs429.queries.RankedQuery;
import cecs429.text.AdvancedTokenProcessor;
import cecs429.text.TokenProcessor;

import static edu.csulb.Driver.ActiveConfiguration.activeCorpus;
import static edu.csulb.Driver.ActiveConfiguration.activeIndex;

import java.sql.SQLOutput;
import java.util.*;


// TODO: implement this so that it encapsulates a single RankedQuery object and a matching List<Integer> of relevant docId's
public class EvaluatedQuery {
    private TokenProcessor mProcessor;

    private RankedQuery mQuery;

    private List<Integer> mTotalRelevant; // list of all the relative docs that should have been retrieved for the
    // mQuery (basically just an exact line read from qRel)
    private List<Integer> mTotalRetrieved; // list of all doc names mQuery's getPostings()

    private HashMap<Integer, Boolean> mRetrievedRelevant; // Map of all retrieved docs and a boolean indicating
    // whether it was relevant

    private List<String> mTotal; // list of all doc names in the entire corpus

    public EvaluatedQuery() {}

    public EvaluatedQuery(QueryComponent q, List<String> qRel) {
        mProcessor = new AdvancedTokenProcessor();
        mQuery = (RankedQuery) q;

        mTotalRelevant = new ArrayList<>();
        for (String s : qRel) {
            mTotalRelevant.add(Integer.parseInt(s));
        }
        Collections.sort(mTotalRelevant);
    }

    public List<Integer> getTotalRetrieved(int kTerms) {
        mQuery.setKterms(kTerms);
        List<Posting> postings = mQuery.getPostingsWithoutPositions(mProcessor, activeIndex);
        mTotalRetrieved = new ArrayList<>();

        for (Posting p : postings) {
            int titleInt = Integer.parseInt(activeCorpus.getDocument(p.getDocumentId()).getTitle());
            mTotalRetrieved.add(titleInt);
        }

        Collections.sort(mTotalRetrieved);
        return mTotalRetrieved;
    }

    public List<Integer> getTotalRelevant() {
        return mTotalRelevant;
    }

    public HashMap<Integer, Boolean> getRetrievedRelevant(int kTerms) {
        mTotalRetrieved = getTotalRetrieved(kTerms);
        mRetrievedRelevant = new HashMap<>();

        for (int ret : mTotalRetrieved) {
            if (mTotalRelevant.contains(ret)) {
                mRetrievedRelevant.put(ret, true);
            }
            else {
                mRetrievedRelevant.put(ret, false);
            }
        }


        return mRetrievedRelevant;
    }
    @Override
    public String toString() {
        String results = "";
        Map<Integer, Boolean> sorted = new TreeMap<>(mRetrievedRelevant);

        try {
            for (int id : sorted.keySet()) {
                results += "\n Retrieved Document: " + id + "    ||     Relevance: " + mRetrievedRelevant.get(id);
            }
        }

        catch (Exception ex) {
            System.out.println("Error: Ranked retrieval results have not been retrieved yet");
        }

        return results;
    }
}