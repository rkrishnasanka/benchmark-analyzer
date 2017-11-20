import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;

public class Main {
    //Read Files in a directory

    public static void main(String[] argv){

        String folders[] = new String[4];
        folders[0] = "/Users/krishna/Desktop/benchmarks/synthetic";
        folders[1] = "/Users/krishna/Desktop/benchmarks/realistic";
        folders[2] = "/Users/krishna/Desktop/benchmarks/chthesis";
        folders[3] = "/Users/krishna/Desktop/benchmarks/grid";

        String sets[] = new String[4];
        sets[0] = "Synthetic";
        sets[1] = "Realistic";
        sets[2] = "Huang_Thesis";
        sets[3] = "Grid";


        FileWriter writer = null;
        try {
            writer = new FileWriter("data.csv",true);
            Analysis.printHeader(writer);

        } catch (IOException e) {
            e.printStackTrace();
        }

        for(int i=0; i< folders.length; i++){
            parsefile(folders[i],writer, sets[i]);

        }

        try {
            writer.append("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void parsefile(String filepath, FileWriter writer, String setname) {
        File benchmarkfolder = new File(filepath);
        if(!benchmarkfolder.exists()){
            System.err.println("Cannot find the techlibrary folder");
            System.exit(0);
        }


        JSONParser parser;
        System.out.println("Read TECH_LIBRARY Directory:");
        System.out.println("Building Tech Library:");
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return (pathname.isFile() && pathname.getName().endsWith(".json"));
            }
        };


        for(File benchmarkfile : benchmarkfolder.listFiles(filter)){
            try {
                parser = new JSONParser();
                JSONObject json = (JSONObject)parser.parse(new FileReader(benchmarkfile));
                System.out.println("--> Added: "+ benchmarkfile.getName());
                writer.append(setname);
                writer.append(",");
                Analysis.cccGen(json,writer);
                Analysis.redGen(json,writer);
                Analysis.graphGen(json,writer);
                writer.append("\n");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

    }

}
