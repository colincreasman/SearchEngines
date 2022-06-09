package cecs429.queries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import cecs429.indexes.Index;
import cecs429.indexes.Posting;

/**
 * An OrQuery composes other QueryComponents and merges their postings with a union-type operation.
 */
public class OrQuery implements QueryComponent {
	// The components of the Or query.
	private List<QueryComponent> mComponents;
	
	public OrQuery(List<QueryComponent> components) {
		mComponents = components;
	}
	
	@Override
	public List<Posting> getPostings(Index index) {
		// initialize a master postings list to which we will add all of the individual lists from each component after any necessary processing
		List<Posting> masterPostingsList = new ArrayList<>();

		for (int i = 0; i < mComponents.size(); i++) {
			// for the first QueryComponent only, automatically add it to the master postings list without any OR processing
			if (i == 0) {
				masterPostingsList.addAll(mComponents.get(i).getPostings(index));
			}
			// for every other query component after that, perform an OR union between the current postings list and the master postings list
			else {
				// store the result of that intersection in a new List<Posting>
				List<Posting> unionedPostings = union(mComponents.get(i).getPostings(index), masterPostingsList);
				// now we can add that List<Posting> to the result list and continue iterating
				masterPostingsList.addAll(unionedPostings);
			}
		}
		return  masterPostingsList;
	}

	// performs the OR union merge of two lists of postings to return a new list containing all postings from both lists (without duplicates)
	public List<Posting> union(List<Posting> top, List<Posting> bottom) {
		// initialize list to store results
		List<Posting> results = new ArrayList<>();

		// ensure there are no duplicates by using a temporary HashSet tp store all of the docId's from both lists
		HashMap<Integer, List<Integer>> postingsMap = new HashMap<>();

		// iterate through both lists simultaneously and to add all postings to the HashSet
		int i = 0; // bottom index
		int j = 0; // top index
		while (i < top.size() || j < bottom.size()) {
			// add the current Posting from both lists to the hash map
			postingsMap.put(top.get(i).getDocumentId(), top.get(i).getTermPositions());
			postingsMap.put(bottom.get(i).getDocumentId(), top.get(i).getTermPositions());

			// only increment the indexes if they haven't reached the end of their lists yet
			if (i != top.size() - 1) {
				i++;
			}
			if (j != top.size() - 1) {
				j++;
			}
		}
		// now the hash map should contain all of the postings from both lists with no duplicated DocId's

		for (int docId : postingsMap.keySet()) {
			// now add each posting from the hash map back into the results List<Posting>
			Posting currPosting = new Posting(docId, postingsMap.get(docId));
			results.add(currPosting);
		}

		return results;
	}
	
	@Override
	public String toString() {
		// Returns a string of the form "[SUBQUERY] + [SUBQUERY] + [SUBQUERY]"
		return "(" +
		 String.join(" + ", mComponents.stream().map(c -> c.toString()).collect(Collectors.toList()))
		 + " )";
	}
}
