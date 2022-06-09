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
		Collections.sort(mTermPositions);
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
}
