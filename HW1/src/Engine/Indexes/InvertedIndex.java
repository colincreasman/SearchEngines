package Engine.Indexes;

import java.util.*;

public class InvertedIndex implements Index {
    // use a HashMap that maps each term to a HashSet of integers representing the document ID's it is found in
    // HashSet<Integer> is used instead of List<Integer> to ensure there are no duplicate postings
    private final HashMap<String, HashSet<Integer>> mMatrix;
    private final List<String> mVocabulary;
  //  private int mCorpusSize;


    /**
     * Constructs an empty Inverted Index  with given vocabulary set and corpus size.
     * @param vocabulary a collection of all terms in the corpus vocabulary.
     */
    public InvertedIndex(Collection<String> vocabulary) {
        mVocabulary = new ArrayList<String>();
        mVocabulary.addAll(vocabulary);
        mMatrix = new HashMap<>();

        Collections.sort(mVocabulary);
    }

    public void addTerm(String term, int documentId) {
        // try to get the current postings list for the term
        HashSet<Integer> postingsList = mMatrix.get(term);
        // if there is no current postings list for this term yet, make a new one with the docId
        if (postingsList == null) {
            postingsList = new HashSet<Integer>();
            postingsList.add(documentId);

            // for debugging
            //System.out.println("A new postings list has been made for the term '" + term + "' starting with the document Id: " + documentId);
        }
        // otherwise, the term must already have a postings list
        else {
            // try to add the documentId to the postings List
            try {
                postingsList.add(documentId);
            }
            // if the documentId can't be added, it must already be present in the postings list for this term
            catch (Exception ex) {
                System.out.println("The postings list for the term: " + term + " already contains the document ID: " + documentId);
            }
        }
        // finally, map the newly updated postings list to the term in the original matrix
        mMatrix.put(term, postingsList);
    }

    @Override
    public List<Posting> getPostings(String term) {
        List<Posting> results = new ArrayList<>();
        // try to get the current postings list for the term
        HashSet<Integer> postingsList = mMatrix.get(term);
        // if the postings list is null,
        if (postingsList == null) {
            System.out.println("There are no documents in the corpus that contain the term '" + term + "'.");
        }
        // create a Posting from each docId in the postings list and add it to the results
        for (int docId : postingsList) {
            Posting currentPosting = new Posting(docId);
            results.add(currentPosting);
        }
        return results;
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

    @Override
    public List<String> getVocabulary() {
        return Collections.unmodifiableList(mVocabulary);
    }

    @Override
    public void addTerm() {
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
    public void addTerm(String term, int id, int position) {

    }
}









