
package edu.sdsc.nbcr.opal.types;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the edu.sdsc.nbcr.opal.types package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _QueryStatusInput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "queryStatusInput");
    private final static QName _LaunchJobInput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "launchJobInput");
    private final static QName _OpalFaultOutput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "opalFaultOutput");
    private final static QName _GetOutputsInput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "getOutputsInput");
    private final static QName _DestroyOutput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "destroyOutput");
    private final static QName _LaunchJobBlockingInput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "launchJobBlockingInput");
    private final static QName _LaunchJobBlockingOutput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "launchJobBlockingOutput");
    private final static QName _GetOutputAsBase64ByNameInput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "getOutputAsBase64ByNameInput");
    private final static QName _LaunchJobOutput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "launchJobOutput");
    private final static QName _DestroyInput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "destroyInput");
    private final static QName _GetAppConfigOutput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "getAppConfigOutput");
    private final static QName _QueryStatusOutput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "queryStatusOutput");
    private final static QName _GetAppConfigInput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "getAppConfigInput");
    private final static QName _GetAppMetadataOutput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "getAppMetadataOutput");
    private final static QName _GetAppMetadataInput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "getAppMetadataInput");
    private final static QName _GetOutputsOutput_QNAME = new QName("http://nbcr.sdsc.edu/opal/types", "getOutputsOutput");
    private final static QName _ParamsTypeDefault_QNAME = new QName("", "default");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: edu.sdsc.nbcr.opal.types
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GroupsArrayType }
     * 
     */
    public GroupsArrayType createGroupsArrayType() {
        return new GroupsArrayType();
    }

    /**
     * Create an instance of {@link JobOutputType }
     * 
     */
    public JobOutputType createJobOutputType() {
        return new JobOutputType();
    }

    /**
     * Create an instance of {@link ImplicitParamsArrayType }
     * 
     */
    public ImplicitParamsArrayType createImplicitParamsArrayType() {
        return new ImplicitParamsArrayType();
    }

    /**
     * Create an instance of {@link InputFileType }
     * 
     */
    public InputFileType createInputFileType() {
        return new InputFileType();
    }

    /**
     * Create an instance of {@link ParamsType }
     * 
     */
    public ParamsType createParamsType() {
        return new ParamsType();
    }

    /**
     * Create an instance of {@link FaultType }
     * 
     */
    public FaultType createFaultType() {
        return new FaultType();
    }

    /**
     * Create an instance of {@link FlagsType }
     * 
     */
    public FlagsType createFlagsType() {
        return new FlagsType();
    }

    /**
     * Create an instance of {@link ParamsArrayType }
     * 
     */
    public ParamsArrayType createParamsArrayType() {
        return new ParamsArrayType();
    }

    /**
     * Create an instance of {@link AppMetadataType }
     * 
     */
    public AppMetadataType createAppMetadataType() {
        return new AppMetadataType();
    }

    /**
     * Create an instance of {@link JobInputType }
     * 
     */
    public JobInputType createJobInputType() {
        return new JobInputType();
    }

    /**
     * Create an instance of {@link StatusOutputType }
     * 
     */
    public StatusOutputType createStatusOutputType() {
        return new StatusOutputType();
    }

    /**
     * Create an instance of {@link GroupsType }
     * 
     */
    public GroupsType createGroupsType() {
        return new GroupsType();
    }

    /**
     * Create an instance of {@link GetOutputAsBase64ByNameOutput }
     * 
     */
    public GetOutputAsBase64ByNameOutput createGetOutputAsBase64ByNameOutput() {
        return new GetOutputAsBase64ByNameOutput();
    }

    /**
     * Create an instance of {@link AppConfigInputType }
     * 
     */
    public AppConfigInputType createAppConfigInputType() {
        return new AppConfigInputType();
    }

    /**
     * Create an instance of {@link OutputFileType }
     * 
     */
    public OutputFileType createOutputFileType() {
        return new OutputFileType();
    }

    /**
     * Create an instance of {@link BlockingOutputType }
     * 
     */
    public BlockingOutputType createBlockingOutputType() {
        return new BlockingOutputType();
    }

    /**
     * Create an instance of {@link OutputsByNameInputType }
     * 
     */
    public OutputsByNameInputType createOutputsByNameInputType() {
        return new OutputsByNameInputType();
    }

    /**
     * Create an instance of {@link FlagsArrayType }
     * 
     */
    public FlagsArrayType createFlagsArrayType() {
        return new FlagsArrayType();
    }

    /**
     * Create an instance of {@link AppMetadataInputType }
     * 
     */
    public AppMetadataInputType createAppMetadataInputType() {
        return new AppMetadataInputType();
    }

    /**
     * Create an instance of {@link AppConfigType }
     * 
     */
    public AppConfigType createAppConfigType() {
        return new AppConfigType();
    }

    /**
     * Create an instance of {@link ImplicitParamsType }
     * 
     */
    public ImplicitParamsType createImplicitParamsType() {
        return new ImplicitParamsType();
    }

    /**
     * Create an instance of {@link JobSubOutputType }
     * 
     */
    public JobSubOutputType createJobSubOutputType() {
        return new JobSubOutputType();
    }

    /**
     * Create an instance of {@link ArgumentsType }
     * 
     */
    public ArgumentsType createArgumentsType() {
        return new ArgumentsType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "queryStatusInput")
    public JAXBElement<String> createQueryStatusInput(String value) {
        return new JAXBElement<String>(_QueryStatusInput_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JobInputType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "launchJobInput")
    public JAXBElement<JobInputType> createLaunchJobInput(JobInputType value) {
        return new JAXBElement<JobInputType>(_LaunchJobInput_QNAME, JobInputType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FaultType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "opalFaultOutput")
    public JAXBElement<FaultType> createOpalFaultOutput(FaultType value) {
        return new JAXBElement<FaultType>(_OpalFaultOutput_QNAME, FaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "getOutputsInput")
    public JAXBElement<String> createGetOutputsInput(String value) {
        return new JAXBElement<String>(_GetOutputsInput_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StatusOutputType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "destroyOutput")
    public JAXBElement<StatusOutputType> createDestroyOutput(StatusOutputType value) {
        return new JAXBElement<StatusOutputType>(_DestroyOutput_QNAME, StatusOutputType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JobInputType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "launchJobBlockingInput")
    public JAXBElement<JobInputType> createLaunchJobBlockingInput(JobInputType value) {
        return new JAXBElement<JobInputType>(_LaunchJobBlockingInput_QNAME, JobInputType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BlockingOutputType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "launchJobBlockingOutput")
    public JAXBElement<BlockingOutputType> createLaunchJobBlockingOutput(BlockingOutputType value) {
        return new JAXBElement<BlockingOutputType>(_LaunchJobBlockingOutput_QNAME, BlockingOutputType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link OutputsByNameInputType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "getOutputAsBase64ByNameInput")
    public JAXBElement<OutputsByNameInputType> createGetOutputAsBase64ByNameInput(OutputsByNameInputType value) {
        return new JAXBElement<OutputsByNameInputType>(_GetOutputAsBase64ByNameInput_QNAME, OutputsByNameInputType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JobSubOutputType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "launchJobOutput")
    public JAXBElement<JobSubOutputType> createLaunchJobOutput(JobSubOutputType value) {
        return new JAXBElement<JobSubOutputType>(_LaunchJobOutput_QNAME, JobSubOutputType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "destroyInput")
    public JAXBElement<String> createDestroyInput(String value) {
        return new JAXBElement<String>(_DestroyInput_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AppConfigType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "getAppConfigOutput")
    public JAXBElement<AppConfigType> createGetAppConfigOutput(AppConfigType value) {
        return new JAXBElement<AppConfigType>(_GetAppConfigOutput_QNAME, AppConfigType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StatusOutputType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "queryStatusOutput")
    public JAXBElement<StatusOutputType> createQueryStatusOutput(StatusOutputType value) {
        return new JAXBElement<StatusOutputType>(_QueryStatusOutput_QNAME, StatusOutputType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AppConfigInputType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "getAppConfigInput")
    public JAXBElement<AppConfigInputType> createGetAppConfigInput(AppConfigInputType value) {
        return new JAXBElement<AppConfigInputType>(_GetAppConfigInput_QNAME, AppConfigInputType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AppMetadataType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "getAppMetadataOutput")
    public JAXBElement<AppMetadataType> createGetAppMetadataOutput(AppMetadataType value) {
        return new JAXBElement<AppMetadataType>(_GetAppMetadataOutput_QNAME, AppMetadataType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AppMetadataInputType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "getAppMetadataInput")
    public JAXBElement<AppMetadataInputType> createGetAppMetadataInput(AppMetadataInputType value) {
        return new JAXBElement<AppMetadataInputType>(_GetAppMetadataInput_QNAME, AppMetadataInputType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JobOutputType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nbcr.sdsc.edu/opal/types", name = "getOutputsOutput")
    public JAXBElement<JobOutputType> createGetOutputsOutput(JobOutputType value) {
        return new JAXBElement<JobOutputType>(_GetOutputsOutput_QNAME, JobOutputType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "default", scope = ParamsType.class)
    public JAXBElement<String> createParamsTypeDefault(String value) {
        return new JAXBElement<String>(_ParamsTypeDefault_QNAME, String.class, ParamsType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "default", scope = FlagsType.class)
    public JAXBElement<Boolean> createFlagsTypeDefault(Boolean value) {
        return new JAXBElement<Boolean>(_ParamsTypeDefault_QNAME, Boolean.class, FlagsType.class, value);
    }

}
