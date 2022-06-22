package edu.csulb;
import cecs429.documents.DirectoryCorpus;
import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.indexes.*;
import cecs429.queries.BooleanQueryParser;
import cecs429.queries.QueryComponent;
import cecs429.queries.QueryParser;
import cecs429.queries.RankedQueryParser;
import cecs429.text.*;
import com.sun.security.jgss.GSSUtil;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.*;

import static edu.csulb.Driver.RunMode.Build;
import static edu.csulb.Driver.RunMode.Query;
import static edu.csulb.Driver.RunMode.Quit;
import static edu.csulb.Driver.QueryMode.Boolean;
import static edu.csulb.Driver.QueryMode.Ranked;
import static edu.csulb.Driver.IndexType.Inverted;
import static edu.csulb.Driver.IndexType.TermDocument;
import static edu.csulb.Driver.IndexType.PositionalInverted;
import static edu.csulb.Driver.IndexType.DiskPositional;

public class Driver {
	/**
	 * enum wrapper for all the types of currently supported querying modes; can be extended to allow additional modes in the future
	 */
	public enum QueryMode {
		Boolean,
		Ranked
	}
	/**
	 * enum wrapper for all the currently supported Run modes; can be extended to allow additional modes in the future
	 */
	public enum RunMode {
		Build,
		Query,
		Quit
	}

	/**
	 * enum wrapper for all the currently supported index types; can be extended to allow additional modes in the future
	 */
	public enum IndexType {
		Inverted,
		TermDocument,
		PositionalInverted,
		DiskPositional
	}

	/**
	 * A simple singleton to encapsulate static instances of every object/variable that will be selected, updated, or otherwise needed for the app's core functionality during a given runtime
	 * <p>
	 * The singleton is constructed with default/null values upon startup using eager instantiation
	 * <p>
	 * Each field is mutable as the user navigates through various menus and/or requests changes to parts of the active configuration(e.g. when changing corpus directory)
	 * <p>
	 * Mutators will only update the fields' values while still referencing the same static instances while actively running
	 */
	public static class ActiveConfiguration {
		public static DocumentCorpus corpus;
		public static Index index;
		public static RunMode runMode;
		public static QueryMode queryMode;
		public static IndexType indexType;
		public static IndexDAO indexDao;
		public static boolean hasDiskIndex; // boolean flag indicating the presence of on-disk index data

		private static ActiveConfiguration instance;

		private ActiveConfiguration() {
		}

		public static ActiveConfiguration getInstance() {
			if (instance == null) {
				synchronized (ActiveConfiguration.class) {
					if (instance == null) {
						instance = new ActiveConfiguration();
					}
				}
			}
			return instance;
		}

		public static DocumentCorpus getCorpus() {
			return corpus;
		}

		public static void setCorpus(DocumentCorpus docCorpus) {
			// whenever a new active corpus is assigned, use its path to create the active indexDAO
			indexDao = new DiskIndexDAO(docCorpus.getPath());
			corpus = docCorpus;

			// now use the new indexDao to determine if there is an on-disk index for the given corpus
			hasDiskIndex = indexDao.hasExistingIndex();
		}

		public static Index getIndex() {
			return index;
		}

		public static void setIndex(Index index) {
			ActiveConfiguration.index = index;
		}

		public static RunMode getRunMode() {
			return runMode;
		}

		public static void setRunMode(RunMode runMode) {
			if (runMode == Quit) {
				System.out.println("Quitting the application - Goodbye!");
				System.exit(0);
			}
			else {
				ActiveConfiguration.runMode = runMode;
			}
		}

		public static QueryMode getQueryMode() {
			return queryMode;
		}

		public static void setQueryMode(QueryMode queryMode) {

			ActiveConfiguration.queryMode = queryMode;
		}

		public static IndexDAO getIndexDao() {
			return indexDao;
		}

		public static void setIndexDao(IndexDAO indexDao) {
			ActiveConfiguration.indexDao = indexDao;
		}

		public static boolean hasDiskIndex() {
			return ActiveConfiguration.indexDao.hasExistingIndex();
		}

		public static void setDiskIndexStatus(boolean isOnDisk) {
			ActiveConfiguration.hasDiskIndex = isOnDisk;
		}

