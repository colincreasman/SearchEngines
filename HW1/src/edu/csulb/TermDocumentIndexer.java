package edu.csulb;
import cecs429.documents.DirectoryCorpus;
import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.indexes.*;
import cecs429.queries.BooleanQueryParser;
import cecs429.queries.QueryComponent;
import cecs429.text.AdvancedTokenProcessor;
import cecs429.text.BasicTokenProcessor;
import cecs429.text.EnglishTokenStream;

import javax.management.Query;
import java.awt.desktop.SystemEventListener;
import java.io.BufferedReader;
import java.io.Reader;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public class TermDocumentIndexer {
	public static void main(String[] args) {
		mainMenu();
	}

	// driver method to route user selections from the main menu
	private static void mainMenu() {
		Scanner in = new Scanner(System.in);

		// setup the initial corpus
		DocumentCorpus corpus = selectCorpusMenu();

		// setup the initial index
		Index index = selectIndexMenu(corpus);

		// loop until user wants to quit
		while (true) {
			System.out.println("Please select an action from the options below: ");
			System.out.println("***********************************");
			System.out.println("(a) Perform a Search Query ");
			System.out.println("(b) Change Index Type ");
			System.out.println("(c) Change Corpus Directory ");
			System.out.println("(d) Stem a Token ");
			System.out.println("(e) Normalize a Token ");
			System.out.println("(f) View Top 1000 Vocabulary Tokens  ");
			System.out.println("(h) View Top 1000 Indexed Terms ");
			System.out.println("(i) Quit Program ");
			String selection = in.nextLine();

			switch (selection) {
				case "a": {
					// TODO: move input gathering to inside method
					processQuery(corpus, index);
					break;
				}
				// display index selection menu again
				case "b": {
					// reset the current index by asking user to choose a new one
					index = selectIndexMenu(corpus);
					break;
				}
				case "c": {
					// reset the current corpusPath by asking user to choose a new one
					corpus = selectCorpusMenu();
					// need to rebuild index after choosing a new corpus
					index = selectIndexMenu(corpus);
					break;
				}
				case "d": {
					showStemmedToken();
					break;
				}
				case "e": {
					showNormalizedToken();
					break;
				}
				case "f": {
					showVocabulary(index);
					break;
				}
				case "g": {
					showIndex(index);
					break;
				}
				case "h": {
					System.out.println("Quitting the application - Goodbye!");
					System.exit(0);
				}
			}
		}
	}

	private static void showIndex(Index index) {
		Scanner in = new Scanner(System.in);
		System.out.println("Showing the first 1000 terms in the index below: \n ");
		System.out.println("\n*****************START OF INDEX******************\n");

		int count = 1;
		String currentTerms;

		if (index.getVocabulary().size() >= 1000) {
			currentTerms = index.toString();
			System.out.println(currentTerms);
		}
		else {
			System.out.println(index.toString());
			System.out.println("\n*****************END OF INDEX******************\n");
			System.out.println("Returning to main menu... \n");
			return;
		}

		System.out.println("Show the next 1000 terms? ('y' to continue, 'n' to quit and return to main menu) \n");
		String choice = in.nextLine();

		while (true) {
			int currentIndex = currentTerms.length();
			int nextIndex = currentIndex + (count * 1000);
			if (choice == "y") {
				if (nextIndex < (index.getVocabulary().size())) {
					currentTerms = index.toString().substring(currentIndex, nextIndex);
					System.out.println(currentTerms);
					count += 1;
				}
				else {
					// if theres not enough terms left to print another 1000, just return however many are left and break out to main menu
					currentTerms = index.toString().substring(currentIndex, index.getVocabulary().size() - 1);
					System.out.println(currentTerms);
					System.out.println("\n*****************END OF INDEX******************\n");
					System.out.println("Returning to main menu... \n");
					break;
				}
			}
			else {
				System.out.println("Returning to main menu... \n");
				break;
			}
		}
	}

	// builds an index (any implementation of the Index interface) using a Document Corpus
	private static Index buildIndex(DocumentCorpus corpus, Index index) {
		System.out.println("Indexing corpus...");
		// start timer
		long start = System.currentTimeMillis();

		AdvancedTokenProcessor processor = new AdvancedTokenProcessor();

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
				// process each token into a term(s)
				List<String> terms = processor.processToken(token);
				for (String term : terms) {
					if (index instanceof PositionalInvertedIndex) {
						// add the term(s) to the index based off its type
						index.addTerm(term, d.getId(), position);
					}
					else {
						index.addTerm(term, d.getId());
					}
					// keep track of the position of each token as we iterate through the document
					position += 1;
				}
			}
		}

		long stop = System.currentTimeMillis();
		double elapsedSeconds = (double) ((stop - start) / 1000.0);
		System.out.println("Indexing completed in approximately " + elapsedSeconds + " seconds.");

		return index;
	}

	// tests the stemming of a single provided token by returning its stemmed term(s)
	private static void showStemmedToken() {
		Scanner in = new Scanner(System.in);
		boolean isContinue = true;
		while (isContinue) {
			System.out.println("Please enter a token a stem: ");
			String token = in.nextLine();

			AdvancedTokenProcessor processor = new AdvancedTokenProcessor();
			String term = processor.stem(token);
			System.out.println("The stemmed term for the provided token is '" + term + "' ");

			System.out.println("Continue stemming tokens? (y/n)");
			String choice = in.nextLine();
			if (Objects.equals(choice, "n")) {
				isContinue = false;
				return;
			}
		}
	}

	private static void showNormalizedToken() {
		Scanner in = new Scanner(System.in);
		boolean isContinue = true;
		while (isContinue) {
			System.out.println("Please enter the token to normalize: ");
			String token = in.nextLine();

			AdvancedTokenProcessor processor = new AdvancedTokenProcessor();
			List<String> terms = processor.processToken(token);
			//System.out.println("Testing which terms are returned by showProcessedTerm: " + terms);
			System.out.println("The normalized term(s) for the provided token are shown below:");
			for (String t : terms) {
				System.out.println(t);
			}

			System.out.println("Continue normalizing tokens? (y/n)");
			String choice = in.nextLine();
			if (Objects.equals(choice, "n")) {
				isContinue = false;
				return;
			}
		}
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
	private static void processQuery(DocumentCorpus corpus, Index index) {
		Scanner in = new Scanner(System.in);
		System.out.println("Please enter your search query: ");
		String query = in.nextLine();

		List<Posting> queryPostings = new ArrayList<>();
		// use a boolean query parser to parse the string query into a single Query component before retrieving its postings
		BooleanQueryParser parser = new BooleanQueryParser();
		QueryComponent fullQuery = parser.parseQuery(query);
		// sort the list of postings and print results
		queryPostings = fullQuery.getPostings(index);
		if ((queryPostings == null) || (queryPostings.contains(null))) {
			System.out.println("No documents were found containing the query '" + query + "'");
		}
		else {
			queryPostings.sort(Comparator.comparingInt(Posting::getDocumentId));
			showQueryResults(queryPostings, corpus, query);
			showDocument(corpus, index);
		}
	}

	private static void showDocument(DocumentCorpus corpus, Index index) {
		Scanner in = new Scanner(System.in);
		System.out.println();
		System.out.println("Please enter the ID of the document you'd like to view: ");
		int docId = in.nextInt();
		BufferedReader contentReader = new BufferedReader(corpus.getDocument(docId).getContent());
		StringBuilder stringBuilder = new StringBuilder();
		String line;

		try {
			while ((line = contentReader.readLine()) != null) {
				stringBuilder.append(line);
				// insert a line break every 15 words for visibility
				if (stringBuilder.length() % 15 == 0) {
					stringBuilder.append("\n");
				}
			}
			String stringContent = stringBuilder.toString();
			System.out.println("The content of document # " + docId + " is shown below: \n" + stringContent);
		}
		catch (Exception ex) {
			System.out.println("Document #" + docId + " has no viewable content.");
		}
	}
	// prints out the results of a given query by showing every document the query was found in - along with its term positions within each document - on a new line
	// also prints out the total number of documents with the query that were found in the corpus
	private static void showQueryResults(List<Posting> results, DocumentCorpus corpus, String query) {
		// initialize counter to keep track of total number of documents the query was found in
		int totalDocuments = 0;
		System.out.println("Query matches were found in the following documents: \n");

		try {
			for (Posting p : results) {
				// increment the counter for each postings in the master list
				totalDocuments += 1;
				System.out.println("Document #" + p.getDocumentId() + ": '" + corpus.getDocument(p.getDocumentId()).getTitle() + "' ");
				System.out.println(" Term Positions: " + p.getTermPositions().toString());
				System.out.println();
			}

			System.out.println("Total number of documents found containing the query: " + totalDocuments);
		}
		catch (NullPointerException ex) {
			System.out.println("No documents were found containing the query '" + query + "'");
		}
	}

	private static DocumentCorpus selectCorpusMenu() {
		Scanner in = new Scanner(System.in);

		// ask the user for corpus directory
		System.out.println("Please choose a corpus by selecting one of the options below:");
		System.out.println("***********************************\n");
		System.out.println("(a) National Parks Websites");
		System.out.println("(b) Moby Dick - First 10 Chapters");
		System.out.println("(c) Test Corpus - Json Files");
		System.out.println("(d) Test Corpus - Txt Files");
		System.out.println("(e) Custom File Path");

		// setup starter path prefix
		String corpusPath = "/Users/colincreasman/Documents/GitHub/SearchEngines/Corpora/";
		String selection = in.nextLine();

		switch (selection) {
			case "a": {
				corpusPath += "all-nps-sites-extracted";
				break;
			}
			case "b": {
				corpusPath += "MobyDick10Chapters";
				break;
			}
			case "c": {
				corpusPath += "test-corpus-json";
				break;
			}
			case "d": {
				corpusPath += "test-corpus-txt";
				break;
			}
			case "e": {
				System.out.println("Please enter the local path for your corpus directory: \n");
				corpusPath = in.nextLine();
				break;
			}
		}
		DocumentCorpus corpus = DirectoryCorpus.loadTextOrJsonDirectory(Paths.get(corpusPath));
		return corpus;
	}

	private static Index selectIndexMenu(DocumentCorpus corpus) {
		Scanner in = new Scanner(System.in);
		System.out.println("Please select the type of index you would like to build:");
		System.out.println("***********************************\n");
		System.out.println("(a) Positional Inverted Index ");
		System.out.println("(b) Term Document Index ");
		String indexSelect = in.nextLine();

		Index index = null;
		HashSet<String> vocabulary = new HashSet<>();

		switch (indexSelect) {
			case ("a"): {
				index = new PositionalInvertedIndex(vocabulary);
				break;
			}
			case ("b"): {
				index = new InvertedIndex(vocabulary);
				break;
			}
		}

		return buildIndex(corpus, index);

	}
}

