package org.odk.collect.android.openrosa;

import androidx.annotation.NonNull;

import org.javarosa.xform.parse.SubmissionParser;
import org.javarosa.xform.parse.XFormParser;
import org.jetbrains.annotations.NotNull;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.odk.collect.android.exception.ParsingException;
import org.odk.collect.android.forms.FormListItem;
import org.odk.collect.android.forms.FormSource;
import org.odk.collect.android.forms.FormSourceException;
import org.odk.collect.android.forms.ManifestFile;
import org.odk.collect.android.forms.MediaFile;
import org.odk.collect.android.forms.SubmissionManifest;
import org.odk.collect.android.utilities.DocumentFetchResult;
import org.odk.collect.android.utilities.WebCredentialsUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.net.ssl.SSLException;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.odk.collect.android.forms.FormSourceException.Type.AUTH_REQUIRED;
import static org.odk.collect.android.forms.FormSourceException.Type.FETCH_ERROR;
import static org.odk.collect.android.forms.FormSourceException.Type.SECURITY_ERROR;
import static org.odk.collect.android.forms.FormSourceException.Type.UNREACHABLE;

public class OpenRosaFormSource implements FormSource {

    private static final String NAMESPACE_OPENROSA_ORG_XFORMS_XFORMS_LIST =
            "http://openrosa.org/xforms/xformsList";

    private static final String NAMESPACE_OPENROSA_ORG_XFORMS_XFORMS_MANIFEST =
            "http://openrosa.org/xforms/xformsManifest";

    private static final String NAMESPACE_OPENROSA_ORG_XFORMS =
            "http://openrosa.org/xforms";
    private static final String NAMESPACE_OPENDATAKIT_ORG_SUBMISSIONS = "http://opendatakit.org/submissions";

    private final OpenRosaXmlFetcher openRosaXMLFetcher;
    private String serverURL;
    private final String formListPath;
    private final String submissionListPath;
    private final String submissionPath;
    private WebCredentialsUtils webCredentialsUtils;

    public OpenRosaFormSource(String serverURL, String formListPath, String submissionListPath, String submissionPath, OpenRosaHttpInterface openRosaHttpInterface, WebCredentialsUtils webCredentialsUtils) {
        this.submissionPath = submissionPath;
        this.webCredentialsUtils = webCredentialsUtils;
        this.openRosaXMLFetcher = new OpenRosaXmlFetcher(openRosaHttpInterface, webCredentialsUtils);
        this.serverURL = serverURL;
        this.formListPath = formListPath;
        this.submissionListPath = submissionListPath;
    }

    @Override
    public List<String> fetchFormSubmissionIds(String formId) throws FormSourceException {
        if (serverURL == null) {
            throw new UnsupportedOperationException("Using deprecated constructor!");
        }

        DocumentFetchResult result = mapException(() -> openRosaXMLFetcher.getXML(getSubmissionListURL(formId)));

        // If we can't get the document, return the error, cancel the task
        if (result.errorMessage != null) {
            if (result.responseCode == HTTP_UNAUTHORIZED) {
                throw new FormSourceException(AUTH_REQUIRED);
            } else if (result.responseCode == HTTP_NOT_FOUND) {
                throw new FormSourceException(UNREACHABLE, serverURL);
            } else {
                throw new FormSourceException(FETCH_ERROR);
            }
        }

        return parseSubmissionIds(result);
    }

    @Override
    public List<FormListItem> fetchFormList() throws FormSourceException {
        if (serverURL == null) {
            throw new UnsupportedOperationException("Using deprecated constructor!");
        }

        DocumentFetchResult result = mapException(() -> openRosaXMLFetcher.getXML(getFormListURL()));

        // If we can't get the document, return the error, cancel the task
        if (result.errorMessage != null) {
            if (result.responseCode == HTTP_UNAUTHORIZED) {
                throw new FormSourceException(AUTH_REQUIRED);
            } else if (result.responseCode == HTTP_NOT_FOUND) {
                throw new FormSourceException(UNREACHABLE, serverURL);
            } else {
                throw new FormSourceException(FETCH_ERROR);
            }
        }

        if (result.isOpenRosaResponse) {
            return parseFormList(result);
        } else {
            return parseLegacyFormList(result);
        }
    }

