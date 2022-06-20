package cecs429.queries;

import java.util.*;

import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.AdvancedTokenProcessor;

/**
 * Represents a phrase literal consisting of one or more terms that must occur in sequence.
 */
public class PhraseLiteral implements QueryComponent {
	// The list of individual terms in the phrase.
	private List<String> mTerms;
	private AdvancedTokenProcessor mProcessor;
	private HashSet<Integer> mDocIds;
	private static List<Posting> mMerges;
	private static List<Posting> mResults;

	/**
	 * Constructs a PhraseLiteral with the given individual phrase terms.
	 */
	public PhraseLiteral(List<String> terms) {
		mProcessor = new AdvancedTokenProcessor();
		mTerms = new ArrayList<>();
		mMerges = new ArrayList<>();
		mDocIds = new HashSet<>();
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
		mMerges = new ArrayList<>();
		mResults = new ArrayList<>();
		mDocIds = new HashSet<>();
	}
	
	@Override
	public List<Posting> getPostings(Index index) {
		// initialize master list to hold all of the postings that pass each positional merge
		//List<Posting> masterList = index.getPostings(mTerms.get(0));
		//	List<Posting> firstList = index.getPostings(mTerms.get(0));
		//HashSet<Integer> masterDocIds = new HashSet<>();

		// if there's only one term in the phrase literal, simply return its postings as if it was a single query component
		if (mTerms.size() <= 1) {
			mMerges = index.getPostings(mTerms.get(0));
		} else {
			// loop through the remaining terms in the phrase and perform the positionalMerge
			for (int i = 1; i < mTerms.size(); i++) {
				List<Posting> previousPostings;
				if (i == 1) {
					previousPostings = index.getPostings(mTerms.get(i - 1));
				}
				else {
					previousPostings = mMerges;
				}
				// update the master list by positionally merging it with the current postings list
				List<Posting> currentPostings = index.getPostings(mTerms.get(i));
				positionalMerge(previousPostings, currentPostings);

			}
		}

		if (mMerges == null) {
		//	System.out.println("No documents were found containing the phrase literal " + this );
			return null;
		}
		else {

			mResults = getResults(mMerges);
			return mResults;
		}
	}

