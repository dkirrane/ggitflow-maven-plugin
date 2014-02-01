import com.dkirrane.maven.plugins.ggitflow.SetupVerifyScriptHelper;

try {

    println "setup.groovy start"

    def helper = new SetupVerifyScriptHelper(basedir, localRepositoryPath, context)
    helper.setUp()
    
    println "setup.groovy complete"

} catch (Exception e) {
    System.err.println(e.getMessage())
    return false;
}
