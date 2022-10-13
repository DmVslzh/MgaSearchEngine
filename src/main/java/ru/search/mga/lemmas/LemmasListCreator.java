package ru.search.mga.lemmas;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;

public class LemmasListCreator {

    public static List<String> prepareWordsList(String inpStr) {

        List<String> lst =
                List.of(inpStr.toLowerCase(Locale.ROOT).replaceAll("[^а-яё]+", " ").replace("ё", "е").split(" "));
        List<String> outputList = new ArrayList<>();
        try {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            for (String str : lst) {
                if (str.length() == 0) {
                    continue;
                }
                List<String> wordInfoList = luceneMorph.getMorphInfo(str);
                String tmpStr = wordInfoList.get(0);
                tmpStr = tmpStr.substring(tmpStr.indexOf("|"), tmpStr.length());
                if (!(tmpStr.contains("МЕЖД") || tmpStr.contains("ПРЕДЛ") ||
                        tmpStr.contains("ЧАСТ") || tmpStr.contains("СОЮЗ") ||
                        tmpStr.contains("МС"))) {
                    outputList.add(str);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return outputList;
        } catch (WrongCharaterException e) {
            e.printStackTrace();
            return outputList;
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return outputList;
        }
        return outputList;
    }

    public static HashMap<String, Integer> countNumberLemmasEntries(String incomingText) {
        List<String> lemmaList = LemmasListCreator.getLemmaList(incomingText);
        return LemmasListCreator.countWordsEntries(lemmaList);
    }

    public static HashMap<String, Float> countNumberLemmasEntriesWithWeight(String incomingText, Float weight) {
        List<String> lemmaList = LemmasListCreator.getLemmaList(incomingText);
        return LemmasListCreator.countWordsEntriesWithWeight(lemmaList, weight);
    }

    public static List<String> getLemmaList(String incomingText) {
        List<String> outList = new ArrayList<>();
        List<String> preparedStringList = LemmasListCreator.prepareWordsList(incomingText);
        try {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            for (String s : preparedStringList) {
                outList.addAll(luceneMorph.getNormalForms(s));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outList;
    }

    public static List<List<String>> getLemmaList(List<String> incomingText) {
        List<List<String>> outList = new ArrayList<>();
        try {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            for (String s : incomingText) {
                outList.add(luceneMorph.getNormalForms(s));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outList;
    }

    private static HashMap<String, Integer> countWordsEntries(List<String> lst) {
        HashMap<String, Integer> out = new HashMap<>();
        List<String> controlList = new ArrayList<>();
        for (String wordForComparison : lst) {
            if (controlList.contains(wordForComparison)) {
                continue;
            } else {
                controlList.add(wordForComparison);
                int counter = 0;
                for (int i = 0; i < lst.size(); i++) {
                    if (lst.get(i).equals(wordForComparison)) {
                        counter++;
                    }
                    out.put(wordForComparison, counter);
                }
            }
        }
        return out;
    }

    private static HashMap<String, Float> countWordsEntriesWithWeight(List<String> lst, Float weight) {
        HashMap<String, Float> out = new HashMap<>();
        List<String> controlList = new ArrayList<>();
        for (String wordForComparison : lst) {
            if (controlList.contains(wordForComparison)) {
                continue;
            } else {
                controlList.add(wordForComparison);
                int counter = 0;
                for (int i = 0; i < lst.size(); i++) {
                    if (lst.get(i).equals(wordForComparison)) {
                        counter++;
                    }
                    out.put(wordForComparison, counter * weight);
                }
            }
        }
        return out;
    }
}
