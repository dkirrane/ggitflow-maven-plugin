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
import com.dkirrane.maven.plugins.ggitflow.util.MavenUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectSorter;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;
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

/**
 *
 */
public class AbstractGitflowMojo extends AbstractMojo {

    public final Pattern matchSnapshotRegex = Pattern.compile("-SNAPSHOT");

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

    /**
     * Skips any calls to mvn install
     *
     * @since 1.2
     */
    @Parameter(property = "skipBuild", defaultValue = "false", required = false)
    protected Boolean skipBuild;

    /**
     * Skips any calls to mvn deploy
     *
     * @since 1.2
     */
    @Parameter(property = "skipDeploy", defaultValue = "false", required = false)
    protected Boolean skipDeploy;

    /**
     * The commit to start the branch from.
     *
     * @since 1.2
     */
    @Parameter(property = "startCommit", defaultValue = "", required = false)
    protected String startCommit;

    /**
     * If true, all branch commits will be squashed into a single commit before
     * the merge.
     *
     * @since 1.2
     */
    @Parameter(property = "squash", defaultValue = "false", required = false)
    protected Boolean squash;

    /**
     * If true, the branch will not be deleted after the merge.
     *
     * @since 1.2
     */
    @Parameter(property = "keep", defaultValue = "false", required = false)
    protected Boolean keep;

    /**
     * If true, any Git tags created will be signed.
     *
     * @since 1.2
     */
    @Parameter(property = "sign", defaultValue = "false", required = false)
    protected Boolean sign;

    /**
     * If GPG key used to sign the tag.
     *
     * @since 1.2
     */
    @Parameter(property = "signingkey", defaultValue = "", required = false)
    protected String signingkey;

    /**
     * Component used to prompt for input.
     */
    @Component
    protected Prompter prompter;

    @Component
    protected Map<String, MavenExecutor> mavenExecutors;

    @Component
    protected ArtifactResolver artifactResolver;

    /**
     * @parameter default-value="${project.artifacts}"
     * @required
     * @readonly
     */
    protected Collection artifacts;

    /**
     * @parameter expression="${localRepository}"
     */
    protected ArtifactRepository localRepository;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    protected List remoteArtifactRepositories;

//    @Component
//    protected MavenProjectBuilder projectBuilder;
//    @Component
//    protected DefaultProjectBuilder projectBuilder;
//    /**
//     * @parameter property="plugin"
//     * @required
//     */
//    @Component
//    protected PluginDescriptor pluginDescriptor;
//
    /**
     * The projects in the reactor.
     */
    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    protected List<MavenProject> reactorProjects;

    /**
     * The project builder
     */
    @Component
    private ProjectBuilder projectBuilder;

