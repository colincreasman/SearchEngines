package cecs429.indexes;

import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.text.AdvancedTokenProcessor;
import cecs429.text.EnglishTokenStream;
import static edu.csulb.Driver.ActiveConfiguration.*;

import cecs429.weights.DocTermWeight;
import cecs429.weights.DocWeight;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        mVocabulary = new ArrayList<>();
        mIndexInMemory = new PositionalInvertedIndex(mVocabulary);
        mDocWeights = new ArrayList<>();
        mTermLocations = new ArrayList<>();
    }

    public void initializeInMemoryIndex() throws IOException {
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
//            HashMap<String, Integer> termFrequencies = new HashMap<>();

            // initialize a counter to keep track of the term positions of tokens found within each document
            int tokenPosition = 0;
            for (String token : tokens) {
                // process each token into a term(s)
                List<String> terms = processor.processToken(token);

                // iterate through each term while keeping a running total of all the times it is found in the current doc
                for (String term : terms) {
                    // whenever a new term is added to the index, a DocTermWeight is automatically created (or updated, if one already exists for this term-doc combo) by the Posting class
                    mIndexInMemory.addTerm(term, d.getId(), tokenPosition);

                    // since the postings lists are always sorted, we can reference the new Posting for the term that was just added by accessing the most recent item in its postings list
                    List<Posting> termPostings = mIndexInMemory.getPostings(term);
                    Posting lastPosting = termPostings.get(termPostings.size() - 1);

                    // now we obtain a reference to the term's wDt through that recently added posting
                    DocTermWeight termWeight = lastPosting.getDocTermWeight();
                    wDts.add(termWeight);
                }
                // only increment the position when moving on to a new token; if normalizing a token produces more than one term, they will all be posted at the same position
                tokenPosition += 1;
            }

            DocWeight docWeight =  new DocWeight(d, wDts);
            docWeight.setDocLength(tokenPosition);
            // after setting all the data for the current docWeight, add it to the list of doc weights and assign it to the Document objet itself so it can be referenced from other locations later on
            mDocWeights.add(docWeight);
            d.setWeight(docWeight);
        }

        long stop = System.currentTimeMillis();
        long elapsedSeconds = (long) ((stop - start) / 1000.0);
        System.out.println("Initialized index in approximately " + elapsedSeconds + " seconds.");

        // now write that index to disk and save the returned byte positions into the static object field for them
        mTermLocations = indexDao.writeIndex(mIndexInMemory, mPath);
        mVocabulary = mIndexInMemory.getVocabulary();
        indexDao.writeVocabulary(mVocabulary, mTermLocations); //TODO
        indexDao.writeDocWeights(mDocWeights);

    }

    // gets the index's vocabulary, term locations, and doc weights by reading them from the existing on-disk index data
    public void load() {
        //List<String> unsorted = indexDao.readVocabulary();
        System.out.println("Loading index data from disk...");
        //  HashMap<Integer, Double> docWeights = new HashMap<>();
        try {
            for (Document d : activeCorpus.getDocuments()) {
                // Tokenize the document's content by constructing an EnglishTokenStream around the document's content.
                EnglishTokenStream stream = new EnglishTokenStream(d.getContent());
            }
        } catch (NullPointerException ex) {
            System.out.println("Failed to load vocabulary index because no documents could were found in the corpus directory");
        }
    }

    /**
     * Retrieves a list of Postings (with positions data) of documents that contain the given term.
     *
     * @param term
     */
    @Override
    public List<Posting> getPostings(String term) {
        List<Posting> postings = new ArrayList<>();

        mDbDao.open("termLocations");
        long byteLocation = mDbDao.readTermLocation(term);
        mDbDao.close("termLocations");

        mBinDao.open("postings");
        postings = mBinDao.readPostings(byteLocation);
        mBinDao.close("postings");

        return postings;
    }

    // queries the persistent RDB to retrieve only the docId's from given term's postings as well as each doc's corresponding tf(t,d) value
    // returns results in a HashMap linking each docId to its tf(t,d)
    //TODO: set up RDB reading here
    @Override
    public List<Posting> getPostingsWithoutPositions(String term) {
        List<Posting> postings = new ArrayList<>();

        mDbDao.open("termLocations");
        long byteLocation = mDbDao.readTermLocation(term);
        mDbDao.close("termLocations");

        mBinDao.open("postings");
        postings = mBinDao.readPostingsWithoutPositions(byteLocation);
        mBinDao.close("postings");

        return postings;
    }

    /**
     * A (sorted) list of all terms in the index vocabulary.
     */
    @Override
    public List<String> getVocabulary() {
        if (mVocabulary == null || mVocabulary.isEmpty()) {
            mVocabulary = indexWriter.readVocabulary();
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

    public List<DocWeight> getDocWeights() {
        return mDocWeights;
    }
}