package Engine.Indexes;

import Engine.DataAccess.BinFileDao;
import Engine.DataAccess.DbFileDao;
import Engine.DataAccess.DiskIndexWriter;
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
    private static DiskIndexWriter mWriter;
    private static DbFileDao mDbDao;
    private static BinFileDao mBinDao;
    private static Index mIndexInMemory;
    private static String mPath;
    private static List<DocWeight> mDocWeights;
    private static List<Long> mByteLocations;
    private static HashMap<String, Long> mTermLocations;
    private static HashMap<String, List<Posting>> mBasicTermPostings;

    public DiskPositionalIndex(DocumentCorpus corpus) {
        mPath = corpus.getPath();
        // initialize the in-memory positional index WITHOUT changing the ActiveConfiguration's in-memory index which is the current DiskIndex being built
        mWriter = new DiskIndexWriter();
        mVocabulary = new ArrayList<>();
        mIndexInMemory = new PositionalInvertedIndex(mVocabulary);
        mDbDao = new DbFileDao();
        mBinDao = new BinFileDao();
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
//
//                    int currFreq = 1;
//                    try {
//                        // try to update the current term's tftd in the map if it exists
//                        currFreq += termFrequencies.get(term);
//                        termFrequencies.put(term, currFreq);
//                    }
//                    catch (NullPointerException ex) {
//                        // if the null ex is caught, this must be the first instance of this term, so we put it in the map with the original freq of 1
//                        termFrequencies.put(term, currFreq);
//                    }
//                    // to mitigate weight calculations, this same DocTermWeight reference is added to the list of termWeights that will eventually compose the current DocWeight
                    DocTermWeight termWeight = mIndexInMemory.getPostings(term).get(d.getId()).getDocTermWeight();
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
        indexWriter.writeIndex(this, mPath);
        mWriter.writeDocWeights(mDocWeights);
        mVocabulary = mIndexInMemory.getVocabulary();
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
        List<Posting> postings = new ArrayList<>();

        mDbDao.open("termLocations");
        mBinDao.open("postings");

        long byteLocation = mDbDao.readTermLocation(term);
        postings = mBinDao.readPostings(byteLocation);

        mDbDao.close("termLocations");
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
        mBinDao.open("postings");

        long byteLocation = mDbDao.readTermLocation(term);
        postings = mBinDao.readPostingsWithoutPositions(byteLocation);

        mDbDao.close("termLocations");
        mBinDao.close("postings");

        return postings;
    }

    /**
     * A (sorted) list of all terms in the index vocabulary.
     */
    @Override
    public List<String> getVocabulary() {
//        if (mVocabulary == null) {
//            mVocabulary = mIndexInMemory
//        }
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
