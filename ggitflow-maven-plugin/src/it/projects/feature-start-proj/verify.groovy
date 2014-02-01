import com.dkirrane.maven.plugins.ggitflow.SetupVerifyScriptHelper;

try {

    println "verify.groovy start"

    def helper = new SetupVerifyScriptHelper(basedir, localRepositoryPath, context)

    // vagrant-maven-plugin invoked
    helper.assertBuildLogContains("vagrant-maven-plugin:")

    // feature-start goal successful
    helper.assertBuildLogContains(":feature-start")

    // the feature branch should be created
    helper.featureBranchExists("Feature-123")

    helper.tearDown()

    println "verify.groovy complete"

} catch (Exception e) {
    System.err.println(e.getMessage())
    return false;
}


////import net.nicoulaj.maven.plugins.vagrant.it.PrePostBuildScriptHelper
//
//try {
//    helper = new PrePostBuildScriptHelper(basedir, localRepositoryPath, context)
//
//    // vagrant-maven-plugin invoked
//    helper.assertBuildLogContains("vagrant-maven-plugin:")
//
//    // init goal successful
//    helper.assertBuildLogContains(":init")
//    helper.assertBuildLogContains("A `Vagrantfile` has been placed in this directory")
//    helper.assertFileExists("Vagrantfile")
//    helper.assertFileContains("Vagrantfile", "config.vm.box = \"testbox\"")
//
//}
//catch (Exception e) {
//    System.err.println(e.getMessage())
//    return false;
//}
