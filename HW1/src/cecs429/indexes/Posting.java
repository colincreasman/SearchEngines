package cecs429.indexes;

import java.util.*;

/**
 * A Posting encapsulates a document ID associated a list of term positions within the document
 */
public class Posting {
	private int mDocumentId;
	private int mPositionsCount;
	private List<Integer> mTermPositions;

	/**
	 * Simple constructor for making a posting with only a documentId at the time of initialization
	 * @param documentId
	 */
	public Posting(int documentId) {
		mDocumentId = documentId;
		mTermPositions = new ArrayList<>();
	}
	/**
	 * Overloaded constructor for making a posting with only a single termPosition at the time of initialization
	 * @param documentId
	 * @param termPosition
	 */
	public Posting(int documentId, int termPosition) {
		mDocumentId = documentId;
		mTermPositions = new ArrayList<>();
		mTermPositions.add(termPosition);
	}
	/**
	 * Overloaded constructor for making a posting with only a a list of termPosition at the time of initialization
	 * @param documentId
	 * @param termPositions
	 */
	public Posting(int documentId, List<Integer> termPositions) {
		mDocumentId = documentId;
		mTermPositions = termPositions;
		//Collections.sort(mTermPositions);
	}
	
	public int getDocumentId() {
		return mDocumentId;
	}

	public List<Integer> getTermPositions() {
		return mTermPositions;
	}


	public void addTermPosition (int position) {
		mTermPositions.add(position);
		Collections.sort(mTermPositions);
	}

	// given two Separate postings objects with the same docID's, returns a single Posting instance that maps their common docId to an aggregate list of  all the term positions from both original Postings (without duplicates)
	public Posting merge(Posting b) {
		///List<Integer> results = new	ArrayList<>();
		// return the original posting if the docId's are not the same
		if  (this.mDocumentId != b.mDocumentId) {
		//	System.out.println("Error: The term positions from the provided postings cannot be merged because they do not map to the same docID. Returning the original (unmerged) Posting\n" );
			return this;
		}
		else {
			// create a HashSet that will store all non-duplicated term positions from both postings
			HashSet<Integer> aggregate = new HashSet<>();
			// add all term positiongs to the set
			aggregate.addAll(this.mTermPositions);
			aggregate.addAll(b.mTermPositions);
			List<Integer> sharedPositions = new ArrayList<>();

			// now convert back into a list to allow Posting instantiation
			sharedPositions.addAll(aggregate);

			Posting results = new Posting(b.mDocumentId, sharedPositions);
			return results;
		}
	}

	// wraps a single posting string as "docId:[pos1,pos2,...,etc.]"
	@Override
	public String toString() {
			return mDocumentId + ":" +mTermPositions.toString();
	}
}
