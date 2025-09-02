package com.app.debate.core;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Tiny dependency-free frequency-based summarizer.
 * Splits into sentences, scores by term frequency, returns top N in original order.
 */
public class SimpleSummarizer {

    private static final Set<String> STOP = Set.of(
            "the","is","are","a","an","and","or","of","to","in","on","for","with",
            "as","by","it","that","this","be","from","at","into","over","about");

    public String summarize(String text, int n) {
        if (text == null || text.isBlank()) return "";
        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length <= n) return text.trim();

        Map<String,Integer> freq = new HashMap<>();
        for (String w : tokenize(text)) {
            if (!STOP.contains(w)) freq.merge(w, 1, Integer::sum);
        }

        double[] scores = new double[sentences.length];
        for (int i = 0; i < sentences.length; i++) {
            for (String w : tokenize(sentences[i])) {
                scores[i] += freq.getOrDefault(w, 0);
            }
        }

        Integer[] idx = new Integer[sentences.length];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(scores[b], scores[a]));
        Set<Integer> keep = new HashSet<>(Arrays.asList(Arrays.copyOf(idx, n)));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sentences.length; i++) {
            if (keep.contains(i)) sb.append(sentences[i].trim()).append(" ");
        }
        return sb.toString().trim();
    }

    private List<String> tokenize(String s) {
        s = s.toLowerCase();
        s = Pattern.compile("[^a-z0-9\\s]").matcher(s).replaceAll(" ");
        String[] parts = s.split("\\s+");
        List<String> out = new ArrayList<>();
        for (String p : parts) if (!p.isBlank()) out.add(p);
        return out;
    }
}
