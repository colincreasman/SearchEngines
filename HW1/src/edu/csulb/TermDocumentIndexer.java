package edu.csulb;
import cecs429.documents.DirectoryCorpus;
import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.indexes.*;
import cecs429.text.AdvancedTokenProcessor;
import cecs429.text.BasicTokenProcessor;
import cecs429.text.EnglishTokenStream;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public class TermDocumentIndexer {
	public static void main(String[] args) {
		mainLoop();
	}

	// driver method to route user selections from the main menu
	private static void mainLoop() {
		Scanner in = new Scanner(System.in);
		// TODO: Comment out for final demo
		// hard coded path for testing
		String corpusPath = "/Users/colincreasman/Documents/GitHub/SearchEngines/Corpora/all-nps-sites-extracted";
		// TODO: Uncomment for final demo
		// ask the user for corpus directory
//		System.out.println("Please enter the local path for your corpus directory: ");
//		String corpusPath = input.nextLine();
//		// Create a DocumentCorpus to load .json documents from the corpus directory.
		DocumentCorpus corpus = DirectoryCorpus.loadJsonDirectory(Paths.get(corpusPath), ".json");
		// Index the documents of the corpus.
		Index index = indexCorpus(corpus);

		// loop until user wants to quit
		while (true) {
			System.out.println("Enter your query: ");
			String input = in.nextLine();
			// check for special commands
			if (Objects.equals(input, ":q")) {
				System.out.println("Quitting the application - Goodbye!");
				System.exit(0);
			}
			else if (Objects.equals(input, ":stem")) {
				System.out.println("Please enter the token you would like stemmed: ");
				String token = in.nextLine();
				List<String> stemmedTerms = showStemmedTerm(token);
				System.out.println("The stemmed term(s) for the token '" + token + "' is: " + stemmedTerms.toString());
			}
			else if (Objects.equals(input, ":index")) {
				mainLoop();
			}
			else if (Objects.equals(input, ":vocab")) {
				showVocabulary(index);
			}
			// end of special queries
			else {
				processQuery(corpus, index, input);
			}
		}
	}

	// builds an index (any implementation of the Index interface) using a Document Corpus
	private static Index indexCorpus (DocumentCorpus corpus) {
		System.out.println("Indexing corpus...");
		// start timer
		long start = System.currentTimeMillis();
		HashSet<String> vocabulary = new HashSet<>();
		AdvancedTokenProcessor processor = new AdvancedTokenProcessor();
		PositionalInvertedIndex index = new PositionalInvertedIndex(vocabulary);

		// THEN, do the loop again! But instead of inserting into the HashSet, add terms to the index with addPosting.
		for (Document d : corpus.getDocuments()) {
			//System.out.println("here");
			// Tokenize the document's content by constructing an EnglishTokenStream around the document's content.
			EnglishTokenStream stream = new EnglishTokenStream(d.getContent());

			// initialize a counter to keep track of the term positions of tokens found within each document
			int position = 0;
			// retrieve tokens from the doc as an iterable
			Iterable<String> tokens = stream.getTokens();
			for (String token : tokens) {
				// keep track of the position of each token as we iterate through the document
				position += 1;
				// process each token into a term(s)
				List<String> terms = processor.processToken(token);
				for (String term : terms) {
					// add the term(s) to the index
					index.addTerm(term, d.getId(), position);
				}
			}
		}

		long stop = System.currentTimeMillis();
		double elapsedSeconds = (double) ((stop - start) / 1000.0);
		System.out.println("Indexing completed in approximately " + elapsedSeconds + " seconds.");

		return index;
	}

	// tests the stemming of a single provided token by returning its stemmed term(s)
	// TODO: update this once Porter2 has been implemented
	private static List<String> showStemmedTerm (String token){
		AdvancedTokenProcessor processor = new AdvancedTokenProcessor();
		List<String> terms = processor.processToken(token);
		return terms;
	}

	// tests the vocabulary of the index by printing out the first 1000 terms in order and the total amount of terms in the vocabulary
	private static void showVocabulary(Index index) {
		System.out.println("Sorting vocabulary...");
		Collections.sort(index.getVocabulary());
		// show the first 1000 terms in the vocabulary
		System.out.println("The first 1000 terms in the sorted vocabulary are listed below: ");
		for (int i = 0; i < 1000; i++) {
			String term = index.getVocabulary().get(i);
			System.out.println(term);
		}
		// show the total number of terms in the vocabulary
		System.out.println("\n The total number of terms in the vocabulary is: " + index.getVocabulary().size());
	}

	// processes a query inputted by the user
	private static void processQuery(DocumentCorpus corpus, Index index, String query) {
		try {
			List<Posting> postings = index.getPostings(query);

			for (Posting p : postings) {
				// TODO: Comment out for final demo
				System.out.println("The query term '" + query + "' appears in Document #" + p.getDocumentId() + ": '" + corpus.getDocument(p.getDocumentId()).getTitle() + "' at the following locations: \n" + p.getTermPositions().toString());
				System.out.println();
			}

			// prints the total number of documents found
			// works only with the INVERTED index, NOT THE POSITIONAL INDEX
			System.out.println("Total number of documents found: " + index.getPostings(query).size());
		}

		catch (NullPointerException ex) {
			System.out.println("There are no documents in the corpus that contain the query '" + query + "'.");
		}
	}
}

