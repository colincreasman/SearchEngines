package cecs429.indexes;

import java.util.*;

public class PositionalInvertedIndex implements Index {
    private final HashMap<String, List<Posting>> mIndex;
    private final List<String> mVocabulary;

    public PositionalInvertedIndex(Collection<String> vocabulary) {
        mVocabulary = new ArrayList<String>();
        mVocabulary.addAll(vocabulary);
        Collections.sort(mVocabulary);
        mIndex = new HashMap<>();
    }
    /**
     * Adds a term to the index using the document it occurs in
     * and the list of integer positions where it occurs
     *
     * @param term
     * @param documentId
     * @param termPosition
     */
    public void addTerm(String term, int documentId, int termPosition) {
        // first check if the term is already in the index by trying to access its postings in the index
        List<Posting> postingsInIndex = mIndex.get(term);

        // if the value is not null, the term must already be in the index
        if (postingsInIndex != null) {
            // now check if the term already has a Posting for this specific docId
            // find the last posting for the term in the HashMap index
            Posting lastPosting = postingsInIndex.get(postingsInIndex.size() - 1);

            // since the postings lists are always sorted, if the current docId is in the term's postings list it would be the last item added to the list
            if (lastPosting.getDocumentId() == documentId) {
                // if true, then the term's postings list already contains a posting for this doc, so we add the new termPosition to the end of the postings list for the term
                lastPosting.addTermPosition(termPosition);
            } else {
                // if false, then the posting list for this term exists but does not yet contain a posting for this doc
                Posting newPosting = new Posting(documentId, termPosition);
                // add a new posting with the docId and termPosition to the HashMap index
                postingsInIndex.add(newPosting);

                // after adding a new term, re-sort the list of postings by their documentId's
                ///postingsInIndex.sort(Comparator.comparingInt(Posting::getDocumentId));
            }
        }
        // if the val for this term is null, the term has not been added to the HashMap index yet
        else {
            // create a postings list and add a new posting with the docId and termPosition to it
            List<Posting> newPostingsList = new ArrayList<>();
            Posting newPosting = new Posting(documentId, termPosition);
            newPostingsList.add(newPosting);
            // add the new term and new postingsList to the HashMap index
            mIndex.put(term, newPostingsList);
        }
    }

    @Override
    public void addTerm(String term, int id) {
    }

    /**
     * Retrieves a list of Postings of documents that contain the given term.
     *
     * @param term
     */
    @Override
    public List<Posting> getPostings(String term) {
        List<Posting> postings = mIndex.get(term);

        return postings;
    }

    /**
     * Retrieves a list of Postings of documents that contain the given term.
     *
     * @param term
     * @return
     */
    @Override
    public List<Posting> getPostingsWithoutPositions(String term) {
        return null;
    }

    /**
     * A (sorted) list of all terms in the index vocabulary.
     */
    @Override
    public List<String> getVocabulary() {
        List<String> sortedVocab = new ArrayList<>();
        sortedVocab.addAll(mIndex.keySet());
        Collections.sort(sortedVocab);
        return sortedVocab;
    }

    @Override
    // Essentially just a toString() for an individual term and its posting(s) from the index
    public String viewTermPostings(String term) {
        String postingString = "\"" + term + "\":" + " {";
        //System.out.println(term);

        for (Posting p : getPostings(term)) {
            postingString += (p.toString() + ", ");
        }

        // remove comma from final term
        postingString = postingString.substring(0, postingString.length() - 2);
        postingString += "}";
        return postingString;
    }


    @Override
    public void addTerm() {
    }
}
