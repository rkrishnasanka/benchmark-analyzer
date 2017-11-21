import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.BiconnectivityInspector;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Analysis {

    private static HashMap<String, Component> componenthashmap;

    public static void cccGen(JSONObject devicejson, FileWriter writer){
        String name = (String)devicejson.get("name");
        JSONArray components = (JSONArray)devicejson.get("components");
        JSONArray connections = (JSONArray)devicejson.get("connections");
        int number_of_components = components.size();
        int number_of_connections = connections.size();

        //Loop trhough all the components and find the components with the largest number of terminals
        int max_terminals = 0;
        for (Object object : components){
            JSONObject componentobject = (JSONObject)object;

            JSONArray ports = (JSONArray)componentobject.get("ports");

            if(max_terminals<ports.size()){
                max_terminals = ports.size();
            }
        }

        try {
            writer.append(name);
            writer.append(",");
            writer.append(Integer.toString(number_of_components));
            writer.append(",");
            writer.append(Integer.toString(number_of_connections));
            writer.append(",");
            writer.append(Integer.toString(max_terminals));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static UndirectedGraph<Component, DefaultEdge> undirectednetlist;
    static DirectedGraph<Component, DefaultEdge> directednetlist;

    public static void redGen(JSONObject json, FileWriter writer) {

        undirectednetlist = new SimpleGraph<>(DefaultEdge.class);
        directednetlist = new SimpleDirectedGraph<>(DefaultEdge.class);

        componenthashmap = new HashMap<>();
        JSONArray components = (JSONArray)json.get("components");
        JSONArray connections = (JSONArray)json.get("connections");

        ArrayList<Double> arealist = new ArrayList<>();
        ArrayList<Double> dimlist = new ArrayList<>();
        ArrayList<Double> aspectratiolist = new ArrayList<>();
        Component componenttoadd;

        for (Object object : components){
            JSONObject componentobject = (JSONObject)object;
            componenttoadd = new Component(componentobject);

            undirectednetlist.addVertex(componenttoadd);
            directednetlist.addVertex(componenttoadd);
            componenthashmap.put(componenttoadd.getid(), componenttoadd);
            arealist.add((double)componenttoadd.getXSpan()*(double)componenttoadd.getYSpan());
            dimlist.add((double)componenttoadd.getXSpan());
            dimlist.add((double)componenttoadd.getYSpan());
            if(componenttoadd.getXSpan()!=0 && componenttoadd.getYSpan()!=0){
                aspectratiolist.add((double)componenttoadd.getXSpan()/ componenttoadd.getYSpan());
            }
        }
        double array[] = new double[dimlist.size()];
        for (int i=0; i<dimlist.size() ; i++){
            array[i] = dimlist.get(i);
        }
        double averagedimension  = StatUtils.mean(array);
        StandardDeviation deviation = new StandardDeviation();
        double stdevdimension =deviation.evaluate(array);

        array = new double[aspectratiolist.size()];
        for (int i=0; i<aspectratiolist.size() ; i++){
            array[i] = aspectratiolist.get(i);
        }
        double averageaspectratio = StatUtils.mean(array);

        array = new double[aspectratiolist.size()];
        for (int i=0; i<aspectratiolist.size() ; i++){
            array[i] = arealist.get(i);
        }

        double averagearea = StatUtils.mean(array);
        try {
            writer.append(",");
            writer.append(Double.toString(averagearea));
            writer.append(",");
            writer.append(Double.toString(averagedimension));
            writer.append(",");
            writer.append(Double.toString(stdevdimension));
            writer.append(",");
            writer.append(Double.toString(averageaspectratio));
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public static void printHeader(FileWriter writer) throws IOException {
        writer.append("set");
        writer.append(",");
        writer.append("name");
        writer.append(",");
        writer.append("components");
        writer.append(",");
        writer.append("connections");
        writer.append(",");
        writer.append("max_connections");
        writer.append(",");
        writer.append("avg_area");
        writer.append(",");
        writer.append("avg_dimension");
        writer.append(",");
        writer.append("stdev_dimension");
        writer.append(",");
        writer.append("avg_aspect_ratio");
        writer.append(",");
        writer.append("reduced_connections");
        writer.append(",");
        writer.append("mean_connections_per_component");
        writer.append(",");
        writer.append("stdev_connections_per_component");
        writer.append(",");
        writer.append("number_of_cycles");
        writer.append(",");
        writer.append("biconnectivity");
        writer.append("\n");

    }

    public static void graphGen(JSONObject json, FileWriter writer) throws IOException {
        // add connections
        JSONArray connections = (JSONArray)json.get("connections");
        for(Object object : connections){
            JSONObject connection = (JSONObject) object;
            JSONObject sourceobject = (JSONObject)connection.get("source");
            Component source = componenthashmap.get((String)sourceobject.get("component"));
            JSONArray sinkobjectarray = (JSONArray) connection.get("sinks");

            for(Object object1 : sinkobjectarray){
                JSONObject sinkobject = (JSONObject) object1;
                Component target = componenthashmap.get((String)sinkobject.get("component"));

                directednetlist.addEdge(source,target);

                if(undirectednetlist.containsEdge(source,target) || undirectednetlist.containsEdge(target,source)){
                    continue;
                }else{
                    undirectednetlist.addEdge(source,target);
                }
            }
        }

        writer.append(",");
        writer.append(Integer.toString(undirectednetlist.edgeSet().size()));


        double insize[] = new double[undirectednetlist.vertexSet().size()];
        int i =0;
        for(Component component: undirectednetlist.vertexSet()){
            insize[i] = undirectednetlist.edgesOf(component).size();
            i++;
        }

        writer.append(",");
        writer.append(Double.toString(StatUtils.mean(insize)));

        StandardDeviation deviation = new StandardDeviation();
        writer.append(",");
        writer.append(Double.toString(deviation.evaluate(insize)));

        CycleDetector cycleDetector = new CycleDetector(directednetlist);

        writer.append(",");
        writer.append(Integer.toString(cycleDetector.findCycles().size()));

        BiconnectivityInspector biconnectivityInspector = new BiconnectivityInspector(undirectednetlist);

        writer.append(",");
        writer.append(Integer.toString(biconnectivityInspector.getCutpoints().size()));



    }
}
