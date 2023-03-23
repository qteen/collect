package org.odk.collect.android.forms;

import java.util.List;

public class SubmissionManifest {
    final List<MediaFile> attachmentList;
    final String submissionXml;
    final String instanceID;
    final String instanceName;

    public SubmissionManifest(String instanceID, String submissionXml, List<MediaFile> attachmentList, String instanceName) {
        this.instanceID = instanceID;
        this.submissionXml = submissionXml;
        this.attachmentList = attachmentList;
        this.instanceName = instanceName;
    }

    public String getInstanceID() {
        return instanceID;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getSubmissionXml() {
        return submissionXml;
    }
}