package edu.csulb;
import cecs429.documents.DirectoryCorpus;
import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.evaluation.EvaluatedQuery;
import cecs429.evaluation.Evaluator;
import cecs429.indexes.*;
import cecs429.queries.*;
import cecs429.text.*;
import cecs429.weights.*;
import com.sun.security.jgss.GSSUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.*;

import static edu.csulb.Driver.ActiveConfiguration.*;
import static edu.csulb.Driver.RunMode.*;
import static edu.csulb.Driver.QueryMode.*;
import static edu.csulb.Driver.WeighingScheme.*;
import static edu.csulb.Driver.IndexType.*;


public class Driver {
	/**
	 * enum wrapper for all the types of currently supported querying modes; can be extended to allow additional modes in the future
	 */
	public enum QueryMode {
		BOOLEAN,
		RANKED
	}
	/**
	 * enum wrapper for all the currently supported Run modes; can be extended to allow additional modes in the future
	 */
	public enum RunMode {
		BUILD,
		QUERY,
		EVALUATE,
		QUIT
	}

	//enum wrapper for all the currently supported activeIndex types; can be extended to allow additional modes in
	// the future
	public enum IndexType {
		INVERTED,
		TERM_DOCUMENT,
		POSITIONAL_INVERTED,
		DISK_POSITIONAL
	}

	// enum wrapper for all the currently supported weighting schemes for ranked retrievals; can be extended to allow
	// additional modes in the future
	public enum WeighingScheme {
		DEFAULT {
			//			private boolean isActive = false;
			@Override
			public DefaultWeigher getInstance() {
//				isActive = true;
				return new DefaultWeigher();
			}
		},
		TF_IDF {
			@Override
			public TfIdfWeigher getInstance() {
				return new TfIdfWeigher();
			}
		},
		OKAPI {
			@Override
			public OkapiWeigher getInstance() {
				return new OkapiWeigher();
			}
		},
		WACKY {
			@Override
			public WackyWeigher getInstance() {
				return new WackyWeigher();
			}
		};
		public abstract WeighingStrategy getInstance();
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
		public static DocumentCorpus activeCorpus;
		public static Index activeIndex;
		public static RunMode runMode;
		public static QueryMode queryMode;
		public static WeighingScheme activeWeighingScheme;
		public static IndexType indexType;
		public static DiskIndexDAO indexDao;
		public static boolean hasDiskIndex; // boolean flag indicating the presence of on-disk activeIndex data
		private static ActiveConfiguration instance; // singleton instance

		private ActiveConfiguration() {
			if (indexDao != null) {
				hasDiskIndex = indexDao.hasExistingIndex();
			}
			activeWeighingScheme = DEFAULT;
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

		// instantiate global config instance
		public static final ActiveConfiguration config = getInstance();

		public static void setRunMode(RunMode runMode) {
			if (runMode == QUIT) {
				System.out.println("Quitting the application - Goodbye!");
				System.exit(0);
			}
			else {
				runMode = runMode;
			}
		}

		public static void setActiveIndex(Index activeIndex) {
			ActiveConfiguration.activeIndex = activeIndex;
		}

		public static void setActiveCorpus(DocumentCorpus docCorpus) {
			// whenever a new active activeCorpus is assigned, use its path to create the active indexDao
			activeCorpus = docCorpus;

			indexDao = new DiskIndexDAO(docCorpus.getPath());

			// now use the new indexDao to determine if there is an on-disk activeIndex for the given activeCorpus
			hasDiskIndex = indexDao.hasExistingIndex();
		}

		public static void setQueryMode(QueryMode queryMode) {
			ActiveConfiguration.queryMode = queryMode;
		}

		public static void setWeightingScheme(WeighingScheme scheme) {
			activeWeighingScheme = scheme;}

		public static void setIndexType(IndexType indexType) {
			ActiveConfiguration.indexType = indexType;
		}

		public static void setindexWriter(DiskIndexDAO indexDao) {
			ActiveConfiguration.indexDao = indexDao;
		}

		public static void setHasDiskIndex(boolean hasDiskIndex) {
			ActiveConfiguration.hasDiskIndex = hasDiskIndex;
		}
	}

