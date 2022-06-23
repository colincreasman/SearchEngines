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

import static edu.csulb.Driver.ActiveConfiguration.*;
import static edu.csulb.Driver.RunMode.*;
import static edu.csulb.Driver.QueryMode.*;
import static edu.csulb.Driver.WeightingScheme.*;
import static edu.csulb.Driver.IndexType.*;


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
	 * enum wrapper for all the currently supported activeIndex types; can be extended to allow additional modes in the future
	 */
	public enum IndexType {
		Inverted,
		TermDocument,
		PositionalInverted,
		DiskPositional
	}

	/**
	 * enum wrapper for all the currently supported weighting schemes for ranked retrievals; can be extended to allow additional modes in the future
	 */
	public enum WeightingScheme {
		Default,
		TF_IDF,
		Okapi,
		Wacky
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
		public static WeightingScheme weightingScheme;
		public static IndexType indexType;
		public static IndexDAO indexDao;
		public static boolean hasDiskIndex; // boolean flag indicating the presence of on-disk activeIndex data
		private static ActiveConfiguration instance; // singleton instance

		private ActiveConfiguration() {
			if (indexDao != null) {
				hasDiskIndex = indexDao.hasExistingIndex();
			}
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

		public static void setCorpus(DocumentCorpus docCorpus) {
			// whenever a new active activeCorpus is assigned, use its path to create the active indexDAO
			indexDao = new DiskIndexDAO(docCorpus.getPath());
			activeCorpus = docCorpus;
			// now use the new indexDao to determine if there is an on-disk activeIndex for the given activeCorpus
			hasDiskIndex = indexDao.hasExistingIndex();
		}
		
		public static void setRunMode(RunMode runMode) {
			if (runMode == Quit) {
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

		public static void setQueryMode(QueryMode queryMode) {
			ActiveConfiguration.queryMode = queryMode;
		}

		public static void setWeightingScheme(WeightingScheme weightingScheme) {
			ActiveConfiguration.weightingScheme = weightingScheme;
		}

		public static void setIndexType(IndexType indexType) {
			ActiveConfiguration.indexType = indexType;
		}

		public static void setIndexDao(IndexDAO indexDao) {
			ActiveConfiguration.indexDao = indexDao;
		}

		public static void setHasDiskIndex(boolean hasDiskIndex) {
			ActiveConfiguration.hasDiskIndex = hasDiskIndex;
		}
	}
	
	
	// instantiate global config instance
	public static final ActiveConfiguration config = getInstance();

	public static void main(String[] args) {
		while (runMode != Quit) {
			selectRunModeMenu();
		}
	}


	private static void selectRunModeMenu() {
		Scanner in = new Scanner(System.in);

		while (true) {
			System.out.println("\nPlease select a run mode from the options below: ");
			System.out.println("*************************************************");
			System.out.println("(a) Build Mode - Build a new index ");
			System.out.println("(b) Query Mode - Query an existing index ");
			System.out.println("(c) Quit Application");
			String choice = in.nextLine();

			switch (choice) {
				case "a": {
					runMode = Build;
					setCorpus(selectCorpusMenu());
					// once the activeCorpus is selected, get the user's activeIndex selection over the chosen activeCorpus
					activeIndex = selectIndexMenu();
					queryMode = selectQueryModeMenu();
					mainMenu();
					break;
				}

				case "b": {
					// get user's chosen activeCorpus before checking if activeIndex exists
					activeCorpus = selectCorpusMenu();

					//  check for on-disk activeIndex first
					if (hasDiskIndex) {
						System.out.println("An existing activeIndex was found written to disk in the activeCorpus directory '" + activeCorpus.getPath() + "'\n");
						System.out.println("Would you like to continue to Query Mode using this activeIndex? (y/n) ");

						if (Objects.equals(in.nextLine(), "y")) {
							System.out.println("Redirecting to Query Mode...");
							runMode = Query;
							break;
						}

						else {
							System.out.println("Ok, disregarding the on-disk activeIndex...");
						}
					}

					else {
						System.out.println("No existing activeIndex was found on disk for the selected activeCorpus. You must switch to another activeCorpus with an existing activeIndex or build a new activeIndex altogether before entering Query Mode. \n");
						runMode = Build;
					}

					// now check for in-mem activeIndex
					if (activeIndex == null) {
						System.out.println("No existing activeIndex was found in memory for the selected activeCorpus. \n Checking for an on-disk activeIndex...");
					}

					else {
						System.out.println("An existing activeIndex was found in memory for the activeCorpus directory '" + activeCorpus.getPath() + "'\n");
						System.out.println("Would you like to continue to Query Mode with the found activeIndex? (y/n) ");

						if (Objects.equals(in.nextLine(), "y")) {
							System.out.println("Redirecting to Query Mode...");
							runMode = Query;
							queryMode = selectQueryModeMenu();
							mainMenu();
						}

						else {
							System.out.println("Ok - Redirecting to Build Mode...");
							runMode = Build;
						}
					}
					break;
				}

				case "c": {
					setRunMode(Quit);
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

		// ask the user for activeCorpus directory
		System.out.println("Please select a activeCorpus from the options below: ");
		System.out.println("**********************************************\n");
		System.out.println("(a) National Parks Websites");
		System.out.println("(b) Moby Dick - First 10 Chapters");
		System.out.println("(c) Test Corpus - Json Files");
		System.out.println("(d) Test Corpus - Txt Files");
		System.out.println("(e) Custom File Path");

		// setup starter path prefix
		String corpusPath = "/Users/colincreasman/Documents/GitHub/SearchEngines/Corpora/";
		Path path = Paths.get(corpusPath);
		//DocumentCorpus activeCorpus;
		// use .txt as the default extension
		String ext = ".txt";

		String selection = in.nextLine();

		switch (selection) {
			case "a": {
				corpusPath += "all-nps-sites-extracted";
				path = Paths.get(corpusPath).toAbsolutePath();
				// only update the ext string when not using .txt files
				ext = ".json";
				break;
			}
			case "b": {
				corpusPath += "MobyDick10Chapters";
				path = Paths.get(corpusPath).toAbsolutePath();

				ext = ".txt";
				break;
			}
			case "c": {
				corpusPath += "test-activeCorpus-json";
				path = Paths.get(corpusPath).toAbsolutePath();

				ext = ".json";
				break;
			}
			case "d": {
				corpusPath += "test-activeCorpus-txt";
				path = Paths.get(corpusPath).toAbsolutePath();

				ext = ".txt";
				break;
			}
			case "e": {
				System.out.println("Please enter the custom  path for your activeCorpus directory: \n");
				corpusPath = in.nextLine();
				path = Paths.get(corpusPath).toAbsolutePath();
				// flag ext to an empty string so we know to load either ext type
				ext = "";
				break;
			}
			default: {
				System.out.println("Error: User input does not match any of of the available options. ");
				break;
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

	private static Index selectIndexMenu() {

		while (true) {
			Scanner in = new Scanner(System.in);
			System.out.println("Please select the type of activeIndex you would like to build for your activeCorpus: ");
			System.out.println("******************************************\n");
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
			System.out.println("*************************************************************************************");
			int count = 0;
			for (QueryMode mode : QueryMode.values())  {
				count += 1;
				System.out.println("(" + count + ")" + mode.toString() + "\n");
			}
			int choice = in.nextInt();

			// if ranked retrieval is chosen, have them chose one of the available weighting schemes
			if (choice == 0) {
				weightingScheme = selectWeightMenu();
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

	private static WeightingScheme selectWeightMenu() {
		Scanner in = new Scanner(System.in);

		while (true) {
			System.out.println("\nPlease select a weighting scheme for ranked retrieval from options below: ");
			System.out.println("*************************************************************************************");
			int count = 0;
			for (WeightingScheme weightingScheme : WeightingScheme.values()) {
				count += 1;
				System.out.println("(" + count + ")" + weightingScheme.toString() + "\n");
			}
			int choice = in.nextInt();

			try {
				return WeightingScheme.values()[choice - 1];
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
				System.out.println("(a) Change Corpus Directory and Index [:activeIndex] ");
				System.out.println("(b) Change Query Mode ");
				System.out.println("(c) Change Weighting Scheme (Ranked Retrieval Only) ");
				System.out.println("(d) Change Run Mode ");
				System.out.println("(e) Begin Querying in " + queryMode + " Retrieval Mode");
				System.out.println("(f) Stem a Token [:stem] ");
				System.out.println("(g) Normalize a Token ");
				System.out.println("(h) View Top 1000 Vocabulary Terms [:vocab] ");
				System.out.println("(i) View Top 1000 Index Postings");
				System.out.println("(j) View Corpus Overview ");
				System.out.println("(k) View a Document ");
				System.out.println("(l) Quit Application [:q]");

				String selection = in.nextLine();

				switch (selection) {
					case "a": {
						// reset the current corpusPath by asking user to choose a new one
						activeCorpus = selectCorpusMenu();
						// then create a new activeIndex on the new activeCorpus
						activeIndex = selectIndexMenu();
						break;
					}
					case "b": {
						queryMode = selectQueryModeMenu();
						break;
					}
					case "c": {
						weightingScheme = selectWeightMenu();
					}
					case "d": {
						selectRunModeMenu();
						break;
					}
					// display activeIndex selection menu again
					case "e": {
						processQuery();
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
						setRunMode(Quit);
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
	private static Index buildIndex(IndexType type) {
		Scanner in = new Scanner(System.in);

		if (activeIndex != null && indexType == type) {
			System.out.println("**** Warning **** An activeIndex of the type '" + type + "' already exists in memory for the current activeCorpus - Building a new activeIndex will overwrite any existing activeIndex in memory. ");
			System.out.println("\n Would you like to continue building the new activeIndex ('y') or use the current activeIndex in memory ('n') ? ");

			if (!Objects.equals(in.nextLine(), "n")) {
				System.out.println("Ok - the current build process will be terminated and the existing activeIndex will be used instead. ");
				return activeIndex;
			}

			else {
				System.out.println("Ok - The current activeIndex in-memory will be overwritten to build the new activeIndex. \n (Note that any existing on-disk data for the activeIndex will still be preserved.");
			}
		}

		System.out.println("Building a new " + type.toString() + "Index from the activeCorpus directory \n'" + activeCorpus.getPath() + "'\n This may take a minute...");

		// start timer
		long start = System.currentTimeMillis();
		Index activeIndex = null;
		// initialize an empty vocab list for the constructors that need it
		List<String> vocabulary = new ArrayList<>();

		// use the given type to instantiate the appropraite activeIndex over the active activeCorpus
		switch (type) {
			case Inverted: {
				activeIndex = new InvertedIndex(vocabulary);
				break;
			}
			case TermDocument: {
				activeIndex = new TermDocumentIndex(vocabulary, activeCorpus.getCorpusSize());
				break;
			}
			case PositionalInverted: {
				activeIndex = new PositionalInvertedIndex(vocabulary);
				break;
			}
			case DiskPositional: {
				// for a DiskPositionalIndex only, skip past all of the token processing/vocabulary building.
				DiskPositionalIndex diskIndex = new DiskPositionalIndex(activeCorpus);

				// Instead, just call initialize() which does the equivalent work but by reading the on-disk data instead
				// but first check if the activeIndex is already written to disk for this activeCorpus
				if (hasDiskIndex) {
					System.out.println("There is already an existing activeIndex written to disk in the current activeCorpus directory. ");
					System.out.println("  -- Would you like to overwrite the existing on-disk data while building the new activeIndex? \n ");
					System.out.println("**** WARNING **** \n Choosing this option will require significantly more indexing time because a new in-memory activeIndex must be built from scratch and written to disk before continuing the build process \n ***************** ");
					System.out.println("\n Please enter 'y' to overwrite the existing activeIndex or 'n' to continue building with on-disk data: ");
					// check if the user wants to use the existing data or overwrite it
					if (Objects.equals(in.nextLine(), "y")) {
						System.out.println("**** WARNING **** - ALL INDEX DATA WILL BE PERMANENTLY WIPED FROM THE DISK! \n ************* \n Please confirm that this is what you want to do entering 'y' again: \n ");
						String confirm = in.nextLine();
						if (Objects.equals(confirm, "y")) {
							System.out.println("Overwrite confirmed - The current on-disk activeIndex will be wiped and overwritten during initialization of the new activeIndex ");
							diskIndex.initializeInMemoryIndex();
						}
					}

					else {
						System.out.println("Ok - Building activeIndex from existing data on disk. ");
						diskIndex.load();
					}
				}

				// if no on-disk activeIndex is found, initialize a new in memory and write it to disk
				else {
					System.out.println("No on-disk activeIndex was found in the current activeCorpus directory. \n " +
							"Building activeIndex from a new in-memory activeIndex that will be built from scratch and written to disk in the current activeCorpus directory. ");
					diskIndex.initializeInMemoryIndex();
				}
				activeIndex = diskIndex;
				break;
			}

			default: {
				System.out.println("Error: Could not build an activeIndex of the type '" + type + "' because it has not been implemented yet. Please try again with a different activeIndex type. ");
				break;
			}
		}

		// use a basic processor by default
		TokenProcessor processor = new BasicTokenProcessor();
		// only upgrade the processor for positional indexes
		if (type == PositionalInverted || type == DiskPositional) {
			processor = new AdvancedTokenProcessor();
		}

		// only proceed with the next steps (tokenizing the activeCorpus and building the vocabulary) for types other than the DiskPositionalIndex
		if (type != DiskPositional) {
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
						if (type == PositionalInverted) {
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
		System.out.println("Indexing completed in approximately " + elapsedSeconds + " seconds.");
		return activeIndex;
		}

	private static void processQuery() {
		Scanner in = new Scanner(System.in);
		String choice = "y";

		TokenProcessor processor = new BasicTokenProcessor();
		QueryParser parser = null;
		if (queryMode == Boolean) {
			parser = new BooleanQueryParser();
			processor = new HyphenTokenProcessor();
		} else if (queryMode == Ranked) {
			parser = new RankedQueryParser();
			processor = new AdvancedTokenProcessor();
		}

		while (!choice.equals("n")) {
			System.out.println("Please enter your search query: ");
			String query = in.nextLine();
			List<Posting> queryPostings = new ArrayList<>();
			QueryComponent fullQuery = parser.parseQuery(query);
				// sort the list of postings and print results
			queryPostings = fullQuery.getPostings(processor, activeIndex);
				if (queryPostings == null || queryPostings.contains(null) || queryPostings.size() < 1) {

					System.out.println("No documents were found containing the query '" + query + "'");
				}

				else {
					queryPostings.sort(Comparator.comparingInt(Posting::getDocumentId));
					viewQueryResults(queryPostings, activeCorpus, query);

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

	private static void viewQueryResults(List<Posting> results, DocumentCorpus activeCorpus, String query) {
		// initialize counter to keep track of total number of documents the query was found in

		try {
			System.out.println("********************************BEGIN QUERY RESULTS********************************");

			int count = 1;
			for (Posting p : results) {
				System.out.println(count + ") Title: '" + activeCorpus.getDocument(p.getDocumentId()).getTitle() + "' ");
				System.out.println("    - DocId: " + p.getDocumentId());
				System.out.println("    - Query Term Positions: " + p.getTermPositions().toString());
				if (count != results.size()) {
					System.out.println();
				}
				count += 1;
			}
		} catch (NullPointerException ex) {
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
			endIndex = Math.min((startIndex + 1000), (activeIndex.getVocabulary().size()));

			int i;
			for (i = startIndex; i < endIndex; i++) {
				String term = activeIndex.getVocabulary().get(i);
				System.out.println(term);
			}

			// stop if reaching the end of the vocabulary's size
			if (i >= activeIndex.getVocabulary().size() - 1) {
				System.out.println("********************END VOCABULARY*******************\n");
				System.out.println("Total number of terms in vocabulary: " + termsCount);
				System.out.println("No terms remaining in vocabulary. ");
				choice = "n";
			} else {
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
		System.out.println("Total number of terms in vocabulary: " + activeIndex.getVocabulary().size());
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
		System.out.println("********************BEGIN OVERVIEW***I*****************");

		for (Document d : activeCorpus.getDocuments()) {
			System.out.println("Document ID: " + d.getId() + "");
			System.out.println("  Title: " + d.getTitle() + "\n");

		}
		System.out.println("*******************END CORPUS*********************\n");
		System.out.println("Returning to main menu...");
	}

}

