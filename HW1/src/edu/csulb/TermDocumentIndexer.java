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

import static java.lang.String.format;

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
			System.out.println("\nPlease select an action from the options below: ");
			System.out.println("*************************************************");
			System.out.println("(a) Change Corpus Directory ");
			System.out.println("(b) Change Index Type ");
			System.out.println("(c) Stem Token ");
			System.out.println("(d) Normalize Token ");
			System.out.println("(e) Top 1000 Vocabulary Tokens  ");
			System.out.println("(f) Top 1000 Index Postings ");
			System.out.println("(g) Corpus Overview ");
			System.out.println("(h) Document Preview ");
			System.out.println("(i) Perform Search Query ");
			System.out.println("(j) Quit ");
			String selection = in.nextLine();

			switch (selection) {
				case "a": {
					// reset the current corpusPath by asking user to choose a new one
					corpus = selectCorpusMenu();
					// need to rebuild index after choosing a new corpus
					index = selectIndexMenu(corpus);
					break;
				}
				// display index selection menu again
				case "b": {
					// reset the current index by asking user to choose a new one
					index = selectIndexMenu(corpus);
					break;
				}
				case "c": {
					showStemmedToken();
					break;
				}
				case "d": {
					showNormalizedToken();
					break;
				}
				case "e": {
					showVocabulary(index);
					break;
				}
				case "f": {
					showIndex(index);
					break;
				}
				case "g": {
					showCorpusOverview(corpus);
					break;
				}
				case "h": {
					showDocument(corpus, index);
					System.out.println("Returning to main menu...");
					break;
				}
				case "i": {
					// TODO: move input gathering to inside method
					processQuery(corpus, index);
					break;
				}
				case "j": {
					System.out.println("Quitting the application - Goodbye!");
					System.exit(0);
				}
			}
		}
	}

	private static DocumentCorpus selectCorpusMenu() {
		Scanner in = new Scanner(System.in);

		// ask the user for corpus directory
		System.out.println("Please select a corpus from the options below: ");
		System.out.println("**********************************************\n");
		System.out.println("(a) National Parks Websites");
		System.out.println("(b) Moby Dick - First 10 Chapters");
		System.out.println("(c) Test Corpus - Json Files");
		System.out.println("(d) Test Corpus - Txt Files");
		System.out.println("(e) Custom File Path");

		// setup starter path prefix
		String corpusPath = "/Users/colincreasman/Documents/GitHub/SearchEngines/Corpora/";
		DocumentCorpus corpus = new DirectoryCorpus(Paths.get(corpusPath));

		String selection = in.nextLine();

		switch (selection) {
			case "a": {
				corpusPath += "all-nps-sites-extracted";
				corpus = DirectoryCorpus.loadJsonDirectory(Paths.get(corpusPath), ".json");
				break;
			}
			case "b": {
				corpusPath += "MobyDick10Chapters";
				corpus = DirectoryCorpus.loadTextDirectory((Paths.get(corpusPath)), ".txt");
				break;
			}
			case "c": {
				corpusPath += "test-corpus-json";
				corpus = DirectoryCorpus.loadJsonDirectory(Paths.get(corpusPath), ".json");
				break;
			}
			case "d": {
				corpusPath += "test-corpus-txt";
				corpus = DirectoryCorpus.loadTextDirectory(Paths.get(corpusPath), ".txt");
				break;
			}
			case "e": {
				System.out.println("Please enter the local path for your corpus directory: \n");
				corpusPath = in.nextLine();
				corpus = DirectoryCorpus.loadTextOrJsonDirectory(Paths.get(corpusPath));
				break;
			}
		}
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

	// builds an index (any implementation of the Index interface) using a Document Corpus45
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

	// tests the postings of the index by printing out terms and their postings in increments of 1000
	private static void showIndex(Index index) {
		System.out.println("Sorting vocabulary...");
		Collections.sort(index.getVocabulary());

		int termsCount = 0;
		Scanner in = new Scanner(System.in);
		// show the first 1000 terms in the vocabulary
		System.out.println("The first 1000 Terms and Postings in the index are shown below: \n");
		System.out.println("**************************BEGIN INDEX*************************");

		String choice = "y";
		int endIndex = 0;
		int startIndex = 0;

		while (!Objects.equals(choice, "n")) {
			// set the new starting index to the end index of the last iteration
			startIndex = endIndex;
			// set the new ending index to the minimum of either 1000 terms after the start index or the size of the index's vocabulary
			endIndex = Math.min((startIndex + 1000), (index.getVocabulary().size()));

			int i;
			for (i = startIndex; i < endIndex; i++) {
				termsCount += 1;
				String term = index.getVocabulary().get(i);
				System.out.println(index.viewTermPostings(term));
			}

			// stop if reaching the end of the vocabulary's size
			if (i >= index.getVocabulary().size() - 1) {
				System.out.println("**************************END INDEX*************************\n");
				System.out.println("Total number of terms with postings in index: " + termsCount);
				System.out.println("No postings remaining in index. Returning to main menu.");
				choice = "n";
			}
			else {
				System.out.println("View the next 1000 index postings? (y/n)");
				choice = in.nextLine();
			}
		}
	}

	// tests the vocabulary of the index by printing out terms in increments of 1000
	private static void showVocabulary(Index index) {
		System.out.println("Sorting vocabulary...");
		Collections.sort(index.getVocabulary());

		int termsCount = index.getVocabulary().size();

		Scanner in = new Scanner(System.in);
		// show the first 1000 terms in the vocabulary
		System.out.println("The first 1000 Terms index are shown below: \n");
		System.out.println("****************BEGIN VOCABULARY****************");

		String choice = "y";
		int endIndex = 0;
		int startIndex = 0;

		while (!Objects.equals(choice, "n")) {
			// set the new starting index to the end index of the last iteration
			startIndex = endIndex;
			// set the new ending index to the minimum of either 1000 terms after the start index or the size of the index's vocabulary
			endIndex = Math.min((startIndex + 1000), (index.getVocabulary().size()));

			int i;
			for (i = startIndex; i < endIndex; i++) {
				String term = index.getVocabulary().get(i);
				System.out.println(term);
			}

			// stop if reaching the end of the vocabulary's size
			if (i >= index.getVocabulary().size() - 1) {
				System.out.println("********************END VOCABULARY*******************\n");
				System.out.println("Total number of terms in vocabulary: " + termsCount);
				System.out.println("No terms remaining in vocabulary. ");
				choice = "n";
			}
			else {
				System.out.println("View the next 1000 vocabulary terms? (y/n)");
				choice = in.nextLine();
			}
			System.out.println("Returning to main menu...\n");
		}
	}

	// tests the stemming of a single provided token by returning its stemmed term(s)
	private static void showStemmedToken() {
		Scanner in = new Scanner(System.in);
		//boolean isContinue = true;
		String choice = "y";

		while (!Objects.equals(choice, "n")) {
			System.out.println("Please enter a token a stem: ");
			String token = in.nextLine();

			AdvancedTokenProcessor processor = new AdvancedTokenProcessor();
			String term = processor.stem(token);
			System.out.println("The stemmed term for the provided token is '" + term + "' ");

			System.out.println("Continue stemming tokens? (y/n)");
			choice = in.nextLine();
		}
	}

	// tests the normalization of a single provided token by returning its fully normalized term(s)
	private static void showNormalizedToken() {
		Scanner in = new Scanner(System.in);
		String choice = "y";
		while (!Objects.equals(choice, "n")) {
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
			choice = in.nextLine();
		}
	}

	private static void showDocument(DocumentCorpus corpus, Index index) {
		Scanner in = new Scanner(System.in);
		System.out.println();
		String choice = "y";
		//boolean isContinue = true;
		while (!Objects.equals(choice, "n")) {

			System.out.println("Please enter the ID of the document you'd like to view: ");
			int docId = Integer.parseInt(in.nextLine());
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
				System.out.println("The content of document #" + docId + " is shown below: \n");
				System.out.println("********************BEGIN CONTENT************************");
				System.out.println( "\n" + stringContent);
				System.out.println("********************END CONTENT**************************" + "\n");
			}

			catch (Exception ex) {
				System.out.println("Document #" + docId + " has no viewable content.");
			}

			finally {
				System.out.println("View another document? (y/n)");
				choice = in.nextLine();
			}
		}
		//System.out.println("Returning to main menu...");
	}

	// processes a query inputted by the user
	private static void processQuery(DocumentCorpus corpus, Index index) {
		Scanner in = new Scanner(System.in);
		String choice = "y";

		while (!choice.equals("n")) {
			System.out.println("Please enter your search query: ");
			String query = in.nextLine();
			List<Posting> queryPostings = new ArrayList<>();
			// use a boolean query parser to parse the string query into a single Query component before retrieving its postings
			BooleanQueryParser parser = new BooleanQueryParser();
			QueryComponent fullQuery = parser.parseQuery(query);
			// sort the list of postings and print results
			queryPostings = fullQuery.getPostings(index);

			if (queryPostings == null || queryPostings.contains(null) || queryPostings.size() < 1) {
				System.out.println("No documents were found containing the query '" + query + "'");
			}
			else {
				queryPostings.sort(Comparator.comparingInt(Posting::getDocumentId));
				showQueryResults(queryPostings, corpus, query);
				String docChoice = "y";
				while (!Objects.equals(docChoice, "n")) {
					System.out.println("Open a document? (y/n) ");
					docChoice = in.nextLine();
					if (Objects.equals(docChoice, "y")) {
						showDocument(corpus, index);
					}
				}
			}
			System.out.println("Perform another query? (y/n)");
			choice = in.nextLine();
		}
		System.out.println("Returning to main menu...");
	}

	// prints out the results of a given query by showing every document the query was found in - along with its term positions within each document - on a new line
	// also prints out the total number of documents with the query that were found in the corpus
	private static void showQueryResults(List<Posting> results, DocumentCorpus corpus, String query) {
		// initialize counter to keep track of total number of documents the query was found in

		try {
			System.out.println("Number of documents queried: " + corpus.getCorpusSize());
			System.out.println("Number of documents found: " + results.size());
			System.out.println("Document matches: \n");
			System.out.println("********************************BEGIN QUERY RESULTS********************************");

			int count = 1;
			for (Posting p : results) {
				System.out.println(count + ") Title: '" +  corpus.getDocument(p.getDocumentId()).getTitle() + "' ");
				System.out.println("    - DocId: " + p.getDocumentId());
				System.out.println("    - Query Term Positions: " + p.getTermPositions().toString());
				if (count != results.size()) {
					System.out.println();
				}
				count += 1;
			}

		}
		catch (NullPointerException ex) {
			System.out.println("Query failed. The corpus does not contain any documents matching the query '" + query + "'");
		}
		System.out.print("********************************END QUERY RESULTS**********************************\n");
		System.out.println();
	}

	// prints out a list of every document in the corpus by showing the title of each document and the internal document ID assigned to it
	private static void showCorpusOverview(DocumentCorpus corpus) {
		System.out.println("An overview of all document ID's and titles in the current corpus is shown below: \n ");
		System.out.println("********************BEGIN OVERVIEW***I*****************");
//		System.out.println("_______________________________________________________");
//		System.out.println("|____________ID____________|___________Title__________|");
//		for (Document d : corpus.getDocuments()) {
//			String strId =  String.valueOf(d.getId());
//			String strTitle = d.getTitle();
//			int digits = strId.length();
//			int length = d.getTitle().length();
//			// figure out how much to pad the ID and Title by to center them
//
//			StringBuilder titleBuilder = new StringBuilder(strTitle);
//			int lines = Math.round(length / 26);
//			for (int i = 0; i < lines; i++) {
//				titleBuilder.insert(25 + (i * 25), "\n");
//			}
//
//			strTitle = titleBuilder.toString();
//
//			int padIdLeft = 13 - Math.floorDiv(digits, 2);
//			int padIdRight = 26 - padIdLeft + digits;
//			int padTitleLeft = 13 - Math.floorDiv(length, 2);
//			int padTitleRight = 26 - padTitleLeft + length;
//
//			String id = String.format("%" + padIdLeft + "s", strId);
//			id = String.format("%-" + padIdRight + "s", id);
//			String title = String.format("%" + padTitleLeft + "s", d.getTitle());
//		//	title = String.format("%-" + padTitleRight + "s" + title);
//
//			String currentRow = "|" + id + "|" + title + "|";
//			System.out.println(currentRow);
//		}

		for (Document d : corpus.getDocuments()) {
			System.out.println("Document ID: " + d.getId() + "");
			System.out.println("  Title: " + d.getTitle() + "\n");

		}
		System.out.println("*******************END CORPUS*********************\n");
		System.out.println("Returning to main menu...");
	}

}

