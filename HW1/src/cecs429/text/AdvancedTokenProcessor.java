package cecs429.text;

import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.SnowballStemmer;


import java.util.ArrayList;
import java.util.List;

public class AdvancedTokenProcessor implements TokenProcessor {

    /**
     * Normalizes a token into a term (or list of terms)
     *
     * @param token
     */
    @Override
    public List<String> processToken(String token) {
        // perform all of necessary processing functions on the single original token before removing hyphens
        String token1 = fixNonAlphaNumerics(token);
       // System.out.println("Testing alphanumberics: " + token1);
        String token2 = fixPunctuation(token1);
        //System.out.println("Testing punc:  " + token2);
        List<String> terms = fixHyphens(token2);
        //System.out.println("Testing terms:  " + terms);
        List<String> stemmedTerms = new ArrayList<>();

        // perform the case and stemming functions on each term in the list
        for (String t : terms) {
            String lowerTerm = fixCase(t);
            String stem = stem(lowerTerm);
            //System.out.println("Testing stem for the term '" + lowerTerm + "' is:" + stem);

            stemmedTerms.add(stem);
        }
        // System.out.println("Testing processToken: " + stemmedTerms.toString());
        return stemmedTerms;
    }

    // Removes all non-alphanumeric characters from the beginning and end of the token, but not the middle.
    public String fixNonAlphaNumerics(String token) {
        // initialize vals for first and last indexes
        int first = 0;
        int last = token.length() - 1;

        // automatically return the token as-is if it is only a single character long
        if (first == last) {
            return token;
        }

        // convert to character array
        char[] tokenCharacters = token.toCharArray();

        // find the first index in the char array that is an alphanumeric character
        for (int i = 0; i < tokenCharacters.length; i++) {
            if (Character.isAlphabetic(tokenCharacters[i]) || Character.isDigit(tokenCharacters[i])) {
                first = i;
                break;
            }
        }

        //  find the last index that is an alphanumeric character by counting backwards from the end
        for (int i = tokenCharacters.length - 1; i >= 0; i--) {
            if (Character.isAlphabetic(tokenCharacters[i]) || Character.isDigit(tokenCharacters[i])) {
                last = i;
                break;
            }
        }
        // then substring between the two to extract the term.
        String result = token.substring(first, last + 1);
        //System.out.println("Testing fixNonAplhanumerics: " + result);
        return result;
    }

    // Removes all apostrophes or quotation marks (single or double quotes) from anywhere in the string
    public String fixPunctuation(String token) {
        // first check if the token contains any relevant punctuations
       // if (token.contains("\"") || token.contains("\'")) {
            // if so, use a regex to remove them
//            token = token.replaceAll("\"", "");
//            token = token.replaceAll("'", "");
       // }
        ///System.out.println("Testing fixPunctuation: " + token);
        char apostrophe = '\'';
        char quote = '\"';
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char currChar = token.charAt(i);

            if (currChar != apostrophe && currChar != quote) {
                builder.append(currChar);
            }
        }
        String result = builder.toString();
        return result;
    }

    /**
     * (a) Removes the hyphens from the token for the first processed token, AND
     * (b) Splits the original hyphenated token into multiple tokens without a hyphen
     * each of which becomes its own processed token
     *
     * @param token
     * @return
     */
    public List<String> fixHyphens(String token) {
        // initialize list of results
        List<String> results = new ArrayList<>();

        // first check if the token even contains any hyphens before processing
        if (token.contains("-")) {
            //System.out.println("Testing token before removing hyphens: " + token);

            // create a clone of the original token and remove all hyphens from it
            String tokenWithoutHyphens = token.replaceAll("[\\s\\-()]", "");
            // System.out.println("Testing token without hyphens: " + tokenWithoutHyphens);

            // add the modified token to the results without changing the original token
            results.add(tokenWithoutHyphens);

            // initialize 2 placeholder indexes:
            int start = 0; // keeps track of the beginning of the current substring
            int end; //  keeps track of the end of the current substring

            // iterate through the original token as a char array
            char[] tokenCharacters = token.toCharArray();
            for (int i = 0; i < tokenCharacters.length; i++) {
                // check each char for hyphens
                if (tokenCharacters[i] == '-') {
                    // when found, set the end placeholder to the current index
                    end = i;
                    // make sure hyphens aren't consequtive
                    if (end - start > 1) {
                        // extract a substring from the original token using the start and end placeholders
                        String partialToken = token.substring(start, end);                // add the substring to the results as its own term
                        results.add(partialToken);
                    }

                    // now reassign the start placeholder to the next index (right after the most recent hyphen was found)
                    start = i + 1;
                    // continue iterating and repeat the process whenever a new hyphen is found
                }
                if (i == tokenCharacters.length - 1) {
                    end = i + 1;
                    if (end - start > 1) {
                        String partialToken = token.substring(start, end);
                        results.add(partialToken);
                    }
                }
            }
        }
        // if there are no hyphens in the original token, add it to the list in its original form
        else {
            results.add(token);
        }
       // System.out.println("Testing fixHyphens: " + results.toString());
        return results;
    }

    //converts the token to lowercase
    public String fixCase(String token) {
        //System.out.println("Testing fixCase: " + token.toLowerCase());
        return token.toLowerCase();
    }

    public String stem(String token) {
        String term = "";
        try {
            Class<?> stemClass = Class.forName("org.tartarus.snowball.ext." + "english" + "Stemmer");
            SnowballStemmer stemmer = (SnowballStemmer) stemClass.getDeclaredConstructor().newInstance();

            stemmer.setCurrent(token);
            stemmer.stem();
            term = stemmer.getCurrent();
            return term;
            //System.out.println("testing with github way term is: " + term);
        }
        catch (Exception ex) {
            return null;
        }
        //System.out.println("Testing Stem: " + term);
       // return term;
    }
}

