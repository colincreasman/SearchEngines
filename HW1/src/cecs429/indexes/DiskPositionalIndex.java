package cecs429.indexes;

import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.text.AdvancedTokenProcessor;
import cecs429.text.EnglishTokenStream;
import org.mapdb.BTreeMap;

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
    //TODO: update so that it takes in a tokenProcessor arg to allow different types of processing at runtime
    public void initializeInMemoryIndex(DocumentCorpus corpus) {
        mDocWeights = new HashMap<>();
        mByteLocations = new ArrayList<>();
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
            HashMap<String, Integer> termFrequenciesPerDoc = new HashMap<>(); // maps docId -> tf(t,d)

            for (String token : tokens) {
                // process each token into a term(s)
                List<String> terms = processor.processToken(token);

                // iterate through each term while keeping a running total of all the times it is found in the current doc
                int currentTermCount;
                for (String term : terms) {
                    // with each new normalized term, reset its  count to 1 before trying to retrieve it from the map
                    currentTermCount = 1;

                    // now check if there's already an existing count  for this term in the results hashmap
                    try {
                        // if found, reassign the count to whatever is currently in the results
                        currentTermCount = termFrequenciesPerDoc.get(term);

                        // increment the count by 1 to account for this occurence
                        int updatedTermCount = currentTermCount + 1;
                        // now replace the original count in the results map with the updated count
                        termFrequenciesPerDoc.replace(term, currentTermCount, updatedTermCount);
                    }

                    // catch null values to still count terms that haven't been added to the results map yet
                    catch (NullPointerException ex) {
                        // since this term hasn't been counted yet, put it into the hashmap with the unmodified val of currentTermCount (should still be 1)
                        termFrequenciesPerDoc.put(term, currentTermCount);
                    }

                    // after handling all the term-doc data, we still need to add each term into the index
                    finally {
                        mIndex.addTerm(term, d.getId(), position);
                    }
                }
                // only increment the position when moving on to a new token
                // if normalizing a token produces more than one term, they will all be posted at the same position
                position += 1;
            }
            // after processing and counting all of the tokens in the current doc, we can now use the final hashmap of tf(t,d) vals to each of its terms to find the aggregate normalized weight
            long docWeight = getNormalizedWeight(termFrequenciesPerDoc);
            // now add this doc's docId and Ld value to the overall map of docWeights per doc
            mDocWeights.put(d.getId(), docWeight);
        }

        long stop = System.currentTimeMillis();
        long elapsedSeconds = (long) ((stop - start) / 1000.0);
        System.out.println("Finished indexing the in-memory positional index in approximately " + elapsedSeconds + " seconds.");

        // now write that index to disk and save the returned byte positions into the static object field for them
        mByteLocations = mIndexDAO.writeIndex(mIndex, mPath);
    }

    // builds the index's vocabulary by reading its terms from the existing on-disk index data
    public void loadVocabulary(DocumentCorpus corpus) {
        String indexDir = corpus + "/index";
        mVocabulary = new ArrayList<>();
        List<String> unsorted = mIndexDAO.readVocabulary(indexDir);

        try {
            mVocabulary.addAll(unsorted);
            // sort
            Collections.sort(mVocabulary);

        }

        catch (NullPointerException ex) {
            System.out.println("Failed to load vocabulary index because the DiskIndexDAO could not find any B+ tree files in the given corpus directory. ");
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
