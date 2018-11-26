package main.git;


import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import javafx.util.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.Patch;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;


public class GitAnalyzer {
    private Git git;
    private Repository repository;
    private Map<String, Integer> changeTimes = new HashMap<String, Integer>();
    private Map<String, Integer> changeTimes_patch_with_one_file = new HashMap<>();
    String patchString = "";
    private ObjectId firstCommit = null;


    private int count0 = 0, count1=0, count3=0, count5=0, count10=0;

    public void print(){
        for(String methodName: this.changeTimes.keySet()){
            //System.out.println(methodName + ": " + this.changeTimes.get(methodName));
            if(this.changeTimes.get(methodName) == 0) count0 ++;
            else if(this.changeTimes.get(methodName) == 1) count1 ++;
            else if(this.changeTimes.get(methodName) <= 3) count3 ++;
            else if(this.changeTimes.get(methodName) <= 5) count5 ++;
            else count10 ++;
        }
        System.out.println("" + count0 + " " + count1 + " " + count3 + " " + count5 + " " + count10);

        count0 = count1= count3 = count5 = count10=0;
        for(String methodName: this.changeTimes.keySet()){
            //System.out.println(methodName + ": " + this.changeTimes_patch_with_one_file.get(methodName));
            if(this.changeTimes_patch_with_one_file.get(methodName) == 0) count0 ++;
            else if(this.changeTimes_patch_with_one_file.get(methodName) == 1) count1 ++;
            else if(this.changeTimes_patch_with_one_file.get(methodName) <= 3) count3 ++;
            else if(this.changeTimes_patch_with_one_file.get(methodName) <= 5) count5 ++;
            else count10 ++;
        }
        System.out.println("" + count0 + " " + count1 + " " + count3 + " " + count5 + " " + count10);
    }

    public List<String> getLog(){
        List<String> result = new ArrayList<String>();

        Iterator<RevCommit> commits = git.log().call().iterator();
        while(commits.hasNext()){
            RevCommit commit = commits.next();
            result.add(commit.getId());
        }
        return result;
    }

