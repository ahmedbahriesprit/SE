package com.securite.urbaine.service;

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
import java.io.InputStream;

/**
 * @author Ahmed BAHRi
 * @author Mejda Sliman
 */
@Service
public class MLService {
    // Déclare l'instance de l'arbre de décision J48
    private J48 tree;
    // Déclare l'objet Instances pour stocker les données
    private Instances data;

    /**
     * Cette méthode entraîne le modèle J48 en utilisant un fichier CSV fourni.
     *g
     * @param file Le fichier CSV contenant les données d'entraînement
     * @throws IOException En cas d'erreur lors du traitement du fichier ou de l'entraînement du modèle
     */
    public void trainModel(MultipartFile file) throws IOException {
        CSVLoader loader = new CSVLoader();
        InputStream inputStream = file.getInputStream();
        loader.setSource(inputStream);

        // Charge les données dans l'objet Instances
        data = loader.getDataSet();

        // Convert numeric class attribute to nominal
        NumericToNominal convert = new NumericToNominal();

        // Définit l'attribut de classe (ici, le dernier attribut)
        convert.setAttributeIndices("last"); // assuming the class attribute is the last attribute

        // Initialise l'instance J48
        try {
            // Construit le classificateur J48 avec les données
            convert.setInputFormat(data);
            data = Filter.useFilter(data, convert);
        } catch (Exception e) {
            throw new IOException("Failed to convert numeric class attribute to nominal: " + e.getMessage(), e);
        }

        data.setClassIndex(data.numAttributes() - 1);

        tree = new J48();
        try {
            tree.buildClassifier(data);
        } catch (Exception e) {
            throw new IOException("Failed to build classifier: " + e.getMessage(), e);
        }
    }

    /**
     * Cette méthode prédit le risque basé sur les valeurs des attributs fournis.
     *
     * @param zone La valeur de l'attribut 'zone'
     * @param time La valeur de l'attribut 'time'
     * @param day  La valeur de l'attribut 'day'
     * @return La prédiction du risque sous forme de chaîne de caractères
     */
    public String predictRisk(int zone, int time, int day) {
        // Vérifie si le modèle est entraîné
        if (tree == null || data == null) {
            return "Model not trained yet";
        }
        try {
            // Crée une nouvelle instance avec le même nombre d'attributs que les données d'entraînement
            Instance instance = new DenseInstance(data.numAttributes());
            instance.setDataset(data);

            // Définit les valeurs des attributs pour l'instance
            instance.setValue(data.attribute("zone"), zone); // Assuming the attribute name is 'zone'
            instance.setValue(data.attribute("time"), time); // Assuming the attribute name is 'time'
            instance.setValue(data.attribute("day"), day); // Assuming the attribute name is 'day'

            // Classifie l'instance et obtient le résultat
            double result = tree.classifyInstance(instance);
            // Retourne la valeur prédite de l'attribut de classe
            return data.classAttribute().value((int) result);
        } catch (Exception e) {
            return "Error in prediction: " + e.getMessage();
        }
    }
}
