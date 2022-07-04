package Engine.Indexes;

import Engine.Documents.Document;
import Engine.Documents.DocumentCorpus;
import Engine.Text.AdvancedTokenProcessor;
import Engine.Text.EnglishTokenStream;
import static App.Driver.ActiveConfiguration.*;

import Engine.Weights.DocTermWeight;
import Engine.Weights.DocWeight;

import java.util.*;

public class DiskPositionalIndex implements Index {
    private static List<String> mVocabulary;
    private static Index mIndexInMemory;
    private static String mPath;
    private static List<DocWeight> mDocWeights;
    private static List<Long> mByteLocations;
    private static HashMap<String, Long> mTermLocations;
    private static HashMap<String, List<Posting>> mBasicTermPostings;

    public DiskPositionalIndex(DocumentCorpus corpus) {
        mPath = corpus.getPath();
        // initialize the in-memory positional index WITHOUT changing the ActiveConfiguration's in-memory index which is the current DiskIndex being built
        mVocabulary = new ArrayList<>();
        mIndexInMemory = new PositionalInvertedIndex(mVocabulary);
        mDocWeights = new ArrayList<>();
        mByteLocations = new ArrayList<>();
        mTermLocations = new HashMap<>();
    }

    public void initializeInMemoryIndex() {
        System.out.println("Initializing the in-memory index ...");
        // start timer
        long start = System.currentTimeMillis();
        AdvancedTokenProcessor processor = new AdvancedTokenProcessor();

        for (Document d : activeCorpus.getDocuments()) {
            EnglishTokenStream stream = new EnglishTokenStream(d.getContent());
            Iterable<String> tokens = stream.getTokens();
            HashMap<String, Integer> termCounts = new HashMap<>();

            // store a list of w(d,t) references for all the terms in the current doc
            List<DocTermWeight> wDts = new ArrayList<>();

            // initialize a counter to keep track of the term positions of tokens found within each document
            int tokenPosition = 0;
            for (String token : tokens) {
                // process each token into a term(s)
                List<String> terms = processor.processToken(token);

                // iterate through each term while keeping a running total of all the times it is found in the current doc
                for (String term : terms) {
//                    int currentTermCount = 1; // freq counter for the current tf(t,d)
//                    // now check if there's already an existing count for this term in the results hashmap
//                    try {
//                        // if found, reassign the count to whatever is currently in the results
//                        currentTermCount = termCounts.get(term);
//                        int updatedTermCount = currentTermCount + 1;
//                        // now replace the original count in the results map with the updated count
//                        termCounts.replace(term, currentTermCount, updatedTermCount);
//                    }
//                    // catch null values to still count terms that haven't been added to the results map yet
//                    catch (NullPointerException ex) {
//                        // since this term hasn't been counted yet, put it into the hashmap with the unmodified val of currentTermCount (should still be 1)
//                        termCounts.put(term, currentTermCount);
//                    }
//                    // after handling all the term-doc data, we still need to add each term into the index
//                    finally {

                    // whenever a new term is added to the index, a DocTermWeight is automatically created (or updated, if one already exists for this term-doc combo) by the Posting class
                    mIndexInMemory.addTerm(term, d.getId(), tokenPosition);

                    // to mitigate weight calculations, this same DocTermWeight reference is added to the list of termWeights that will eventually compose the current DocWeight
                    DocTermWeight termWeight = mIndexInMemory.getPostings(term).get(d.getId()).getDocTermWeight();
                    wDts.add(termWeight);
                    }
                // only increment the position when moving on to a new token; if normalizing a token produces more than one term, they will all be posted at the same position
                tokenPosition += 1;
            }
//            for (String term : termCounts.keySet()) {
//                = new DocTermWeight(d, termCounts.get(term));
//                wDts.add(w);
//            }
            // now use the list of termWeight references to compose a DocWeight instance for the current doc and add it to the list for all DocWeights
            DocWeight docWeight = new DocWeight(d, wDts);
            docWeight.setDocLength(tokenPosition);
            mDocWeights.add(docWeight);
        }


        long stop = System.currentTimeMillis();
        long elapsedSeconds = (long) ((stop - start) / 1000.0);
        System.out.println("Initialized index in approximately " + elapsedSeconds + " seconds.");

        // now write that index to disk and save the returned byte positions into the static object field for them
        mByteLocations = indexDao.writeIndex(this, mPath);
        //indexDao.writeDocWeights(mDocWeights);
        mVocabulary = mIndexInMemory.getVocabulary();
    }

    // gets the index's vocabulary, term locations, and doc weights by reading them from the existing on-disk index data
    public void load() {
        //List<String> unsorted = indexDao.readVocabulary();
        System.out.println("Loading index data from disk...");
        //  HashMap<Integer, Double> docWeights = new HashMap<>();
        try {
            //mDocWeights = indexDao.readDocWeights();

           // mTermLocations = indexDao.readTermLocations();

//            mVocabulary.addAll(mTermLocations.keySet());
//            mByteLocations.addAll(mTermLocations.values());
//            System.out.println("Finished loading byte locations from B+ tree.");

            for (Document d : activeCorpus.getDocuments()) {
                // Tokenize the document's content by constructing an EnglishTokenStream around the document's content.
                EnglishTokenStream stream = new EnglishTokenStream(d.getContent());
            }

           // Iterable<Document> documents = activeCorpus.getDocuments();

        }
        catch (NullPointerException ex) {
            System.out.println("Failed to load vocabulary index because the DiskIndexDao could not find any B+ tree files in the given corpus directory. ");
        }
    }

    /**
     * Retrieves a list of Postings (with positions data) of documents that contain the given term.
     *
     * @param term
     */
    @Override
    public List<Posting> getPostings(String term) {
        List<Posting> results = new ArrayList<>();

//        if (mVocabulary == null) {
//            System.out.println("Could not retrieve postings because  no vocabulary data has been loaded for the current index. \n Loading vocabulary from disk now...");
//
//        }

        long byteLocation = indexDao.readByteLocation(term);

        return indexDao.readPostings(byteLocation);
    }

    // queries the persistent RDB to retrieve only the docId's from given term's postings as well as each doc's corresponding tf(t,d) value
    // returns results in a HashMap linking each docId to its tf(t,d)
    //TODO: set up RDB reading here
    @Override
    public List<Posting> getPostingsWithoutPositions(String term) {
        List<Posting> results = new ArrayList<>();

        long byteLocation = indexDao.readByteLocation(term);

        return indexDao.readPostingsWithoutPositions(byteLocation);
    }

    /**
     * A (sorted) list of all terms in the index vocabulary.
     */
    @Override
    public List<String> getVocabulary() {
        if (mVocabulary == null) {
            mVocabulary = indexDao.readVocabulary();
        }
        return mVocabulary;
    }

    @Override
    public void addTerm() {

    }

    @Override
    public void addTerm(String term, int id, int position) {

    }

    @Override
    public void addTerm(String term, int id) {

    }

    @Override
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
}