	/**
	 * Given two postings lists that each corresponds to a term (or group of terms) from the list of mTerms, finds the postings of documents that contain both of them together in order
	 * @param oldPostings represents the master "top"-level that holds all the postings of terms that have already been positionally merged together
	 * @param newPostings represents the new "bottom"-level list of postings for the new term that is being merged with the master
	 * @return a new list containing only the postings that survived the merge
	 */
	public void positionalMerge(List<Posting> oldPostings, List<Posting> newPostings) {
		// setup indexes for iterating through the postings list of both top and bottom lists
		int oldIndex = 0;
		int newIndex = 0;

		// starting with the 0th index of each list, iterate through their postings lists simultaneously
		while (oldIndex < oldPostings.size() && newIndex < newPostings.size()) {
			// check if the current docIds match before proceeding
			if (oldPostings.get(oldIndex).getDocumentId() == newPostings.get(newIndex).getDocumentId()) {
				// lists to store the term positions at the current index in the top and bottom lists
				List<Integer> oldTermPositions = oldPostings.get(oldIndex).getTermPositions();
				List<Integer> newTermPositions = newPostings.get(newIndex).getTermPositions();

				// create lists of matching term locations for each original list
				HashSet<Integer> oldMatches = new HashSet<>();
				HashSet<Integer> newMatches = new HashSet<>();

				// starting at current top-level index of each list, iterate through their lists of the term positions using new indexes
				int oldPositionsIndex = 0;
				int newPositionsIndex = 0;

				while (oldPositionsIndex < oldTermPositions.size() && newPositionsIndex < newTermPositions.size()) {
					// start at 1 above the top index because it is impossible for a matching to occur at the 0th index of the bottom (this would require the matching in the top to occur at the -1st index)
					int currentOldPosition = oldTermPositions.get(oldPositionsIndex); // the actual term location found at a given index in the bottom list
					int currentNewPosition = newTermPositions.get(newPositionsIndex); // the actual term location found at a given index in the top list

					int newToOldMargin = currentNewPosition - currentOldPosition; // the numerical difference between the current values of the top and bottom's term locations

					// success case occurs when the currentBottom location is one number higher than the currentTop location
					if (newToOldMargin == 1) {
						// however, the match would only be valid if there are enough positions before this point in newTermPositions to account for all the term(s) already in the query
//						if ((newIndex == 0) || (newTermPositions.get(newPositionsIndex - 1) != newTermPositions.get(p)) {
							// its possible to have many pairs of matching term positions among the two lists
							// so for each pair of matches, store the location from the top list into its own list of matches and the location from the bottom into a separate list of matches
							oldMatches.add(currentOldPosition);
							newMatches.add(currentNewPosition); // note that we do NOT store (currentTop + 1) here; instead we store unmodified currentTop value that the match was found at
							// combine both sets into a hashset to ensure there are no duplicates
							//now convert the set of all matches back into a list
							List<Integer> allMatches = new ArrayList<>();
							allMatches.addAll(oldMatches);
							allMatches.addAll(newMatches);

							int currentDocId = newPostings.get(newIndex).getDocumentId();
							Posting match = new Posting(currentDocId, allMatches);


//							// try to merge the new Posting object with a Posting with its docId that has already been merged
//							try {
							if (mDocIds.contains(currentDocId)) {

								// find the posting that already has this docId
								Posting existing = mMerges.stream().filter(posting -> currentDocId == posting.getDocumentId()).findFirst().orElse(null);
								// merge it with the match posting
								Posting merge = match.merge(existing);
								// must re-sort term positions after the merging
								Collections.sort(merge.getTermPositions());
								// now remove the existing posting and replace it with the merged posting
								mMerges.remove(existing);
								mMerges.add(merge);
							//	System.out.println("mergePosting is null? " + mergePosting);
							}

							// if
//							catch (NullPointerException ex) {
							else {
								//System.out.println("The requested postings cannot be merged because they have different docId's. Adding a new Posting to the results list instead. \n ");

								// in this case, just add the current posting to the final list of results
								mMerges.add(match);
							}

							// re-sort the final list of merged postings by docId
						    mMerges.sort(Comparator.comparingInt(Posting::getDocumentId));
							mDocIds.add(currentDocId);

						// increment both indexes regardless of if the matching was a true success or not
							oldPositionsIndex++;
							newPositionsIndex++;
					}
					// now we must handle the remaining cases, in which a successful matching is not immediately found but is still possible
					// the first (and easier) way is when the top's current term position is greater than the bottom's
					else if (newToOldMargin <= 0) {
						newPositionsIndex++;
					}
					// the second (more difficult) way is when the bottom's current term location is still greater than the top's, buy by a difference of more than 1
					else {
						oldPositionsIndex++;
					}
				}
				// finally, increment both posting indexes before continuing to iterate
				oldIndex++;
				newIndex++;
			}


			// if the current docId's aren't equal, increment the smaller one to catch up
			else if (oldPostings.get(oldIndex).getDocumentId() < newPostings.get(newIndex).getDocumentId()) {
				oldIndex++;
			}
			else {
				newIndex++;
			}
		}
		//return mergedPostings;
	}

	public List<Posting> getResults(List<Posting> mergedPostings) {
		List<Posting> results = new ArrayList<>();

		for (Posting p: mergedPostings) {
			List<Integer> positions = p.getTermPositions();
			List<Integer> sequence = new ArrayList<>();

			for (int i = 1; i < positions.size(); i++) {
				int difference = positions.get(i) - positions.get(i - 1);
				int count = 1;
				int j = i - 1;

				while ( (difference == 1) && (j < positions.size()) ) {
					if (count >= mTerms.size()) {
						sequence.addAll(positions.subList(j - count + 1, j + 1));
						// if a matching sequence is found, reset i and break
						i = j + 1;
						break;
//						count = 0;
//						j = i - 1;
					}
					else if (j >= positions.size() - 1) {
						break;
					}
					else {
						count += 1;
						j += 1;
					}
					// now recalculate difference after incrementing j
					difference = positions.get(j) - positions.get(j - 1);
				}
			}

			// check if any matching sequences were found for the current posting
			if (sequence.size() > 0) {
				// if so, create a new posting with only the positions from the matching sequences and add it to the results
				Posting match = new Posting(p.getDocumentId(), sequence);
				results.add(match);
			}
		}
		return results;
	}

	@Override
	public String toString() {
		return "\"" + String.join(" ", mTerms) + "\"";
	}
}
