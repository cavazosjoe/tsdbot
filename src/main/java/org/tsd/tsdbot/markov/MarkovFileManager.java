package org.tsd.tsdbot.markov;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.util.MarkovUtil;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Singleton
public class MarkovFileManager {

    private static final Logger log = LoggerFactory.getLogger(MarkovFileManager.class);

    private static final String wordDelimiter = ",";
    private static final int maxSentenceWords = 25;

    private final File baseDir;
    private final Random random;
    private final ExecutorService executorService;

    private final Map<File, Object> locks = new HashMap<>();

    @Inject
    public MarkovFileManager(@Named("markovDirectory") File baseDir,
                             Random random,
                             ExecutorService executorService) {
        this.baseDir = baseDir;
        this.random = random;
        this.executorService = executorService;
        File[] files = baseDir.listFiles();
        if(files != null) {
            locks.putAll(
                    Arrays.asList(files).parallelStream()
                            .collect(Collectors.toMap(file -> file, file -> new Object()))
            );
        }
    }

    public List<File> allFiles() {
        return Arrays.asList(baseDir.listFiles());
    }

    protected File getFile(String name) throws IOException {
        return createFileIfNotExists(name);
    }

    protected File createFileIfNotExists(String name) throws IOException {
        name = MarkovUtil.sanitize(name);
        File f = new File(baseDir, name);
        if(!f.exists()) {
            log.info("Markov file {} does not exist, creating...", name);
            f.createNewFile();
            locks.put(f, new Object());
        }
        log.info("Found markov file: {} -> {}", name, f);
        return f;
    }

    public void process(String filename, String... values) throws IOException {
        List<String> sanitizedWords = new LinkedList<>();
        sanitizedWords.addAll(
                Arrays.asList(values).stream()
                        .map(MarkovUtil::sanitize)
                        .collect(Collectors.toList())
        );

        executorService.submit(() -> {
            String[] sanitizedWordsArray = sanitizedWords.toArray(new String[sanitizedWords.size()]);
            int keyLength = 2;
            IntStream.range(keyLength-1, sanitizedWordsArray.length-1)
                    .forEach(i -> {
                        MarkovKey key = new MarkovKey(ArrayUtils.subarray(sanitizedWordsArray, i - (keyLength - 1), i + 1));
                        try {
                            addToFile(filename, key, sanitizedWordsArray[i + 1]);
                        } catch (Exception e) {
                            log.error("Error processing markov entry, filename="+filename+", values="+ArrayUtils.toString(values), e);
                            throw new RuntimeException(e);
                        }
                    });
        });
    }

