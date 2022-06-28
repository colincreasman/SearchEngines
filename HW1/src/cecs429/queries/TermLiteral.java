package cecs429.queries;

import java.util.ArrayList;
import java.util.List;

import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.AdvancedTokenProcessor;
import cecs429.text.TokenProcessor;

/**
 * A TermLiteral represents a single term in a subquery.
 */
public class TermLiteral implements QueryComponent {
	private String mTerm;
	private AdvancedTokenProcessor mProcessor;

	public TermLiteral(String term) {
		// the TermLiteral should represent a procesed term, not the original token, so we must first process it with a new processor
		//mProcessor = processor;
		// "note: do not perform the split on hyphens step on query literals; use the whole literal, including the hyphen"
		//mTerm = mProcessor.processTokenWithHyphens(term);
		mTerm = term;
	}
	
	public String getTerm() {
		return mTerm;
	}

	// Since an Index can already give the postings for a single term with its own getPostings method, a TermLiteral simply calls that method on the given Index.
	@Override
	public List<Posting> getPostings(TokenProcessor processor, Index index) {
		try {
			List<String> processedTerms = processor.processToken(mTerm);
			List<Posting> results = new ArrayList<>();
			for (String t : processedTerms) {
				results.addAll(index.getPostings(t));
			}
			return results;
		}
		catch (NullPointerException ex) {
			System.out.println("No documents were found containing the term literal '" + mTerm + "'");
			return null;
		}
	}

	@Override
	public List<String> getProcessedTerms() {
		return null;
	}

	@Override
	public List<Posting> getPostingsWithoutPositions(TokenProcessor processor, Index activeIndex) {
		return null;
	}

	@Override
	public String toString() {
		return mTerm;
	}

}
