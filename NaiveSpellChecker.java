package spellchecker;


import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class NaiveSpellChecker implements SpellChecker {

    public NaiveSpellChecker(Reader dictionaryReader, Reader stopWordsReader) {
        dictionary = readInput(dictionaryReader);
        stopWords = readInput(stopWordsReader);
    }

    @Override
    public void analyze(Reader textReader, Writer output, int suggestionsCount) {
        try (var reader = new BufferedReader(textReader);
             var writer = new BufferedWriter(output)) {
            Set<String> wordsInText = beautifyInput(reader.lines().collect(Collectors.toSet()))
                    .stream().filter(word -> word.length() > 1).collect(Collectors.toSet());
            Set<String> misspelledWords = findMisspelledWord(wordsInText);
            Map<String, List<String>> misspelledWordsAndSuggestions = new HashMap<>();

            for (var currentWord : misspelledWords) {
                List<String> suggestions;
                suggestions = findClosestWords(currentWord, suggestionsCount);
                misspelledWordsAndSuggestions.put(currentWord, suggestions);
                writer.write("Suggestions for the corretion of " + currentWord + ": " +
                        suggestions.toString() + System.lineSeparator());
                writer.flush();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("There is a problem with reading from this input.");
        }

    }

    @Override
    public Metadata metadata(Reader textReader) {
        return null;
    }

    @Override
    public List<String> findClosestWords(String word, int n) {

        Map<String, Integer> vectorFromWord = makeVectorFromWord(word);
        double vectorLength = findVectorLength(vectorFromWord);

        Map<String, Double> wordsAndSimilarity = new HashMap<>();

        for (String currentWord : dictionary) {
            double cosineSimilarity = findCosineSimilarity(vectorFromWord, vectorLength, currentWord);
            wordsAndSimilarity.put(currentWord, cosineSimilarity);
        }

        LinkedHashMap<String, Double> sortedDescendingMap = new LinkedHashMap<>();
        wordsAndSimilarity.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).
                forEachOrdered(x -> sortedDescendingMap.put(x.getKey(), x.getValue()));

        return sortedDescendingMap.keySet().stream().limit(n).collect(Collectors.toList());

    }

    private Set<String> readInput(Reader input) {
        try (var reader = new BufferedReader(input)) {
            Set<String> destination = beautifyInput(reader.lines().collect(Collectors.toSet()));
            return destination;
        } catch (IOException exception) {
            throw new IllegalStateException("There is a problem with reading from this input.");
        }
    }

    private Set<String> beautifyInput(Set<String> words) {
        words = words.stream().map(word -> word.strip())
                .map(word -> word.toLowerCase())
                .map(word -> word.replaceAll("\\p{Punct}&&[^-]", ""))
                .collect(Collectors.toSet());

        return words;
    }

    private Set<String> findMisspelledWord(Set<String> inputWords) {
        Set<String> misspelledWords = inputWords.stream().filter(word -> stopWords.contains(word) == false).
                filter(word -> dictionary.contains(word) == false).collect(Collectors.toSet());

        return misspelledWords;
    }


    private List<String> findTwoGrams(String word) {
        List<String> twoGramms = new ArrayList<>();

        for (int i = 0; i < word.length(); ++i) {
            String currentTwoGram = word.substring(i, i + 2);
            twoGramms.add(currentTwoGram);
        }

        return twoGramms;
    }

    private Map<String, Integer> makeVectorFromWord(String word) {
        List<String> twoGramms = findTwoGrams(word);
        Map<String, Integer> result = new HashMap<>();

        twoGramms.stream().map(w -> appearsInListCount(w, twoGramms)).forEach(x -> result.put(x.getKey(), x.getValue()));

        return result;
    }


    private Map.Entry<String, Integer> appearsInListCount(String word, List<String> words) {
        int count = 0;
        for (String currentWord : words) {
            if (currentWord.equals(word)) {
                ++count;
            }
        }

        return new java.util.AbstractMap.SimpleEntry<String, Integer>(word, count);
    }


    private double findVectorLength(Map<String, Integer> vector) {

        return Math.sqrt(vector.values().stream().reduce(0, (a, b) -> a ^ 2 + b ^ 2));
    }


    private double findCosineSimilarity(Map<String, Integer> vectorFromWord, double vectorLength, String currentWord) {
        var vectorFromCurrentWord = makeVectorFromWord(currentWord);
        double currentVectorLength = findVectorLength(vectorFromCurrentWord);

        var vectorFromWordEntrySet = vectorFromWord.entrySet();
        var vectorFromCurrentWordEntrySet = vectorFromCurrentWord.entrySet();

        var intersection = new HashSet<>(vectorFromWordEntrySet);
        intersection.retainAll(vectorFromCurrentWordEntrySet);

        double vectorDot = intersection.stream().map(x -> x.getValue()).reduce(0, (a, b) -> (a * a + b * b));

        double result = vectorDot / (currentVectorLength + vectorLength);

        return result;
    }


    private Set<String> dictionary = new HashSet<>();
    private Set<String> stopWords = new HashSet<>();
}
