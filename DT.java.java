
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.*;


public class Assignment13 {

    private static String INPUT_FILE = "input.csv";
    private static String OUTPUT_FILE = "output.txt";
    private FileWriter fw;

    enum Feature
    {
        Alternate, Bar, Fri_Sat, Hungry, Patrons, Price, Raining, Reservation, Type, WaitEstimate, WillWait;
    }

    //Method for reading restaurant.csv.
    private Map<Feature, List<String>> read() throws Exception {
        Map<Feature, List<String>> dataSet = new HashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(INPUT_FILE)));
        String inputLineStr =  null;
        while( (inputLineStr = br.readLine()) != null) {
            String inputData[] =  inputLineStr.split(",");
            int i = 0;
            for(Feature f : Feature.values()) {
                if(!dataSet.containsKey(f)) {
                    dataSet.put(f, new ArrayList<>());
                }
                dataSet.get(f).add(inputData[i].trim());
                i++;
            }
        }
        return dataSet;

    }

    //Iterates through rows in the input file.
    private List<Integer> getrIndices(List<String> strList, String value) {
        List<Integer> rowList = new ArrayList<>();
        int i = 0;
        for(String str : strList) {
            if(str.equals(value)) {
                rowList.add(i);
            }
            i++;
        }
        return rowList;
    }

    private String getValue(List<String> domainValues) {
        Random rand = new Random();
        Map<String,Integer> countMap = new HashMap<>();
        for(String str : domainValues) {
            if(!countMap.containsKey(str)) {
                countMap.put(str, 0);
            }
            countMap.put(str, countMap.get(str) +1);
        }
        int max = -1;
        String retStr = null;
        for(Map.Entry<String, Integer> entry : countMap.entrySet()) {
            if(max == entry.getValue()) {
                retStr =  rand.nextBoolean() ? entry.getKey() : retStr;
            }
            if(max < entry.getValue()) {
                max  = entry.getValue();
                retStr = entry.getKey();
            }
        }
        return  retStr;
    }

    //Creates Decision Tree based on maximum Info Gain amongst features.
    private TreeNode createDTree(Map<Feature, List<String>> dataSet) {
        // recursive terminal statement when no attribute is present.
        if(dataSet.size() == 1) {
            TreeNode node = new TreeNode(getValue(dataSet.get(Feature.WillWait)),true);
            return node;
        }

        if(isSane(dataSet.get(Feature.WillWait))) {
            TreeNode node = new TreeNode(dataSet.get(Feature.WillWait).get(0),true);
            return node;
        }


        Double maxInfoGain = -Double.MAX_VALUE;
        double targetEntropy = getEntropy(dataSet.get(Feature.WillWait));
        Feature maxGainFeature = null;
        for(Feature f : dataSet.keySet()) {
            if(f.equals(Feature.WillWait)) {
                continue;
            }
            Double infoGain_i = getInfoGain(targetEntropy, dataSet.get(f), dataSet.get(Feature.WillWait));
            if(maxInfoGain == null || maxInfoGain < infoGain_i) {
                maxGainFeature = f;
                maxInfoGain = infoGain_i;
            }
        }
        TreeNode rootNode = new TreeNode(maxGainFeature.toString(), false) ;
        Set<String> featureDomain = new HashSet<>(dataSet.get(maxGainFeature));
        for(String domainValue : featureDomain) {

            Map<Feature, List<String>> filteredDataSet = newObject(dataSet);
            List<Integer> rowIndex = getrIndices(dataSet.get(maxGainFeature), domainValue);
            filterData(filteredDataSet, rowIndex);
            filteredDataSet.remove(maxGainFeature);

            TreeNode node =  createDTree( filteredDataSet);
            rootNode.getChildNodes().put(domainValue, node);
        }

        return rootNode;
    }

    //Weeds out attributes and appropriate rows off the dataset.
    private void filterData(Map<Feature, List<String>> dataSet, List<Integer> rowList) {
        for(Feature f : dataSet.keySet()) {
            List<String> filteredValues = new ArrayList<>();
            for(Integer row : rowList) {
                filteredValues.add(dataSet.get(f).get(row));
            }
            dataSet.put(f,filteredValues);
        }
    }

    //Checks for pure set.
    private boolean isSane(List<String> resultList) {
        String str = resultList.get(0);
        for (String compareStr : resultList) {
            if (!compareStr.equals(str))
                return false;
        }
        return true;
    }

    public  void start() throws Exception {
        fw = new FileWriter(OUTPUT_FILE);
        Map<Feature, List<String>> dataSet = read();
        TreeNode root = createDTree(dataSet);
        fw.write("\n");
        printOutput(null, root, 0);
        fw.write("\n");
        fw.flush();
    }

    //Log value for Entropy.
    private double Log2(double value) {
        return Math.log(value) / Math.log(2);
    }

    //Calcultes info gain for features in dataset.
    private double getInfoGain(double rootEntropy, List<String> featureValues, List<String> targetFeatureValues) {
        Set<String> domainValues = new HashSet<>(featureValues);
        Double totalEntropy = 0.0;
        int size = featureValues.size();
        for(String domain : domainValues) {
            List<String> targetSubset = new ArrayList<>();
            int domainCount = 0;
            int i = 0;
            for(String str : featureValues) {
                if(str.equals(domain)) {
                    targetSubset.add(targetFeatureValues.get(i));
                    domainCount++;
                }
                i++;
            }
            double domainEntropy = domainCount/(double)size *getEntropy(targetSubset);
            totalEntropy += domainEntropy;
        }
        return  rootEntropy - totalEntropy;
    }

    // sum of -P * log2(P)
    private double getEntropy(List<String> featureValues) {
        Map<String, Integer> valueCountMap = new HashMap<>();

        for(String str : featureValues ) {
            if(!valueCountMap.containsKey(str)) {
                valueCountMap.put(str,0);
            }
            valueCountMap.put(str, valueCountMap.get(str) + 1);
        }
        int size = featureValues.size();
        double entropy = 0.0;
        for(Map.Entry<String, Integer> valueCountEntry : valueCountMap.entrySet()) {
            double prob = valueCountEntry.getValue() /(double)size;
            double valueEntropy = -prob*Log2(prob);
            entropy += valueEntropy;
        }
        return entropy;
    }

    //Data Copy.
    private Map<Feature,List<String>> newObject(Map<Feature, List<String>> featureListMap) {
        Map<Feature,List<String>> featureMap = new HashMap<>();
        for(Map.Entry<Feature, List<String>> entry : featureListMap.entrySet()) {
            featureMap.put(entry.getKey(), newObject(entry.getValue()));
        }
        return featureMap;
    }

    //Data Copy.
    private List<String> newObject(List<String> strList) {
        List<String> arr = new ArrayList<>();
        for(String str: strList) {
            arr.add(str);
        }
        return arr;
    }

    //Prints the decision tree formed.
    private void printOutput(String domainValue, TreeNode node, int spacing) throws Exception {
        fw.write("\n");
        for(int i = 0; i<spacing-1; i++) {
            fw.write("|\t");
        }
        if(spacing > 0)
            fw.write("|---");
        if(domainValue !=null) {
            fw.write(  domainValue + " ==>>>");
        }
        fw.write(node.getData());
        for(Map.Entry<String, TreeNode> entry : node.getChildNodes().entrySet()) {
            printOutput(entry.getKey(), entry.getValue(), spacing + 1);
        }
    }

    public static void main(String arg[]) throws Exception {
        Assignment13 assignment13 = new Assignment13();
//        fw = new FileWriter(OUTPUT_FILE);
        assignment13.start();

    }

    //Structure for tree nodes.
    class TreeNode {
        private String data;
        //    private String data;
        private boolean isPureDataSet;
        private Map<String,TreeNode> childNodes;

        public TreeNode(String data, boolean isPureDataSet) {
            this.data = data;
            this.isPureDataSet = isPureDataSet;
            childNodes = new HashMap<>();
        }

        public String getData() {
            return data;
        }

        public Map<String, TreeNode> getChildNodes() {
            return childNodes;
        }

        @Override
        public String toString() {
            return data +  " "  + isPureDataSet;
        }
    }

}

