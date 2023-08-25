package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;

public class LemmaDetector {
    private final HashMap<String, Integer> mapLemmas = new HashMap<>();
    private static final String[] serviceParts = new String[]{"ПРЕДЛ", "ПРЕДК", "СОЮЗ", "МЕЖД"};
    private String text;

    public LemmaDetector(String text) {
        this.text = text;
    }

    public HashMap<String, Integer> getMapLemmas() throws IOException {
        String delTags = deleteTags(text);

        String[] words = Arrays.stream(delTags.split("\s+"))
                .map(String::toLowerCase)
                .toArray(String[]::new);

        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();

        List<String> wordBaseForms = new ArrayList<>();
        for (String word : words) {
            if (luceneMorphology.checkString(word) && !isServicePart(word)) {
                wordBaseForms.addAll(luceneMorphology.getNormalForms(word.toLowerCase(Locale.ROOT)));
            }
        }

        HashMap<String, Integer> mapLemmas = new HashMap<>();

        for (String word : wordBaseForms) {
            if (mapLemmas.containsKey(word)) {
                mapLemmas.put(word, mapLemmas.get(word) + 1);
            } else {
                mapLemmas.put(word, 1);
            }
        }
        return mapLemmas;
    }

    private String deleteTags(String text) {
        String delScript = text.replaceAll("<script.*>.*<\\/script>", "");
        String delTags = delScript.replaceAll("<.*>", "");
        String delMnemonicCode = delTags.replaceAll("&.*;", " ");
        String delPunctuation = delMnemonicCode.replaceAll("\\p{Punct}", " ");

        return delPunctuation;
    }

    private boolean isServicePart(String word) {
        LuceneMorphology luceneMorphology;
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            return false;
        }
        boolean result = false;

        List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
        for (String servicePart: serviceParts) {
            for (String wordBaseForm: wordBaseForms) {
                if (wordBaseForm.contains(servicePart)) {
                    result = true;
                }
            }
        }
        return result;
    }
}
