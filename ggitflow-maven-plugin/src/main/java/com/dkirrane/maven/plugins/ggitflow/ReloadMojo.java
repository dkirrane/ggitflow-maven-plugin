package com.dkirrane.maven.plugins.ggitflow;

import com.dkirrane.gitflow.groovy.GitflowInit;
import com.dkirrane.maven.plugins.ggitflow.prompt.Prompter;
import static com.google.common.collect.Lists.newArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.locator.DefaultModelLocator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.project.ProjectSorter;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * Standalone Mojo for testing reload of POM files.
 *
 * <br/><br/>
 * Test with Maven 3.0.5
 * <pre>
 *      export M2_HOME=C:/apache-maven-3.0.5
 *      ${M2_HOME}/bin/mvn com.dkirrane.maven.plugins:ggitflow-maven-plugin:3.0-SNAPSHOT:reload
 * </pre>
 *
 * Test with Maven 3.3.9
 * <pre>
 *      export M2_HOME=C:/apache-maven-3.3.9
 *      ${M2_HOME}/bin/mvn gitflow:reload
 * </pre>
 */
@Mojo(name = "reload", aggregator = true)
public class ReloadMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    protected List<MavenProject> reactorProjects;

    @Component
    private ProjectBuilder projectBuilder;

    @Component(role = Prompter.class)
    protected Prompter prompter;

    private GitflowInit init;

    public final Pattern matchSnapshotRegex = Pattern.compile("-SNAPSHOT");

    @Override
    public void execute() throws MojoExecutionException {

//        List<String> tags = getGitflowInit().gitAllTags();
//        if (tags.isEmpty()) {
//            throw new MojoExecutionException("Could not find any tags!");
//        }
//
//        String promptChoice;
//        try {
//            promptChoice = prompter.promptChoice("Git tags", "Please select a Git tag", tags);
//        } catch (IOException ex) {
//            throw new MojoExecutionException("Failed to prompt user", ex);
//        }
//        getLog().info("promptChoice = " + promptChoice);
        System.out.println("\n\n");
        checkoutBranch("develop");
        reloadReactorProjects();
//        checkForSnapshotDependencies();

        System.out.println("\n\n");
        checkoutBranch("master");
        reloadReactorProjects();
//        checkForSnapshotDependencies();
    }

    protected final void reloadReactorProjects() {
        getLog().debug("Reloading poms...");

        List<MavenProject> updatedReactorProjects = new ArrayList<>();

        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        getLog().debug("rootProject = " + rootProject);
        try {
            if (rootProject.getFile().exists() && rootProject.getFile().canRead()) {
                MavenExecutionRequest mavenExecutionRequest = session.getRequest();
                ProjectBuildingRequest projectBuildingRequest = mavenExecutionRequest.getProjectBuildingRequest();
                try {
                    List<ProjectBuildingResult> buildResults = projectBuilder.build(newArrayList(rootProject.getFile()), true, projectBuildingRequest);
                    for (ProjectBuildingResult buildResult : buildResults) {
                        MavenProject reloadProject = buildResult.getProject();
                        reloadProject.setActiveProfiles(rootProject.getActiveProfiles());
                        updatedReactorProjects.add(reloadProject);
                    }
                } catch (ProjectBuildingException ex) {
                    getLog().error("Build error reloading Maven projects", ex);
                }
            }
        } catch (Exception ex) {
            getLog().error("Failed to reload reactor projects", ex);
            throw ex;
        }

        try {
            ProjectSorter projectSorter = new ProjectSorter(updatedReactorProjects);
            updatedReactorProjects = projectSorter.getSortedProjects();
        } catch (CycleDetectedException | DuplicateProjectException ex) {
            getLog().error("Failed to sort reactor projects", ex);
        }

        session.setProjects(updatedReactorProjects);
        project = ReleaseUtil.getRootProject(updatedReactorProjects);
        reactorProjects = updatedReactorProjects;

        if (getLog().isDebugEnabled()) {
            getLog().debug("Reloaded MavenProject: " + project);
            getLog().debug("Reloaded reactorProjects: ");
            for (int i = 0; i < reactorProjects.size(); i++) {
                MavenProject updatedReactorProject = updatedReactorProjects.get(i);
                System.out.printf(" %-4s %-70s [Parent:%s]\n",
                        i + ")",
                        updatedReactorProject.getArtifact(),
                        updatedReactorProject.getParentArtifact());
//                DependencyManagement dependencyManagement = updatedReactorProject.getDependencyManagement();
//                if (null != dependencyManagement && null != dependencyManagement.getDependencies()) {
//                    System.out.printf("\t\t\t DependencyManagement:\n");
//                    for (Dependency dependency : dependencyManagement.getDependencies()) {
//                        System.out.printf("\t\t\t\t\t %s:%s:%s \n", dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
//                    }
//                }
//                if (null != updatedReactorProject.getDependencies()) {
//                    System.out.printf("\t\t\t Dependencies:\n");
//                    for (Dependency dependency : updatedReactorProject.getDependencies()) {
//                        System.out.printf("\t\t\t\t\t %s:%s:%s \n", dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
//                    }
//                }
            }
        }

        getLog().debug("Reloading poms complete");
    }

    private static Model readModel(File pomFile) {
        DefaultModelProcessor modelProcessor = new DefaultModelProcessor();
        modelProcessor.setModelLocator(new DefaultModelLocator());
        modelProcessor.setModelReader(new DefaultModelReader());

        try {
            HashMap<String, Object> options = new HashMap<>();
            return modelProcessor.read(pomFile, options);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build model from pom " + pomFile, ex);
        }
    }

    protected final void checkForSnapshotDependencies() throws MojoExecutionException {
        getLog().info("Checking for SNAPSHOT dependencies");
        Boolean hasDepSnapshots = false;
        Boolean hasParentSnapshot = false;
        for (MavenProject mavenProject : reactorProjects) {
            String artifactId = mavenProject.getArtifactId();

            /* Check <parent> */
            Artifact parentArtifact = mavenProject.getParentArtifact();
            if (parentArtifact != null && parentArtifact.isSnapshot()) {
                getLog().error("Parent of project " + artifactId + " is a SNAPSHOT " + parentArtifact.getId());
                hasParentSnapshot = true;
            }

            /* Check <dependencyManagement> */
            DependencyManagement dependencyManagement = mavenProject.getDependencyManagement();
            if (null != dependencyManagement) {
                if (checkForSnapshot(artifactId, dependencyManagement.getDependencies())) {
                    hasDepSnapshots = true;
                }
            }

            /* Check <dependencies> */
            if (checkForSnapshot(artifactId, mavenProject.getDependencies())) {
                hasDepSnapshots = true;
            }
        }
        if (hasParentSnapshot || hasDepSnapshots) {
            throw new MojoExecutionException("Cannot release because SNAPSHOT dependencies exist");
        }
    }

    private boolean checkForSnapshot(String artifactId, List<Dependency> dependencies) throws MojoExecutionException {
        Boolean hasSnapshotDependency = false;
        for (Dependency dependency : dependencies) {
            String version = dependency.getVersion();
            Matcher versionMatcher = matchSnapshotRegex.matcher(version);
            if (versionMatcher.find() && versionMatcher.end() == version.length()) {
                getLog().error("Project " + artifactId + " contains SNAPSHOT dependency: " + dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + version);
                hasSnapshotDependency = true;
            }
        }
        return hasSnapshotDependency;
    }

    private static void watch(final Process process) {
        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        try {
            while ((line = input.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkoutBranch(String branch) throws MojoExecutionException {
        ProcessBuilder builder = new ProcessBuilder("git", "checkout", branch);
        builder.redirectErrorStream(true);
        final Process process;
        try {
            process = builder.start();
        } catch (IOException ex) {
            throw new MojoExecutionException("", ex);
        }
        watch(process);
    }

    protected final GitflowInit getGitflowInit() {
        if (null == init) {
            getLog().debug("Initialising Gitflow");
            init = new GitflowInit();
            File basedir = project.getBasedir();
            getLog().debug("Setting base directory " + basedir);
            init.setRepoDir(basedir);
            init.setMasterBrnName("master");
            init.setDevelopBrnName("develop");
            init.setFeatureBrnPref("feature/");
            init.setReleaseBrnPref("release/");
            init.setHotfixBrnPref("hotfix/");
            init.setSupportBrnPref("support/");
            init.setVersionTagPref("");

            /* root Git directory may not be the pom directory */
            String gitRootPath = init.executeLocal("git rev-parse --show-toplevel");
            getLog().debug("Git repo top level " + gitRootPath);
            File baseGitDir = new File(gitRootPath);
            if (baseGitDir.isDirectory()) {
                getLog().debug("Setting git base directory " + baseGitDir);
                init.setRepoDir(baseGitDir);
            }

            /* Create temp directory - delete any previous first */
//            try {
//                Finder finder = new Finder(Syntax.glob, "mvn-gitflow*");
//                Files.walkFileTree(Paths.get(System.getProperty("java.io.tmpdir")), finder);
//                List<Path> paths = finder.getPaths();
//                getLog().debug("Deleting old temp directories " + paths);
//                for (Path path : paths) {
//                    FileUtils.deleteDirectory(path.toFile());
//                }
//                tempDir = Files.createTempDirectory("mvn-gitflow");
//            } catch (IOException ioe) {
//                getLog().error("Failed to create temp directory", ioe);
//            }
        }
        return init;
    }

}
