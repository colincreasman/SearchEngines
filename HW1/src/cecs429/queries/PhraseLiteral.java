package cecs429.queries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.AdvancedTokenProcessor;

/**
 * Represents a phrase literal consisting of one or more terms that must occur in sequence.
 */
public class PhraseLiteral implements QueryComponent {
	// The list of individual terms in the phrase.
	private List<String> mTerms = new ArrayList<>();
	private AdvancedTokenProcessor mProcessor;


	/**
	 * Constructs a PhraseLiteral with the given individual phrase terms.
	 */
	public PhraseLiteral(List<String> terms) {
		mProcessor = new AdvancedTokenProcessor();
		// before getting postings, each term in the list must be processed (without removing hyphens)
		for (String t : terms) {
			String processedTerm = mProcessor.processTokenWithHyphens(t);
			mTerms.add(processedTerm);
		}
	}
	
	/**
	 * Constructs a PhraseLiteral given a string with one or more individual terms separated by spaces.
	 */
	public PhraseLiteral(String terms) {
		mTerms.addAll(Arrays.asList(terms.split(" ")));
	}
	
	@Override
	public List<Posting> getPostings(Index index) {
		return null;
		// TODO: program this method. Retrieve the postings for the individual terms in the phrase,
		// and positional merge them together.
	}
	
	@Override
	public String toString() {
		return "\"" + String.join(" ", mTerms) + "\"";
	}
}