		public static IndexType getIndexType() {
			return indexType;
		}

		public static void setIndexType(IndexType indexType) {
			ActiveConfiguration.indexType = indexType;
		}
	}

	// instantiate global config instance
	public static ActiveConfiguration config = ActiveConfiguration.getInstance();

	public static void main(String[] args) {
		while (ActiveConfiguration.runMode != Quit) {
			selectRunModeMenu();
		}
	}


	private static void selectRunModeMenu() {
		Scanner in = new Scanner(System.in);

		while (true) {
			System.out.println("\nPlease select a run mode from the options below: ");
			System.out.println("*************************************************************************************");
			System.out.println("(a) Build Mode - Build a new index for a specified corpus ");
			System.out.println("(b) Query Mode - Query a corpus with an existing index ");
			System.out.println("(c) Quit Application");
			String choice = in.nextLine();

			switch (choice) {
				case "a": {
					ActiveConfiguration.runMode = Build;
					ActiveConfiguration.corpus = selectCorpusMenu();
					// once the corpus is selected, get the user's index selection over the chosen corpus
					ActiveConfiguration.index = selectIndexMenu(ActiveConfiguration.corpus);
					ActiveConfiguration.queryMode = selectQueryModeMenu();
					mainMenu();
					break;
				}

				case "b": {
					// get user's chosen corpus before checking if index exists
					ActiveConfiguration.corpus = selectCorpusMenu();

					//  check for on-disk index first
					if (ActiveConfiguration.hasDiskIndex) {
						System.out.println("An existing index was found written to disk in the corpus directory '" + ActiveConfiguration.corpus.getPath() + "'\n");
						System.out.println("Would you like to continue to Query Mode using this index? (y/n) ");

						if (Objects.equals(in.nextLine(), "y")) {
							System.out.println("Redirecting to Query Mode...");
							ActiveConfiguration.runMode = Query;
							break;
						}

						else {
							System.out.println("Ok, disregarding the on-disk index...");
						}
					}

					else {
						System.out.println("No existing index was found on disk for the selected corpus. You must switch to another corpus with an existing index or build a new index altogether before entering Query Mode. \n");
						ActiveConfiguration.runMode = Build;
					}

					// now check for in-mem index
					if (ActiveConfiguration.index == null) {
						System.out.println("No existing index was found in memory for the selected corpus. \n Checking for an on-disk index...");
					}

					else {
						System.out.println("An existing index was found in memory for the corpus directory '" + ActiveConfiguration.corpus.getPath() + "'\n");
						System.out.println("Would you like to continue to Query Mode with the found index? (y/n) ");

						if (Objects.equals(in.nextLine(), "y")) {
							System.out.println("Redirecting to Query Mode...");
							ActiveConfiguration.runMode = Query;
							ActiveConfiguration.queryMode = selectQueryModeMenu();
							mainMenu();
						}

						else {
							System.out.println("Ok - Redirecting to Build Mode...");
							ActiveConfiguration.runMode = Build;
						}
					}
					break;
				}

				case "c": {
					ActiveConfiguration.setRunMode(Quit);
					break;
				}

				default: {
					System.out.println("Error: Selection does not match any of of the available options. Please try again ");
					break;
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
		//DocumentCorpus corpus;
		// use .txt as the default extension
		String ext = ".txt";
		String selection = in.nextLine();

		switch (selection) {
			case "a": {
				corpusPath += "all-nps-sites-extracted";
				// only update the ext string when not using .txt files
				ext = ".json";
				break;
			}
			case "b": {
				corpusPath += "MobyDick10Chapters";
				break;
			}
			case "c": {
				corpusPath += "test-corpus-json";
				ext = ".json";
				break;
			}
			case "d": {
				corpusPath += "test-corpus-txt";
				break;
			}
			case "e": {
				System.out.println("Please enter the custom  path for your corpus directory: \n");
				corpusPath = in.nextLine();
				// flag ext to an empty string so we know to load either ext type
				ext = "";
				break;
			}
			default: {
				System.out.println("Error: User input does not match any of of the available options. ");
				break;
			}
		}

		// now use the updated corpusPath to construct an actual Path
		Path path = Paths.get(corpusPath).toAbsolutePath();

		// now return the corpus after loading files into it according to the final ext value
		if (ext.equals(".txt")) {
			return DirectoryCorpus.loadTextDirectory(path, ext);
		}
		else if (ext.equals(".json")) {
			return DirectoryCorpus.loadJsonDirectory(path, ext);
		}

		// if no ext was assigned allow both types to be loaded (could happen with a custom directory
		else {
			return DirectoryCorpus.loadTextOrJsonDirectory(path);
		}
	}

	private static Index selectIndexMenu(DocumentCorpus corpus) {
		ActiveConfiguration.corpus = corpus;

		while (true) {
			Scanner in = new Scanner(System.in);
			System.out.println("Please select the type of index you would like to build for your corpus: ");
			System.out.println("******************************************\n");
			System.out.println("(1) Inverted Index ");
			System.out.println("(2) Term Document Index ");
			System.out.println("(3) Positional Inverted Index ");
			System.out.println("(4) Disk Positional Index ");

			int selection = in.nextInt();
			// if the selection matches one of the number options provided, we should be able to just directly assign the index type via enum ID
			try {
				// decrement by 1 since the enums start at 0
				ActiveConfiguration.indexType = IndexType.values()[selection - 1];
				break;
			}

			// any exception will be due to a user input that doesn't match one of the available options, so keep looping them through the menu until they do
			catch (Exception ex) {
				System.out.println("Error: User input does not match any of of the available options. " + ex);
			}
		}
		return buildIndex(ActiveConfiguration.indexType);
	}

	private static QueryMode selectQueryModeMenu() {
		Scanner in = new Scanner(System.in);

		while (true) {
			System.out.println("\nPlease select a querying mode from the options below: ");
			System.out.println("*************************************************************************************");
			System.out.println("(1) Boolean Retrieval ");
			System.out.println("(2) Ranked Retrieval ");
			int choice = in.nextInt();

			try {
				return QueryMode.values()[choice - 1];
			}

			catch (Exception ex) {
				System.out.println("Error: User input does not match any of of the available options. " + ex + "\n Please try again with one of the options listed. ");
				return null;
			}

		}
	}

	// driver method to route user selections from the main menu
	private static void mainMenu() {
			Scanner in = new Scanner(System.in);

			// loop until user wants to quit
			while (true) {
				System.out.println("\nPlease select an action from the options below: ");
				System.out.println("*************************************************");
				System.out.println("(a) Change Corpus Directory and Index [:index] ");
				System.out.println("(b) Change Query Mode ");
				System.out.println("(c) Change Run Mode ");
				System.out.println("(d) Begin Querying in " + ActiveConfiguration.queryMode + " Retrieval Mode");
				System.out.println("(e) Stem a Token [:stem] ");
				System.out.println("(f) Normalize a Token ");
				System.out.println("(g) View Top 1000 Vocabulary Terms [:vocab] ");
				System.out.println("(h) View Top 1000 Index Postings");
				System.out.println("(i) View Corpus Overview ");
				System.out.println("(j) View a Document ");
				System.out.println("(k) Quit Application [:q]");

				String selection = in.nextLine();

				switch (selection) {
					case "a": {
						// reset the current corpusPath by asking user to choose a new one
						ActiveConfiguration.corpus = selectCorpusMenu();
						// then create a new index on the new corpus
						ActiveConfiguration.index = selectIndexMenu(ActiveConfiguration.corpus);
						break;
					}
					case "b": {
						ActiveConfiguration.queryMode = selectQueryModeMenu();
						break;
					}
					case "c": {
						selectRunModeMenu();
						break;
					}
					// display index selection menu again
					case "d": {
						processQuery(ActiveConfiguration.corpus, ActiveConfiguration.index);
						break;
					}
					case "e": {
						viewStemmedToken();
						break;
					}
					case "f": {
						viewNormalizedToken();
						break;
					}
					case "g": {
						viewVocabulary(ActiveConfiguration.index);
						break;
					}
					case "h": {
						viewPostings(ActiveConfiguration.index);
						break;
					}
					case "i": {
						viewCorpusOverview(ActiveConfiguration.corpus);
						break;
					}
					case "j": {
						viewDocument(ActiveConfiguration.corpus, ActiveConfiguration.index);
						//System.out.println("Returning to main menu...");
						break;
					}
					case "k": {
						ActiveConfiguration.setRunMode(Quit);
						break;
					}
					default: {
						System.out.println("Input Error: Please try again and select one of the options listed. ");
						break;
					}
				}
			}
		}

//	private static DiskPositionalIndex buildDiskIndex(DocumentCorpus corpus) {
//
//			// before building a new disk index, check if one already exists in the corpus directory
//			String indexDir = corpus.getPath();
//			DiskIndexDAO dao = new DiskIndexDAO(indexDir);
//			DiskPositionalIndex diskIndex = new DiskPositionalIndex(corpus);
//
//			// check if current on-disk data exists to decide to initialize or load
//			if (dao.hasExistingIndex()) {
//				// if on-disk data exists, laod the vocabulary into this object
//				diskIndex.loadVocabulary(corpus);
//			}
//			// if no on-disk data exists, we need to initialize the in memory index and write it to disk
//			else {
//				// everything is done inside the single method, so we set the timers out here before and after calling it
//				long start = System.currentTimeMillis();
//				diskIndex.initializeInMemoryIndex(corpus);
//				long stop = System.currentTimeMillis();
//				double elapsedSeconds = (stop - start) / 1000.0;
//				System.out.println("Writing index to disk completed in " + elapsedSeconds + " seconds.");
//			}
//			return diskIndex;
//		}

	// builds an index (any implementation of the Index interface) using a Document Corpus
	private static Index buildIndex(IndexType type) {
		Scanner in = new Scanner(System.in);

		if (ActiveConfiguration.index != null && ActiveConfiguration.indexType == type) {
			System.out.println("Warning: An index of the type '" + type + "' already exists for the current corpus - Building a new index will overwrite the existing index and wipe its data. \n Would you like to continue building the index or use the existing index instead? (y/n)");

			if (!Objects.equals(in.nextLine(), "y")) {
				System.out.println("Ok - the current build process will be terminated and the exising index will be used instead. ");
				return ActiveConfiguration.index;
			}

			else {
				System.out.println("Ok - the existing index will be wiped from memory while the new index is built. \n (Note that any on-disk data for the existing index will still be preserved on disk regardless.");
			}
		}

		System.out.println("Building a new " + type.toString() + "Index from the corpus directory \n'" + ActiveConfiguration.corpus.getPath() + "'\n This may take a minute...");

		// start timer
		long start = System.currentTimeMillis();
		Index activeIndex = null;
		// initialize an empty vocab list for the constructors that need it
		List<String> vocabulary = new ArrayList<>();

		// use the given type to instantiate the appropraite index over the active corpus
		switch (type) {
			case Inverted: {
				activeIndex = new InvertedIndex(vocabulary);
				break;
			}
			case TermDocument: {
				activeIndex = new TermDocumentIndex(vocabulary, ActiveConfiguration.corpus.getCorpusSize());
				break;
			}
			case PositionalInverted: {
				activeIndex = new PositionalInvertedIndex(vocabulary);
				break;
			}
			case DiskPositional: {
				// for a DiskPositionalIndex only, skip past all of the token processing/vocabulary building.
				DiskPositionalIndex diskIndex = new DiskPositionalIndex(ActiveConfiguration.corpus);

				// Instead, just call initialize() which does the equivalent work but by reading the on-disk data instead
				diskIndex.initializeInMemoryIndex(ActiveConfiguration.corpus);
				activeIndex = diskIndex;
				break;
			}
			default: {
				System.out.println("Could not instantiate an index of the type '" + type + "' because it has not been implemented yet. Please try again with one of the options listed. ");
				break;
			}
		}

		// use a basic processor by default
		TokenProcessor processor = new BasicTokenProcessor();
		// only upgrade the processor for positional indexes
		if (type == PositionalInverted || type == DiskPositional) {
			processor = new AdvancedTokenProcessor();
		}

		// only proceed with the next steps (tokenizing the corpus and building the vocabulary) for types other than the DiskPositionalIndex
		if (type != DiskPositional) {
			// add all the docs in the corpus to the index
			for (Document d : ActiveConfiguration.corpus.getDocuments()) {
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

						// check for PositionalInvertedIndex before adding terms because it handles the method differently than the others
						if (type == PositionalInverted) {
							// add the term(s) to the index based off its type
							activeIndex.addTerm(term, d.getId(), position);
						} else {
							activeIndex.addTerm(term, d.getId());
						}
					}
					// only increment the position when moving on to a new token
					// if normalizing a token produces more than one term, they will all be posted at the same position
					position += 1;
				}
			}
		}
		long stop = System.currentTimeMillis();
		double elapsedSeconds = (stop - start) / 1000.0;
		System.out.println("Indexing completed in approximately " + elapsedSeconds + " seconds.");
		return activeIndex;
		}

	private static void processQuery(DocumentCorpus corpus, Index index) {
		Scanner in = new Scanner(System.in);
		String choice = "y";

		TokenProcessor processor = new BasicTokenProcessor();
		QueryParser parser = null;
		if (ActiveConfiguration.queryMode == Boolean) {
			parser = new BooleanQueryParser();
			processor = new HyphenTokenProcessor();
		} else if (ActiveConfiguration.queryMode == Ranked) {
			parser = new RankedQueryParser();
			processor = new AdvancedTokenProcessor();
		}

		while (!choice.equals("n")) {
			System.out.println("Please enter your search query: ");
			String query = in.nextLine();
			List<Posting> queryPostings = new ArrayList<>();
			QueryComponent fullQuery = parser.parseQuery(query);
				// sort the list of postings and print results
			queryPostings = fullQuery.getPostings(processor, index);
				if (queryPostings == null || queryPostings.contains(null) || queryPostings.size() < 1) {

					System.out.println("No documents were found containing the query '" + query + "'");
				}

				else {
					queryPostings.sort(Comparator.comparingInt(Posting::getDocumentId));
					viewQueryResults(queryPostings, corpus, query);

					System.out.println("View a document? (y/n) ");
					String docChoice = in.nextLine();
					if (Objects.equals(docChoice, "y")) {
						viewDocument(corpus, index);
					}

				}
				System.out.println("Perform another query? (y/n)");
				choice = in.nextLine();
			}
		System.out.println("Returning to main menu...");
	}

	private static void viewQueryResults(List<Posting> results, DocumentCorpus corpus, String query) {
		// initialize counter to keep track of total number of documents the query was found in

		try {
			System.out.println("********************************BEGIN QUERY RESULTS********************************");

			int count = 1;
			for (Posting p : results) {
				System.out.println(count + ") Title: '" + corpus.getDocument(p.getDocumentId()).getTitle() + "' ");
				System.out.println("    - DocId: " + p.getDocumentId());
				System.out.println("    - Query Term Positions: " + p.getTermPositions().toString());
				if (count != results.size()) {
					System.out.println();
				}
				count += 1;
			}
		} catch (NullPointerException ex) {
			System.out.println("Query failed. The corpus does not contain any documents matching the query '" + query + "'");
		}
		System.out.print("**********************************END QUERY RESULTS**********************************\n");

		System.out.println();
		System.out.println("Number of documents queried: " + corpus.getCorpusSize());
		System.out.println("Number of matches found: " + results.size());
		System.out.println();
	}

	// tests the postings of the index by printing out terms and their postings in increments of 1000
	private static void viewPostings(Index index) {
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
			} else {
				System.out.println("View the next 1000 index postings? (y/n)");
				choice = in.nextLine();
			}
		}
	}

	// tests the vocabulary of the index by printing out terms in increments of 1000
	private static void viewVocabulary(Index index) {
		System.out.println("Sorting vocabulary...");
		Collections.sort(index.getVocabulary());

		int termsCount = index.getVocabulary().size();

		Scanner in = new Scanner(System.in);
		// show the first 1000 terms in the vocabulary
		System.out.println("The first 1000 Terms index are shown below: \n");
		System.out.println("****************BEGIN VOCABULARY****************");

		//TODO: uncomment this when done testing total terms
//		String choice = "y";
//		int endIndex = 0;
//		int startIndex = 0;
//
//		while (!Objects.equals(choice, "n")) {
//			// set the new starting index to the end index of the last iteration
//			startIndex = endIndex;
//			// set the new ending index to the minimum of either 1000 terms after the start index or the size of the index's vocabulary
//			endIndex = Math.min((startIndex + 1000), (index.getVocabulary().size()));
//
//			int i;
//			for (i = startIndex; i < endIndex; i++) {
//				String term = index.getVocabulary().get(i);
//				System.out.println(term);
//			}
//
//			// stop if reaching the end of the vocabulary's size
//			if (i >= index.getVocabulary().size() - 1) {
//				System.out.println("********************END VOCABULARY*******************\n");
//				System.out.println("Total number of terms in vocabulary: " + termsCount);
//				System.out.println("No terms remaining in vocabulary. ");
//				choice = "n";
//			} else {
//				System.out.println("View the next 1000 vocabulary terms? (y/n)");
//				choice = in.nextLine();
//			}
		int count = 0;
		for (String t : index.getVocabulary()) {
			if (count >= 1000) {
				break;
			}
			System.out.println(t);
			System.out.println("Returning to main menu...\n");
			count += 1;
		}
		System.out.println("Total number of terms in vocabulary: " + index.getVocabulary().size());

	}

	// tests the stemming of a single provided token by returning its stemmed term(s)
	private static void viewStemmedToken() {
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
	private static void viewNormalizedToken() {
		Scanner in = new Scanner(System.in);
		String choice = "y";
		TokenProcessor processor = new BasicTokenProcessor();


		while (!Objects.equals(choice, "n")) {
			System.out.println("Please select the type of Processor you would like to use for normalization: ");
			System.out.println("(a) Basic Token Processor");
			System.out.println("(a) Hyphen Token Processor (no split on hyphenated words) ");
			System.out.println("(c) Advanced Token Processor (splits and separates hyphenated words) ");
			String typeChoice = in.nextLine();
			String type = "";
			switch (typeChoice) {
				case "a": {
					processor = new BasicTokenProcessor();
					type = "Basic Token Processor";
					break;
				}
				case "b": {
					processor = new HyphenTokenProcessor();
					type = "Hyphen Token Processor";
					break;
				}
				case "c": {
					processor = new AdvancedTokenProcessor();
					type = "Advanced Token Processor";
					break;
				}
				default: {
					System.out.println("Error: User input does not match any of of the available options. ");
					break;
				}
			}
			System.out.println("Please enter the token to normalize: ");
			String token = in.nextLine();

			//AdvancedTokenProcessor processor = new AdvancedTokenProcessor();
			List<String> terms = processor.processToken(token);
			//System.out.println("Testing which terms are returned by showProcessedTerm: " + terms);
			System.out.println("The " + type + " has processed the provided token into the following term(s)");
			for (String t : terms) {
				System.out.println(t);
			}

			System.out.println("Continue normalizing tokens? (y/n)");
			choice = in.nextLine();
		}
	}

	private static void viewDocument(DocumentCorpus corpus, Index index) {
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
//					if (stringBuilder.length() % 15 == 0) {
//						stringBuilder.append("\n");
//					}
				}
				String stringContent = stringBuilder.toString();
				System.out.println("The content of document #" + docId + " is shown below: \n");
				System.out.println("********************BEGIN CONTENT************************");
				System.out.println("\n" + stringContent + "\n");
				System.out.println("********************END CONTENT**************************" + "\n");
			} catch (Exception ex) {
				System.out.println("Document #" + docId + " has no viewable content.");
			} finally {
				System.out.println("View another document? (y/n)");
				choice = in.nextLine();
			}
		}
		//System.out.println("Returning to main menu...");
	}

	// prints out a list of every document in the corpus by showing the title of each document and the internal document ID assigned to it
	private static void viewCorpusOverview(DocumentCorpus corpus) {
		System.out.println("An overview of all document ID's and titles in the current corpus is shown below: \n ");
		System.out.println("********************BEGIN OVERVIEW***I*****************");

		for (Document d : corpus.getDocuments()) {
			System.out.println("Document ID: " + d.getId() + "");
			System.out.println("  Title: " + d.getTitle() + "\n");

		}
		System.out.println("*******************END CORPUS*********************\n");
		System.out.println("Returning to main menu...");
	}
}