    public GitAnalyzer(String filePath){

        try {
            git = Git.open(new File(filePath));
            repository = git.getRepository();
            getFirstCommit();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void getFirstCommit(){
        try {
            Iterator<RevCommit> commits = git.log().call().iterator();
            System.out.println("getting first commit");
            while (commits.hasNext()) {
                firstCommit = commits.next().getId();
            }
            System.out.println("first commit: " + firstCommit.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    List<String> parse(String former, String latter, Patch patch){
        return null;
    }

    public void start(){
        List<String> files = getAllFiles(Constants.HEAD, ".java");
        System.out.println(files.size());
        int count = 0;
        for(String file: files){
            if (count % 100 == 0){
                System.out.println("" + count0 + " " + count1 + " " + count3 + " " + count5 + " " + count10);
            }
            System.out.println(file);
            analyzeSingleFile(file);
            count ++;
        }
    }


    public void analyzeSingleFile(String filePath){
        List<Pair<ObjectId,Pair<String,String>>> commitPairs = getAllCommitModifyAFile(filePath);
        for(Pair pair: commitPairs) {
            //System.out.println("  " + commit.toString());
            try {
                ObjectId commit = (ObjectId) (pair.getKey());
                Pair<String, String> filePair = (Pair<String,String>)(pair.getValue());
                String newFile = getFileFromCommit(repository.resolve(commit.getName()), filePair.getValue().toString());
                String oldFile = getFileFromCommit(repository.resolve(commit.getName() + "^"), filePair.getKey().toString());
                ClassParser newParser = new ClassParser(newFile);
                Set<String> methodNames = newParser.getAllMethods();

                ClassParser oldParser = new ClassParser(oldFile);
                methodNames.addAll(oldParser.getAllMethods());

                for(String methodName: methodNames){
                    if(!changeTimes.containsKey(methodName)){
                        changeTimes.put(methodName,0);
                        changeTimes_patch_with_one_file.put(methodName, 0);
                    }
                }

                Patch patch = getPatch(commit, null, filePath);
                int ccc = 0;
                for(FileHeader header : patch.getFiles()){
                    ccc ++;
                }


                Set<String> changeMethodName = new HashSet<>();
                changeMethodName.addAll(newParser.getChagneMethod(patch,true));
                changeMethodName.addAll(oldParser.getChagneMethod(patch,false));
                for(String methodName: newParser.getChagneMethod(patch, true)){
                    changeTimes.put(methodName, changeTimes.get(methodName) + 1);
                    if(ccc == 1){
                        changeTimes_patch_with_one_file.put(methodName, changeTimes_patch_with_one_file.get(methodName) + 1);
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public Patch getPatch(ObjectId newId, ObjectId oldId, String filePath){
        Patch patch = new Patch();
        try (ObjectReader reader = repository.newObjectReader()) {
            CanonicalTreeParser old = new CanonicalTreeParser();
            ObjectId oldTreeId = repository.resolve(newId.getName() + "^{tree}");
            old.reset(reader, oldTreeId);

            CanonicalTreeParser n = new CanonicalTreeParser();
            ObjectId newTreeId = repository.resolve((oldId == null? newId.getName() + "^" : oldId.getName())+ "^{tree}");
            n.reset(reader, newTreeId);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<DiffEntry> diffs = git.diff().setNewTree(n).setOldTree(old).
                    setPathFilter(PathFilter.create(filePath)).setOutputStream(out).call();
            String s = out.toString();
            patchString = s;

            byte[] bytes = s.getBytes();
            patch.parse(new ByteInputStream(bytes, bytes.length));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return patch;
    }

    List<String> getAllFiles(String version, String fileFilter ){
        List<String> result = new ArrayList<>();
        try(TreeWalk treeWalk = new TreeWalk(repository)){
            ObjectId commitId = repository.resolve(version + "^{tree}");
            treeWalk.reset(commitId);

            int count = 0;
            while(treeWalk.next()){
                if(treeWalk.isSubtree()){
                    treeWalk.enterSubtree();
                }else{
                    String path = treeWalk.getPathString();
                    if(fileFilter == null || path.endsWith(fileFilter)){
                        result.add(path);
                        count ++;
                    }
                }
            }
            System.out.println(count);
        }catch (Exception e){
            e.printStackTrace();
        }

        return result;
    }

    List<Method> getChangedMethod(ObjectId version, DiffEntry diff){
        return null;
    }

    String getFormerName(ObjectId commitId, String file){
        try {
            ObjectReader objectReader = repository.newObjectReader();
            ObjectLoader objectLoader = objectReader.open(commitId);
            RevCommit commit = RevCommit.parse(objectLoader.getBytes());
            return getFormerName(commit, file);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    String getFormerName(RevCommit cur, String file){
        String formerName = null;
        try{
            TreeWalk tw = new TreeWalk(repository);
            tw.setRecursive(true);
            tw.addTree(repository.resolve(cur.getName() + "^{tree}"));
            tw.addTree(repository.resolve(cur.getName() + "^^{tree}"));

            RenameDetector rd = new RenameDetector(repository);
            rd.addAll(DiffEntry.scan(tw));

            List<DiffEntry> diffs = rd.compute(tw.getObjectReader(), null);
            for(DiffEntry diff: diffs){
                if(diff.getScore() >= rd.getRenameScore() && diff.getOldPath().equals(file)){
                    formerName = diff.getNewPath();
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            return formerName;
        }
    }

    List<Pair<ObjectId, Pair<String, String>>> getAllCommitModifyAFile(String filePath){
        List<Pair<ObjectId, Pair<String, String>>> result = new ArrayList<>();
        try {
            ObjectId endId = repository.resolve("HEAD");
            while(true){
                Iterator<RevCommit> commits = git.log().addPath(filePath)
                        .addRange(firstCommit, endId)
                        .setMaxCount(10000).call().iterator();
                boolean signal = false;
                while(commits.hasNext()){
                    RevCommit commit = commits.next();

                    if(commits.hasNext())
                        result.add(new Pair<ObjectId, Pair<String, String>>(commit, new Pair<String, String>(filePath,filePath)));
                    else{
                        String oldPath = filePath;
                        filePath = getFormerName(commit, filePath);
                        if(filePath != null){
                            result.add(new Pair<ObjectId, Pair<String, String>>(commit, new Pair<String, String>(filePath,oldPath)));
                            signal = true;
                            endId = repository.resolve(commit.getName() + "^");
                            break;
                        }else{
                            result.add(new Pair<ObjectId, Pair<String, String>>(commit, new Pair<String, String>(oldPath,oldPath)));
                        }

                    }
                }
                if(!signal)
                    break;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    String getFileFromCommit(ObjectId version, String filePath){
        String result = "";
        try(TreeWalk treeWalk = new TreeWalk(repository)){
            treeWalk.reset(repository.resolve(version.getName()+"^{tree}"));
            treeWalk.setFilter(PathFilter.create(filePath));
            treeWalk.setRecursive(true);
            if(treeWalk.next()){
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                result = new String(loader.getBytes());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String[] args){

        //GitAnalyzer poi_analyzer = new GitAnalyzer("C:\\Users\\oliver\\Downloads\\lucene-solr-master\\poi");
        //poi_analyzer.start();

        GitAnalyzer lucene_analyzer = new GitAnalyzer("C:\\Users\\oliver\\Downloads\\lucene-solr-master\\lucene-solr");
        lucene_analyzer.start();


        lucene_analyzer.print();
        //poi_analyzer.print();

    }

}

//lucene/core/src/java/org/apache/lucene/util/BytesRefArray.java