package cecs429.indexes;

import java.util.List;
import java.util.Set;

/**
 * An Index can retrieve postings for a term from a data structure associating terms and the documents
 * that contain them.
 */
public interface Index {
	/**
	 * Retrieves a list of Postings of documents that contain the given term.
	 */
	List<Posting> getPostings(String term);
	
	/**
	 * A (sorted) list of all terms in the index vocabulary.
	 */
	List<String> getVocabulary();

	String toString();
	
	public void addTerm();

	void addTerm(String term, int id, int position);

	void addTerm(String term, int id);
}
