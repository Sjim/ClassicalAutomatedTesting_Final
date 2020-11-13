import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class findInfectedTestMethods {
    private  static final ClassLoader WALA_CLASSLOADER= findInfectedTestMethods.class.getClassLoader();
    private static final String BASIC_FILE = "scope.txt";
    private static final String EXCLUSION_FILE = "src/main/resources/exclusion.txt";
    private static String target;
    private static String changeInfo;
    private ClassHierarchy cha;
    private CHACallGraph cg;
    private Vector<CGNode> cgnode_set = new Vector<>();
    private Set<String> dotFileForMethod = new HashSet<String>();
    private Set<String> dotFileForClass = new HashSet<String>();
    private Vector<String> changeMethods = new Vector<>();
    private Set<String> outputRes = new HashSet<>();
    findInfectedTestMethods(){

    }
    public static void main(String[] args) throws IOException, WalaException, IllegalArgumentException, InvalidClassFileException, CancelException {


        findInfectedTestMethods t= new findInfectedTestMethods();
        //get variables from commandline
        String type = args[0];//-m or -c
        target = args[1];//root path of classes
        changeInfo = args[2];//change_info.txt with changed methods

        try {
            t.initialize();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        //  int i=0;
//        for(CGNode node:t.cg){//辅助输出信息
//            if(node.getMethod() instanceof ShrikeBTMethod){
//
//                ShrikeBTMethod method = (ShrikeBTMethod)node.getMethod();
//                if("Application".equals(method.getDeclaringClass().getClassLoader().toString())){
//                    String classInnerName= method.getDeclaringClass().getName().toString();
//                    String signature=method.getSignature();
//                    System.out.println(classInnerName+" "+signature);
//                }else{
//                    //System.out.printf("'%s'不是一个ShrikeBTMethod:%s%n",node.getMethod(),node.getMethod().getClass());
//                    System.out.println(i++);
//                }
//            }
//        }

//        t.CG_DFS(t.cg.getFakeRootNode(),0);
//        generateFiles(t.dotFileForClass,"cmd","class");
//        generateFiles(t.dotFileForClass,"cmd","method"); //
        if(type.equals("-m")){//-m for method
            File output = new File("selection-method.txt");
            if(!output.exists()){
                output.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(output);
            for(String s: t.changeMethods){
                CGNode method = t.findCGNode(t.cg.getFakeRootNode(),s);
                getAffectedTests(method,t);
            }
            for (String s:
                 t.outputRes) {
                fileWriter.write(s);
            }
            fileWriter.close();
        }else if(type.equals("-c")){
            File output = new File("selection-class.txt");
            if(!output.exists()){
                output.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(output);
            for(String s: t.changeMethods){
                for (CGNode node:
                     Iterator2Iterable.make(t.cg.getSuccNodes(t.cg.getFakeRootNode()))) {
                    if(node.toString().split(",")[1].trim().equals(s.split(" ")[0])){
                        getAffectedTests(node,t);
                    }
                }
            }
            for (String s:
                    t.outputRes) {
                fileWriter.write(s);
            }
            fileWriter.close();
        }

    }
    void initialize() throws IOException, ClassHierarchyException, CancelException, InvalidClassFileException {

        //init scope
        AnalysisScope scope = AnalysisScopeReader.readJavaScope(BASIC_FILE, new File(EXCLUSION_FILE), WALA_CLASSLOADER);
        //add classes into scope
        File folder = new File(target);
        Vector<File> classes = new Vector<>();
        callGrahp.getClasses(folder,classes);
        for (File f:
                classes) {
            scope.addClassFileToScope(ClassLoaderReference.Application,f);
        }

        cha = ClassHierarchyFactory.make(scope);
        Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);
        // find the entrypoint, i.e., main()
        cg = new CHACallGraph(cha);
        // build the call graph
        cg.init(eps);
        //read the changeInfo
        File change = new File(changeInfo);
        FileInputStream file_reader = new FileInputStream(change.getAbsolutePath());
        InputStreamReader reader = new InputStreamReader(file_reader, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();
        while(line!=null){
            changeMethods.add(line);
            line = br.readLine();
        }
        file_reader.close();
    }
    CGNode findCGNode(CGNode current_node,String tar){//dfs to get node which name is tar

        // print information of current node
        if (cgnode_set.contains(current_node)){
            return null;
        }
        if(current_node.getMethod() instanceof ShrikeBTMethod){
            ShrikeBTMethod method = (ShrikeBTMethod)current_node.getMethod();
            if(method.getDeclaringClass().getClassLoader().toString().equals("Application")) {
               if(tar.equals(current_node.toString().split(",")[1].trim()+" "+cutMethod(current_node.toString()))){
                   return current_node;
                }
            }else{
                return null;
            }
        }
        cgnode_set.add(current_node);
        CGNode res = null;
        for (CGNode cn: Iterator2Iterable.make(cg.getSuccNodes(current_node))){
            res =findCGNode(cn,tar);
            if(res !=null){
                return res;
            }
        }
        cgnode_set.remove(current_node);
        return res;
    }
    static private String cutMethod(String origin){//format
        String res="";
        String[] temp= origin.split(",");
        res+=temp[1].replace("/",".").trim().substring(1)+"."+temp[2].split(" >")[0].trim();
        return res;
    }
    static private String cutClass(String origin){//format
        String res="";
        String[] temp= origin.split(",");
        res+=temp[1].trim();
        return res;
    }
    static void generateFiles(Set<String>dotFile,String name,String type) throws IOException {//from .dot set to .dot file
        File file  = new File(type+"-"+name+".dot");
        if(!file.exists()){
            file.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(file.getName());
        fileWriter.write("digraph "+name+"_"+type+"{\n");
        for (String s:
             dotFile) {
            fileWriter.write("\t"+s+";\n");
        }
        fileWriter.write("}");
        fileWriter.close();
    }
    static void getAffectedTests(CGNode cgNode, findInfectedTestMethods t){
        for (CGNode node:
                Iterator2Iterable.make(t.cg.getPredNodes(cgNode))) {
            String[] temp = node.toString().split(",");
            //format
            String methodInfected=temp[1].trim()+" "+temp[1].substring(2).replace("/",".")+"."+temp[2].split(" >")[0].trim();
            if(methodInfected.endsWith("V"))//select the tests
                t.outputRes.add(methodInfected+"\n");
        }
        for (CGNode node:
                Iterator2Iterable.make(t.cg.getPredNodes(cgNode))) {
            getAffectedTests(node,t);
        }
    }
}

