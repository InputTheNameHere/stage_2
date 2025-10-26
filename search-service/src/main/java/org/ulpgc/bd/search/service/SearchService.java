package org.ulpgc.bd.search.service;

import org.ulpgc.bd.search.model.SearchResult;

import java.util.List;
import java.util.stream.Collectors;

public class SearchService {

    private final List<SearchResult> allBooks = List.of(
            new SearchResult(5, "Robinson Crusoe", "Daniel Defoe", "en", 1719),
            new SearchResult(1342, "Pride and Prejudice", "Jane Austen", "en", 1813),
            new SearchResult(158, "Emma", "Jane Austen", "en", 1815),
            new SearchResult(201, "Sense and Sensibility", "Jane Austen", "en", 1811),
            new SearchResult(6500, "Les Misérables", "Victor Hugo", "fr", 1862),
            new SearchResult(4201, "Vingt mille lieues sous les mers", "Jules Verne", "fr", 1870),
            new SearchResult(11, "Alice’s Adventures in Wonderland", "Lewis Carroll", "en", 1865),
            new SearchResult(12, "De la Terre à la Lune", "Jules Verne", "fr", 1865)
    );

    public List<SearchResult> search(String term, String author, String language, Integer year) {
        return allBooks.stream()
                .filter(b -> term.isEmpty() || b.title.toLowerCase().contains(term.toLowerCase()))
                .filter(b -> author == null || b.author.equalsIgnoreCase(author))
                .filter(b -> language == null || b.language.equalsIgnoreCase(language))
                .filter(b -> year == null || b.year == year)
                .collect(Collectors.toList());
    }
}