    public void addToFile(String filename, MarkovKey key, String... values) throws IOException {
        File markovFile = getFile(filename);

        synchronized (locks.get(markovFile)) {
            // during loop, record each line so the file can be re-compiled at the end
            File temp = Files.createTempFile(RandomStringUtils.randomAlphabetic(20), ".txt").toFile();

            log.info("Adding to markov file: key=\"{}\", values={}", key, ArrayUtils.toString(values));
            boolean foundKey = false;
            try (
                    BufferedReader reader = new BufferedReader(new FileReader(markovFile));
                    BufferedWriter tempWriter = new BufferedWriter(new FileWriter(temp))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.trace("Evaluating markov line: {}", line);
                    if (line.startsWith(key.toString())) {
                        log.info("Line starts with key: {}", key);
                        foundKey = true;
                        String[] words = line.substring(key.toString().length()).split(wordDelimiter);
                        log.info("Parsed words: {}", Arrays.toString(words));
                        for (String value : values) {
                            value = MarkovUtil.sanitize(value);
                            log.info("Sanitized word: {}", value);
                            words = ArrayUtils.add(words, value);
                        }
                        line = key.toString() + StringUtils.join(words, wordDelimiter);
                        log.info("Adding revised line: {}", line);
                    }
                    tempWriter.write(line);
                    tempWriter.newLine();
                }

                if (!foundKey) {
                    String[] words = new String[values.length];
                    IntStream.range(0, values.length)
                            .forEach(i -> {
                                String value = MarkovUtil.sanitize(values[i]);
                                log.info("Sanitized word: {}", value);
                                words[i] = value;
                            });
                    line = key.toString() + StringUtils.join(words, wordDelimiter);
                    log.info("Did not find key in markov file, adding line: {}", line);
                    tempWriter.write(line);
                }
            }

            FileUtils.copyFile(temp, markovFile);
            if (!temp.delete()) {
                log.warn("Temporary file not deleted: {}", temp);
            }
        }
    }

    protected String[] getWordsForKey(String filename, MarkovKey key) throws IOException {
        File markovFile = getFile(filename);
        log.info("Reading from markov file: key=\"{}\"", key);

        synchronized (locks.get(markovFile)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(markovFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.trace("Evaluating markov line: {}", line);
                    if (line.startsWith(key.toString())) {
                        log.info("Line starts with key: {}", key);
                        String wordsRaw = line.substring(key.toString().length());
                        log.info("Parsed words: {}", wordsRaw);
                        return StringUtils.isNotBlank(wordsRaw) ? wordsRaw.split(wordDelimiter) : null;
                    }
                }
            }
        }

        log.info("Failed to find line for key: {}", key);
        return null;
    }

    protected MarkovKey getRandomKey(String filename) throws IOException {
        File markovFile = getFile(filename);
        String[] lines;
        try(BufferedReader reader = new BufferedReader(new FileReader(markovFile))) {
            lines = reader.lines().toArray(String[]::new);
            log.info("Read {} lines from file {}", lines.length, markovFile);
        }

        String keyLine = lines[random.nextInt(lines.length)];
        keyLine = keyLine.substring(keyLine.indexOf("[")+1, keyLine.indexOf("]"));
        String[] keyParts = keyLine.split(" ");
        return new MarkovKey(keyParts);
    }

    public String generateChain(String filename, int chainLength) throws IOException {
        MarkovKey searchingKey = getRandomKey(filename);
        log.info("Using random key: {}", searchingKey);

        List<String> rawWords = new LinkedList<>();
        String[] possibleNextWords;
        String nextWord;
        do {
            possibleNextWords = getWordsForKey(filename, searchingKey);
            if(possibleNextWords != null) {
                nextWord = possibleNextWords[random.nextInt(possibleNextWords.length)];
                rawWords.add(nextWord);
                searchingKey = new MarkovKey(searchingKey, nextWord);
            }
        } while(possibleNextWords != null && possibleNextWords.length > 0 && sizeOfList(rawWords) < chainLength);

        boolean capitalize = true;
        StringBuilder chain = new StringBuilder();
        int wordsLeftInSentence = random.nextInt(maxSentenceWords)+1;
        String[] sentenceEnds = {".", "...", ",", "?", "!"};
        Set<String> wordsNotToEndOn = new HashSet<>(Arrays.asList("the", "and"));

        for(String word : rawWords) {

            if(chain.length() > 0) {
                chain.append(" ");
            }

            if(capitalize) {
                word = word.substring(0, 1).toUpperCase() + word.substring(1, word.length());
                capitalize = false;
            }

            if(word.equals("i")) {
                word = "I";
            }

            chain.append(word);
            wordsLeftInSentence--;
            if(wordsLeftInSentence <= 0 && !wordsNotToEndOn.contains(word)) {
                String punctuation = sentenceEnds[random.nextInt(sentenceEnds.length)];
                chain.append(sentenceEnds[random.nextInt(sentenceEnds.length)]);
                capitalize = !punctuation.equals(",");
                wordsLeftInSentence = random.nextInt(maxSentenceWords)+1;
            }
        }

        return chain.toString();
    }

    private int sizeOfList(List<String> strings) {
        return strings.stream().mapToInt(String::length).sum();
    }
}