    @Override
    public ManifestFile fetchManifest(String manifestURL) throws FormSourceException {
        if (manifestURL == null) {
            return null;
        }

        DocumentFetchResult result = mapException(() -> openRosaXMLFetcher.getXML(manifestURL));

        if (result.errorMessage != null) {
            throw new FormSourceException(FETCH_ERROR);
        }

        if (!result.isOpenRosaResponse) {
            throw new FormSourceException(FETCH_ERROR);
        }

        return parseManifest(result);
    }

    @Override
    public InputStream fetchForm(String formURL) throws FormSourceException {
        return fetchFile(formURL);
    }

    @Override
    public SubmissionManifest fetchData(String formId, String formVersion, String submissionId) throws FormSourceException, ParsingException {
        if (serverURL == null) {
            throw new UnsupportedOperationException("Using deprecated constructor!");
        }

        DocumentFetchResult result = mapException(() -> openRosaXMLFetcher.getXML(getDownloadSubmissionURL(formId, formVersion, submissionId)));

        // If we can't get the document, return the error, cancel the task
        if (result.errorMessage != null) {
            if (result.responseCode == HTTP_UNAUTHORIZED) {
                throw new FormSourceException(AUTH_REQUIRED);
            } else if (result.responseCode == HTTP_NOT_FOUND) {
                throw new FormSourceException(UNREACHABLE, serverURL);
            } else {
                throw new FormSourceException(FETCH_ERROR);
            }
        }

        return parseSubmissionResponse(result);
    }

    @Override
    public InputStream fetchMediaFile(String mediaFileURL) throws FormSourceException {
        return mapException(() -> openRosaXMLFetcher.getFile(mediaFileURL, null));
    }

    @Override
    public void updateUrl(String url) {
        this.serverURL = url;
    }

    @Override
    public void updateWebCredentialsUtils(WebCredentialsUtils webCredentialsUtils) {
        this.openRosaXMLFetcher.updateWebCredentialsUtils(webCredentialsUtils);
    }

    @NotNull
    private InputStream fetchFile(String formURL) throws FormSourceException {
        InputStream formFile = mapException(() -> openRosaXMLFetcher.getFile(formURL, null));

        if (formFile != null) {
            return formFile;
        } else {
            throw new FormSourceException(FETCH_ERROR);
        }
    }

    private List<String> parseSubmissionIds(DocumentFetchResult result) throws FormSourceException {
        List<String> submissionIdList = new ArrayList<>();

        Element xformsElement = result.doc.getRootElement();
        Element idListElement = xformsElement.getElement(0);
        if(idListElement.getName().equals("idList")) {
            int idListCount = idListElement.getChildCount();
            for (int j = 0; j < idListCount; j+=2) {
                Element element = idListElement.getElement(j);
                submissionIdList.add((String) element.getChild(0));
            }
        }

        return submissionIdList;
    }

