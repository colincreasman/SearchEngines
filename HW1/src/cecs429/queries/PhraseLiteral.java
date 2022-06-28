package cecs429.queries;

import java.util.*;

import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.AdvancedTokenProcessor;
import cecs429.text.TokenProcessor;

/**
 * Represents a phrase literal consisting of one or more terms that must occur in sequence.
 */
public class PhraseLiteral implements QueryComponent {
	// The list of individual terms in the phrase.
	private static List<QueryComponent> mComponents;
	private static List<String> mPhraseTerms;
	private static List<String> mProcessedTerms;

	//private HashSet<Integer> mDocIds;
	private static List<Posting> mMerges;
	private static List<Posting> mPostings;

	/**
	 * Constructs a PhraseLiteral with the given individual phrase terms.
	 */
	public PhraseLiteral(List<String> listTerms) {
		//mComponents = new ArrayList<>();
		mPhraseTerms = listTerms;
		mProcessedTerms = new ArrayList<>();
		mMerges = new ArrayList<>();
		mPostings = new ArrayList<>();
		//mDocIds = new HashSet<>();
	}
	
	/**
	 * Constructs a PhraseLiteral given a string with one or more individual terms separated by spaces.
	 */
	public PhraseLiteral(String stringTerms) {
		mProcessedTerms = new ArrayList<>();
		mMerges = new ArrayList<>();
		mPostings = new ArrayList<>();
		//mDocIds = new HashSet<>();

		mPhraseTerms = Arrays.asList(stringTerms.split(" "));
	}
	
	@Override
	public List<Posting> getPostings(TokenProcessor processor, Index index) {
		// process and normalize tokens into terms to use to search the index
		for (String t : mPhraseTerms) {
			List<String> currentTerms = processor.processToken(t);
			mProcessedTerms.addAll((currentTerms));
		}
		//Collections.sort(mProcessedTerms);

		// if there's only one term in the phrase literal, simply return its postings as if it was a single query component
		if (mProcessedTerms.size() <= 1) {
			mMerges = index.getPostings(mProcessedTerms.get(0));
		}

		else {
			// loop through the remaining terms in the phrase and perform the positionalMerge
			for (int i = 1; i < mProcessedTerms.size(); i++) {
				List<Posting> previousPostings;
				if (i == 1) {
					previousPostings = index.getPostings(mProcessedTerms.get(0));
				}
				else {
					previousPostings = mMerges;
				}
				// update the master list by positionally merging it with the current postings list
				List<Posting> currentPostings = index.getPostings(mProcessedTerms.get(i));
				positionalMerge(previousPostings, currentPostings);
			}
		}
		if (mMerges == null) {
		//	System.out.println("No documents were found containing the phrase literal " + this );
			return null;
		}
		else {
			mPostings = mergePostings(mMerges);
			return mPostings;
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

//				// create lists of matching term locations for each original list
				List<Integer> matches = new ArrayList<>();

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
						// its possible to have many pairs of matching term positions among the two lists
						// so for each pair of matches, store the location from the top list into its own list of matches and the location from the bottom into a separate list of matches
//						if (!matches.contains(currentOldPosition)) {
//							matches.add(currentOldPosition);
//						}
//						if (!matches.contains(currentNewPosition)) {
//							matches.add(currentNewPosition); // note that we do NOT store (currentTop + 1) here; instead we store unmodified currentTop value that the match was found at
//						}
						matches.add(currentNewPosition);
						matches.add(currentOldPosition);
						int currentDocId = newPostings.get(newIndex).getDocumentId();
						Posting match = new Posting(currentDocId, matches);
						// try to merge the new Posting object with a Posting with its docId that has already been merged

						// try to get a posting that already has this docId
						Posting existing = mMerges.stream().filter(posting -> currentDocId == posting.getDocumentId()).findFirst().orElse(null);

						// if an existing posting was found, try to merge the new Posting object with it
						if (existing != null) {
							// merge it with the matched posting
							Posting merge = match.merge(existing);
							// must re-sort term positions after the merging
							Collections.sort(merge.getTermPositions());
							// now remove the existing posting and replace it with the merged posting
							mMerges.remove(existing);
							mMerges.add(merge);
						//	System.out.println("mergePosting is null? " + mergePosting);
						}

						else {
							// in this case, just add the current posting to the final list of results
							mMerges.add(match);
						}

						// re-sort the final list of merged postings by docId
						mMerges.sort(Comparator.comparingInt(Posting::getDocumentId));
						//mDocIds.add(currentDocId);

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

	public List<Posting> mergePostings(List<Posting> mergedPostings) {
		List<Posting> results = new ArrayList<>();

		for (Posting p: mergedPostings) {
			List<Integer> positions = p.getTermPositions();
	//		Collections.sort(positions);
			List<Integer> sequence = new ArrayList<>();

			for (int i = 1; i < positions.size(); i++) {
				int difference = positions.get(i) - positions.get(i - 1);
				int count = 1;
				int j = i - 1;

				while ( (difference == 1) && (j < positions.size()) ) {
					if (count >= mPhraseTerms.size()) {
						sequence.addAll(positions.subList(j - count + 1, j + 1));
						// if a matching sequence is found, reset i and break
						i = j + 1;
						break;
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
		return "\"" + String.join(" ", mProcessedTerms) + "\"";
	}
}
