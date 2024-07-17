package com.securite.urbaine.service;

import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import weka.classifiers.trees.J48;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;

import java.io.IOException;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service de machine learning utilisant WEKA pour entraîner un modèle de classification J48.
 *
 * @author Ahmed BAHRI
 * @author Mejda Sliman
 */
@Service
public class MLService {
    private J48 tree;
    private Instances data;
    @Setter
    private int numThreads;
    private final BlockingQueue<Instances> dataQueue;
    private final List<J48> trees;
    private static final int QUEUE_CAPACITY = 100;
    private static final int MIN_INSTANCES_FOR_PARALLEL = 1000;
    private final AtomicInteger progress = new AtomicInteger(0);

    /**
     * Constructeur par défaut. Initialise les ressources nécessaires.
     */
    public MLService() {
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.dataQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        this.trees = new CopyOnWriteArrayList<>();
    }

    /**
     * Réinitialise la progression de la formation du modèle.
     */
    public void resetProgress() {
        progress.set(0);
    }

    /**
     * Incrémente la progression de la formation du modèle.
     */
    public void incrementProgress() {
        progress.incrementAndGet();
    }

    /**
     * Obtient la progression actuelle de la formation du modèle.
     *
     * @return la progression actuelle.
     */
    public int getProgress() {
        return progress.get();
    }

    /**
     * Entraîne le modèle avec les données fournies.
     *
     * @param file le fichier contenant les données de formation.
     * @throws IOException si une erreur survient lors du chargement ou de la préparation des données.
     */
    public void trainModel(MultipartFile file) throws IOException {
        resetProgress();
        loadAndPrepareData(file);

        if (data.numInstances() < MIN_INSTANCES_FOR_PARALLEL) {
            trainSequentially();
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(numThreads + 1);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicBoolean producerDone = new AtomicBoolean(false);

        executor.submit(() -> produceData(producerDone));//P
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                consumeData(latch, producerDone);//C
                incrementProgress();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread interrupted", e);
        } finally {
            executor.shutdownNow();
        }

        combineResults();
    }

    /**
     * Charge et prépare les données à partir du fichier fourni.
     *
     * @param file le fichier contenant les données.
     * @throws IOException si une erreur survient lors du chargement ou de la préparation des données.
     */
    private void loadAndPrepareData(MultipartFile file) throws IOException {
        CSVLoader loader = new CSVLoader();
        loader.setSource(file.getInputStream());
        data = loader.getDataSet();

        NumericToNominal convert = new NumericToNominal();
        convert.setAttributeIndices("last");

        try {
            convert.setInputFormat(data);
            data = Filter.useFilter(data, convert);
        } catch (Exception e) {
            throw new IOException("Failed to convert numeric class attribute to nominal: " + e.getMessage(), e);
        }

        data.setClassIndex(data.numAttributes() - 1);
    }

    /**
     * Entraîne le modèle de manière séquentielle.
     *
     * @throws IOException si une erreur survient lors de la formation du classifieur.
     */
    private void trainSequentially() throws IOException {
        tree = new J48();
        try {
            tree.buildClassifier(data);
        } catch (Exception e) {
            throw new IOException("Failed to build classifier: " + e.getMessage(), e);
        }
    }

    /**
     * Produit des parties de données pour la formation parallèle.
     *
     * @param done indicateur atomique pour signaler la fin de la production.
     */
    private void produceData(AtomicBoolean done) {
        int partSize = Math.max(data.numInstances() / (numThreads * 2), 1);
        int start = 0;
        while (start < data.numInstances()) {
            int end = Math.min(start + partSize, data.numInstances());
            Instances dataPart = new Instances(data, start, end - start);
            try {
                dataQueue.put(dataPart);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            start = end;
        }
        done.set(true);
    }

    /**
     * Consomme les parties de données et entraîne des classifieurs locaux.
     *
     * @param latch        le count down latch pour synchroniser les threads.
     * @param producerDone indicateur atomique pour signaler la fin de la production.
     */
    private void consumeData(CountDownLatch latch, AtomicBoolean producerDone) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Instances dataPart = dataQueue.poll(100, TimeUnit.MILLISECONDS);
                if (dataPart == null) {
                    if (producerDone.get() && dataQueue.isEmpty()) {
                        break;
                    }
                    continue;
                }
                J48 localTree = new J48();
                try {
                    localTree.buildClassifier(dataPart);
                    trees.add(localTree);
                } catch (Exception e) {
                    // Log l'erreur ou gérer l'exception
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            latch.countDown();
        }
    }

    /**
     * Combine les résultats des classifieurs formés.
     */
    private void combineResults() {
        if (!trees.isEmpty()) {
            tree = trees.get(0);
        }
    }

    /**
     * Prédit le risque basé sur les valeurs des attributs zone, time et day.
     *
     * @param zone la valeur de la zone.
     * @param time la valeur du temps.
     * @param day  la valeur du jour.
     * @return la prédiction de risque sous forme de chaîne.
     */
    public String predictRisk(int zone, int time, int day) {
        if (tree == null || data == null) {
            return "Model not trained yet";
        }
        try {
            Instance instance = new DenseInstance(data.numAttributes());
            instance.setDataset(data);
            instance.setValue(data.attribute("zone"), zone);
            instance.setValue(data.attribute("time"), time);
            instance.setValue(data.attribute("day"), day);

            double result = tree.classifyInstance(instance);
            return data.classAttribute().value((int) result);
        } catch (Exception e) {
            return "Error in prediction: " + e.getMessage();
        }
    }
}