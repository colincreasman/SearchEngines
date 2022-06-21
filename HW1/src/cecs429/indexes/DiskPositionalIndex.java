package cecs429.indexes;

import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.text.AdvancedTokenProcessor;
import cecs429.text.EnglishTokenStream;

import java.util.*;

public class DiskPositionalIndex implements Index {
    private static List<String> mVocabulary;
    private static Index mIndex;
    private static IndexDAO mIndexDAO;
    private static String mPath;
    private static HashMap<Integer, Long> mDocWeights;
    private static List<Long> mByteLocations;
    private static DocumentCorpus mCorpus;


    public DiskPositionalIndex(DocumentCorpus corpus) {
        mCorpus = corpus;
        mPath = corpus.getPath();
        mIndexDAO = new DiskIndexDAO(mPath);

    }

    // builds an in-memory positional inverted index and calculates tf(t,d) data while processing the tokens of each doc
    public void initializeInMemoryIndex(DocumentCorpus corpus) {
        //
        List<String> vocab = new ArrayList<>();
        mIndex = new PositionalInvertedIndex(vocab);
        System.out.println("Indexing corpus in memory...");
        // start timer
        long start = System.currentTimeMillis();

        AdvancedTokenProcessor processor = new AdvancedTokenProcessor();

        for (Document d : corpus.getDocuments()) {
            // get doc Content
            EnglishTokenStream stream = new EnglishTokenStream(d.getContent());
            // initialize a counter to keep track of the term positions of tokens found within each document
            int position = 0;
            // retrieve tokens from the doc as an iterable
            Iterable<String> tokens = stream.getTokens();

            // setup hashmap to store all the terms and found in the current doc and their frequencies within the doc
            HashMap<String, Integer> termFrequencies = new HashMap<>(); // maps docId -> tf(t,d)

            for (String token : tokens) {
                // process each token into a term(s)
                List<String> terms = processor.processToken(token);

                for (String term : terms) {
                    // for each normalized term, initialize its frequency counter to 1 before trying to retrieve it from the map
                    int termCounter = 1;

                    try {
                        termCounter = termFrequencies.get(term);
                        // if the exception isn't thrown, this term must already be in the map
                        int updatedCounter = termCounter + 1;
                        // update the term's frequency to increment by 1
                        termFrequencies.replace(term, termCounter, updatedCounter);
                    }

                    // catch null values to find terms that havent been counted yet
                    catch (NullPointerException ex) {
                        // since this term hasn't been counted yet, put it into the hashmap with an initial frequency of 1
                        termFrequencies.put(term, termCounter);
                    }

                    finally {
                        mIndex.addTerm(term, d.getId(), position);
                    }
                }

                //TODO: update this in main too
                // only increment the position when moving on to a new token
                // if normalizing a token produces more than one term, they will all be posted at the same position
                position += 1;

                // use the final hashmap of tf(t,d) vals for this doc to find its normalized weight and add it to the static map of docWeights
                long docWeight = getNormalizedWeight(termFrequencies);
                mDocWeights.put(d.getId(), docWeight);

            }
        }
        long stop = System.currentTimeMillis();
        long elapsedSeconds = (long) ((stop - start) / 1000.0);
        System.out.println("Finished indexing the in-memory positional index in approximately " + elapsedSeconds + " seconds.");
        // now write that index to disk
        mByteLocations = mIndexDAO.writeIndex(mIndex, mPath);
    }

    // builds the index's vocabulary by reading its terms from the existing on-disk index data
    public void loadVocabulary(DocumentCorpus corpus) {
        mVocabulary = new ArrayList<>();
        // easiest way to load all terms in the vocabulary is by reading from the relational DB/Tree map that stores terms and their byte positions as an iterator
        for (Long l : mByteLocations) {
            try {
                String currTerm = mIndexDAO.readTermFromLocation(l);
                mVocabulary.add(currTerm);
            }
            catch (Exception ex) {
                System.out.println("Failed to read the term at byte location: " + l +"\n");
                ex.printStackTrace();
            }
        }
    }


    // uses the hashmap of terms and their tf(t,d) values to calculate the Euclidean Normalized document weights of of the given doc and return them all as a list of Doubles
    public long getNormalizedWeight(HashMap<String, Integer> frequencies) {
        long finalWeight;
        long weightSums = 0;

        for (String term : frequencies.keySet()) {
            // cast to string then convert back to double to prevent truncating
            String ogLog = String.valueOf(Math.log(frequencies.get(term)));
            long dubLog = Long.parseLong(ogLog);
            long squareLog = (long) Math.pow(dubLog, 2.0);
            long basicWeight = 1 + squareLog; //w(t,d) = 1 + ln(tf(t,d)
            weightSums += basicWeight;
        }

        finalWeight = (long) Math.sqrt(weightSums);
        return finalWeight;
    }

    /**
     * Retrieves a list of Postings (with positions data) of documents that contain the given term.
     *
     * @param term
     */
    @Override
    public List<Posting> getPostings(String term) {
        if (mByteLocations == null) {
            System.out.println("Cannot retrieve postings because no on-disk index data was found. ");
            return null;
        }
        // if the list of byte positions is non null, we know there must be on disk data
        else {
           // w(q,t) = ln(1 + N/df(t)
            //
            // read the df(t) and tf(t,d) values of each term


            // use each byte position to

        }
        return null;
    }

    // queries the persistent RDB to retrieve only the docId's from given term's postings as well as each doc's corresponding tf(t,d) value
    // returns results in a HashMap linking each docId to its tf(t,d)
    //TODO: set up RDB reading here
    @Override
    public HashMap<Integer, Integer> getPostingsWithoutPositions(String term) {
        HashMap<Integer, Integer> results = new HashMap<>();

        // get the list of docIds for the term
        List<Integer> docs = mIndexDAO.readDocIds(mIndex, term);



        // get the list of tf(t,d) values for each doc
        for (Integer d : docs) {
            Integer frequency = mIndexDAO.readTermDocFrequency(term, d);
            // add components into final hashmap
            results.put(d, frequency);
        }
        // for a ranked retrieval, we only need the tf(t,d) values for all the documents containing the term
        return results;
    }

    public List<Posting> getPostings(Index index, String term) {
        return null;
    }

    /**
     * A (sorted) list of all terms in the index vocabulary.
     */
    @Override
    public List<String> getVocabulary() {
        return null;
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
        return null;
    }
}