    /**
     * The project currently being build.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    @Component
    protected MavenProject project;

    /**
     * The current Maven session.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    @Component
    protected MavenSession session;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    @Component
    protected BuildPluginManager pluginManager;

    private GitflowInit init;

    protected final MavenProject getProject() {
        return project;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (null == project) {
            throw new NullPointerException("MavenProject is null");
        } else {
            getLog().debug("Gitflow pom  '" + project.getBasedir() + "'");
        }

        GitflowInit gitflowInit = getGitflowInit();

        gitflowInit.requireGitRepo();

        if (!gitflowInit.gitflowIsInitialized()) {
            try {
                gitflowInit.cmdDefault();
            } catch (GitflowException ex) {
                throw new MojoExecutionException("Failed to inintialise Gitflow " + ex.getMessage(), ex);
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
            init = new GitflowInit();
            init.setRepoDir(getProject().getBasedir());
            init.setMasterBrnName(prefixes.getMasterBranch());
            init.setDevelopBrnName(prefixes.getDevelopBranch());
            init.setFeatureBrnPref(prefixes.getFeatureBranchPrefix());
            init.setReleaseBrnPref(prefixes.getReleaseBranchPrefix());
            init.setHotfixBrnPref(prefixes.getHotfixBranchPrefix());
            init.setSupportBrnPref(prefixes.getSupportBranchPrefix());
            init.setVersionTagPref(prefixes.getVersionTagPrefix());

            /* root Git directory may not be the pom directory */
            String dotGitDir = init.executeLocal("git rev-parse --git-dir");
            File dotGitFolder = new File(dotGitDir);
            assert dotGitFolder.exists();
            init.setRepoDir(dotGitFolder.getParentFile());
        }
        return init;
    }

    protected final void setVersion(String version) throws MojoExecutionException, MojoFailureException {
        getLog().debug("START org.codehaus.mojo:versions-maven-plugin:2.1:set '" + version + "'");
        MavenProject rootProject = MavenUtil.getRootProject(reactorProjects);
        executeMojo(
                plugin(
                        groupId("org.codehaus.mojo"),
                        artifactId("versions-maven-plugin"),
                        version("2.1")
                ),
                goal("set"),
                configuration(
                        element(name("generateBackupPoms"), "false"),
                        element(name("newVersion"), version)
                ),
                executionEnvironment(
                        rootProject,
                        session,
                        pluginManager
                )
        );
        getLog().debug("DONE org.codehaus.mojo:versions-maven-plugin:2.1:set '" + version + "'");

        if (!getGitflowInit().gitIsCleanWorkingTree()) {
            String msg = getMsgPrefix() + "Updating poms to version " + version + "" + getMsgSuffix();
            getGitflowInit().executeLocal("git add -A .");
            String[] cmtPom = {"git", "commit", "-m", "\"" + msg + "\""};
            getGitflowInit().executeLocal(cmtPom);

            String currentBranch = getGitflowInit().gitCurrentBranch();
            if (getGitflowInit().gitRemoteBranchExists(currentBranch)) {
                String origin = getGitflowInit().getOrigin();
                String[] cmtPush = {"git", "push", origin, currentBranch};
                Integer exitCode = getGitflowInit().executeRemote(cmtPush);
                if (exitCode != 0) {
                    throw new MojoExecutionException("Failed to push version change " + version + " to origin. ExitCode:" + exitCode);
                }
            }
        }
        /* We don't want to fail maybe the version was manually set correctly */
//        else {
//            throw new MojoFailureException("Failed to update poms to version " + version);
//        }
    }

    protected final void setReleaseVersions() throws MojoExecutionException, MojoFailureException {
        getLog().debug("START org.codehaus.mojo:versions-maven-plugin:2.1:use-releases");
        MavenProject rootProject = MavenUtil.getRootProject(reactorProjects);
        executeMojo(
                plugin(
                        groupId("org.codehaus.mojo"),
                        artifactId("versions-maven-plugin"),
                        version("2.1")
                ),
                goal("use-releases"),
                configuration(
                        element(name("generateBackupPoms"), "false")
                ),
                executionEnvironment(
                        rootProject,
                        session,
                        pluginManager
                )
        );
        getLog().debug("DONE org.codehaus.mojo:versions-maven-plugin:2.1:use-releases");

        if (!getGitflowInit().gitIsCleanWorkingTree()) {
            String msg = getMsgPrefix() + "Replacing -SNAPSHOT versions with their corresponding releases" + getMsgSuffix();
            getGitflowInit().executeLocal("git add -A .");
            String[] cmtPom = {"git", "commit", "-m", "\"" + msg + "\""};
            getGitflowInit().executeLocal(cmtPom);

            String currentBranch = getGitflowInit().gitCurrentBranch();
            if (getGitflowInit().gitRemoteBranchExists(currentBranch)) {
                String origin = getGitflowInit().getOrigin();
                String[] cmtPush = {"git", "push", origin, currentBranch};
                Integer exitCode = getGitflowInit().executeRemote(cmtPush);
                if (exitCode != 0) {
                    throw new MojoExecutionException("Failed to push release version change to origin. ExitCode:" + exitCode);
                }
            }
        }
    }

    protected final void clean() throws MojoExecutionException, MojoFailureException {
        getLog().debug("START org.apache.maven.plugins:maven-clean-plugin:2.5:clean");
        MavenProject rootProject = MavenUtil.getRootProject(reactorProjects);
        session.setCurrentProject(rootProject);
        session.setProjects(reactorProjects);
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-clean-plugin"),
                        version("2.5")
                ),
                goal("clean"),
                configuration(
                        element(name("skip"), "false")
                ),
                executionEnvironment(
                        rootProject,
                        session,
                        pluginManager
                )
        );
        getLog().debug("DONE org.apache.maven.plugins:maven-clean-plugin:2.5:clean");
    }

    protected final void install() throws MojoExecutionException, MojoFailureException {
        getLog().debug("START org.apache.maven.plugins:maven-install-plugin:2.5.1:install");
        MavenProject rootProject = MavenUtil.getRootProject(reactorProjects);
        session.setCurrentProject(rootProject);
        session.setProjects(reactorProjects);
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-install-plugin"),
                        version("2.5.1")
                ),
                goal("install"),
                configuration(
                        element(name("skip"), "false")
                ),
                executionEnvironment(
                        rootProject,
                        session,
                        pluginManager
                )
        );
        getLog().debug("DONE org.apache.maven.plugins:maven-install-plugin:2.5.1:install");
    }

    protected final void deploy() throws MojoExecutionException, MojoFailureException {
        getLog().debug("START org.apache.maven.plugins:maven-deploy-plugin:2.8.1:deploy");
        MavenProject rootProject = MavenUtil.getRootProject(reactorProjects);
        session.setCurrentProject(rootProject);
        session.setProjects(reactorProjects);
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-deploy-plugin"),
                        version("2.8.1")
                ),
                goal("deploy"),
                configuration(
                        element(name("skip"), "false"),
                        element(name("retryFailedDeploymentCount"), "1")
                ),
                executionEnvironment(
                        rootProject,
                        session,
                        pluginManager
                )
        );
        getLog().debug("DONE org.apache.maven.plugins:maven-deploy-plugin:2.8.1:deploy");
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
            getLog().warn("Removing build specifier " + buildSpecifierAsString + " from version " + version);
        }

        return result.toString();
    }

    protected final void checkForSnapshotDependencies() throws MojoExecutionException {
        getLog().info("Checking for SNAPSHOT dependencies");
        DependencyManagement dependencyManagement = getProject().getDependencyManagement();
        if (null != dependencyManagement) {
            checkForSnapshot(dependencyManagement.getDependencies());
        }
        checkForSnapshot(getProject().getDependencies());
    }

    private void checkForSnapshot(List<Dependency> dependencies) throws MojoExecutionException {
        Boolean hasSnapshotDependency = false;
        for (Dependency dependency : dependencies) {
            String version = dependency.getVersion();
            Matcher versionMatcher = matchSnapshotRegex.matcher(version);
            if (versionMatcher.find() && versionMatcher.end() == version.length()) {
                getLog().error("Found SNAPSHOT depednency " + dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + version);
            }
        }
        if (hasSnapshotDependency) {
            throw new MojoExecutionException("Cannot start release because SNAPSHOT dependencies exist");
        }
    }

    protected final void reloadReactorProjects() throws MojoExecutionException {
        getLog().debug("Reloading poms...");

        List<MavenProject> newReactorProjects;
        try {
            newReactorProjects = buildReactorProjects();
        } catch (ProjectBuildingException e) {
            getLog().error("Re-parse aborted due to malformed pom.xml file(s)", e);
            throw new MojoExecutionException("Re-parse aborted due to malformed pom.xml file(s)", e);
        } catch (CycleDetectedException e) {
            getLog().error("Re-parse aborted due to dependency cycle in project model", e);
            throw new MojoExecutionException("Re-parse aborted due to dependency cycle in project model", e);
        } catch (DuplicateProjectException e) {
            getLog().error("Re-parse aborted due to duplicate projects in project model", e);
            throw new MojoExecutionException("Re-parse aborted due to duplicate projects in project model", e);
        } catch (Exception e) {
            getLog().error("Re-parse aborted due a problem that prevented sorting the project model", e);
            throw new MojoExecutionException("Re-parse aborted due a problem that prevented sorting the project model", e);
        }
        MavenProject newProject = findProject(newReactorProjects, this.project);
        if (newProject == null) {
            throw new MojoExecutionException("A pom.xml change appears to have removed " + this.project.getId() + " from the build plan.");
        }

        this.project = newProject;
        this.reactorProjects = newReactorProjects;

        getLog().debug("Reloading poms complete...");
    }

    private List<MavenProject> buildReactorProjects() throws Exception {

        List<MavenProject> projects = new ArrayList<MavenProject>();
        for (MavenProject p : reactorProjects) {
            ProjectBuildingRequest request = new DefaultProjectBuildingRequest();

            request.setProcessPlugins(true);
            request.setProfiles(request.getProfiles());
            request.setActiveProfileIds(session.getRequest().getActiveProfiles());
            request.setInactiveProfileIds(session.getRequest().getInactiveProfiles());
            request.setRemoteRepositories(session.getRequest().getRemoteRepositories());
            request.setSystemProperties(session.getSystemProperties());
            request.setUserProperties(session.getUserProperties());
            request.setRemoteRepositories(session.getRequest().getRemoteRepositories());
            request.setPluginArtifactRepositories(session.getRequest().getPluginArtifactRepositories());
            request.setRepositorySession(session.getRepositorySession());
            request.setLocalRepository(localRepository);
            request.setBuildStartTime(session.getRequest().getStartTime());
            request.setResolveDependencies(true);
            request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_STRICT);
            projects.add(projectBuilder.build(p.getFile(), request).getProject());
        }
        return new ProjectSorter(projects).getSortedProjects();
    }

    private MavenProject findProject(List<MavenProject> newReactorProjects, MavenProject oldProject) {
        for (MavenProject newProject : newReactorProjects) {
            if (oldProject.getGroupId().equals(newProject.getGroupId())
                    && oldProject.getArtifactId().equals(newProject.getArtifactId())) {
                return newProject;
            }
        }
        return null;
    }

}
