package edu.csulb;
import cecs429.documents.DirectoryCorpus;
import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.indexes.Index;
import cecs429.indexes.InvertedIndex;
import cecs429.indexes.Posting;
import cecs429.indexes.TermDocumentIndex;
import cecs429.text.BasicTokenProcessor;
import cecs429.text.EnglishTokenStream;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;

public class TermDocumentIndexer {
	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);

		// ask the user for corpus directory
		// "/Users/colincreasman/Documents/GitHub/SearchEngines/Corpora/all-nps-sites-extracted"
		System.out.println("Please enter the local path for your corpus directory: ");

		String corpusPath = input.nextLine();

		// Create a DocumentCorpus to load .json documents from the corpus directory.
		DocumentCorpus corpus = DirectoryCorpus.loadJsonDirectory(Paths.get(corpusPath), ".json");

		// Index the documents of the corpus.
		Index index = indexCorpus(corpus);

		// We aren't ready to use a full query parser; for now, we'll only support single-term queries.
		System.out.println("Enter your query term: ");
		String query = input.nextLine();

		for (Posting p : index.getPostings(query)) {
			//System.out.println("here");
			System.out.println("Document #" + p.getDocumentId() + ": " + corpus.getDocument(p.getDocumentId()).getTitle());
		}

		// prints the total number of documents found
		// works only with the INVERTED index, NOT THE POSITIONAL INDEX
		System.out.println("Total number of documents found: " + index.getPostings(query).size());

		// TODO: fix this application so the user is asked for a term to search.
	}

	private static Index indexCorpus(DocumentCorpus corpus) {
		HashSet<String> vocabulary = new HashSet<>();
		BasicTokenProcessor processor = new BasicTokenProcessor();


		InvertedIndex index = new InvertedIndex(vocabulary);
		// THEN, do the loop again! But instead of inserting into the HashSet, add terms to the index with addPosting.
		for (Document d : corpus.getDocuments()) {
			//System.out.println("here");
			// Tokenize the document's content by constructing an EnglishTokenStream around the document's content.
			EnglishTokenStream stream = new EnglishTokenStream(d.getContent());
			// Iterate through the tokens in the document, processing them using a BasicTokenProcessor,
			// add them to the TermDocumentIndex
			Iterable<String> tokens = stream.getTokens();
			for (String token : tokens) {
				BasicTokenProcessor processer = new BasicTokenProcessor();
				String processedToken = processor.processToken(token);
				index.addTerm(processedToken, d.getId());
			}
		}
		
		return index;
	}
}
