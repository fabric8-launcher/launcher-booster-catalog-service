/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster.catalog;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public enum DeploymentType {
    /**
     * Deploy in Openshift
     */
    CD("Continuous delivery"),
    /**
     * Deploy as a ZIP file
     */
    ZIP("ZIP File"),
    /**
     * Deploy on Openshift.io (not used in launch)
     */
    OSIO("Openshift.io");

    private DeploymentType(String description) {
        this.description = description;
    }

    private String description;

    public String getDescription() {
        return description;
    }
}