    private SubmissionManifest parseSubmissionResponse(DocumentFetchResult result) throws ParsingException {
        Document doc = result.doc;
        // and parse the document...
        List<MediaFile> attachmentList = new ArrayList();
        Document rootDoc = new Document();
        Element rootSubmissionElement = null;
        String instanceID = null;
        String instanceName = null;

        // Attempt parsing
        Element submissionElement = doc.getRootElement();
        if (!submissionElement.getName().equals("submission")) {
            String msg = "Parsing downloadSubmission reply -- root element is not <submission> :"
                    + submissionElement.getName();
            throw new ParsingException(msg);
        }
        String namespace = submissionElement.getNamespace();
        if (!namespace.equalsIgnoreCase(NAMESPACE_OPENDATAKIT_ORG_SUBMISSIONS)) {
            String msg = "Parsing downloadSubmission reply -- root element namespace is incorrect:"
                    + namespace;
            throw new ParsingException(msg);
        }
        int nElements = submissionElement.getChildCount();
        for (int i = 0; i < nElements; ++i) {
            if (submissionElement.getType(i) != Element.ELEMENT) {
                // e.g., whitespace (text)
                continue;
            }
            Element subElement = (Element) submissionElement.getElement(i);
            namespace = subElement.getNamespace();
            if (!namespace.equalsIgnoreCase(NAMESPACE_OPENDATAKIT_ORG_SUBMISSIONS)) {
                // someone else's extension?
                continue;
            }
            String name = subElement.getName();
            if (name.equalsIgnoreCase("data")) {
                // find the root submission element and get its instanceID attribute
                int nIdElements = subElement.getChildCount();
                for (int j = 0; j < nIdElements; ++j) {
                    if (subElement.getType(j) != Element.ELEMENT) {
                        // e.g., whitespace (text)
                        continue;
                    }
                    rootSubmissionElement = (Element) subElement.getElement(j);
                    break;
                }
                if (rootSubmissionElement == null) {
                    throw new ParsingException("no submission body found in submissionDownload response");
                }

                instanceID = rootSubmissionElement.getAttributeValue(null, "instanceID");
                if (instanceID == null) {
                    throw new ParsingException("instanceID attribute value is null");
                }
            } else if (name.equalsIgnoreCase("mediaFile")) {
                int nIdElements = subElement.getChildCount();
                String filename = null;
                String hash = null;
                String downloadUrl = null;
                for (int j = 0; j < nIdElements; ++j) {
                    if (subElement.getType(j) != Element.ELEMENT) {
                        // e.g., whitespace (text)
                        continue;
                    }
                    Element mediaSubElement = (Element) subElement.getElement(j);
                    name = mediaSubElement.getName();
                    if (name.equalsIgnoreCase("filename")) {
                        filename = XFormParser.getXMLText(mediaSubElement, true);
                    } else if (name.equalsIgnoreCase("hash")) {
                        hash = XFormParser.getXMLText(mediaSubElement, true);
                    } else if (name.equalsIgnoreCase("downloadUrl")) {
                        downloadUrl = XFormParser.getXMLText(mediaSubElement, true);
                    }
                }
                attachmentList.add(new MediaFile(filename, hash, downloadUrl));
            } else {
            }
        }

        if (rootSubmissionElement == null) {
            throw new ParsingException("No submission body found");
        }
        if (instanceID == null) {
            throw new ParsingException("instanceID attribute value is null");
        }

        // write submission to a string
        StringWriter fo = new StringWriter();
        KXmlSerializer serializer = new KXmlSerializer();

        serializer.setOutput(fo);
        // setting the response content type emits the xml header.
        // just write the body here...
        // this has the xmlns of the submissions download, indicating that it
        // originated from a briefcase download. Might be useful for discriminating
        // real vs. recovered data?
        rootSubmissionElement.setPrefix(null, NAMESPACE_OPENDATAKIT_ORG_SUBMISSIONS);
        Object child = rootSubmissionElement.getChild(rootSubmissionElement.getChildCount() - 1);
        if(child instanceof Element) {
            Element meta = (Element) child;
            if(meta.getNamespace().equals(NAMESPACE_OPENROSA_ORG_XFORMS_XFORMS_MANIFEST)) {
                meta.setPrefix(null, NAMESPACE_OPENROSA_ORG_XFORMS);
            }
            Element element = meta.getElement(null, "instanceName");
            if(element!=null && element.getChildCount()>0)
                instanceName = element.getText(0);
        }
        rootDoc.addChild(Node.ELEMENT, rootSubmissionElement);
        try {
            rootDoc.write(serializer);
            serializer.flush();
            serializer.endDocument();
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new ParsingException("Unexpected IOException: " + e.getMessage());
        }

        return new SubmissionManifest(instanceID, fo.toString(), attachmentList, instanceName);
    }

