package org.jfrog.hudson.generic;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloader;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Primary implementation of dependencies downloader,
 * handles Jenkins slaves and re-use a client for HTTP communication.
 *
 * @author Shay Yaakov
 */
public class DependenciesDownloaderImpl implements DependenciesDownloader {

    private ArtifactoryDependenciesClient client;
    private FilePath workspace;
    private Log log;
    private boolean flatDownload = false;

    public DependenciesDownloaderImpl(ArtifactoryDependenciesClient client, FilePath workspace, Log log) {
        this.client = client;
        this.workspace = workspace;
        this.log = log;
    }

    public ArtifactoryDependenciesClient getClient() {
        return client;
    }

    public List<Dependency> download(Set<DownloadableArtifact> downloadableArtifacts) throws IOException {
        DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(this, log);
        return helper.downloadDependencies(downloadableArtifacts);
    }

    public String getTargetDir(String targetDir, String relativeDir) throws IOException {
        try {
            String downloadFileRelativePath = this.flatDownload && relativeDir.contains("/") ?
                    StringUtils.substringAfterLast(relativeDir, "/") : relativeDir;
            FilePath targetDirFile = new FilePath(workspace, targetDir).child(downloadFileRelativePath);
            return targetDirFile.absolutize().getRemote();
        } catch (InterruptedException e) {
            log.warn("Caught interrupted exception: " + e.getLocalizedMessage());
        }

        return null;
    }

    public Map<String, String> saveDownloadedFile(InputStream is, String filePath) throws IOException {
        try {
            FilePath child = workspace.child(filePath);
            child.copyFrom(is);
            return child.act(new DownloadFileCallable(log));
        } catch (InterruptedException e) {
            log.warn("Caught interrupted exception: " + e.getLocalizedMessage());
        }

        return null;
    }

    public boolean isFileExistsLocally(String filePath, String md5, String sha1) throws IOException {
        try {
            FilePath child = workspace.child(filePath);
            if (!child.exists()) {
                return false;
            }

            if (child.isDirectory()) {
                return false;
            }

            Map<String, String> checksumsMap = child.act(new DownloadFileCallable(log));
            boolean isExists = checksumsMap != null &&
                    StringUtils.isNotBlank(md5) && StringUtils.equals(md5, checksumsMap.get("md5")) &&
                    StringUtils.isNotBlank(sha1) && StringUtils.equals(sha1, checksumsMap.get("sha1"));
            if (isExists) {
                return true;
            } else {
                log.info(String.format("Overriding existing in destination file: %s", child));
                return false;
            }
        } catch (InterruptedException e) {
            log.warn("Caught interrupted exception: " + e.getLocalizedMessage());
        }

        return false;
    }

    public void removeUnusedArtifactsFromLocal(Set<String> allResolvesFiles, Set<String> forDeletionFiles)
            throws IOException {
        try {
            for (String resolvedFile : forDeletionFiles) {
                FilePath resolvedFileParent = workspace.child(resolvedFile).getParent();
                if (!resolvedFileParent.exists()) {
                    continue;
                }

                List<FilePath> fileSiblings = resolvedFileParent.list();
                if (fileSiblings == null || fileSiblings.isEmpty()) {
                    continue;
                }

                for (FilePath sibling : fileSiblings) {
                    String siblingPath = sibling.absolutize().getRemote();
                    if (!isResolvedOrParentOfResolvedFile(allResolvesFiles, siblingPath)) {
                        sibling.deleteRecursive();
                        log.info("Deleted unresolved file '" + siblingPath + "'");
                    }
                }
            }
        } catch (InterruptedException e) {
            log.warn("Caught interrupted exception: " + e.getLocalizedMessage());
        }
    }

    public void setFlatDownload(boolean flat) {
        this.flatDownload = flat;
    }

    private boolean isResolvedOrParentOfResolvedFile(Set<String> resolvedFiles, final String path) {
        return Iterables.any(resolvedFiles, new Predicate<String>() {
            public boolean apply(String filePath) {
                return StringUtils.equals(filePath, path) || StringUtils.startsWith(filePath, path);
            }
        });
    }

    private static class DownloadFileCallable extends MasterToSlaveFileCallable<Map<String, String>> {
        private Log log;

        public DownloadFileCallable(Log log) {
            this.log = log;
        }

        public Map<String, String> invoke(File f, VirtualChannel channel) throws IOException {
            try {
                return FileChecksumCalculator.calculateChecksums(f, "md5", "sha1");
            } catch (NoSuchAlgorithmException e) {
                log.warn("Could not find checksum algorithm: " + e.getLocalizedMessage());
            }

            return null;
        }
    }
}
