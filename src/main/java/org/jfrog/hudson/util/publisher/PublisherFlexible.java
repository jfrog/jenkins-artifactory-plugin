package org.jfrog.hudson.util.publisher;

import hudson.model.AbstractProject;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.jenkins_ci.plugins.flexible_publish.ConditionalPublisher;
import org.jenkins_ci.plugins.flexible_publish.FlexiblePublisher;

import java.util.List;

/**
 * The class is used to find publishers which are wrapped by {@link org.jenkins_ci.plugins.flexible_publish.FlexiblePublisher}
 *
 * @author Lior Hasson
 */
public class PublisherFlexible<T extends Publisher> implements PublisherFind<T> {

    public static String FLEXIBLE_PUBLISH_PLUGIN = "flexible-publish";

    /**
     * Gets the publisher wrapped by the specofoed FlexiblePublisher.
     * @param publisher                     The FlexiblePublisher wrapping the publisher.
     * @param type                          The type of the publisher wrapped by the FlexiblePublisher.
     * @return                              The publisher object wrapped by the FlexiblePublisher.
     *                                      Null is returned if the FlexiblePublisher does not wrap a publisher of the specified type.
     * @throws IllegalArgumentException     In case publisher is not of type
     *                                      {@link org.jenkins_ci.plugins.flexible_publish.FlexiblePublisher}
     */
    private T getWrappedPublisher(Publisher flexiblePublisher, Class<T> type) {
        if (!(flexiblePublisher instanceof FlexiblePublisher)) {
            throw new IllegalArgumentException(String.format("Publisher should be of type: '%s'. Found type: '%s'",
                FlexiblePublisher.class, flexiblePublisher.getClass()));
        }

        List<ConditionalPublisher> conditions = ((FlexiblePublisher) flexiblePublisher).getPublishers();
        for (ConditionalPublisher condition : conditions) {
            if (type.isInstance(condition.getPublisherList().get(0))) {
                return type.cast(condition.getPublisherList().get(0));
            }
        }

        return null;
    }

    /**
     * Gets the publisher of the specified type, if it is wrapped by the "Flexible Publish" publisher in a project.
     * Null is returned if no such publisher is found.
     * @param project   The project
     * @param type      The type of the publisher
     */
    public T find(AbstractProject<?, ?> project, Class<T> type) {
        // First check that the Flexible Publish plugin is installed:
        if (Jenkins.get().getPlugin(FLEXIBLE_PUBLISH_PLUGIN) != null) {
            // Iterate all the project's publishers and find the flexible publisher:
            for (Publisher publisher : project.getPublishersList()) {
                // Found the flexible publisher:
                if (publisher instanceof FlexiblePublisher) {
                    // See if it wraps a publisher of the specified type and if it does, return it:
                    T pub = getWrappedPublisher(publisher, type);
                    if (pub != null) {
                        return pub;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Determines whether a project has the specified publisher type, wrapped by the "Flexible Publish" publisher.
     * @param project   The project
     * @param type      The type of the publisher
     * @return          true if the project contains a publisher of the specified type wrapped by the "Flexible Publish" publisher.
     */
    public boolean isPublisherWrapped(AbstractProject<?, ?> project, Class<T> type) {
        return find(project, type) != null;
    }
}