    private List<FormListItem> parseFormList(DocumentFetchResult result) throws FormSourceException {
        // Attempt OpenRosa 1.0 parsing
        Element xformsElement = result.doc.getRootElement();
        if (!xformsElement.getName().equals("xforms")) {
            throw new FormSourceException(FETCH_ERROR);
        }
        if (!isXformsListNamespacedElement(xformsElement)) {
            throw new FormSourceException(FETCH_ERROR);
        }

        List<FormListItem> formList = new ArrayList<>();

        int elements = xformsElement.getChildCount();
        for (int i = 0; i < elements; ++i) {
            if (xformsElement.getType(i) != Element.ELEMENT) {
                // e.g., whitespace (text)
                continue;
            }
            Element xformElement = xformsElement.getElement(i);
            if (!isXformsListNamespacedElement(xformElement)) {
                // someone else's extension?
                continue;
            }
            String name = xformElement.getName();
            if (!name.equalsIgnoreCase("xform")) {
                // someone else's extension?
                continue;
            }

            // this is something we know how to interpret
            String formId = null;
            String formName = null;
            String version = null;
            String majorMinorVersion = null;
            String description = null;
            String downloadUrl = null;
            String manifestUrl = null;
            String hash = null;
            // don't process descriptionUrl
            int fieldCount = xformElement.getChildCount();
            for (int j = 0; j < fieldCount; ++j) {
                if (xformElement.getType(j) != Element.ELEMENT) {
                    // whitespace
                    continue;
                }
                Element child = xformElement.getElement(j);
                if (!isXformsListNamespacedElement(child)) {
                    // someone else's extension?
                    continue;
                }
                String tag = child.getName();
                switch (tag) {
                    case "formID":
                        formId = XFormParser.getXMLText(child, true);
                        if (formId != null && formId.length() == 0) {
                            formId = null;
                        }
                        break;
                    case "name":
                        formName = XFormParser.getXMLText(child, true);
                        if (formName != null && formName.length() == 0) {
                            formName = null;
                        }
                        break;
                    case "version":
                        version = XFormParser.getXMLText(child, true);
                        if (version != null && version.trim().isEmpty()) {
                            version = null;
                        }
                        break;
                    case "majorMinorVersion":
                        majorMinorVersion = XFormParser.getXMLText(child, true);
                        if (majorMinorVersion != null && majorMinorVersion.length() == 0) {
                            majorMinorVersion = null;
                        }
                        break;
                    case "descriptionText":
                        description = XFormParser.getXMLText(child, true);
                        if (description != null && description.length() == 0) {
                            description = null;
                        }
                        break;
                    case "downloadUrl":
                        downloadUrl = XFormParser.getXMLText(child, true);
                        if (downloadUrl != null && downloadUrl.length() == 0) {
                            downloadUrl = null;
                        }
                        break;
                    case "manifestUrl":
                        manifestUrl = XFormParser.getXMLText(child, true);
                        if (manifestUrl != null && manifestUrl.length() == 0) {
                            manifestUrl = null;
                        }
                        break;
                    case "hash":
                        hash = XFormParser.getXMLText(child, true);
                        if (hash != null && hash.length() == 0) {
                            hash = null;
                        }
                        break;
                }
            }

            if (formId == null || downloadUrl == null || formName == null) {
                formList.clear();
                throw new FormSourceException(FETCH_ERROR);
            }

            FormListItem formListItem = new FormListItem(downloadUrl, formId, version, hash, formName, manifestUrl);
            formList.add(formListItem);
        }

        return formList;
    }

    private List<FormListItem> parseLegacyFormList(DocumentFetchResult result) throws FormSourceException {
        // Aggregate 0.9.x mode...
        // populate HashMap with form names and urls
        Element formsElement = result.doc.getRootElement();
        int formsCount = formsElement.getChildCount();
        String formId = null;

        List<FormListItem> formList = new ArrayList<>();

        for (int i = 0; i < formsCount; ++i) {
            if (formsElement.getType(i) != Element.ELEMENT) {
                // whitespace
                continue;
            }
            Element child = formsElement.getElement(i);
            String tag = child.getName();
            if (tag.equals("formID")) {
                formId = XFormParser.getXMLText(child, true);
                if (formId != null && formId.length() == 0) {
                    formId = null;
                }
            }
            if (tag.equalsIgnoreCase("form")) {
                String formName = XFormParser.getXMLText(child, true);
                if (formName != null && formName.length() == 0) {
                    formName = null;
                }
                String downloadUrl = child.getAttributeValue(null, "url");
                downloadUrl = downloadUrl.trim();
                if (downloadUrl.length() == 0) {
                    downloadUrl = null;
                }
                if (formName == null) {
                    formList.clear();
                    throw new FormSourceException(FETCH_ERROR);
                }

                formList.add(new FormListItem(downloadUrl, formId, null, null, formName, null));
                formId = null;
            }
        }

        return formList;
    }

