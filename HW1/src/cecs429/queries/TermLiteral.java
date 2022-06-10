package cecs429.queries;

import java.util.List;

import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.AdvancedTokenProcessor;

/**
 * A TermLiteral represents a single term in a subquery.
 */
public class TermLiteral implements QueryComponent {
	private String mTerm;
	private AdvancedTokenProcessor mProcessor;

	public TermLiteral(String term) {
		// the TermLiteral should represent a procesed term, not the original token, so we must first process it with a new processor
		mProcessor = new AdvancedTokenProcessor();
		// "note: do not perform the split on hyphens step on query literals; use the whole literal, including the hyphen"
		mTerm = mProcessor.processTokenWithHyphens(term);
	}
	
	public String getTerm() {
		return mTerm;
	}

	// Since an Index can already give the postings for a single term with its own getPostings method, a TermLiteral simply calls that method on the given Index.
	@Override
	public List<Posting> getPostings(Index index) {
		try {
			return index.getPostings(mTerm);
		}
		catch (NullPointerException ex) {
			System.out.println("No documents were found containing the term literal '" + mTerm + "'");
			return null;
		}


	}
	
	@Override
	public String toString() {
		return mTerm;
	}

}