	public static void main(String[] args) throws IOException {
		while (runMode != QUIT) {
			selectRunModeMenu();
		}
	}

	private static void selectRunModeMenu() throws IOException {
		Scanner in = new Scanner(System.in);

		while (true) {
			System.out.println("\nPlease select a run mode from the options below: ");
			System.out.println("*************************************************");
			System.out.println("(1) Build Mode - Build a new index ");
			System.out.println("(2) Query Mode - Query an existing index ");
//			System.out.println("(3) Evaluate Mode -  Evaluate the precision and recall of ranked retrievals ");
			System.out.println("(3) Quit Application");
			int choice = in.nextInt();

			switch (choice) {
				case 1: {
					runMode = BUILD;
					setActiveCorpus(selectCorpusMenu());
					// once the activeCorpus is selected, get the user's index selection over the chosen corpus
					activeIndex = selectIndexMenu();
					queryMode = selectQueryModeMenu();
					mainMenu();
					break;
				}

				case 2: {
					// get user's chosen activeCorpus before checking if activeIndex exists
					setActiveCorpus(selectCorpusMenu());


					//  check for on-disk activeIndex first
					if (hasDiskIndex) {
						System.out.println("An existing index was found written to disk in the corpus directory '" + activeCorpus.getPath() + "'\n");
						System.out.println("Would you like to continue to Query Mode using this index? (y/n) ");
						String choice2 = in.nextLine();

						if (Objects.equals(in.nextLine(), "y")) {
							System.out.println("Ok - An in-memory index must be built from the on-disk data before querying...");
							activeIndex = buildIndex(DISK_POSITIONAL);
							System.out.println("Entering QUERY Mode...");
							runMode = QUERY;
							queryMode = selectQueryModeMenu();
							mainMenu();
							break;
						}

						else {
							System.out.println("Ok, disregarding the on-disk index...");
							activeIndex = selectIndexMenu();

						}
					}

					else {
						System.out.println("No existing index was found on disk for the selected corpus. You must switch to another corpus with an existing on-disk index or build a new index altogether before entering Query Mode. \n");
						runMode = BUILD;
					}

					// now check for in-mem activeIndex
					if (activeIndex == null) {
						System.out.println("No existing index was found in memory for the selected corpus. \n Checking for an on-disk activeIndex...");
					}

					else {
						System.out.println("An existing index was found in memory for the activeCorpus directory '" + activeCorpus.getPath() + "'\n");
						System.out.println("Would you like to continue to Query Mode with the existing index? (y/n) ");

						if (Objects.equals(in.nextLine(), "y")) {
							System.out.println("Redirecting to Query Mode...");
							runMode = QUERY;
							queryMode = selectQueryModeMenu();
							mainMenu();
						}

						else {
							System.out.println("Ok - Redirecting to BUILD Mode...");
							runMode = BUILD;
						}
					}
					break;
				}

//				case 3: {
//					//TODO: immplement this!
//					setRunMode(EVALUATE); // the new Evaluate run mode allows the user to evaluate the performance of the search engine's  ranked queries and compare them across various weighing strategies
//					evaluateQuery();
//					break;
//				}

				case 3: {
					setRunMode(QUIT);
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

		// ask the user for activeCorpus directory
		System.out.println("Please select a corpus from the options below: ");
		System.out.println("**********************************************");
		System.out.println("(1) National Parks Websites");
		System.out.println("(2) Moby Dick - First 10 Chapters");
		System.out.println("(3) Test Corpus - Json Files");
		System.out.println("(4) Test Corpus - Txt Files");
		System.out.println("(5) Cranfield Corpus ");
		System.out.println("(6) Custom File Path");

		// setup starter path prefix
		String corpusPath = "/Users/colincreasman/Documents/GitHub/SearchEngines/Corpora/";
		Path path = Paths.get(corpusPath);
		//DocumentCorpus activeCorpus;
		// use .txt as the default extension
		String ext = ".txt";

		int selection = in.nextInt();

		switch (selection) {
			case 1: {
				corpusPath += "all-nps-sites-extracted";
				path = Paths.get(corpusPath).toAbsolutePath();
				// only update the ext string when not using .txt files
				ext = ".json";
				break;
			}
			case 2: {
				corpusPath += "MobyDick10Chapters";
				path = Paths.get(corpusPath).toAbsolutePath();
				ext = ".txt";
				break;
			}
			case 3: {
				corpusPath += "test-corpus-json";
				path = Paths.get(corpusPath).toAbsolutePath();
				ext = ".json";
				break;
			}
			case 4: {
				corpusPath += "test-corpus-txt";
				path = Paths.get(corpusPath).toAbsolutePath();
				ext = ".txt";
				break;
			}
			case 5: {
				corpusPath += "relevance_cranfield";
				path = Paths.get(corpusPath).toAbsolutePath();
				ext = ".json";
				break;
			}
			case 6: {
				System.out.println("Please enter the custom  path for your activeCorpus directory: \n");
				corpusPath = in.nextLine();
				path = Paths.get(corpusPath).toAbsolutePath();
				// flag ext to an empty string so we know to load either ext type
				ext = "";
				break;
			}
			default: {
				System.out.println("Error: User input does not match any of of the available options. ");
				return selectCorpusMenu();
			}
		}

		// now return the activeCorpus after loading files into it according to the final ext value
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

	private static Index selectIndexMenu() throws IOException {

		while (true) {
			Scanner in = new Scanner(System.in);
			System.out.println("Please select the type of index you would like to build for your corpus: ");
			System.out.println("**************************************************");
			System.out.println("(1) Inverted Index ");
			System.out.println("(2) Term Document Index ");
			System.out.println("(3) Positional Inverted Index ");
			System.out.println("(4) Disk Positional Index ");

			int selection = in.nextInt();
			// if the selection matches one of the number options provided, we should be able to just directly assign the activeIndex type via enum ID
			try {
				// decrement by 1 since the enums start at 0
				indexType = IndexType.values()[selection - 1];
				break;
			}

			// any exception will be due to a user input that doesn't match one of the available options, so keep looping them through the menu until they do
			catch (InputMismatchException ex) {
				System.out.println("Error: User input does not match any of of the available options. " + ex);
			}
		}
		return buildIndex(indexType);
	}

	private static QueryMode selectQueryModeMenu() {
		Scanner in = new Scanner(System.in);

		while (true) {
			System.out.println("\nPlease select a querying mode from the options below: ");
			System.out.println("*******************************************************************");
			int count = 0;
			for (QueryMode mode : QueryMode.values())  {
				count += 1;
				System.out.println("(" + count + ")" + mode.toString());
			}
			int choice = in.nextInt();

			// if ranked retrieval is chosen, have them chose one of the available weighting schemes
			if (choice == 2) {
				activeWeighingScheme = selectWeightMenu();
			}

			try {
				return QueryMode.values()[choice - 1];
			}

			catch (Exception ex) {
				System.out.println("Error: User input does not match any of of the available options. " + ex + "\n Please try again with one of the options listed. ");
				return null;
			}
		}
	}

	private static WeighingScheme selectWeightMenu() {
		Scanner in = new Scanner(System.in);

		while (true) {
			System.out.println("\nPlease select a weighting scheme for ranked retrieval from options below: ");
			System.out.println("******************************************************************************");
			int count = 0;
			for (WeighingScheme weightingScheme : WeighingScheme.values()) {
				count += 1;
				System.out.println("(" + count + ")" + weightingScheme.toString());
			}
			int choice = in.nextInt();

			try {
				return WeighingScheme.values()[choice - 1];
			}

			catch (Exception ex) {
				System.out.println("Error: User input does not match any of of the available options. " + ex + "\n Please try again with one of the options listed. ");
				return null;
			}
		}
	}

	// driver method to route user selections from the main menu
	private static void mainMenu() throws IOException {
		Scanner in = new Scanner(System.in);

		// loop until user wants to quit
		while (true) {

			System.out.println("\nPlease select an action from the options below: ");
			System.out.println("*************************************************");
			System.out.println("(a) Begin Querying in " + queryMode + " Retrieval Mode");
			System.out.println("(b) Change Corpus/Index [:index] ");
			System.out.println("(c) Change Query Mode ");
			System.out.println("(d) Change Weighting Scheme (Ranked Retrieval Only) ");
			System.out.println("(e) Change Run Mode ");
			System.out.println("(f) Stem a Token [:stem] ");
			System.out.println("(g) Normalize a Token ");
			System.out.println("(h) View Top 1000 Vocabulary Terms [:vocab] ");
			System.out.println("(i) View Top 1000 Index Postings");
			System.out.println("(j) View Corpus Overview ");
			System.out.println("(k) View a Document ");
			System.out.println("(l) Run query evaluator ");
			System.out.println("(m) Quit Application [:q]");

			String selection = in.nextLine();

			switch (selection) {
				case "a": {
					processQuery();
					break;
				}
				case "b": {
					// reset the current corpusPath by asking user to choose a new one
					activeCorpus = selectCorpusMenu();
					// then create a new activeIndex on the new activeCorpus
					activeIndex = selectIndexMenu();
					break;
				}
				case "c": {
					queryMode = selectQueryModeMenu();
					break;
				}
				case "d": {
					activeWeighingScheme = selectWeightMenu();
				}
				case "e": {
					selectRunModeMenu();
					break;
				}
				case "f": {
					viewStemmedToken();
					break;
				}
				case "g": {
					viewNormalizedToken();
					break;
				}
				case "h": {
					viewVocabulary();
					break;
				}
				case "i": {
					viewPostings();
					break;
				}
				case "j": {
					viewCorpusOverview();
					break;
				}
				case "k": {
					viewDocument();
					//System.out.println("Returning to main menu...");
					break;
				}
				case "l": {
					setRunMode(EVALUATE);
					evaluateQueryMenu();
				}
				case "m": {
					setRunMode(QUIT);
					break;
				}
				default: {
					System.out.println("Input Error: Please try again and select one of the options listed. ");
					break;
				}
			}
		}
	}

	// builds an activeIndex (any implementation of the Index interface) using a Document Corpus
	private static Index buildIndex(IndexType type) throws IOException {
		Scanner in = new Scanner(System.in);

		if (activeIndex != null && indexType == type) {
			System.out.println("**** Warning **** An index of the type '" + type + "' already exists in memory for the current corpus - Building a new index will overwrite any existing index in memory. ");


			System.out.println("\n Would you like to use the current index in-memory ('y') or build a new index to replace it ('n') ? ");

			if (!Objects.equals(in.nextLine(), "n")) {
				System.out.println("Ok - the current build process will be terminated and the existing index will be used instead. ");
				return activeIndex;
			}

			else {
				System.out.println("Ok - The current index in-memory will be overwritten to build the new index. \n (Note that any existing on-disk data for the index will still be preserved.");
			}
		}

		System.out.println("Building an in-memory " + type.toString() + "Index for the corpus directory:  \n'" + activeCorpus.getPath());
		System.out.println("Building an in-memory " + type.toString() + "Index for the corpus directory:  \n'" + activeCorpus.getPath());
		System.out.println("This may take a minute... \n");

		// start timer
		long start = System.currentTimeMillis();
		Index activeIndex = null;
		List<String> vocabulary = new ArrayList<>();

		// use the given type to instantiate the appropraite activeIndex over the active activeCorpus
		switch (type) {
			case INVERTED: {
				activeIndex = new InvertedIndex(vocabulary);
				break;
			}
			case TERM_DOCUMENT: {
				activeIndex = new TermDocumentIndex(vocabulary, activeCorpus.getCorpusSize());
				break;
			}
			case POSITIONAL_INVERTED: {
				activeIndex = new PositionalInvertedIndex(vocabulary);
				break;
			}
			case DISK_POSITIONAL: {
				// for a DiskPositionalIndex only, skip past all of the token processing/vocabulary building.
				DiskPositionalIndex diskIndex = new DiskPositionalIndex(activeCorpus);

				// Instead, just call initialize() which does the equivalent work but by reading the on-disk data instead
				// but first check if the activeIndex is already written to disk for this activeCorpus
				if (hasDiskIndex) {
					if (runMode == BUILD) {
						System.out.println("There is already an existing index written to disk in the current corpus directory. ");
						System.out.println("  -- Would you like to overwrite the existing on-disk data while building the new index? \n ");
						System.out.println("**** WARNING **** \n Choosing this option will require significantly more indexing time because a new in-memory index must be built from scratch and written to disk before continuing the build process ");

						System.out.println("\n Please enter 'y' to overwrite the existing index or 'n' to continue building with on-disk data: ");
						// check if the user wants to use the existing data or overwrite it
						if (Objects.equals(in.nextLine(), "y")) {
							System.out.println("**** WARNING **** \n - ALL INDEX DATA WILL BE PERMANENTLY WIPED FROM THE DISK! \n Please confirm that this is what you want to do entering 'y' again: \n ");
							String confirm = in.nextLine();
							if (Objects.equals(confirm, "y")) {
								System.out.println("Overwrite confirmed - The current on-disk index will be wiped and overwritten during initialization of the new index ");
								diskIndex.initializeInMemoryIndex();
								activeIndex = diskIndex;
							} else {
								System.out.println("Overwrite canceled - Continuing the build process using the existing index data found on disk... \n ");
								diskIndex.load();
							}
						} else {
							System.out.println("Ok - Continuing the build process using the existing index data found on disk... \n ");
							diskIndex.load();
							activeIndex = diskIndex;
						}
					}
					else {
						diskIndex.load();
						activeIndex = diskIndex;
					}
				}

				// if no on-disk activeIndex is found, initialize a new in memory and write it to disk
				else {
					System.out.println("No exising index data was found in the current corpus directory. \n " +
							"A new index will be built in memory and written to disk in the current corpus directory...\n");
					diskIndex.initializeInMemoryIndex();
					activeIndex = diskIndex;
				}
				break;
			}
			default: {
				System.out.println("Error: Could not build the index of type '" + type + "' because it has not been implemented yet. Please try again with a different index type. ");
				activeIndex = selectIndexMenu();
				break;
			}
		}

		// use a basic processor by default
		TokenProcessor processor = new BasicTokenProcessor();
		// only upgrade the processor for positional indexes
		if (type == POSITIONAL_INVERTED || type == DISK_POSITIONAL) {
			processor = new AdvancedTokenProcessor();
		}

		// only proceed with the next steps (tokenizing the activeCorpus and building the vocabulary) for types other than the DiskPositionalIndex
		if (type != DISK_POSITIONAL) {
			// add all the docs in the activeCorpus to the activeIndex
			for (Document d : activeCorpus.getDocuments()) {
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
						if (type == POSITIONAL_INVERTED) {
							// add the term(s) to the activeIndex based off its type
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
		System.out.println("Indexing completed in approximately " + elapsedSeconds + " seconds. \n");
		return activeIndex;
	}

	private static void processQuery() {
		Scanner in = new Scanner(System.in);
		String choice = "y";

		TokenProcessor processor = new BasicTokenProcessor();
		QueryParser parser = null;

		if (queryMode == BOOLEAN) {
			parser = new BooleanQueryParser();
			processor = new HyphenTokenProcessor();
		}
		else if (queryMode == RANKED) {
			parser = new RankedQueryParser();
			processor = new AdvancedTokenProcessor();
		}

		while (!choice.equals("n")) {
			System.out.println("Please enter your search query: ");
			String query = in.nextLine();
			List<Posting> queryPostings = new ArrayList<>();
			QueryComponent fullQuery = parser.parseQuery(query);
			// sort the list of postings and print results
			if (queryMode == BOOLEAN) {
				queryPostings = fullQuery.getPostings(processor, activeIndex);
			}
			else if (queryMode == RANKED){
				queryPostings = fullQuery.getPostingsWithoutPositions(processor, activeIndex);
			}

			if (queryPostings == null || queryPostings.contains(null) || queryPostings.size() < 1) {

				System.out.println("No documents were found containing the query '" + query + "'");
			}

			else {
				//queryPostings.sort(Comparator.comparingInt(Posting::getDocumentId));
				viewQueryResults(queryPostings, activeCorpus, fullQuery);

				System.out.println("View a document? (y/n) ");
				String docChoice = in.nextLine();
				if (Objects.equals(docChoice, "y")) {
					viewDocument();
				}

			}
			System.out.println("Perform another query? (y/n)");
			choice = in.nextLine();
		}
		System.out.println("Returning to main menu...");
	}

	private static void evaluateQueryMenu() throws IOException {
		Scanner in = new Scanner(System.in);
		Evaluator evaluator = new Evaluator();
		TokenProcessor processor = new AdvancedTokenProcessor();
		QueryParser parser = new RankedQueryParser();

		while (true) {
			System.out.println("\nPlease select an evaluation from the options below: ");
			System.out.println("*******************************************************************");
			System.out.println("(1) Ranked Retrieval Results with Relevance ");
			System.out.println("(2) Average Precision by Weighing Scheme ");
			System.out.println("(3) Mean Average Precision by Weighing Scheme ");
			System.out.println("(4) Throughput by Iteration Count ");
			System.out.println("(5) Precision Recall Graph ");
			System.out.println("(6) Return to Main Menu");

			int choice = in.nextInt();
			switch (choice) {
				case 1: {
					String queryChoice = "y";

					while (!queryChoice.equals("n")) {
						activeWeighingScheme = selectWeightMenu();
						System.out.println("Please enter the line number of the query from file to be evaluated (e.i. enter 1 to evaluate the query from the first line of the file, etc.): ");
						int queryNum = in.nextInt();

						EvaluatedQuery result = evaluator.evaluateFileQuery(queryNum);

						System.out.println("Please enter the number of ranked results to retrieve (K): ");
						int kTerms = in.nextInt();

						result.getRetrievedRelevant(kTerms);
						System.out.println("Ranked retrieval results with relevance are shown below: \n" + result);

						System.out.println("View relevance results for another query? (y/n)");
						queryChoice = in.nextLine();
					}
					break;
				}
				case 2: {
					String queryChoice = "y";

					while (!queryChoice.equals("n")) {
						activeWeighingScheme = selectWeightMenu();
						System.out.println("Please enter the line number of the query from file to be evaluated (e.i. enter 1 to evaluate the query from the first line of the file, etc.): ");
						int queryNum = in.nextInt();

						EvaluatedQuery q = evaluator.evaluateFileQuery(queryNum);

						System.out.println("Please enter the number of ranked results (K) to use for the average " +
								"precision calculation :");
						int kTerms = in.nextInt();
						double result = evaluator.calculateAvgPrecision(activeWeighingScheme, kTerms, q);

						System.out.println("The average precision results are shown below: \n" );
						System.out.println(" - Query: '" + q.getQueryString() + "' " );
						System.out.println(" - Ranked Retrieval Results: " + q);
						System.out.println("\n - Average Precision: " + result + "\n");
						System.out.println("\nView relevance results for another query? (y/n)");
						queryChoice = in.nextLine();
					}
					break;
				}
				case 3: {
					String queryChoice = "y";

					while (!queryChoice.equals("n")) {
						activeWeighingScheme = selectWeightMenu();

						System.out.println("Please enter the number of ranked results (K) to use for the Mean Average" +
								" Precision calculations over each weighing scheme: ");

						int kTerms = in.nextInt();

						System.out.println("The Mean Average Precision results for each weighing scheme are shown " +
								"below: " +
								"\n" );

//						for (WeighingScheme w : WeighingScheme.values()) {
//							w = activeWeighingScheme;
							double map = evaluator.calculateMeanAvgPrecision(activeWeighingScheme,
									kTerms);
							System.out.println("Total MAP Results Retrieved for the Weighing Scheme " + activeWeighingScheme + " over k = " + kTerms + " results: ");
//							System.out.println(" - Weighing Scheme: " + w);
							System.out.println(" - Mean Average Precision: " + map);
							///}

						System.out.println("\nView relevance results for another query? (y/n)");
						queryChoice = in.nextLine();
					}
					break;
				}
				case 4: {
					String queryChoice = "y";

					while (!queryChoice.equals("n")) {
						System.out.println("Please enter the line number of the query from file to be evaluated (e.i. enter 1 to evaluate the query from the first line of the file, etc.): ");
						int queryNum = in.nextInt();

						QueryComponent q = evaluator.readFileQuery(queryNum);

						System.out.println("Please enter the number of iterations to measure throughout over:");
						int iterations = in.nextInt();
						double result = evaluator.calculateThroughput(q, iterations);

						System.out.println("The throughput results are shown below: \n");
						System.out.println(" - Query: '" + q + "' ");
						System.out.println(" - Iterations: " + iterations);
						System.out.println(" - Mean Average Response Time: " + (1 / result));
						System.out.println(" - Throughput: " + result + " Queries/Second ");

						System.out.println("\nView throughput calculations for another query? (y/n)");
						queryChoice = in.nextLine();
					}
					break;
				}
				case 5: {
					System.out.println("Not implemented");
					break;
				}
				case 6: {
					System.out.println("Returning to main menu...");
					mainMenu();
					break;
				}
				default: {
					System.out.println("Input Error: Please try again and select one of the options listed. ");
					break;
				}
			}
		}
	}


	private static void viewQueryResults(List<Posting> results, DocumentCorpus activeCorpus, QueryComponent query) {
		// initialize counter to keep track of total number of documents the query was found in

		try {
			System.out.println("********************************BEGIN QUERY RESULTS********************************");

			int count = 1;
			for (int i = results.size() - 1; i >= 0; i--) {
				Posting p = results.get(i);
				System.out.println(count + ") Title: '" + activeCorpus.getDocument(p.getDocumentId()).getTitle() + "' ");
				System.out.println("    - DocId: " + p.getDocumentId());

				if (queryMode == RANKED) {
					System.out.println("    - Doc Weight (Ld): " + p.getDocWeight().getValue());

					System.out.println("    - Final Accumulator Value (Ad): " + p.getDocWeight().getAccumulator());

					System.out.println("    - Term Weights: ");
					for (QueryTermWeight wQt : ((RankedQuery) query).getQueryWeights()) {
//						Optional<Posting> docTermPosting = query.getTermPostings().values().stream().toList().stream().filter(posting -> posting.getDocumentId()).
//								findFirst();

						//Optional<Person> matchingObject = objects.stream().
						//    filter(p -> p.email().equals("testemail")).
						//    findFirst()
						System.out.println("         - '" + wQt.getTerm() + "': [" + wQt.getValue());
					}
				}

				else if (queryMode == BOOLEAN) {
					System.out.println("    - Query Term Positions: " + p.getTermPositions().toString());
				}

				count += 1;
			}
		}

		catch (NullPointerException ex) {
			System.out.println("Query failed. The activeCorpus does not contain any documents matching the query '" + query + "'");
		}
		System.out.print("**********************************END QUERY RESULTS**********************************\n");

		System.out.println();
		System.out.println("Number of documents queried: " + activeCorpus.getCorpusSize());
		System.out.println("Number of matches found: " + results.size());
		System.out.println();
	}

	// tests the postings of the activeIndex by printing out terms and their postings in increments of 1000
	private static void viewPostings() {
		System.out.println("Sorting vocabulary...");
		Collections.sort(activeIndex.getVocabulary());

		int termsCount = 0;
		Scanner in = new Scanner(System.in);
		// show the first 1000 terms in the vocabulary
		System.out.println("The first 1000 Terms and Postings in the activeIndex are shown below: \n");
		System.out.println("**************************BEGIN INDEX*************************");

		String choice = "y";
		int endIndex = 0;
		int startIndex = 0;

		while (!Objects.equals(choice, "n")) {
			// set the new starting activeIndex to the end activeIndex of the last iteration
			startIndex = endIndex;
			// set the new ending activeIndex to the minimum of either 1000 terms after the start activeIndex or the size of the activeIndex's vocabulary
			endIndex = Math.min((startIndex + 1000), (activeIndex.getVocabulary().size()));

			int i;
			for (i = startIndex; i < endIndex; i++) {
				termsCount += 1;

				String term = activeIndex.getVocabulary().get(i);
				System.out.println(activeIndex.viewTermPostings(term));
			}

			// stop if reaching the end of the vocabulary's size
			if (i >= activeIndex.getVocabulary().size() - 1) {
				System.out.println("**************************END INDEX*************************\n");
				System.out.println("Total number of terms with postings in activeIndex: " + termsCount);
				System.out.println("No postings remaining in activeIndex. Returning to main menu.");
				choice = "n";
			} else {
				System.out.println("View the next 1000 activeIndex postings? (y/n)");
				choice = in.nextLine();
			}
		}
	}

	// tests the vocabulary of the activeIndex by printing out terms in increments of 1000
	private static void viewVocabulary() {
		System.out.println("Sorting vocabulary...");
		Collections.sort(activeIndex.getVocabulary());

		int termsCount = activeIndex.getVocabulary().size();

		Scanner in = new Scanner(System.in);
		// show the first 1000 terms in the vocabulary
		System.out.println("The first 1000 Terms activeIndex are shown below: \n");
		System.out.println("****************BEGIN VOCABULARY****************");

		//TODO: uncomment this when done testing total terms
		String choice = "y";
		int endIndex = 0;
		int startIndex = 0;

		while (!Objects.equals(choice, "n")) {
			// set the new starting activeIndex to the end activeIndex of the last iteration
			startIndex = endIndex;
			// set the new ending activeIndex to the minimum of either 1000 terms after the start activeIndex or the size of the activeIndex's vocabulary
			List<String> terms = activeIndex.getVocabulary();
			endIndex = Math.min((startIndex + 1000), (terms.size()));

			int i;
			for (i = startIndex; i < endIndex; i++) {
				String term = activeIndex.getVocabulary().get(i);
				System.out.println(term);
			}

			// stop if reaching the end of the vocabulary's size
			if (i >= terms.size() - 1) {
				System.out.println("********************END VOCABULARY*******************\n");
				System.out.println("Total number of terms in vocabulary: " + termsCount);
				System.out.println("No terms remaining in vocabulary. ");
				choice = "n";
			} else {
				System.out.println("Total number of terms in vocabulary: " + terms.size());
				System.out.println("View the next 1000 vocabulary terms? (y/n)");
				choice = in.nextLine();
			}
		}
//		int count = 0;
//		for (String t : activeIndex.getVocabulary()) {
//			if (count >= 1000) {
//				break;
//			}
//			System.out.println(t);
//			System.out.println("Returning to main menu...\n");
//			count += 1;
//		}

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

	private static void viewDocument() {
		Scanner in = new Scanner(System.in);
		System.out.println();
		String choice = "y";
		//boolean isContinue = true;
		while (!Objects.equals(choice, "n")) {

			System.out.println("Please enter the ID of the document you'd like to view: ");
			int docId = Integer.parseInt(in.nextLine());
			BufferedReader contentReader = new BufferedReader(activeCorpus.getDocument(docId).getContent());
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

	// prints out a list of every document in the activeCorpus by showing the title of each document and the internal document ID assigned to it
	private static void viewCorpusOverview() {
		System.out.println("An overview of all document ID's and titles in the current activeCorpus is shown below: \n ");
		System.out.println("********************BEGIN OVERVIEW********************");

		for (Document d : activeCorpus.getDocuments()) {
			System.out.println("Document ID: " + d.getId() + "");
			System.out.println("  Title: " + d.getTitle() + "\n");

		}
		System.out.println("*******************END CORPUS*********************\n");
		System.out.println("Returning to main menu...");
	}

}