    private ManifestFile parseManifest(DocumentFetchResult result) throws FormSourceException {
        // Attempt OpenRosa 1.0 parsing
        Element manifestElement = result.doc.getRootElement();

        if (!manifestElement.getName().equals("manifest")) {
            throw new FormSourceException(FETCH_ERROR);
        }

        if (!isXformsManifestNamespacedElement(manifestElement)) {
            throw new FormSourceException(FETCH_ERROR);
        }

        int elements = manifestElement.getChildCount();
        List<MediaFile> files = new ArrayList<>();
        for (int i = 0; i < elements; ++i) {
            if (manifestElement.getType(i) != Element.ELEMENT) {
                // e.g., whitespace (text)
                continue;
            }
            Element mediaFileElement = manifestElement.getElement(i);
            if (!isXformsManifestNamespacedElement(mediaFileElement)) {
                // someone else's extension?
                continue;
            }
            String name = mediaFileElement.getName();
            if (name.equalsIgnoreCase("mediaFile")) {
                String filename = null;
                String hash = null;
                String downloadUrl = null;
                // don't process descriptionUrl
                int childCount = mediaFileElement.getChildCount();
                for (int j = 0; j < childCount; ++j) {
                    if (mediaFileElement.getType(j) != Element.ELEMENT) {
                        // e.g., whitespace (text)
                        continue;
                    }
                    Element child = mediaFileElement.getElement(j);
                    if (!isXformsManifestNamespacedElement(child)) {
                        // someone else's extension?
                        continue;
                    }
                    String tag = child.getName();
                    switch (tag) {
                        case "filename":
                            filename = XFormParser.getXMLText(child, true);
                            if (filename != null && filename.length() == 0) {
                                filename = null;
                            }
                            break;
                        case "hash":
                            hash = XFormParser.getXMLText(child, true);
                            if (hash != null && hash.length() == 0) {
                                hash = null;
                            }
                            break;
                        case "downloadUrl":
                            downloadUrl = XFormParser.getXMLText(child, true);
                            if (downloadUrl != null && downloadUrl.length() == 0) {
                                downloadUrl = null;
                            }
                            break;
                    }
                }

                if (filename == null || downloadUrl == null || hash == null) {
                    throw new FormSourceException(FETCH_ERROR);
                }

                files.add(new MediaFile(filename, hash, downloadUrl));
            }
        }

        return new ManifestFile(result.getHash(), files);
    }

    private <T> T mapException(Callable<T> callable) throws FormSourceException {
        try {
            return callable.call();
        } catch (UnknownHostException e) {
            throw new FormSourceException(UNREACHABLE, serverURL);
        } catch (SSLException e) {
            throw new FormSourceException(SECURITY_ERROR, serverURL);
        } catch (Exception e) {
            throw new FormSourceException(FETCH_ERROR, serverURL);
        }
    }

    @NotNull
    private String getFormListURL() {
        String downloadListUrl = serverURL;

        while (downloadListUrl.endsWith("/")) {
            downloadListUrl = downloadListUrl.substring(0, downloadListUrl.length() - 1);
        }

        downloadListUrl += formListPath;
        return downloadListUrl;
    }

    @NotNull
    private String getSubmissionListURL(String formId) {
        String downloadListUrl = serverURL;

        while (downloadListUrl.endsWith("/")) {
            downloadListUrl = downloadListUrl.substring(0, downloadListUrl.length() - 1);
        }

        downloadListUrl += submissionListPath
                + "?formId=" + formId
                + "&dataAssignee=" + webCredentialsUtils.getUserNameFromPreferences()
                + "&notCompletedOnly=true";
        return downloadListUrl;
    }

    @NonNull
    private String getDownloadSubmissionURL(String formId, String formVersion, String submissionId) throws UnsupportedEncodingException {
        String downloadListUrl = serverURL;

        while (downloadListUrl.endsWith("/")) {
            downloadListUrl = downloadListUrl.substring(0, downloadListUrl.length() - 1);
        }

        downloadListUrl += submissionPath
                + "?formId="
                + URLEncoder.encode(formId + "[@version=Optional[" + formVersion
                + "] and @uiVersion=null]/data[@key=" + submissionId + "]",
                StandardCharsets.UTF_8.toString());
        return downloadListUrl;
    }

    private static boolean isXformsListNamespacedElement(Element e) {
        return e.getNamespace().equalsIgnoreCase(NAMESPACE_OPENROSA_ORG_XFORMS_XFORMS_LIST);
    }

    private static boolean isXformsManifestNamespacedElement(Element e) {
        return e.getNamespace().equalsIgnoreCase(NAMESPACE_OPENROSA_ORG_XFORMS_XFORMS_MANIFEST);
    }
}
