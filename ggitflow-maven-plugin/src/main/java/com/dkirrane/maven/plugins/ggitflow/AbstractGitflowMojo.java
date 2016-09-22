/*
 * Copyright 2014 Desmond Kirrane.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dkirrane.maven.plugins.ggitflow;

import com.dkirrane.gitflow.groovy.GitflowInit;
import com.dkirrane.gitflow.groovy.ex.GitflowException;
import com.dkirrane.maven.plugins.ggitflow.prompt.Prompter;
import com.dkirrane.maven.plugins.ggitflow.util.Finder;
import com.dkirrane.maven.plugins.ggitflow.util.Finder.Syntax;
import com.dkirrane.maven.plugins.ggitflow.util.MavenUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import static com.google.common.collect.Lists.newArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jfrog.hudson.util.GenericArtifactVersion;
import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

public class AbstractGitflowMojo extends AbstractMojo {

    public final Pattern matchSnapshotRegex = Pattern.compile("-SNAPSHOT");

    public static final ImmutableList<String> DEFAULT_INSTALL_ARGS = ImmutableList.of(
            "-DinstallAtEnd=false");

    public static final ImmutableList<String> DEFAULT_DEPLOY_ARGS = ImmutableList.of(
            "-DdeployAtEnd=false",
            "-DretryFailedDeploymentCount=2");

    public static final Splitter PROFILES_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
    public static final Joiner PROFILES_JOINER = Joiner.on(',').skipNulls();

    private static final Plugin VERSIONS_MVN_PLUGIN = plugin(
            groupId("org.codehaus.mojo"),
            artifactId("versions-maven-plugin"),
            version("2.1")
    );

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    protected List<MavenProject> reactorProjects;

    @Component
    private ProjectBuilder projectBuilder;

    @Component
    protected BuildPluginManager pluginManager;

    @Component
    protected Map<String, MavenExecutor> mavenExecutors;

    @Component(role = Prompter.class)
    protected Prompter prompter;

    /**
     * Gitflow branches and prefixes to use.
     *
     * @since 1.2
     */
    @Parameter(defaultValue = "${prefixes}")
    protected Prefixes prefixes;

    /**
     * Message prefix used for any commits made by this plugin.
     *
     * @since 1.2
     */
    @Parameter(property = "msgPrefix", defaultValue = "", required = false)
    protected String msgPrefix;

    /**
     * Message suffix used for any commits made by this plugin.
     *
     * @since 1.2
     */
    @Parameter(property = "msgSuffix", defaultValue = "", required = false)
    protected String msgSuffix;

    private GitflowInit init;
    private Path tempDir;

    protected final MavenProject getProject() {
        return project;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (null == project) {
            throw new NullPointerException("MavenProject is null");
        } else {
            getLog().debug("Gitflow pom  '" + project.getBasedir() + "'");
        }

        GitflowInit gitflowInit = getGitflowInit();

        gitflowInit.requireGitRepo();
        gitflowInit.checkRemoteConnection();

        if (!gitflowInit.gitflowIsInitialized()) {
            try {
                gitflowInit.cmdDefault();
            } catch (GitflowException ex) {
                throw new MojoExecutionException("Failed to initialise Gitflow " + ex.getMessage(), ex);
            }
        }

        gitflowInit.requireCleanWorkingTree();
    }

    public String getMsgPrefix() {
        return (StringUtils.isBlank(msgPrefix)) ? "" : msgPrefix + " ";
    }

    public String getMsgSuffix() {
        return (StringUtils.isBlank(msgSuffix)) ? "" : " " + msgSuffix;
    }

    protected final GitflowInit getGitflowInit() {
        if (null == init) {
            getLog().debug("Initialising Gitflow");
            init = new GitflowInit();
            File basedir = getProject().getBasedir();
            getLog().debug("Setting base directory " + basedir);
            init.setRepoDir(basedir);
            init.setMasterBrnName(prefixes.getMasterBranch());
            init.setDevelopBrnName(prefixes.getDevelopBranch());
            init.setFeatureBrnPref(prefixes.getFeatureBranchPrefix());
            init.setReleaseBrnPref(prefixes.getReleaseBranchPrefix());
            init.setHotfixBrnPref(prefixes.getHotfixBranchPrefix());
            init.setSupportBrnPref(prefixes.getSupportBranchPrefix());
            init.setVersionTagPref(prefixes.getVersionTagPrefix());

            /* root Git directory may not be the pom directory */
            String gitRootPath = init.executeLocal("git rev-parse --show-toplevel");
            getLog().debug("Git repo top level " + gitRootPath);
            File baseGitDir = new File(gitRootPath);
            if (baseGitDir.isDirectory()) {
                getLog().debug("Setting git base directory " + baseGitDir);
                init.setRepoDir(baseGitDir);
            }

            /* Create temp directory - delete any previous first */
            try {
                Finder finder = new Finder(Syntax.glob, "mvn-gitflow*");
                Files.walkFileTree(Paths.get(System.getProperty("java.io.tmpdir")), finder);
                List<Path> paths = finder.getPaths();
                getLog().debug("Deleting old temp directories " + paths);
                for (Path path : paths) {
                    FileUtils.deleteDirectory(path.toFile());
                }
                tempDir = Files.createTempDirectory("mvn-gitflow");
            } catch (IOException ioe) {
                getLog().error("Failed to create temp directory", ioe);
            }
        }
        return init;
    }

    protected final boolean setVersion(String version, Boolean push, String branch) throws MojoExecutionException, MojoFailureException {
        MavenProject rootProject = MavenUtil.getRootProject(reactorProjects);
        session.setCurrentProject(rootProject);
        session.setProjects(reactorProjects);
        MavenProject topLevelProject = session.getTopLevelProject();
        session.setCurrentProject(topLevelProject);

        String goal = goal("set");
        Xpp3Dom configuration = configuration(
                element(name("generateBackupPoms"), "false"),
                element(name("newVersion"), version)
        );

        String mavenCommand = getMavenCommand(VERSIONS_MVN_PLUGIN, goal);

        getLog().info("");
        getLog().info("--- " + mavenCommand + " " + topLevelProject.getArtifactId() + " to " + version + " (" + branch + ") ---");
        executeMyMojo(VERSIONS_MVN_PLUGIN, goal, configuration);
        getLog().info("------------------------------------------------------------------------");

        boolean commitMade = false;
        if (!getGitflowInit().gitIsCleanWorkingTree()) {
            String msg = getMsgPrefix() + "Updating poms to version " + version + "" + getMsgSuffix();
            getGitflowInit().executeLocal("git add -A .");
            String[] cmtPom = {"git", "commit", "-m", "\"" + msg + "\""};
            getGitflowInit().executeLocal(cmtPom);

            String currentBranch = getGitflowInit().gitCurrentBranch();
            if (push && getGitflowInit().gitRemoteBranchExists(currentBranch)) {
                String origin = getGitflowInit().getOrigin();
                String[] cmtPush = {"git", "push", origin, currentBranch};
                Integer exitCode = getGitflowInit().executeRemote(cmtPush);
                if (exitCode != 0) {
                    throw new MojoExecutionException("Failed to push version change " + version + " to origin. ExitCode:" + exitCode);
                }
            }
            commitMade = true;
        }
        /* We don't want to fail maybe the version was manually set correctly */
//        else {
//            throw new MojoFailureException("Failed to update poms to version " + version);
//        }
        return commitMade;
    }

    protected final boolean setNextVersions(Boolean allowSnapshots, Boolean updateParent, String includes) throws MojoExecutionException, MojoFailureException {
        getLog().debug("setNextVersions");

        MavenProject rootProject = MavenUtil.getRootProject(reactorProjects);
        session.setCurrentProject(rootProject);
        session.setProjects(reactorProjects);
        MavenProject topLevelProject = session.getTopLevelProject();
        session.setCurrentProject(topLevelProject);

        if (updateParent) {
            String updateParentGoal = goal("update-parent");
            Xpp3Dom configuration = configuration(
                    element(name("generateBackupPoms"), "false"),
                    element(name("allowSnapshots"), allowSnapshots.toString())
            );

            String mavenCommand = getMavenCommand(VERSIONS_MVN_PLUGIN, updateParentGoal);
            getLog().info("");
            getLog().info("--- " + mavenCommand + " " + topLevelProject.getArtifactId() + " ---");
            executeMyMojo(VERSIONS_MVN_PLUGIN, updateParentGoal, configuration);
            getLog().info("------------------------------------------------------------------------");
        }

        if (!StringUtils.isBlank(includes)) {
            String useNextVersionsGoal = goal("use-next-versions");
            Xpp3Dom configuration = configuration(
                    element(name("generateBackupPoms"), "false"),
                    element(name("allowSnapshots"), allowSnapshots.toString()),
                    element(name("includesList"), includes)
            );

            String mavenCommand = getMavenCommand(VERSIONS_MVN_PLUGIN, useNextVersionsGoal);
            getLog().info("");
            getLog().info("--- " + mavenCommand + " " + topLevelProject.getArtifactId() + " ---");
            executeMyMojo(VERSIONS_MVN_PLUGIN, useNextVersionsGoal, configuration);
            getLog().info("------------------------------------------------------------------------");
        } else {
            getLog().warn("Parameter <includes> is not set. Skipping dependency updates");
        }

        boolean commitMade = false;
        if (!getGitflowInit().gitIsCleanWorkingTree()) {
            String msg;
            if (allowSnapshots) {
                msg = getMsgPrefix() + "Replaces any release versions with the next snapshot version (if it has been deployed)." + getMsgSuffix();
            } else {
                msg = getMsgPrefix() + "Replaces snapshot versions with the corresponding release version" + getMsgSuffix();
            }

            getGitflowInit().executeLocal("git add -A .");
            String[] cmtPom = {"git", "commit", "-m", "\"" + msg + "\""};
            getGitflowInit().executeLocal(cmtPom);

            String currentBranch = getGitflowInit().gitCurrentBranch();
            if (getGitflowInit().gitRemoteBranchExists(currentBranch)) {
                String origin = getGitflowInit().getOrigin();
                String[] cmtPush = {"git", "push", origin, currentBranch};
                Integer exitCode = getGitflowInit().executeRemote(cmtPush);
                if (exitCode != 0) {
                    throw new MojoExecutionException("Failed to push version change to origin. ExitCode:" + exitCode);
                }
            }
            commitMade = true;
        }
        return commitMade;
    }

    protected final void executeMyMojo(Plugin plugin, String goal, Xpp3Dom configuration) throws MojoExecutionException, MojoFailureException {
        String mavenCommand = getMavenCommand(plugin, goal);
        MavenProject topLevelProject = session.getTopLevelProject();
        String projArtifactId = topLevelProject.getArtifactId();
        getLog().debug("START " + mavenCommand + " on " + projArtifactId);
        getLog().debug("configuration " + configuration.toUnescapedString());

        /* Maven 3.3.x log settings */
//        session.getRequest().setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_WARN);
//        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
//        System.setProperty("maven.logging.root.level", "error");
        PrintStream stdout = System.out;
        PrintStream stoutStream = null;
        Path tempStoutFile = null;
        try {

            /* Capture System.out */
            if (!getLog().isDebugEnabled()) {
                try {
                    String logFileName = plugin.getArtifactId() + '-' + goal + '-';
                    tempStoutFile = Files.createTempFile(tempDir, logFileName, ".log");
                    stoutStream = new PrintStream(new FileOutputStream(tempStoutFile.toFile(), true));
                    System.setOut(stoutStream);
                } catch (Exception ioe) {
                    getLog().warn("Failed to capture System.out", ioe);
                }
            }

            /* Execute Maven plugin goal */
            executeMojo(
                    plugin,
                    goal,
                    configuration,
                    executionEnvironment(
                            session.getTopLevelProject(),
                            session,
                            pluginManager
                    )
            );

        } catch (MojoExecutionException mee) {
            /* Reset System.out */
            System.setOut(stdout);
            String rootCauseMessage = ExceptionUtils.getRootCauseMessage(mee);
            if (rootCauseMessage.contains("Project version is inherited from parent")) {
                getLog().debug("Skipping " + mavenCommand + " for project " + projArtifactId + ". Project version is inherited from parent.");
            } else {
                getLog().error("Failed to execute " + mavenCommand + " on " + projArtifactId, mee);
                throw mee;
            }
        } finally {
            if (!getLog().isDebugEnabled()) {
                /* Reset System.out */
                System.setOut(stdout);

                if (null != tempStoutFile) {
                    getLog().info(mavenCommand + " log " + tempStoutFile.toString());
                }

                if (null != stoutStream) {
                    try {
                        stoutStream.close();
                    } catch (Exception e) {
                    }
                }
            }
            getLog().debug("DONE " + mavenCommand);
        }
    }

    private String getMavenCommand(Plugin plugin, String goal) {
        return plugin.getArtifactId() + ':' + plugin.getVersion() + ':' + goal;
    }

    protected final void runGoals(String goals, List<String> additionalArgs) throws MojoExecutionException, MojoFailureException {
        getLog().debug("START executing " + goals + " with args " + additionalArgs);

        MavenProject rootProject = MavenUtil.getRootProject(reactorProjects);
        File basedir = rootProject.getBasedir();

        ReleaseResult result = new ReleaseResult();
        ReleaseEnvironment env = new DefaultReleaseEnvironment();
        env.setSettings(session.getSettings());
        MavenExecutor mavenExecutor = mavenExecutors.get(env.getMavenExecutorId());

        Joiner joiner = Joiner.on(" ").skipNulls();
        String additionalArguments = joiner.join(additionalArgs);
        getLog().debug("additionalArguments " + additionalArguments);

        try {
            mavenExecutor.executeGoals(basedir, goals, env, false, additionalArguments, result);
        } catch (MavenExecutorException ex) {
            throw new MojoExecutionException(result.getOutput(), ex);
        }
        getLog().debug("DONE executing " + goals);
    }

    protected final String getReleaseVersion(String version) throws MojoFailureException {
        getLog().debug("Current Develop version '" + version + "'");

        GenericArtifactVersion artifactVersion = new GenericArtifactVersion(version);

        String primaryNumbersAsString = artifactVersion.getPrimaryNumbersAsString();
        String annotationAsString = artifactVersion.getAnnotationAsString();
        String buildSpecifierAsString = artifactVersion.getBuildSpecifierAsString();

        final StringBuilder result = new StringBuilder(30);
        result.append(primaryNumbersAsString).append(annotationAsString);

        if (!StringUtils.isBlank(buildSpecifierAsString)) {
            getLog().debug("Removing build specifier " + buildSpecifierAsString + " from version " + version);
        }

        return result.toString();
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

    protected void reloadReactorProjects() {
        getLog().debug("Reloading poms...");

        List<MavenProject> updatedReactorProjects = new ArrayList<>();

        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        getLog().debug("rootProject = " + rootProject);
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

        try {
            ReactorManager reactorManager = new ReactorManager(updatedReactorProjects);
            updatedReactorProjects = reactorManager.getSortedProjects();
        } catch (CycleDetectedException | DuplicateProjectException ex) {
            getLog().error("Failed to sort reactor projects", ex);
        }

        session.setProjects(updatedReactorProjects);
        project = ReleaseUtil.getRootProject(updatedReactorProjects);
        reactorProjects = updatedReactorProjects;

        if (getLog().isDebugEnabled()) {
            getLog().debug("Updated MavenSession: " + session);
            getLog().debug("Updated MavenProject: " + project);
            getLog().debug("Updated reactorProjects: ");
            for (int i = 0; i < reactorProjects.size(); i++) {
                MavenProject updatedReactorProject = updatedReactorProjects.get(i);
                System.out.println("");
                System.out.printf(" %d) %s:%s \n \t\t\t Parent: %s \n \t\t\t DependencyManagement:%s \n \t\t\t Dependencies:%s \n",
                        i,
                        updatedReactorProject.getArtifactId(), updatedReactorProject.getVersion(),
                        updatedReactorProject.getParentArtifact(),
                        null != updatedReactorProject.getDependencyManagement() ? updatedReactorProject.getDependencyManagement().getDependencies() : null,
                        updatedReactorProject.getDependencies());
                System.out.println("");
            }
        }

        getLog().debug("Reloading poms complete");
    }

    protected List<String> rearrange(String input, List<String> strings) {
        strings.remove(input);
        strings.add(0, input);
        return strings;
    }

}
