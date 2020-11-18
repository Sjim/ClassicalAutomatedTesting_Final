import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
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
import java.util.*;

public class callGrahp {
    private  static final ClassLoader WALA_CLASSLOADER= findInfectedTestMethods.class.getClassLoader();
    private static final String BASIC_FILE = "scope.txt";
    private static final String EXCLUSION_FILE = "src/main/resources/exclusion.txt";
    private ClassHierarchy cha;
    private CHACallGraph cg;
    private Vector<CGNode> cgnode_set = new Vector<>();
    private Set<String> dotFileForMethod = new HashSet<String>();
    private Set<String> dotFileForClass = new HashSet<String>();

    public static void main(String[] args) throws WalaException, IOException {
        callGrahp callGrahp= new callGrahp();
        try {
            callGrahp.init();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        callGrahp.CG_DFS(callGrahp.cg.getFakeRootNode(),0);
        //output the dot file
        findInfectedTestMethods.generateFiles(callGrahp.dotFileForMethod,"DataLog","method");
        findInfectedTestMethods.generateFiles(callGrahp.dotFileForClass,"DataLog","class");
    }
    void init() throws IOException, ClassHierarchyException, CancelException, InvalidClassFileException {

        //init scope
        AnalysisScope scope = AnalysisScopeReader.readJavaScope(BASIC_FILE, new File(EXCLUSION_FILE), WALA_CLASSLOADER);
        //add target class files
        //TODO change Here to get the target files
        File target = new File("E:\\study\\大三上\\自动化测试\\ClassicalAutomatedTesting_Final\\ClassicalAutomatedTest\\src\\test\\java");

        Vector<File> classes = new Vector<>();
        getClasses(target, classes);
        for (File f:
             classes) {
            scope.addClassFileToScope(ClassLoaderReference.Application,f);
        }
        //System.out.println(scope);
        cha = ClassHierarchyFactory.make(scope);
        // find the entrypoint, i.e., main()
        Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);
        cg = new CHACallGraph(cha);
        // build the call graph
        cg.init(eps);

    }
    public int CG_DFS(CGNode current_node, int level) throws WalaException {
        if (cgnode_set.contains(current_node)){
            return 0;
        }
        if(current_node.getMethod() instanceof ShrikeBTMethod){
            ShrikeBTMethod method = (ShrikeBTMethod)current_node.getMethod();
            if(method.getDeclaringClass().getClassLoader().toString().equals("Application")) {
                System.out.println("level" + String.format("%-3d", level) + ":" +
                        String.join("", Collections.nCopies(level, "\t")) + current_node.toString());
                //print information of current node
                for(CGNode suc: Iterator2Iterable.make(cg.getPredNodes(current_node))){
                    if(suc.getMethod() instanceof ShrikeBTMethod){
                        ShrikeBTMethod nextMethod = (ShrikeBTMethod)suc.getMethod();
                        if(nextMethod.getDeclaringClass().getClassLoader().toString().equals("Application")){
                            //add the method and class calling relations into dotFile
                            String methodString = "\""+cutMethod(current_node.toString())+"\""+" -> "+"\""+cutMethod(suc.toString())+"\"";
                            dotFileForMethod.add(methodString);
                            String classString = "\""+cutClass(current_node.toString())+"\""+" -> "+"\""+cutClass(suc.toString())+"\"";
                            dotFileForClass.add(classString);
                        }
                    }
                }
            }else{
                return 0;
            }
        }
        cgnode_set.add(current_node);//dfs of graph
        for (CGNode cn: Iterator2Iterable.make(cg.getSuccNodes(current_node))){
            CG_DFS(cn, level+1);
        }
        cgnode_set.remove(current_node);
        return 0;
    }
    static private String cutMethod(String origin){ //format
        String res="";
        String[] temp= origin.split(",");
        res+=temp[1].replace("/",".").trim().substring(1)+"."+temp[2].split(" >")[0].trim();
        return res;
    }
    static private String cutClass(String origin){ // format
        String res="";
        String[] temp= origin.split(",");
        res+=temp[1].trim();
        return res;
    }
    static public void getClasses(File origin,Vector<File> classes){//get all .class files in the root file
        if(!origin.isFile()){
            for (File innerFolder:
                    Objects.requireNonNull(origin.listFiles())) {
                if(innerFolder.isFile()&&innerFolder.getName().endsWith(".class")){
                    classes.add(innerFolder);
                }else {
                    getClasses(innerFolder,classes);
                }
            }
        }


    }
}
